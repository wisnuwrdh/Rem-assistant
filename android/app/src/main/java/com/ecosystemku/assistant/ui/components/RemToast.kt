package com.ecosystemku.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SemanticSuccess
import com.ecosystemku.assistant.ui.theme.SemanticWarning
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.UiSans
import com.ecosystemku.assistant.ui.theme.isEcosystemLightTheme
import kotlinx.coroutines.delay

enum class RemToastVariant {
    SUCCESS,
    ACTION,
    WARNING
}

data class RemToastData(
    val id: Long,
    val message: String,
    val variant: RemToastVariant = RemToastVariant.SUCCESS,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 3800L
)

class RemToastController {
    private var nextId by mutableLongStateOf(1L)
    var current by mutableStateOf<RemToastData?>(null)
        private set

    fun show(
        message: String,
        variant: RemToastVariant = RemToastVariant.SUCCESS,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        durationMs: Long = 3800L
    ) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        val id = nextId++
        current = RemToastData(
            id = id,
            message = clean,
            variant = variant,
            actionLabel = actionLabel,
            onAction = onAction,
            durationMs = durationMs
        )
    }

    fun success(message: String, durationMs: Long = 3400L) {
        show(message = message, variant = RemToastVariant.SUCCESS, durationMs = durationMs)
    }

    fun action(
        message: String,
        actionLabel: String,
        onAction: () -> Unit,
        durationMs: Long = 4500L
    ) {
        show(
            message = message,
            variant = RemToastVariant.ACTION,
            actionLabel = actionLabel,
            onAction = onAction,
            durationMs = durationMs
        )
    }

    fun warning(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        durationMs: Long = 4500L
    ) {
        show(
            message = message,
            variant = RemToastVariant.WARNING,
            actionLabel = actionLabel,
            onAction = onAction,
            durationMs = durationMs
        )
    }

    fun dismiss() {
        current = null
    }
}

val LocalRemToast = staticCompositionLocalOf<RemToastController?> { null }

/**
 * Host floating pill toast/banner Rem yang mengambang di bagian atas layar
 * sehingga tetap terlihat bersih meski keyboard atau modal lain sedang terbuka.
 */
@Composable
fun RemToastHost(
    controller: RemToastController,
    modifier: Modifier = Modifier
) {
    val toast = controller.current

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(toast.durationMs)
            if (controller.current?.id == toast.id) {
                controller.dismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (toast != null) {
                RemFloatingToastPill(
                    toast = toast,
                    onDismiss = { controller.dismiss() }
                )
            }
        }
    }
}

/**
 * Floating Pill Toast / Banner Rem (Success Emerald / Action Blue / Warning Amber).
 */
@Composable
fun RemFloatingToastPill(
    toast: RemToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (toast.variant) {
        RemToastVariant.SUCCESS -> SemanticSuccess
        RemToastVariant.ACTION -> AccentBlue
        RemToastVariant.WARNING -> SemanticWarning
    }
    val ringBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = 0.85f),
            accent.copy(alpha = 0.55f)
        )
    )
    val pillShape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = pillShape,
                ambientColor = accent.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(pillShape)
            .background(ElevatedSurface)
            .border(1.dp, accent.copy(alpha = 0.30f), pillShape)
            .clickable { onDismiss() }
            .padding(start = 10.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RemCompactAvatar(
            size = 36.dp,
            ringBrush = ringBrush,
            ringColor = accent,
            cutoutSurface = ElevatedSurface,
            badgeContent = when (toast.variant) {
                RemToastVariant.SUCCESS -> {
                    {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(SemanticSuccess)
                                .border(2.dp, ElevatedSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF081C10),
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
                RemToastVariant.WARNING -> {
                    {
                        Box(
                            modifier = Modifier
                                .offset(x = 1.dp, y = 1.dp)
                                .size(13.dp)
                                .shadow(6.dp, CircleShape, ambientColor = SemanticWarning, spotColor = SemanticWarning)
                                .clip(CircleShape)
                                .background(SemanticWarning)
                                .border(2.dp, ElevatedSurface, CircleShape)
                        )
                    }
                }
                RemToastVariant.ACTION -> null
            }
        )

        Text(
            text = toast.message,
            fontFamily = UiSans,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        when (toast.variant) {
            RemToastVariant.SUCCESS -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SemanticSuccess.copy(alpha = 0.12f))
                        .border(1.dp, SemanticSuccess.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = SemanticSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            RemToastVariant.ACTION -> {
                val label = toast.actionLabel
                val action = toast.onAction
                if (!label.isNullOrBlank() && action != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 30.dp)
                            .shadow(6.dp, pillShape, ambientColor = AccentBlue, spotColor = AccentBlue)
                            .clip(pillShape)
                            .background(AccentBlue)
                            .clickable(role = Role.Button) {
                                onDismiss()
                                action()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0E0C)
                        )
                    }
                }
            }
            RemToastVariant.WARNING -> {
                val label = toast.actionLabel
                val action = toast.onAction
                if (!label.isNullOrBlank() && action != null) {
                    val chipTextColor = if (isEcosystemLightTheme) Color(0xFF92400E) else Color(0xFFFDE68A)
                    Box(
                        modifier = Modifier
                            .heightIn(min = 30.dp)
                            .clip(pillShape)
                            .background(SemanticWarning.copy(alpha = 0.14f))
                            .border(1.dp, SemanticWarning.copy(alpha = 0.38f), pillShape)
                            .clickable(role = Role.Button) {
                                onDismiss()
                                action()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = chipTextColor
                        )
                    }
                }
            }
        }
    }
}
