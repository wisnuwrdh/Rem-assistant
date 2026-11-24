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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Dialog input suara custom bertema Rem menggunakan Android SpeechRecognizer,
 * menggantikan popup eksternal Google agar menampilkan avatar Rem, animasi
 * gelombang suara real-time, dan transkrip parsial langsung.
 */
@Composable
fun RemVoiceInputDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
    onFallbackSystemVoice: () -> Unit
) {
    val context = LocalContext.current
    var retrySeed by remember { mutableIntStateOf(0) }
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Menyiapkan mikrofon...") }
    var liveTranscript by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var rawRms by remember { mutableFloatStateOf(0f) }
    var activeRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

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

    DisposableEffect(retrySeed) {
        errorText = null
        liveTranscript = ""
        rawRms = 0f
        isListening = false
        statusText = "Menyiapkan mikrofon..."

        val recognizer = runCatching {
            SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        }.getOrNull()
        activeRecognizer = recognizer

        if (recognizer == null) {
            errorText = "Layanan pengenal suara tidak tersedia di perangkat ini."
        } else {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    errorText = null
                    statusText = "Bicara dengan Rem..."
                }

                override fun onBeginningOfSpeech() {
                    isListening = true
                    statusText = "Rem sedang mendengarkan..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Nilai tipikal SpeechRecognizer RMS berkisar -2 dB (diam) s.d. 10 dB (lantang)
                    rawRms = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    isListening = false
                    rawRms = 0f
                    statusText = "Memproses suara..."
                }

                override fun onError(error: Int) {
                    isListening = false
                    rawRms = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Rem belum menangkap ucapan Tuan."
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
                    isListening = false
                    rawRms = 0f
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (spoken.isNotEmpty()) {
                        onResult(spoken)
                    } else if (liveTranscript.isNotBlank()) {
                        onResult(liveTranscript.trim())
                    } else {
                        errorText = "Rem belum menangkap ucapan Tuan."
                        statusText = "Ketuk Coba Lagi untuk berbicara"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (partial.isNotEmpty()) {
                        liveTranscript = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            runCatching {
                recognizer.startListening(intent)
            }.onFailure { err ->
                errorText = err.message ?: "Gagal memulai pengenal suara."
            }
        }

        onDispose {
            activeRecognizer = null
            runCatching {
                recognizer?.cancel()
                recognizer?.destroy()
            }
        }
    }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp, max = 110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SecondarySurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = liveTranscript.ifBlank {
                        if (errorText != null) "Tidak ada teks tertangkap." else "Ucapan Tuan akan muncul di sini..."
                    },
                    fontFamily = UiSans,
                    fontSize = 13.sp,
                    color = if (liveTranscript.isNotBlank()) TextPrimary else TextSubtle,
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentBlue)
                            .clickable(role = Role.Button) {
                                if (isListening) {
                                    runCatching { activeRecognizer?.stopListening() }
                                } else if (liveTranscript.isNotBlank()) {
                                    onResult(liveTranscript.trim())
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
