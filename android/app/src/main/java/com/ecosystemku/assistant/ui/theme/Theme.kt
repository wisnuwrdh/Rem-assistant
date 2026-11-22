package com.ecosystemku.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
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

@Composable
fun EcosystemKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = EcosystemKuTypography,
        content = content
    )
}
