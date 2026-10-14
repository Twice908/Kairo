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
    primary = Color(0xFFB63E32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE1DA),
    onPrimaryContainer = Color(0xFF481B15),
    secondary = Color(0xFF286E65),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5EEE8),
    onSecondaryContainer = Color(0xFF123B35),
    tertiary = Color(0xFF8B641C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E8C9),
    onTertiaryContainer = Color(0xFF302000),
    background = Color(0xFFF4F5F1),
    onBackground = Color(0xFF1A211E),
    surface = Color(0xFFF9FAF7),
    onSurface = Color(0xFF1A211E),
    surfaceVariant = Color(0xFFE8ECE7),
    onSurfaceVariant = Color(0xFF46514C),
    outline = Color(0xFF737D77),
    outlineVariant = Color(0xFFD2D9D3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A7),
    onPrimary = Color(0xFF651B13),
    primaryContainer = Color(0xFF8F2A20),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFF9AD2C5),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF1C5148),
    onSecondaryContainer = Color(0xFFB6EEE1),
    tertiary = Color(0xFFE7C77E),
    onTertiary = Color(0xFF3E2F00),
    tertiaryContainer = Color(0xFF5A470F),
    onTertiaryContainer = Color(0xFFF3E8C9),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE2E8E2),
    surface = Color(0xFF151C18),
    onSurface = Color(0xFFE2E8E2),
    surfaceVariant = Color(0xFF29332D),
    onSurfaceVariant = Color(0xFFC1CCC4),
    outline = Color(0xFF89958D),
    outlineVariant = Color(0xFF414C45),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val KairoTypography = Typography().let { defaults ->
    defaults.copy(
        displaySmall = defaults.displaySmall.copy(fontSize = 38.sp, lineHeight = 43.sp),
        headlineLarge = defaults.headlineLarge.copy(fontSize = 31.sp, lineHeight = 37.sp),
        headlineMedium = defaults.headlineMedium.copy(fontSize = 26.sp, lineHeight = 32.sp),
        titleLarge = defaults.titleLarge.copy(fontSize = 19.sp, lineHeight = 25.sp),
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
    val medium = 12.dp
    val large = 20.dp
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