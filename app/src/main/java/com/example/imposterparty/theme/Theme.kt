package com.example.imposterparty.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ImposterPrimary,
    onPrimary = TextOnPrimary,
    primaryContainer = ImposterPrimaryDark,
    onPrimaryContainer = ImposterPrimaryLight,
    secondary = ImposterSecondary,
    onSecondary = DarkBackground,
    secondaryContainer = ImposterSecondaryDark,
    onSecondaryContainer = ImposterSecondaryLight,
    tertiary = ImposterTertiary,
    onTertiary = DarkBackground,
    tertiaryContainer = ImposterTertiaryDark,
    onTertiaryContainer = ImposterTertiaryLight,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,
    error = DangerRed,
    onError = TextOnPrimary,
    outline = TextOnDarkSecondary,
    outlineVariant = DarkSurfaceHigh,
)

@Composable
fun ImposterPartyTheme(
    content: @Composable () -> Unit,
) {
    // Always dark theme for the party game aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
