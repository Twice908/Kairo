package com.kairo.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F0E6),
    onPrimaryContainer = Color(0xFF123D34),
    secondary = Color(0xFF52665D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E9E2),
    onSecondaryContainer = Color(0xFF283A32),
    tertiary = Color(0xFF9A5B21),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC0),
    onTertiaryContainer = Color(0xFF341A00),
    background = Color(0xFFF6F6F2),
    onBackground = Color(0xFF191D1A),
    surface = Color(0xFFF6F6F2),
    onSurface = Color(0xFF191D1A),
    surfaceVariant = Color(0xFFE8EBE5),
    onSurfaceVariant = Color(0xFF454D47),
    outline = Color(0xFF747B74),
    outlineVariant = Color(0xFFD5DAD3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D9C5),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF145143),
    onPrimaryContainer = Color(0xFFC1EED9),
    secondary = Color(0xFFB9CCC1),
    onSecondary = Color(0xFF24352D),
    secondaryContainer = Color(0xFF3A4B42),
    onSecondaryContainer = Color(0xFFD5E8DC),
    tertiary = Color(0xFFFFB978),
    onTertiary = Color(0xFF512400),
    tertiaryContainer = Color(0xFF713700),
    onTertiaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF111512),
    onBackground = Color(0xFFE2E5DF),
    surface = Color(0xFF111512),
    onSurface = Color(0xFFE2E5DF),
    surfaceVariant = Color(0xFF303832),
    onSurfaceVariant = Color(0xFFC0C8C0),
    outline = Color(0xFF89928A),
    outlineVariant = Color(0xFF414A43),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val KairoTypography = Typography().let { defaults ->
    defaults.copy(
        displaySmall = defaults.displaySmall.copy(fontSize = 36.sp, lineHeight = 42.sp),
        headlineLarge = defaults.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = defaults.headlineMedium.copy(fontSize = 25.sp, lineHeight = 31.sp),
        titleLarge = defaults.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        bodyLarge = defaults.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = defaults.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = defaults.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
    )
}

object KairoSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
    val xxLarge = 48.dp
}

object KairoSizes {
    val touchTarget = 48.dp
    val miniPlayerArtwork = 48.dp
    val trackArtwork = 52.dp
    val iconSmall = 18.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val contentMaxWidth = 720.dp
}

object KairoCorners {
    val small = 8.dp
    val medium = 14.dp
    val large = 22.dp
}

object KairoElevation {
    val none = 0.dp
    val low = 2.dp
    val floating = 6.dp
}

@Composable
fun KairoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = KairoTypography,
        shapes = Shapes(),
        content = content,
    )
}