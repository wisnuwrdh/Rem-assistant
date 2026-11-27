package com.ecosystemku.assistant.ui.asisten

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ecosystemku.assistant.R
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object VoiceTranscriptHelper {
    private val ECOSYSTEM_TERM_REGEX = Regex(
        """\b(rem|kronik|linkku|link ku|cucimoney|cuci money|cuci mani|ideku|ide ku|daily os|dailyos|hesych)\b""",
        RegexOption.IGNORE_CASE
    )
    private val CUCI_MONEY_REGEX = Regex("""\bcuci\s*(?:money|mani)\+?\b""", RegexOption.IGNORE_CASE)
    private val DAILY_OS_REGEX = Regex("""\b(?:daily\s*os|deili\s*os|deli\s*os)\b""", RegexOption.IGNORE_CASE)
    private val LINK_KU_REGEX = Regex("""\blink\s+ku\b""", RegexOption.IGNORE_CASE)
    private val IDE_KU_REGEX = Regex("""\bide\s+ku\b""", RegexOption.IGNORE_CASE)

    fun pickBestCandidate(candidates: List<String>, confidenceScores: FloatArray? = null): String {
        val cleaned = candidates.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return ""
        if (cleaned.size == 1) return normalizeAppTerms(cleaned.first())

        val topScore = confidenceScores?.firstOrNull() ?: -1f
        val bestEcosystemIdx = cleaned.indices.firstOrNull { idx ->
            val scoreOk = confidenceScores == null ||
                idx >= confidenceScores.size ||
                confidenceScores[idx] < 0f ||
                topScore < 0f ||
                confidenceScores[idx] >= (topScore - 0.12f)
            scoreOk && ECOSYSTEM_TERM_REGEX.containsMatchIn(cleaned[idx])
        }
        val chosen = if (bestEcosystemIdx != null) cleaned[bestEcosystemIdx] else cleaned.first()
        return normalizeAppTerms(chosen)
    }

    fun normalizeAppTerms(raw: String): String {
        return raw.trim()
            .replace(CUCI_MONEY_REGEX, "CuciMoney")
            .replace(DAILY_OS_REGEX, "Daily OS")
            .replace(LINK_KU_REGEX, "LinkKu")
            .replace(IDE_KU_REGEX, "IdeKu")
    }

    fun joinSegments(confirmed: String, live: String): String {
        val c = confirmed.trim()
        val l = live.trim()
        return when {
            c.isEmpty() -> l
            l.isEmpty() -> c
            else -> "$c $l"
        }
    }
}

/**
 * Dialog input suara custom bertema Rem menggunakan Android SpeechRecognizer,
 * mendukung mode dengar berkelanjutan lintas jeda napas agar pengguna tidak
 * perlu bicara terburu-buru sebelum menekan "Selesai".
 */
@Composable
fun RemVoiceInputDialog(
    profileName: String = "Tuan",
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    onFallbackSystemVoice: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callerName = profileName.trim().ifBlank { "Tuan" }
    var retrySeed by remember { mutableIntStateOf(0) }
    var isListening by remember { mutableStateOf(false) }
    var isPausedBySilence by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Menyiapkan mikrofon...") }
    var confirmedTranscript by remember { mutableStateOf("") }
    var liveTranscript by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var rawRms by remember { mutableFloatStateOf(0f) }
    var activeRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var finishRequested by remember { mutableStateOf(false) }

    val smoothLevel by animateFloatAsState(
        targetValue = if (isListening) rawRms else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "voiceRmsLevel"
    )
    val idlePulse by rememberInfiniteTransition(label = "voiceIdlePulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voicePulseScale"
    )

    fun buildSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    DisposableEffect(retrySeed) {
        errorText = null
        isPausedBySilence = false
        finishRequested = false
        liveTranscript = ""
        rawRms = 0f
        isListening = false
        statusText = "Menyiapkan mikrofon..."

        var disposed = false
        var consecutiveSilentErrors = 0
        var restartJob: Job? = null

        val recognizer = runCatching {
            SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        }.getOrNull()
        activeRecognizer = recognizer

        fun startRecognizerListening() {
            if (disposed || recognizer == null) return
            runCatching {
                recognizer.startListening(buildSpeechIntent())
            }.onFailure { err ->
                isListening = false
                errorText = err.message ?: "Gagal memulai pengenal suara."
            }
        }

        fun scheduleNextSegment(delayMs: Long = 160L) {
            restartJob?.cancel()
            restartJob = scope.launch {
                delay(delayMs)
                if (!disposed && !finishRequested) {
                    startRecognizerListening()
                }
            }
        }

        if (recognizer == null) {
            errorText = "Layanan pengenal suara tidak tersedia di perangkat ini."
        } else {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (disposed) return
                    isListening = true
                    isPausedBySilence = false
                    errorText = null
                    statusText = if (confirmedTranscript.isBlank()) {
                        "Bicara santai dengan Rem..."
                    } else {
                        "Lanjut bicara atau ketuk Selesai..."
                    }
                }

                override fun onBeginningOfSpeech() {
                    if (disposed) return
                    isListening = true
                    consecutiveSilentErrors = 0
                    statusText = "Rem sedang mendengarkan..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (disposed) return
                    // Nilai tipikal SpeechRecognizer RMS berkisar -2 dB (diam) s.d. 10 dB (lantang)
                    rawRms = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    if (disposed) return
                    isListening = false
                    rawRms = 0f
                    statusText = if (finishRequested) "Menyelesaikan..." else "Memproses jeda..."
                }

                override fun onError(error: Int) {
                    if (disposed) return
                    isListening = false
                    rawRms = 0f

                    val isSilentTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

                    if (finishRequested) {
                        val finalCombined = VoiceTranscriptHelper.joinSegments(confirmedTranscript, liveTranscript)
                        if (finalCombined.isNotBlank()) {
                            onResult(finalCombined)
                        } else {
                            finishRequested = false
                            errorText = "Rem belum menangkap ucapan $callerName."
                            statusText = "Ketuk Coba Lagi untuk berbicara"
                        }
                        return
                    }

                    if (isSilentTimeout) {
                        val partial = VoiceTranscriptHelper.normalizeAppTerms(liveTranscript)
                        if (partial.isNotBlank()) {
                            confirmedTranscript = VoiceTranscriptHelper.joinSegments(confirmedTranscript, partial)
                            liveTranscript = ""
                            consecutiveSilentErrors = 0
                            scheduleNextSegment(180L)
                            return
                        }
                        consecutiveSilentErrors++
                        if (confirmedTranscript.isNotBlank()) {
                            if (consecutiveSilentErrors < 2) {
                                scheduleNextSegment(200L)
                            } else {
                                isPausedBySilence = true
                                statusText = "Dijeda karena hening — ketuk Lanjut atau Selesai"
                            }
                            return
                        }
                        if (consecutiveSilentErrors < 2) {
                            scheduleNextSegment(200L)
                            return
                        }
                    }

                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Rem belum menangkap ucapan $callerName."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara terdeteksi."
                        SpeechRecognizer.ERROR_AUDIO -> "Gagal merekam audio dari mikrofon."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Koneksi jaringan bermasalah saat mengenali suara."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mikrofon sedang sibuk, silakan coba lagi."
                        else -> "Pengenal suara mengalami gangguan (kode $error)."
                    }
                    errorText = msg
                    statusText = "Ketuk Coba Lagi untuk berbicara"
                }

                override fun onResults(results: Bundle?) {
                    if (disposed) return
                    isListening = false
                    rawRms = 0f
                    consecutiveSilentErrors = 0

                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val segment = VoiceTranscriptHelper.pickBestCandidate(matches, scores)
                        .ifBlank { VoiceTranscriptHelper.normalizeAppTerms(liveTranscript) }

                    if (segment.isNotBlank()) {
                        confirmedTranscript = VoiceTranscriptHelper.joinSegments(confirmedTranscript, segment)
                    }
                    liveTranscript = ""

                    if (finishRequested) {
                        if (confirmedTranscript.isNotBlank()) {
                            onResult(confirmedTranscript)
                        } else {
                            finishRequested = false
                            errorText = "Rem belum menangkap ucapan $callerName."
                            statusText = "Ketuk Coba Lagi untuk berbicara"
                        }
                        return
                    }

                    // Lanjutkan mendengarkan setelah jeda kalimat agar pengguna tidak perlu buru-buru
                    statusText = "Tersambung — lanjut bicara atau ketuk Selesai"
                    scheduleNextSegment(160L)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (disposed) return
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        consecutiveSilentErrors = 0
                        liveTranscript = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            startRecognizerListening()
        }

        onDispose {
            disposed = true
            restartJob?.cancel()
            activeRecognizer = null
            runCatching {
                recognizer?.cancel()
                recognizer?.destroy()
            }
        }
    }

    val combinedTranscript = VoiceTranscriptHelper.joinSegments(confirmedTranscript, liveTranscript)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ElevatedSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rem",
                        fontFamily = EditorialSerif,
                        fontSize = 22.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Input Suara · Indonesia (id-ID)",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SecondarySurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Avatar Rem dengan ring animasi RMS real-time
            val outerScale = if (isListening) (idlePulse + smoothLevel * 0.28f) else 1f
            val innerScale = if (isListening) (1.03f + smoothLevel * 0.16f) else 1f

            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .scale(outerScale)
                        .clip(CircleShape)
                        .background(
                            if (errorText != null) SemanticDanger.copy(alpha = 0.14f)
                            else AccentBlue.copy(alpha = if (isListening) 0.16f + smoothLevel * 0.16f else 0.08f)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(innerScale)
                        .clip(CircleShape)
                        .background(
                            if (errorText != null) SemanticDanger.copy(alpha = 0.22f)
                            else AccentBlue.copy(alpha = if (isListening) 0.25f + smoothLevel * 0.18f else 0.14f)
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.rem_chat_avatar),
                    contentDescription = "Rem Voice",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(10.dp, CircleShape, ambientColor = AccentBlue, spotColor = AccentBlue)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (errorText != null) SemanticDanger else AccentBlue,
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 14.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (errorText != null) SemanticDanger else AccentBlue)
                        .border(2.dp, ElevatedSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = DominantBg,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = errorText ?: statusText,
                fontFamily = UiSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (errorText != null) SemanticDanger else TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            val transcriptScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp, max = 140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SecondarySurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .verticalScroll(transcriptScroll)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = combinedTranscript.ifBlank {
                        if (errorText != null) "Tidak ada teks tertangkap." else "Ucapan $callerName akan muncul di sini..."
                    },
                    fontFamily = UiSans,
                    fontSize = 13.sp,
                    color = if (combinedTranscript.isNotBlank()) TextPrimary else TextSubtle,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (errorText != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onFallbackSystemVoice()
                        }
                    ) {
                        Text("Pakai Google", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentBlue)
                            .clickable(role = Role.Button) { retrySeed++ }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = DominantBg,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Coba Lagi",
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DominantBg
                        )
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", fontFamily = UiSans, fontSize = 12.5.sp, color = TextMuted)
                    }
                    if (isPausedBySilence) {
                        TextButton(onClick = { retrySeed++ }) {
                            Text("Lanjut", fontFamily = UiSans, fontSize = 12.5.sp, color = AccentBlue)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentBlue)
                            .clickable(role = Role.Button) {
                                if (isListening) {
                                    if (liveTranscript.isBlank() && confirmedTranscript.isNotBlank()) {
                                        runCatching { activeRecognizer?.cancel() }
                                        onResult(confirmedTranscript)
                                    } else {
                                        finishRequested = true
                                        statusText = "Menyelesaikan..."
                                        runCatching { activeRecognizer?.stopListening() }
                                    }
                                } else if (combinedTranscript.isNotBlank()) {
                                    onResult(combinedTranscript)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selesai",
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DominantBg
                        )
                    }
                }
            }
        }
    }
}
