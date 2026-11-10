package com.ecoalertum.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Design-system colours mapped onto Material 3 roles, so every Material component picks them up. */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = TealTint,
    onPrimaryContainer = PrimaryTeal,
    secondary = LeafGreen,
    onSecondary = Color.White,
    secondaryContainer = LeafTint,
    onSecondaryContainer = Color(0xFF12492C),
    tertiary = AlertAmber,
    onTertiary = Ink,
    tertiaryContainer = AmberTint,
    onTertiaryContainer = AmberInk,
    error = SeverityDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFBE4DF),
    onErrorContainer = Color(0xFF6E140E),
    background = Background,
    onBackground = Ink,
    surface = Background,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3E9E7),
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF2F0),
    surfaceContainerHighest = Color(0xFFE8EDEB),
    outline = Color(0xFF7C8A87),
    outlineVariant = Line,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealDark,
    onPrimary = Color(0xFF00201C),
    primaryContainer = TealTintDark,
    onPrimaryContainer = Color(0xFFBDEDE4),
    secondary = Color(0xFF7FD49B),
    onSecondary = Color(0xFF003919),
    tertiary = AlertAmber,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF5C3A08),
    onTertiaryContainer = AmberTint,
    error = Color(0xFFFFB4A9),
    background = BackgroundDark,
    onBackground = InkDark,
    surface = BackgroundDark,
    onSurface = InkDark,
    onSurfaceVariant = Color(0xFFB4C4C0),
    surfaceContainerLow = Color(0xFF172120),
    surfaceContainer = Color(0xFF1B2625),
    outlineVariant = Color(0xFF34423F),
)

/**
 * App theme. Android's wallpaper colours (dynamic colour) are off by default so the app always
 * shows the EcoAlert UM design system.
 */
@Composable
fun EcoAlertUMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
