package com.filmlightmeter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Палитра под старый экспонометр: кремовые цифры на тёмно-оливковом корпусе
val LeatherDark = Color(0xFF1A1814)
val LeatherBrown = Color(0xFF2B241C)
val CreamDial = Color(0xFFE8DFC4)
val CreamSoft = Color(0xFFCDBF9A)
val NeedleRed = Color(0xFFB4442F)
val BrassAccent = Color(0xFFC59B58)
val EngravedGreen = Color(0xFF4F5B3C)
val ShadowBlack = Color(0xFF0D0B08)

private val RetroDarkColors = darkColorScheme(
    primary = BrassAccent,
    onPrimary = LeatherDark,
    secondary = NeedleRed,
    onSecondary = CreamDial,
    tertiary = EngravedGreen,
    background = LeatherDark,
    onBackground = CreamDial,
    surface = LeatherBrown,
    onSurface = CreamDial,
    surfaceVariant = Color(0xFF231D16),
    onSurfaceVariant = CreamSoft,
    error = Color(0xFFD66A55),
    outline = BrassAccent
)

private val RetroLightColors = lightColorScheme(
    primary = Color(0xFF6D4E20),
    onPrimary = CreamDial,
    secondary = NeedleRed,
    onSecondary = CreamDial,
    tertiary = EngravedGreen,
    background = CreamDial,
    onBackground = LeatherDark,
    surface = CreamSoft,
    onSurface = LeatherDark,
    outline = Color(0xFF6D4E20)
)

val RetroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 1.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun FilmLightMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) RetroDarkColors else RetroDarkColors // всегда ретро-тёмная
    MaterialTheme(
        colorScheme = colors,
        typography = RetroTypography,
        content = content
    )
}
