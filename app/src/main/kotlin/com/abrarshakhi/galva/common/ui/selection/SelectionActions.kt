package com.abrarshakhi.galva.common.ui.selection

import com.abrarshakhi.galva.core.media.domain.model.MediaItem

/** Items currently selected, in the order they appear on screen. */
fun List<MediaItem>.selectedBy(selection: SelectionState): List<MediaItem> =
    filter { selection.contains(it.id) }

/**
 * Whether the favourite action should set or clear the flag.
 *
 * A mixed selection favourites everything, matching how the toggle reads: the button only turns
 * "off" once every selected item is already a favourite.
 */
fun List<MediaItem>.shouldFavorite(): Boolean = isNotEmpty() && any { !it.isFavorite }
