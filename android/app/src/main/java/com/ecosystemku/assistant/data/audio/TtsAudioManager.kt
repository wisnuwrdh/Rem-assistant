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
import kotlin.math.min
import kotlin.math.sqrt

@Serializable
private data class TtsRequestDto(
    val text: String,
    val translated: Boolean
)

@OptIn(UnstableApi::class)
class TtsAudioManager(
    context: Context,
    private val httpClient: OkHttpClient,
    private val baseUrl: String = BuildConfig.API_BASE_URL
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
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
            _mouthLevel.value = 0f
        }

        override fun handleBuffer(buffer: ByteBuffer) {
            val dup = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val shortCount = dup.remaining() / 2
            if (shortCount <= 0) return

            var sumSq = 0.0
            val step = maxOf(1, shortCount / 128)
            var sampled = 0
            var i = 0
            while (i < shortCount) {
                val sample = dup.getShort(i * 2).toDouble() / 32768.0
                sumSq += sample * sample
                sampled++
                i += step
            }
            val rms = if (sampled > 0) sqrt(sumSq / sampled) else 0.0
            val normalized = min(1.0, rms * 3.8).toFloat()
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

    fun playTts(text: String, isJapanese: Boolean) {
        if (text.isBlank()) return
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

            if (myGen != playbackGen.get() || files.isEmpty()) {
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
        if (filename.isBlank()) return
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

    private fun fetchTtsChunkToFile(
        text: String,
        isJapanese: Boolean,
        gen: Int,
        index: Int
    ): File? {
        return runCatching {
            val bodyJson = json.encodeToString(TtsRequestDto(text = text, translated = isJapanese))
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/tts")
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                val outFile = File(appContext.cacheDir, "tts_${gen}_${index}.mp3")
                outFile.writeBytes(bytes)
                outFile
            }
        }.getOrNull()
    }
}
