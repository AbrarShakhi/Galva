package com.abrarshakhi.galva.common.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

fun adaptiveColumnCount(preferredColumns: Int, availableWidth: Dp): Int {
    val scaled = (preferredColumns * (availableWidth / ReferenceWidth)).roundToInt()
    return scaled.coerceIn(preferredColumns, MaxColumns)
}

fun albumGridColumns(availableWidth: Dp, availableHeight: Dp): Int {
    val cover =
        minOf(PreferredAlbumCover, availableHeight - AlbumLabelBlock).coerceAtLeast(MinAlbumCover)
    return ceil(availableWidth / cover).toInt().coerceIn(MinAlbumColumns, MaxColumns)
}

private val ReferenceWidth = 360.dp
private const val MaxColumns = 12

private val AlbumLabelBlock = 52.dp
private val PreferredAlbumCover = 200.dp
private val MinAlbumCover = 96.dp
private const val MinAlbumColumns = 2
