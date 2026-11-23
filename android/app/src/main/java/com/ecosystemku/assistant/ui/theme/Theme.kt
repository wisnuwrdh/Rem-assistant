package com.ecosystemku.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun EcosystemKuTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useLight = when (themeMode) {
        "light" -> true
        "system" -> !systemDark
        else -> false
    }
    isEcosystemLightTheme = useLight
    SideEffect {
        isEcosystemLightTheme = useLight
    }

    val colorScheme = if (useLight) {
        lightColorScheme(
            primary = AccentBlue,
            onPrimary = SecondarySurface,
            background = DominantBg,
            onBackground = TextPrimary,
            surface = SecondarySurface,
            onSurface = TextPrimary,
            surfaceVariant = ElevatedSurface,
            onSurfaceVariant = TextMuted,
            outline = BorderSubtle
        )
    } else {
        darkColorScheme(
            primary = AccentBlue,
            onPrimary = DominantBg,
            background = DominantBg,
            onBackground = TextPrimary,
            surface = SecondarySurface,
            onSurface = TextPrimary,
            surfaceVariant = ElevatedSurface,
            onSurfaceVariant = TextMuted,
            outline = BorderSubtle
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EcosystemKuTypography,
        content = content
    )
}
