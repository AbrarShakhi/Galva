package com.abrarshakhi.galva.common.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Scales the user's grid-size preference to the width actually available.
 *
 * The setting is expressed for a compact portrait phone. Applied literally in landscape or on a
 * tablet it would stretch four cells across the full width and turn a photo wall into a filmstrip,
 * so the count is scaled to hold cell size roughly constant instead — four columns at 360dp is the
 * same cell size as nine at 810dp.
 *
 * Width is read rather than orientation, so split-screen and freeform windows get the same
 * treatment as a rotation.
 */
/**
 * Kept a pure function so the scaling is testable without a composition or a device that can be
 * rotated. Callers measure their own width — the window's is wrong once a rail takes part of it.
 */
fun adaptiveColumnCount(preferredColumns: Int, availableWidth: Dp): Int {
    val scaled = (preferredColumns * (availableWidth / ReferenceWidth)).roundToInt()
    // Never fewer than the user asked for: a narrow window should not silently override them.
    return scaled.coerceIn(preferredColumns, MaxColumns)
}

/**
 * Column count for the albums grid, chosen so a whole tile — square cover plus its name and count —
 * fits the height available.
 *
 * Width alone is not enough here. In landscape a cover sized purely by width is taller than the
 * viewport, which pushes every album's name below the fold and leaves a wall of unlabelled
 * thumbnails, so the cover is capped by height and the columns follow from that.
 */
fun albumGridColumns(availableWidth: Dp, availableHeight: Dp): Int {
    val cover = minOf(PreferredAlbumCover, availableHeight - AlbumLabelBlock)
        .coerceAtLeast(MinAlbumCover)
    return ceil(availableWidth / cover).toInt().coerceIn(MinAlbumColumns, MaxColumns)
}

private val ReferenceWidth = 360.dp
private const val MaxColumns = 12

/** Room the name and count need beneath a cover. */
private val AlbumLabelBlock = 52.dp
private val PreferredAlbumCover = 200.dp
private val MinAlbumCover = 96.dp
private const val MinAlbumColumns = 2
