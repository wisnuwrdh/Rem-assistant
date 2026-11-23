package com.ecosystemku.assistant.data.audio

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.ecosystemku.assistant.BuildConfig
import com.ecosystemku.assistant.data.local.AiConfigField
import com.ecosystemku.assistant.data.local.ApiKeyStore
import com.ecosystemku.assistant.data.network.ChatTurnDto
import com.ecosystemku.assistant.data.network.DirectLlmClient
import com.ecosystemku.assistant.data.network.SseTagParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

@Serializable
private data class FishAudioTtsRequestDto(
    val text: String,
    val reference_id: String,
    val format: String = "mp3"
)

@OptIn(UnstableApi::class)
class TtsAudioManager(
    context: Context,
    private val httpClient: OkHttpClient,
    @Suppress("unused") private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }
    private val playbackGen = AtomicInteger(0)
    private var activeFetchJob: Job? = null
    private var activeBundledClip: String? = null
    private var fallbackPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _mouthLevel = MutableStateFlow(0f)
    val mouthLevel: StateFlow<Float> = _mouthLevel.asStateFlow()

    private val rmsSink = object : TeeAudioProcessor.AudioBufferSink {
        @Volatile
        private var pcmEncoding: Int = C.ENCODING_PCM_16BIT
        private var smoothedLevel: Double = 0.0

        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            pcmEncoding = encoding
            smoothedLevel = 0.0
            _mouthLevel.value = 0f
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            val dup = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val isFloat = pcmEncoding == C.ENCODING_PCM_FLOAT
            val bytesPerSample = if (isFloat) 4 else 2
            val sampleCount = dup.remaining() / bytesPerSample
            if (sampleCount <= 0) return

            var sumSq = 0.0
            val step = maxOf(1, sampleCount / 128)
            var sampled = 0
            var i = 0
            while (i < sampleCount) {
                val sample = if (isFloat) {
                    dup.getFloat(i * 4).toDouble().coerceIn(-1.0, 1.0)
                } else {
                    dup.getShort(i * 2).toDouble() / 32768.0
                }
                sumSq += sample * sample
                sampled++
                i += step
            }
            val rms = if (sampled > 0) sqrt(sumSq / sampled) else 0.0
            // Skala dB logaritmik (-40 dB .. -14 dB) agar bukaan mulut setara Web Audio FFT di PWA
            val db = 20.0 * log10(maxOf(rms, 1e-6))
            val rawLevel = ((db + 40.0) / 26.0).coerceIn(0.0, 1.0)
            smoothedLevel = if (rawLevel > smoothedLevel) {
                smoothedLevel + (rawLevel - smoothedLevel) * 0.80
            } else {
                smoothedLevel + (rawLevel - smoothedLevel) * 0.35
            }
            val normalized = if (smoothedLevel < 0.03) 0f else smoothedLevel.toFloat()
            val prev = _mouthLevel.value
            if ((normalized == 0f && prev != 0f) || abs(normalized - prev) >= 0.02f) {
                _mouthLevel.value = normalized
            }
        }
    }

    private val exoPlayer: ExoPlayer = run {
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(rmsSink)))
                    .build()
            }
        }

        ExoPlayer.Builder(appContext, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            activeBundledClip = null
                            _isSpeaking.value = false
                            _mouthLevel.value = 0f
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            _isSpeaking.value = true
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("TtsAudioManager", "ExoPlayer error: ${error.errorCodeName}", error)
                        val failedClip = activeBundledClip
                        activeBundledClip = null
                        _isSpeaking.value = false
                        _mouthLevel.value = 0f
                        if (failedClip != null) {
                            playAssetViaMediaPlayer(failedClip)
                        }
                    }
                })
            }
    }

    @Volatile
    private var isMuted: Boolean = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stop()
        }
    }

    fun playTts(text: String, isJapanese: Boolean) {
        if (isMuted || text.isBlank()) return
        stop()

        val myGen = playbackGen.incrementAndGet()
        val chunks = SseTagParser.splitForTts(text)
        if (chunks.isEmpty()) return

        activeFetchJob = scope.launch {
            val files = withContext(Dispatchers.IO) {
                coroutineScope {
                    chunks.mapIndexed { idx, chunk ->
                        async {
                            fetchTtsChunkToFile(chunk, isJapanese, myGen, idx)
                        }
                    }.awaitAll()
                }
            }.filterNotNull()

            if (isMuted || myGen != playbackGen.get() || files.isEmpty()) {
                _isSpeaking.value = false
                _mouthLevel.value = 0f
                return@launch
            }

            activeBundledClip = null
            val mediaItems = files.map { MediaItem.fromUri(Uri.fromFile(it)) }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun playBundledAssetClip(filename: String) {
        if (isMuted || filename.isBlank()) return
        scope.launch {
            if (_isSpeaking.value && activeBundledClip == null) return@launch
            playbackGen.incrementAndGet()
            activeFetchJob?.cancel()
            activeFetchJob = null
            releaseFallbackPlayer()
            activeBundledClip = filename
            runCatching {
                val assetUri = Uri.parse("file:///android_asset/live2d/characters/rem/audio/$filename")
                exoPlayer.setMediaItem(MediaItem.fromUri(assetUri))
                exoPlayer.prepare()
                exoPlayer.play()
            }.onFailure { err ->
                Log.e("TtsAudioManager", "ExoPlayer asset play failed, falling back to MediaPlayer", err)
                activeBundledClip = null
                playAssetViaMediaPlayer(filename)
            }
        }
    }

    private fun playAssetViaMediaPlayer(filename: String) {
        runCatching {
            releaseFallbackPlayer()
            val afd = appContext.assets.openFd("live2d/characters/rem/audio/$filename")
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    PlatformAudioAttributes.Builder()
                        .setContentType(PlatformAudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener {
                    _isSpeaking.value = true
                    start()
                }
                setOnCompletionListener {
                    _isSpeaking.value = false
                    _mouthLevel.value = 0f
                    releaseFallbackPlayer()
                }
                setOnErrorListener { _, _, _ ->
                    _isSpeaking.value = false
                    _mouthLevel.value = 0f
                    releaseFallbackPlayer()
                    true
                }
                prepareAsync()
            }
            fallbackPlayer = mp
        }.onFailure { err ->
            Log.e("TtsAudioManager", "MediaPlayer asset fallback failed for $filename", err)
            _isSpeaking.value = false
            _mouthLevel.value = 0f
        }
    }

    private fun releaseFallbackPlayer() {
        fallbackPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        fallbackPlayer = null
    }

    fun stop() {
        scope.launch {
            playbackGen.incrementAndGet()
            activeFetchJob?.cancel()
            activeFetchJob = null
            activeBundledClip = null
            releaseFallbackPlayer()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _isSpeaking.value = false
            _mouthLevel.value = 0f
        }
    }

    fun release() {
        scope.launch {
            playbackGen.incrementAndGet()
            activeFetchJob?.cancel()
            activeFetchJob = null
            activeBundledClip = null
            releaseFallbackPlayer()
            exoPlayer.release()
        }
    }

    private suspend fun fetchTtsChunkToFile(
        text: String,
        isJapanese: Boolean,
        gen: Int,
        index: Int
    ): File? {
        val apiKey = ApiKeyStore.getValue(AiConfigField.TTS_API_KEY, appContext)
        if (apiKey.isBlank()) return null

        val ttsBase = ApiKeyStore.getValue(AiConfigField.TTS_BASE_URL, appContext)
            .ifBlank { DEFAULT_FISH_AUDIO_BASE }
        if (!ttsBase.startsWith("http://", ignoreCase = true) &&
            !ttsBase.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }
        val ttsModel = ApiKeyStore.getValue(AiConfigField.TTS_MODEL, appContext)
            .ifBlank { DEFAULT_TTS_MODEL }
        val ttsVoice = ApiKeyStore.getValue(AiConfigField.TTS_VOICE_ID, appContext)
            .ifBlank { DEFAULT_VOICE_ID }

        val cleaned = cleanTtsText(text)
        if (cleaned.isEmpty()) return null

        val japanese = if (isJapanese) cleaned else translateToJapanese(cleaned)
        val finalText = truncateTtsText(japanese, MAX_TTS_CHARS)
        if (finalText.isEmpty()) return null

        return runCatching {
            val bodyJson = json.encodeToString(
                FishAudioTtsRequestDto(
                    text = finalText,
                    reference_id = ttsVoice,
                    format = "mp3"
                )
            )
            val request = Request.Builder()
                .url(ttsBase)
                .header("Authorization", "Bearer $apiKey")
                .header("model", ttsModel)
                .header("Accept", "audio/mpeg")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes() ?: return@use null
                val outFile = File(appContext.cacheDir, "tts_${gen}_${index}.mp3")
                outFile.writeBytes(bytes)
                outFile
            }
        }.getOrNull()
    }

    private suspend fun translateToJapanese(text: String): String {
        val res = DirectLlmClient.complete(
            httpClient = httpClient,
            messages = listOf(
                ChatTurnDto(
                    role = "system",
                    content = "Terjemahkan teks Indonesia ke bahasa Jepang natural gaya sopan, " +
                        "memanggil lawan bicara \"ご主人様\". Output HANYA terjemahan Jepang, " +
                        "tanpa romaji, tanpa penjelasan."
                ),
                ChatTurnDto(role = "user", content = text)
            ),
            temperature = 0.3,
            maxTokens = 800
        )
        return res.getOrNull()?.takeIf { it.isNotBlank() } ?: text
    }

    private fun cleanTtsText(raw: String): String = raw
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""\*(.*?)\*"""), "$1")
        .replace(Regex("""`([^`]+)`"""), "$1")
        .replace(Regex("""#{1,6}\s+"""), "")
        .replace(Regex("""\n[-*•]\s+"""), ". ")
        .replace(Regex("""\n+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

    private fun truncateTtsText(text: String, max: Int): String {
        if (text.length <= max) return text
        val cut = text.take(max)
        val lastPunct = maxOf(
            cut.lastIndexOf('.'),
            cut.lastIndexOf('?'),
            cut.lastIndexOf('!'),
            cut.lastIndexOf(','),
            cut.lastIndexOf('。'),
            cut.lastIndexOf('？'),
            cut.lastIndexOf('！'),
            cut.lastIndexOf('、')
        )
        return if (lastPunct > max * 0.5) cut.substring(0, lastPunct + 1) else "$cut…"
    }

    companion object {
        private const val DEFAULT_FISH_AUDIO_BASE = "https://api.fish.audio/v1/tts"
        private const val DEFAULT_TTS_MODEL = "s2.1-pro-free"
        private const val DEFAULT_VOICE_ID = "eb79df9571014449b42bcd869f86fe0a"
        private const val MAX_TTS_CHARS = 500
    }
}
