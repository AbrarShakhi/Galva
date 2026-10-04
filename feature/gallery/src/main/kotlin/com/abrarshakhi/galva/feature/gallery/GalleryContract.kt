package com.abrarshakhi.galva.feature.gallery

import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.TimelineSection
import com.abrarshakhi.galva.core.ui.actions.MediaAction
import com.abrarshakhi.galva.core.ui.actions.MediaActionEvent
import com.abrarshakhi.galva.core.ui.actions.MediaActionsState
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState
import com.abrarshakhi.galva.core.ui.selection.SelectionState

data class GalleryUiState(
    val sections: List<TimelineSection> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isRefreshing: Boolean = false,
    val actions: MediaActionsState = MediaActionsState(),
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val items: List<MediaItem> get() = sections.flatMap(TimelineSection::items)

    val selectedItems: List<MediaItem> get() = items.filter { selection.contains(it.id) }

    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()

    val allSelected: Boolean
        get() = selection.isActive && selection.count == items.size
}

sealed interface GalleryIntent : UiIntent {

    data object Refresh : GalleryIntent

    data class ToggleSelection(val item: MediaItem) : GalleryIntent

    data class ToggleSection(val section: TimelineSection) : GalleryIntent

    data object SelectAll : GalleryIntent

    data object ClearSelection : GalleryIntent

    data class Perform(val action: MediaAction) : GalleryIntent

    data class ActionEvent(val event: MediaActionEvent) : GalleryIntent

    data class MessageShown(val id: Long) : GalleryIntent
}
