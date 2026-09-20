package com.abrarshakhi.galva.common.ui.selection

import com.abrarshakhi.galva.core.media.domain.model.MediaItem

fun List<MediaItem>.selectedBy(selection: SelectionState): List<MediaItem> =
    filter { selection.contains(it.id) }

fun List<MediaItem>.shouldFavorite(): Boolean = isNotEmpty() && any { !it.isFavorite }
