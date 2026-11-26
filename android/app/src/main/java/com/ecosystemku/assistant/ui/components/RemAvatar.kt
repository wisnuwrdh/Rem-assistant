package com.ecosystemku.assistant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecosystemku.assistant.R
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.TextMuted

/**
 * Avatar bulat Rem dengan ring cahaya berlapis serta badge status opsional
 * (mengikuti desain modal & toast editorial Rem menggunakan foto asli R.drawable.rem_chat_avatar).
 */
@Composable
fun RemHaloAvatar(
    accentColor: Color = AccentBlue,
    outerSize: Dp = 118.dp,
    innerHaloSize: Dp = 100.dp,
    avatarSize: Dp = 86.dp,
    cutoutSurface: Color = SecondarySurface,
    badgeIcon: ImageVector? = null,
    badgeBgColor: Color = accentColor,
    badgeIconTint: Color = Color(0xFF0F0E0C),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer soft translucent halo
        Box(
            modifier = Modifier
                .size(outerSize)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.28f),
                    spotColor = accentColor.copy(alpha = 0.28f)
                )
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            // Secondary inner translucent cushion ring
            Box(
                modifier = Modifier
                    .size(innerHaloSize)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                // Crisp circular portrait frame
                Image(
                    painter = painterResource(id = R.drawable.rem_chat_avatar),
                    contentDescription = "Avatar Rem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .border(2.dp, accentColor.copy(alpha = 0.85f), CircleShape)
                )
            }
        }

        if (badgeIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .size(29.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(badgeBgColor)
                    .border(3.dp, cutoutSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeIconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Avatar ringkas Rem (44.dp / 36.dp) untuk header dialog input & floating pill toast.
 */
@Composable
fun RemCompactAvatar(
    size: Dp = 44.dp,
    ringBrush: Brush = Brush.linearGradient(
        colors = listOf(
            AccentBlue.copy(alpha = 0.92f),
            AccentBlue,
            AccentBlue.copy(alpha = 0.72f)
        )
    ),
    ringColor: Color = AccentBlue,
    cutoutSurface: Color = SecondarySurface,
    badgeContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = ringColor.copy(alpha = 0.25f),
                    spotColor = ringColor.copy(alpha = 0.25f)
                )
                .clip(CircleShape)
                .background(ringBrush)
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.rem_chat_avatar),
                contentDescription = "Avatar Rem",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, cutoutSurface, CircleShape)
            )
        }

        if (badgeContent != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                badgeContent()
            }
        }
    }
}

/**
 * Tombol tutup bulat 34.dp seragam untuk semua custom modal Rem.
 */
@Composable
fun RemModalCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Tutup dialog",
            tint = TextMuted,
            modifier = Modifier.size(15.dp)
        )
    }
}
