package com.abrarshakhi.galva.feature.albums

import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaAction
import com.abrarshakhi.galva.core.ui.actions.MediaActionEvent
import com.abrarshakhi.galva.core.ui.actions.MediaActionsState
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState
import com.abrarshakhi.galva.core.ui.selection.SelectionState

data class AlbumDetailUiState(
    val source: MediaSource,
    val albumName: String = "",
    val items: List<MediaItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val canManage: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteAlbumDialog: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val actions: MediaActionsState = MediaActionsState(),
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == items.size

    val title: String get() = if (selection.isActive) "${selection.count} selected" else albumName
}

sealed interface AlbumDetailIntent : UiIntent {

    data class ToggleSelection(val item: MediaItem) : AlbumDetailIntent

    data object SelectAll : AlbumDetailIntent

    data object ClearSelection : AlbumDetailIntent

    data class Perform(val action: MediaAction) : AlbumDetailIntent

    data class ActionEvent(val event: MediaActionEvent) : AlbumDetailIntent

    data object RemoveFromAlbum : AlbumDetailIntent

    data object RenameRequested : AlbumDetailIntent

    data object RenameDismissed : AlbumDetailIntent

    data class RenameConfirmed(val name: String) : AlbumDetailIntent

    data object DeleteAlbumRequested : AlbumDetailIntent

    data object DeleteAlbumDismissed : AlbumDetailIntent

    data object DeleteAlbumConfirmed : AlbumDetailIntent

    data class MessageShown(val id: Long) : AlbumDetailIntent
}
