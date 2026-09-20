package com.abrarshakhi.galva.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LocalGalvaPalette = staticCompositionLocalOf { DarkPalette }
