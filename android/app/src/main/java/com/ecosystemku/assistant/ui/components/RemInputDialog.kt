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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans

/**
 * Dialog input modal kustom Rem dengan avatar header 44dp, judul editorial serif,
 * outlined input terfokus, dan counter karakter opsional.
 */
@Composable
fun RemInputDialog(
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "",
    helperText: String? = null,
    helperColor: Color = TextMuted,
    errorText: String? = null,
    maxLength: Int? = null,
    singleLine: Boolean = true,
    isSecret: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    confirmText: String = "Simpan",
    dismissText: String = "Batal",
    confirmEnabled: Boolean = true,
    destructiveActionText: String? = null,
    onDestructiveAction: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var revealSecret by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 368.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            // Header: 44dp Rem Avatar + Editorial Title Stack + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemCompactAvatar(
                        size = 44.dp,
                        cutoutSurface = SecondarySurface
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "Rem",
                            fontFamily = EditorialSerif,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = subtitle,
                            fontFamily = UiSans,
                            fontSize = 13.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                RemModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!helperText.isNullOrBlank()) {
                Text(
                    text = helperText,
                    fontFamily = UiSans,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = helperColor
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Outlined Input Box with subtle blue focus halo
            val borderColor = if (isFocused) AccentBlue else BorderLight
            val borderWidth = if (isFocused) 1.5.dp else 1.dp
            val ringModifier = if (isFocused) {
                Modifier
                    .border(3.dp, AccentBlue.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(2.dp)
            } else {
                Modifier
            }

            Box(modifier = Modifier.fillMaxWidth().then(ringModifier)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSurface)
                        .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                fontFamily = UiSans,
                                fontSize = 14.5.sp,
                                color = TextSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = { next ->
                                onValueChange(if (maxLength != null) next.take(maxLength) else next)
                            },
                            singleLine = singleLine,
                            visualTransformation = if (isSecret && !revealSecret) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                            textStyle = TextStyle(
                                fontFamily = UiSans,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }

                    if (isSecret) {
                        Icon(
                            imageVector = if (revealSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (revealSecret) "Sembunyikan" else "Tampilkan",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(19.dp)
                                .clickable { revealSecret = !revealSecret }
                        )
                    }
                }
            }

            // Optional error message & character counter
            if (!errorText.isNullOrBlank() || maxLength != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = SemanticDanger,
                        modifier = Modifier.weight(1f)
                    )
                    if (maxLength != null) {
                        Text(
                            text = "${value.length}/$maxLength",
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!destructiveActionText.isNullOrBlank() && onDestructiveAction != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button, onClick = onDestructiveAction)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = destructiveActionText,
                            fontFamily = UiSans,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SemanticDanger
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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

                    Box(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .shadow(
                                elevation = if (confirmEnabled) 8.dp else 0.dp,
                                shape = RoundedCornerShape(999.dp),
                                ambientColor = AccentBlue.copy(alpha = 0.28f),
                                spotColor = AccentBlue.copy(alpha = 0.28f)
                            )
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (confirmEnabled) AccentBlue else AccentBlue.copy(alpha = 0.4f))
                            .clickable(enabled = confirmEnabled, role = Role.Button, onClick = onConfirm)
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmText,
                            fontFamily = UiSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F0E0C)
                        )
                    }
                }
            }
        }
    }
}
