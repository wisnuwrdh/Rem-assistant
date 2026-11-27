package com.ecosystemku.assistant.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isEcosystemLightTheme by mutableStateOf(false)

// ── 60-30-10 Design Tokens (Reactive Dark / Light) ──────────────────────
val DominantBg: Color
    get() = if (isEcosystemLightTheme) Color(0xFFF6F4EE) else Color(0xFF0F0E0C)
val SecondarySurface: Color
    get() = if (isEcosystemLightTheme) Color(0xFFFFFFFF) else Color(0xFF1A1917)
val ElevatedSurface: Color
    get() = if (isEcosystemLightTheme) Color(0xFFECE9E0) else Color(0xFF232220)
val AccentBlue: Color
    get() = if (isEcosystemLightTheme) Color(0xFF2563EB) else Color(0xFF5B9CF6)

val TextPrimary: Color
    get() = if (isEcosystemLightTheme) Color(0xFF181715) else Color(0xFFF0EFE9)
val TextMuted: Color
    get() = if (isEcosystemLightTheme) Color(0xFF63615C) else Color(0xFF9A9994)
val TextSubtle: Color
    get() = if (isEcosystemLightTheme) Color(0xFF8E8B84) else Color(0xFF6B6A67)
val BorderSubtle: Color
    get() = if (isEcosystemLightTheme) Color(0x14000000) else Color(0x0FFFFFFF)
val BorderLight: Color
    get() = if (isEcosystemLightTheme) Color(0x24000000) else Color(0x1EFFFFFF)

// ── Status & Semantic Tokens ────────────────────────────────────────────
val StatusOnline = Color(0xFF4ADE80)
val StatusSpeaking = Color(0xFF60A5FA)
val SemanticSuccess = Color(0xFF22C55E)
val SemanticWarning = Color(0xFFF59E0B)
val SemanticDanger = Color(0xFFEF4444)

// ── Module Accents ──────────────────────────────────────────────────────
val AppDailyOs = Color(0xFF2F8F83)
val AppIdeKu = Color(0xFF6366F1)
val AppKronik = Color(0xFFC9A84C)
val AppCuciMoney = Color(0xFF2F5D8A)
val AppCuciMoneyLight = Color(0xFF5A97D1)
val AppLinkKu = Color(0xFF8B5CF6)
val AppHesych = Color(0xFF10B981)
