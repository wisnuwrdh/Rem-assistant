package com.ecosystemku.assistant.ui.asisten

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

@Composable
fun AsistenScreen(
    uiState: AsistenUiState,
    stageController: WebViewLive2DStageController,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStageDragDelta: (Float) -> Unit,
    onDismissError: () -> Unit,
    onDismissKronikBanner: () -> Unit = {},
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
                // Aura cahaya radial biru neon di belakang Rem
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

                // Kanvas Live2D Offline (WebViewAssetLoader)
                AndroidView(
                    factory = { ctx -> stageController.createWebView(ctx) },
                    modifier = Modifier.fillMaxSize()
                )

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

                // Idle / Touch Thought Bubble
                AnimatedVisibility(
                    visible = uiState.idleBubbleText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 32.dp)
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
                            text = uiState.idleBubbleText.orEmpty(),
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            color = TextPrimary
                        )
                    }
                }

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
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "Opsi Obrolan (UI Preview)",
                            tint = TextSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                }

                // Daftar Pesan Chat (Room Persistent + Live Streaming)
                val listState = rememberLazyListState()
                val totalItems = uiState.messages.size + if (uiState.streamingText.isNotEmpty() || uiState.isThinking) 1 else 0

                LaunchedEffect(totalItems, uiState.streamingText.length) {
                    if (totalItems > 0) {
                        listState.animateScrollToItem(totalItems - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        ChatBubbleRow(
                            isUser = msg.role == "user",
                            text = msg.content
                        )
                    }

                    if (uiState.streamingText.isNotEmpty()) {
                        item(key = "streaming") {
                            ChatBubbleRow(isUser = false, text = uiState.streamingText)
                        }
                    } else if (uiState.isThinking) {
                        item(key = "thinking") {
                            ChatBubbleRow(isUser = false, text = "…")
                        }
                    }
                }

                // Capsule Input Bar (Mic UI-only + Send aktif)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp, top = 4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(50))
                        .padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (uiState.inputText.isEmpty()) {
                            Text(
                                text = "Ketik pesan...",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                color = TextSubtle
                            )
                        }
                        BasicTextField(
                            value = uiState.inputText,
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

                    // Tombol Mikrofon (UI-only sesuai konfirmasi)
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(8.dp, CircleShape, ambientColor = AccentBlue, spotColor = AccentBlue)
                            .clip(CircleShape)
                            .background(AccentBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Input Suara (UI Preview)",
                            tint = DominantBg,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tombol Kirim (Aktif -> /api/chat SSE + /api/tts)
                    IconButton(
                        onClick = onSend,
                        enabled = !uiState.isThinking && uiState.inputText.isNotBlank(),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.inputText.isNotBlank() && !uiState.isThinking) AccentBlue
                                else AccentBlue.copy(alpha = 0.45f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pesan",
                            tint = DominantBg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DominantBg)
                    .border(1.dp, BorderLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    fontFamily = EditorialSerif,
                    fontSize = 16.sp,
                    color = AccentBlue
                )
            }

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
