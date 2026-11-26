package com.ecosystemku.assistant.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.SemanticWarning
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.UiSans

enum class RemAlertTone {
    INFO,
    DANGER,
    WARNING
}

/**
 * Dialog modal kustom Rem untuk alert info, izin, peringatan, maupun konfirmasi hapus data.
 * Mengikuti desain "Rem Custom Modal Alert Dialogs UI" dengan avatar Rem asli.
 */
@Composable
fun RemAlertDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Batal",
    tone: RemAlertTone = RemAlertTone.INFO,
    badgeIcon: ImageVector? = null,
    secondaryConfirmText: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true
) {
    val accent = when (tone) {
        RemAlertTone.INFO -> AccentBlue
        RemAlertTone.DANGER -> SemanticDanger
        RemAlertTone.WARNING -> SemanticWarning
    }
    val primaryInk = when (tone) {
        RemAlertTone.INFO -> Color(0xFF0F0E0C)
        RemAlertTone.DANGER -> Color.White
        RemAlertTone.WARNING -> Color(0xFF0F0E0C)
    }
    val resolvedBadgeIcon = badgeIcon ?: when (tone) {
        RemAlertTone.INFO -> Icons.Outlined.Info
        RemAlertTone.DANGER -> Icons.Outlined.DeleteOutline
        RemAlertTone.WARNING -> Icons.Outlined.WarningAmber
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 356.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            // Top Editorial Header Row ("Rem" + Circular X Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rem",
                    fontFamily = EditorialSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                RemModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Centered Rem Avatar with Layered Glowing Halo & Status Badge
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RemHaloAvatar(
                    accentColor = accent,
                    badgeIcon = resolvedBadgeIcon,
                    badgeBgColor = accent,
                    badgeIconTint = primaryInk,
                    cutoutSurface = SecondarySurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Centered Title & Description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontFamily = UiSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = description,
                    fontFamily = UiSans,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Right-aligned Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dismissText,
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                if (!secondaryConfirmText.isNullOrBlank() && onSecondaryConfirm != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(SemanticWarning.copy(alpha = 0.16f))
                            .border(1.dp, SemanticWarning.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button, onClick = onSecondaryConfirm)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secondaryConfirmText,
                            fontFamily = UiSans,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SemanticWarning
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .shadow(
                            elevation = if (confirmEnabled) 8.dp else 0.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = accent.copy(alpha = 0.28f),
                            spotColor = accent.copy(alpha = 0.28f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (confirmEnabled) accent else accent.copy(alpha = 0.4f))
                        .clickable(enabled = confirmEnabled, role = Role.Button, onClick = onConfirm)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmText,
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryInk
                    )
                }
            }
        }
    }
}
