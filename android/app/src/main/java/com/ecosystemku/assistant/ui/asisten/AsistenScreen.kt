package com.ecosystemku.assistant.ui.asisten

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecosystemku.assistant.R
import com.ecosystemku.assistant.data.local.ChatChoicesCodec
import com.ecosystemku.assistant.data.local.ChatChoicesData
import com.ecosystemku.assistant.data.local.ChatMessageEntity
import com.ecosystemku.assistant.data.local.ChatSessionEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onToggleVoiceClipMute: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onSelectChoices: (Long, List<String>) -> Unit = { _, _ -> },
    onNewChatSession: () -> Unit = {},
    onSwitchChatSession: (Long) -> Unit = {},
    onRenameChatSession: (Long, String) -> Unit = { _, _ -> },
    onDeleteChatSession: (Long) -> Unit = {},
    onDeleteMessage: (Long) -> Unit = {},
    onRegenerateMessage: (Long) -> Unit = {},
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
                    var showSessionsDialog by remember { mutableStateOf(false) }
                    val activeSession = uiState.chatSessions.firstOrNull { it.id == uiState.currentSessionId }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(999.dp))
                                .background(ElevatedSurface.copy(alpha = 0.7f))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                                .clickable { showSessionsDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Log Obrolan",
                                tint = AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = activeSession?.title ?: "Obrolan Baru",
                                fontFamily = UiSans,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 175.dp)
                            )
                            if (uiState.chatSessions.size > 1) {
                                Text(
                                    text = "${uiState.chatSessions.size}",
                                    fontFamily = UiSans,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Obrolan Baru",
                                tint = if (uiState.isThinking) TextSubtle else AccentBlue,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(enabled = !uiState.isThinking) { onNewChatSession() }
                            )
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
                                                text = "Obrolan Baru",
                                                fontFamily = UiSans,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        enabled = !uiState.isThinking,
                                        onClick = {
                                            menuExpanded = false
                                            onNewChatSession()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Log Obrolan (${uiState.chatSessions.size})",
                                                fontFamily = UiSans,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            showSessionsDialog = true
                                        }
                                    )
                                    HorizontalDivider(color = BorderSubtle)
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
                                                text = if (uiState.isVoiceClipMuted) "Nyalakan Voice Klip" else "Matikan Voice Klip",
                                                fontFamily = UiSans,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (uiState.isVoiceClipMuted) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                                                contentDescription = null,
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onToggleVoiceClipMute()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Bersihkan Obrolan Sesi Ini",
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

                    if (showSessionsDialog) {
                        ChatSessionsDialog(
                            sessions = uiState.chatSessions,
                            currentSessionId = uiState.currentSessionId,
                            isThinking = uiState.isThinking,
                            onDismiss = { showSessionsDialog = false },
                            onNewSession = {
                                showSessionsDialog = false
                                onNewChatSession()
                            },
                            onSwitchSession = { sid ->
                                showSessionsDialog = false
                                onSwitchChatSession(sid)
                            },
                            onRenameSession = onRenameChatSession,
                            onDeleteSession = onDeleteChatSession
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                }

                // Daftar Pesan Chat (Room Persistent + Live Streaming terisolasi)
                ChatMessageList(
                    messages = uiState.messages,
                    isThinking = uiState.isThinking,
                    streamingTextFlow = streamingTextFlow,
                    onSelectChoices = onSelectChoices,
                    onDeleteMessage = onDeleteMessage,
                    onRegenerateMessage = onRegenerateMessage,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubbleRow(
    isUser: Boolean,
    text: String,
    messageId: Long? = null,
    choices: ChatChoicesData? = null,
    isThinking: Boolean = false,
    onSelectChoices: (List<String>) -> Unit = {},
    onDeleteMessage: (() -> Unit)? = null,
    onRegenerateMessage: (() -> Unit)? = null
) {
    var bubbleMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val allowActions = messageId != null

    val bubbleActionMenu: @Composable () -> Unit = {
        if (allowActions) {
            DropdownMenu(
                expanded = bubbleMenuExpanded,
                onDismissRequest = { bubbleMenuExpanded = false },
                modifier = Modifier.background(ElevatedSurface)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Salin Pesan",
                            fontFamily = UiSans,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        bubbleMenuExpanded = false
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Pesan disalin", Toast.LENGTH_SHORT).show()
                    }
                )
                if (!isUser && onRegenerateMessage != null && !isThinking) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Ulangi Jawaban",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            bubbleMenuExpanded = false
                            onRegenerateMessage()
                        }
                    )
                }
                if (onDeleteMessage != null && !isThinking) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Hapus Pesan",
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
                            bubbleMenuExpanded = false
                            onDeleteMessage()
                        }
                    )
                }
            }
        }
    }

    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                        .then(
                            if (allowActions) {
                                Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = { bubbleMenuExpanded = true }
                                )
                            } else Modifier
                        )
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
                bubbleActionMenu()
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

            Column(
                modifier = Modifier.widthIn(max = 276.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                            .then(
                                if (allowActions) {
                                    Modifier.combinedClickable(
                                        onClick = {},
                                        onLongClick = { bubbleMenuExpanded = true }
                                    )
                                } else Modifier
                            )
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
                    bubbleActionMenu()
                }

                if (choices != null && choices.items.isNotEmpty()) {
                    ChatChoicesCard(
                        choices = choices,
                        isThinking = isThinking,
                        onSelectChoices = onSelectChoices
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatChoicesCard(
    choices: ChatChoicesData,
    isThinking: Boolean,
    onSelectChoices: (List<String>) -> Unit
) {
    val isLocked = choices.selected.isNotEmpty()
    val isMulti = choices.mode.equals("multi", ignoreCase = true)
    var draftSelected by remember(choices) { mutableStateOf(choices.selected.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            text = if (isMulti) "Pilih satu atau lebih" else "Pilih salah satu",
            fontFamily = UiSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )

        if (isMulti) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                choices.items.forEach { item ->
                    val isActive = if (isLocked) item in choices.selected else item in draftSelected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) AccentBlue.copy(alpha = 0.13f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isActive) AccentBlue.copy(alpha = 0.38f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isLocked && !isThinking) {
                                draftSelected = if (item in draftSelected) draftSelected - item else draftSelected + item
                            }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (isActive) TextPrimary else Color.Transparent)
                                .border(1.5.dp, if (isActive) TextPrimary else TextSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = DominantBg,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Text(
                            text = item,
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            color = if (isLocked && !isActive) TextMuted else TextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pickedOrdered = choices.items.filter { it in draftSelected }
                Text(
                    text = when {
                        isLocked -> "Kamu memilih: ${choices.selected.joinToString(", ")}"
                        pickedOrdered.isNotEmpty() -> "Dipilih: ${pickedOrdered.joinToString(", ")}"
                        else -> "Dipilih: Belum ada"
                    },
                    fontFamily = UiSans,
                    fontSize = 11.5.sp,
                    fontWeight = if (isLocked) FontWeight.Medium else FontWeight.Normal,
                    color = if (isLocked) StatusOnline else TextMuted,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                if (!isLocked) {
                    val canSubmit = pickedOrdered.isNotEmpty() && !isThinking
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (canSubmit) AccentBlue else AccentBlue.copy(alpha = 0.4f))
                            .clickable(enabled = canSubmit, role = Role.Button) {
                                onSelectChoices(pickedOrdered)
                            }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Kirim",
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DominantBg
                        )
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                choices.items.forEach { item ->
                    val isSelected = item in choices.selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                when {
                                    isSelected -> TextPrimary
                                    isLocked -> ElevatedSurface.copy(alpha = 0.52f)
                                    else -> ElevatedSurface
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isSelected -> TextPrimary
                                    isLocked -> BorderSubtle
                                    else -> BorderLight
                                },
                                RoundedCornerShape(999.dp)
                            )
                            .clickable(enabled = !isLocked && !isThinking, role = Role.Button) {
                                onSelectChoices(listOf(item))
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = item,
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = when {
                                isSelected -> DominantBg
                                isLocked -> TextMuted
                                else -> TextPrimary
                            }
                        )
                    }
                }
            }

            if (isLocked) {
                Text(
                    text = "Kamu memilih: ${choices.selected.joinToString(", ")}",
                    fontFamily = UiSans,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = StatusOnline
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
    onSelectChoices: (Long, List<String>) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onRegenerateMessage: (Long) -> Unit,
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
            val decodedChoices = remember(msg.choicesJson) { ChatChoicesCodec.decode(msg.choicesJson) }
            ChatBubbleRow(
                isUser = msg.role == "user",
                text = msg.content,
                messageId = msg.id,
                choices = decodedChoices,
                isThinking = isThinking,
                onSelectChoices = { picked -> onSelectChoices(msg.id, picked) },
                onDeleteMessage = { onDeleteMessage(msg.id) },
                onRegenerateMessage = { onRegenerateMessage(msg.id) }
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
    val context = LocalContext.current
    val inputText by inputTextFlow.collectAsStateWithLifecycle()
    var showVoiceDialog by remember { mutableStateOf(false) }

    fun appendSpokenText(spoken: String) {
        val clean = spoken.trim()
        if (clean.isNotEmpty()) {
            val combined = if (inputText.isBlank()) clean else "${inputText.trim()} $clean"
            onInputChanged(combined)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            appendSpokenText(matches?.firstOrNull().orEmpty())
        }
    }

    fun launchSystemSpeechFallback() {
        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan Rem...")
            }
            speechLauncher.launch(intent)
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                showVoiceDialog = true
            } else {
                launchSystemSpeechFallback()
            }
        } else {
            Toast.makeText(context, "Izin mikrofon diperlukan untuk input suara", Toast.LENGTH_SHORT).show()
        }
    }

    if (showVoiceDialog) {
        RemVoiceInputDialog(
            onDismiss = { showVoiceDialog = false },
            onResult = { spoken ->
                showVoiceDialog = false
                appendSpokenText(spoken)
            },
            onFallbackSystemVoice = {
                showVoiceDialog = false
                launchSystemSpeechFallback()
            }
        )
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

        // Tombol Mikrofon (Input Suara STT Custom Rem + fallback sistem)
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
                        val hasMicPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMicPerm) {
                            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                showVoiceDialog = true
                            } else {
                                launchSystemSpeechFallback()
                            }
                        } else {
                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

@Composable
private fun ChatSessionsDialog(
    sessions: List<ChatSessionEntity>,
    currentSessionId: Long,
    isThinking: Boolean,
    onDismiss: () -> Unit,
    onNewSession: () -> Unit,
    onSwitchSession: (Long) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    var renamingSession by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ElevatedSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Log Obrolan",
                        fontFamily = EditorialSerif,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${sessions.size} sesi tersimpan",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isThinking) TextSubtle.copy(alpha = 0.25f) else AccentBlue)
                        .clickable(enabled = !isThinking) { onNewSession() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ Baru",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DominantBg
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle)

            if (sessions.isEmpty()) {
                Text(
                    text = "Belum ada sesi obrolan tersimpan.",
                    fontFamily = UiSans,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        val isActive = session.id == currentSessionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isActive) AccentBlue.copy(alpha = 0.14f) else SecondarySurface
                                )
                                .border(
                                    1.dp,
                                    if (isActive) AccentBlue.copy(alpha = 0.45f) else BorderSubtle,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !isThinking) { onSwitchSession(session.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = session.title.ifBlank { "Obrolan" },
                                        fontFamily = UiSans,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isActive) {
                                        Text(
                                            text = "Aktif",
                                            fontFamily = UiSans,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = StatusOnline
                                        )
                                    }
                                }
                                Text(
                                    text = formatter.format(Date(session.updatedAt)),
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Ubah Judul Sesi",
                                    tint = TextMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            renamingSession = session
                                            renameDraft = session.title
                                        }
                                )
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Hapus Sesi",
                                    tint = if (isThinking) TextSubtle else TextMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(enabled = !isThinking) { onDeleteSession(session.id) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Tutup",
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        color = AccentBlue
                    )
                }
            }
        }
    }

    val targetRename = renamingSession
    if (targetRename != null) {
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = "Ubah Judul Obrolan",
                    fontFamily = EditorialSerif,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(60) },
                    singleLine = true,
                    placeholder = { Text("Judul sesi", fontSize = 13.sp) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(targetRename.id, renameDraft)
                        renamingSession = null
                    }
                ) {
                    Text("Simpan", color = AccentBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }
}


