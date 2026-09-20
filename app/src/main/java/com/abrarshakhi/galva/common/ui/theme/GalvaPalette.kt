package com.abrarshakhi.galva.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Surface fills that Material's colour roles cannot express.
 *
 * Ente's navigation bar sits on `fillLight` and marks the active tab with `fillDark`, and in dark
 * mode that active fill is *darker* than the bar it sits on. Material's container roles only step
 * one way — lighter as elevation rises — so these two live alongside the scheme rather than inside
 * it. Values are Ente's own (`lib/theme/colors.dart`).
 */
@Immutable
data class GalvaPalette(
    val navBarContainer: Color,
    val navBarActivePill: Color,
)

private val LightPalette = GalvaPalette(
    navBarContainer = Color(0xFFFFFFFF),
    navBarActivePill = Color(0xFFF5F5F5),
)

private val DarkPalette = GalvaPalette(
    navBarContainer = Color(0xFF212121),
    navBarActivePill = Color(0xFF0A0A0A),
)

internal fun galvaPaletteFor(darkTheme: Boolean): GalvaPalette =
    if (darkTheme) DarkPalette else LightPalette

/**
 * Defaults to the dark palette so a stray read outside [GalvaTheme] is visibly wrong in light mode
 * rather than silently plausible.
 */
val LocalGalvaPalette = staticCompositionLocalOf { DarkPalette }
