package com.hextapp.vpsadmin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B0F17)
val SurfaceDark = Color(0xFF131A26)
val SurfaceVariantDark = Color(0xFF1C2536)
val PrimaryCyan = Color(0xFF38BDF8)
val SecondaryEmerald = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF1F5F9)
val TextMuted = Color(0xFF94A3B8)

private val DarkColors = darkColorScheme(
    primary = PrimaryCyan,
    secondary = SecondaryEmerald,
    tertiary = WarningAmber,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
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
