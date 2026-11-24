package com.hextapp.vpsadmin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF06070D)
val SurfaceDark = Color(0xFF0E101C)
val SurfaceVariantDark = Color(0xFF151829)
val OutlineGlass = Color(0xFF282D4A)
val PrimaryCyan = Color(0xFF00D4FF)
val AccentPurple = Color(0xFF7B5CFF)
val SecondaryEmerald = Color(0xFF00E5B0)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFE54868)
val TextPrimary = Color(0xFFF4F6FF)
val TextMuted = Color(0xFF9EA4C4)

private val DarkColors = darkColorScheme(
    primary = PrimaryCyan,
    secondary = SecondaryEmerald,
    tertiary = AccentPurple,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineGlass,
    error = DangerRed,
    onPrimary = Color(0xFF001E2E),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
)

@Composable
fun HextappVpsAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
