package com.abrarshakhi.galva.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val GalvaDarkColors = darkColorScheme(
    primary = GalvaGreen,
    onPrimary = Ink,
    primaryContainer = GalvaGreenContainer,
    onPrimaryContainer = GalvaGreen,
    secondary = GalvaGreen,
    onSecondary = Ink,
    secondaryContainer = InkElevatedHighest,
    onSecondaryContainer = TextOnInk,
    tertiary = GalvaGreen,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextOnInk,
    surface = Ink,
    onSurface = TextOnInk,
    surfaceVariant = InkElevated,
    onSurfaceVariant = TextOnInkMuted,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkLow,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkElevatedHigh,
    surfaceContainerHighest = InkElevatedHighest,
    outline = InkOutline,
    outlineVariant = InkElevatedHighest,
    error = DangerRed,
    onError = Ink,
)

private val GalvaLightColors = lightColorScheme(
    primary = GalvaGreenDark,
    onPrimary = Paper,
    primaryContainer = GalvaGreenContainerLight,
    onPrimaryContainer = GalvaGreenDark,
    secondary = GalvaGreenDark,
    onSecondary = Paper,
    secondaryContainer = PaperElevatedHighest,
    onSecondaryContainer = TextOnPaper,
    tertiary = GalvaGreenDark,
    onTertiary = Paper,
    background = Paper,
    onBackground = TextOnPaper,
    surface = Paper,
    onSurface = TextOnPaper,
    surfaceVariant = PaperElevated,
    onSurfaceVariant = TextOnPaperMuted,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = PaperElevated,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperElevatedHigh,
    surfaceContainerHighest = PaperElevatedHighest,
    outline = PaperOutline,
    outlineVariant = PaperElevatedHighest,
    error = DangerRed,
    onError = Paper,
)

/**
 * Dynamic colour is intentionally not offered: wallpaper-derived hues would tint the chrome
 * surrounding the user's photos and shift the meaning of the green selection accent.
 */
@Composable
fun GalvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGalvaPalette provides galvaPaletteFor(darkTheme)) {
        MaterialTheme(
            colorScheme = if (darkTheme) GalvaDarkColors else GalvaLightColors,
            typography = Typography,
            content = content,
        )
    }
}
