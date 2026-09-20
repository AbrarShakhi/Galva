package com.abrarshakhi.galva.common.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing the gallery surfaces share.
 *
 * Grid gaps are deliberately tiny: a photo wall reads as one surface when the seams are hairlines,
 * and every pixel spent on padding is a pixel not spent on the image.
 */
object GalvaDimens {
    val GridSpacing = 2.dp
    val GridMinCell = 108.dp

    val ScreenPadding = 16.dp
    val SectionSpacing = 20.dp
    val ItemSpacing = 12.dp

    val AlbumCorner = 8.dp
    val AlbumGridSpacing = 12.dp
}
