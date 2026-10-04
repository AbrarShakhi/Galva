package com.abrarshakhi.galva.core.ui.selection

import com.abrarshakhi.galva.core.model.MediaItem

fun List<MediaItem>.selectedBy(selection: SelectionState): List<MediaItem> =
    filter { selection.contains(it.id) }

fun List<MediaItem>.shouldFavorite(): Boolean = isNotEmpty() && any { !it.isFavorite }
