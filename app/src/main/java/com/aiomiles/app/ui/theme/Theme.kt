package com.aiomiles.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NavyPrimary = Color(0xFF0F2537)
val NavyContainer = Color(0xFFD8E6F3)
val TealSecondary = Color(0xFF006D77)
val GoldAccent = Color(0xFFD99B26)
val SurfaceLight = Color(0xFFF6F8FA)
val CardLight = Color(0xFFFFFFFF)

val GarudaColor = Color(0xFF00778B)
val KrisFlyerColor = Color(0xFF1B365D)
val AsiaMilesColor = Color(0xFF006564)

private val LightColors = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = NavyPrimary,
    secondary = TealSecondary,
    onSecondary = Color.White,
    tertiary = GoldAccent,
    background = SurfaceLight,
    surface = CardLight,
    onBackground = Color(0xFF101820),
    onSurface = Color(0xFF101820),
    surfaceVariant = Color(0xFFE9EFF4),
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0B192C),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF5EEAD4),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B1118),
    surface = Color(0xFF141D28),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun AIOMilesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
