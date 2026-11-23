package com.ecosystemku.assistant.ui.asisten

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecosystemku.assistant.R
import com.ecosystemku.assistant.data.local.ChatMessageEntity
import com.ecosystemku.assistant.live2d.WebViewLive2DStageController
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.StatusOnline
import com.ecosystemku.assistant.ui.theme.StatusSpeaking
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AsistenScreen(
    uiState: AsistenUiState,
    stageController: WebViewLive2DStageController,
    inputTextFlow: StateFlow<String> = MutableStateFlow(""),
    streamingTextFlow: StateFlow<String> = MutableStateFlow(""),
    idleBubbleTextFlow: StateFlow<String?> = MutableStateFlow(null),
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStageDragDelta: (Float) -> Unit,
    onDismissError: () -> Unit,
    onDismissKronikBanner: () -> Unit = {},
    onRetryModel: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onClearChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .imePadding()
    ) {
        val focusManager = LocalFocusManager.current
        LaunchedEffect(uiState.isChatOpen) {
            if (!uiState.isChatOpen) {
                focusManager.clearFocus()
            }
        }

        val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx().coerceAtLeast(1f) }
        val animatedStageWeight by animateFloatAsState(
            targetValue = if (uiState.isChatOpen) uiState.stageFraction else 1f,
            animationSpec = tween(durationMillis = 280),
            label = "stageWeightAnim"
        )
        val stageWeight = animatedStageWeight.coerceIn(0.35f, 1f)
        val sheetWeight = (1f - stageWeight).coerceAtLeast(0f)

        Column(modifier = Modifier.fillMaxSize()) {

            // ══════════════════════════════════════════════════════════════
            // BAGIAN ATAS (~56-58%): Panggung Live2D + Aura Biru + Editorial
            // ══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(stageWeight)
                    .background(DominantBg)
            ) {
                // Aura cahaya radial biru neon fallback sebelum WebView siap
                if (!uiState.isModelReady) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AccentBlue.copy(alpha = 0.26f),
                                    AccentBlue.copy(alpha = 0.09f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.55f),
                                radius = size.maxDimension * 0.55f
                            )
                        )
                    }
                }

                // Kanvas Live2D Offline (WebViewAssetLoader)
                key(uiState.stageGeneration) {
                    AndroidView(
                        factory = { ctx -> stageController.createWebView(ctx) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading indikator sebelum model siap
                if (!uiState.isModelReady && uiState.modelError == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Model gagal dimuat / konteks WebGL hilang → tampilkan alasan + tombol muat ulang
                if (uiState.modelError != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Rem gagal ditampilkan",
                            fontFamily = UiSans,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.modelError,
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        TextButton(onClick = onRetryModel) {
                            Text(text = "Muat Ulang", color = AccentBlue)
                        }
                    }
                }

                // Header Editorial Kiri Atas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Rem",
                        fontFamily = EditorialSerif,
                        fontSize = 36.sp,
                        lineHeight = 38.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI Companion",
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }

                // Idle / Touch Thought Bubble (terisolasi StateFlow-nya sendiri)
                IdleThoughtBubble(
                    idleBubbleTextFlow = idleBubbleTextFlow,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 32.dp)
                )

                // Floating Status Tag ("Rem · idle / berbicara…")
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 28.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(SecondarySurface.copy(alpha = 0.80f))
                        .border(1.dp, BorderLight, RoundedCornerShape(40.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val statusDotColor = if (uiState.isSpeaking) StatusSpeaking else StatusOnline
                    val statusText = when {
                        uiState.isSpeaking -> "berbicara…"
                        uiState.isThinking -> "mengetik…"
                        uiState.currentExpression != "none" -> uiState.currentExpression
                        else -> "idle"
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = "Rem",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "· $statusText",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════
            // BAGIAN BAWAH (~42-44%): Glassmorphic Draggable Chat Sheet
            // ══════════════════════════════════════════════════════════════
            if (sheetWeight > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(sheetWeight)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF201F1D).copy(alpha = 0.94f),
                                    SecondarySurface
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = BorderLight,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                // Drag Handle + Header Mini Sheet Chat
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(totalHeightPx) {
                            detectVerticalDragGestures { _, dragAmount ->
                                onStageDragDelta(dragAmount / totalHeightPx)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 8.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TextSubtle.copy(alpha = 0.7f))
                    )

                    var menuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(19.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = if (uiState.isMuted) "Nyalakan Suara TTS" else "Matikan Suara TTS",
                                tint = if (uiState.isMuted) TextMuted else AccentBlue,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { onToggleMute() }
                            )
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.MoreHoriz,
                                    contentDescription = "Opsi Obrolan",
                                    tint = TextSubtle,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { menuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(ElevatedSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (uiState.isMuted) "Nyalakan Suara TTS" else "Matikan Suara TTS (Mute)",
                                                fontFamily = UiSans,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (uiState.isMuted) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                                contentDescription = null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onToggleMute()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Bersihkan Riwayat Obrolan",
                                                fontFamily = UiSans,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onClearChat()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                }

                // Daftar Pesan Chat (Room Persistent + Live Streaming terisolasi)
                ChatMessageList(
                    messages = uiState.messages,
                    isThinking = uiState.isThinking,
                    streamingTextFlow = streamingTextFlow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                // Capsule Input Bar (terisolasi dari AsistenUiState agar ketikan tidak me-recompose parent)
                ChatInputCapsule(
                    inputTextFlow = inputTextFlow,
                    isThinking = uiState.isThinking,
                    onInputChanged = onInputChanged,
                    onSend = onSend
                )
            }
            }
        }

        // Snackbar konfirmasi catatan Kronik dari chat Rem
        if (uiState.kronikBannerMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ElevatedSurface,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = onDismissKronikBanner) {
                        Text("OK", color = AccentBlue)
                    }
                }
            ) {
                Text(uiState.kronikBannerMessage, fontSize = 12.5.sp)
            }
        }

        // Error Snackbar
        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ElevatedSurface,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = onDismissError) {
                        Text("Tutup", color = AccentBlue)
                    }
                }
            ) {
                Text(uiState.errorMessage, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(
    isUser: Boolean,
    text: String
) {
    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    fontFamily = UiSans,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = TextPrimary
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Avatar Mini Melingkar Rem
            Image(
                painter = painterResource(id = R.drawable.rem_chat_avatar),
                contentDescription = "Rem",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, BorderLight, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    fontFamily = UiSans,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun IdleThoughtBubble(
    idleBubbleTextFlow: StateFlow<String?>,
    modifier: Modifier = Modifier
) {
    val bubbleText by idleBubbleTextFlow.collectAsStateWithLifecycle()
    androidx.compose.animation.AnimatedVisibility(
        visible = bubbleText != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SecondarySurface.copy(alpha = 0.90f))
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = bubbleText.orEmpty(),
                fontFamily = UiSans,
                fontSize = 12.5.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<ChatMessageEntity>,
    isThinking: Boolean,
    streamingTextFlow: StateFlow<String>,
    modifier: Modifier = Modifier
) {
    val streamingText by streamingTextFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val totalItems = messages.size + if (streamingText.isNotEmpty() || isThinking) 1 else 0

    // Animasi scroll hanya saat jumlah item bertambah (bukan per-token saat streaming)
    LaunchedEffect(totalItems) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Throttle scroll saat streaming berlangsung agar tidak membatalkan animasi per-karakter
    LaunchedEffect(streamingText) {
        if (streamingText.isNotEmpty() && totalItems > 0) {
            delay(100L)
            listState.scrollToItem(totalItems - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            ChatBubbleRow(
                isUser = msg.role == "user",
                text = msg.content
            )
        }

        if (streamingText.isNotEmpty()) {
            item(key = "streaming") {
                ChatBubbleRow(isUser = false, text = streamingText)
            }
        } else if (isThinking) {
            item(key = "thinking") {
                ChatBubbleRow(isUser = false, text = "…")
            }
        }
    }
}

@Composable
private fun ChatInputCapsule(
    inputTextFlow: StateFlow<String>,
    isThinking: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    val inputText by inputTextFlow.collectAsStateWithLifecycle()
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) {
                val combined = if (inputText.isBlank()) spoken else "${inputText.trim()} $spoken"
                onInputChanged(combined)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp, top = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(50))
            .padding(start = 16.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Ketik pesan...",
                    fontFamily = UiSans,
                    fontSize = 13.sp,
                    color = TextSubtle
                )
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = UiSans,
                    fontSize = 13.5.sp,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(AccentBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tombol Mikrofon (Input Suara STT Android)
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(8.dp, CircleShape, ambientColor = AccentBlue, spotColor = AccentBlue)
                .clip(CircleShape)
                .background(AccentBlue)
                .clickable(
                    enabled = !isThinking,
                    role = Role.Button,
                    onClick = {
                        runCatching {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan Rem...")
                            }
                            speechLauncher.launch(intent)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Input Suara",
                tint = DominantBg,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Tombol Kirim (Aktif -> /api/chat SSE + /api/tts)
        val canSend = inputText.isNotBlank() && !isThinking
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (canSend) AccentBlue else AccentBlue.copy(alpha = 0.45f))
                .clickable(enabled = canSend, role = Role.Button, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Kirim Pesan",
                tint = DominantBg,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

