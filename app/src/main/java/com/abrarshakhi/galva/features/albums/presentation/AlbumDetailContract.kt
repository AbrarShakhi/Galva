package com.abrarshakhi.galva.features.albums.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.common.ui.selection.SelectionState
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.settings.domain.AppSettings

data class AlbumDetailUiState(
    val albumName: String = "",
    val items: List<MediaItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val showAddToAlbum: Boolean = false,
    /** Rename, delete and remove-from only apply to albums this app owns. */
    val canManage: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteAlbumDialog: Boolean = false,
    val isLoading: Boolean = true,
) : UiState {

    val isEmpty: Boolean get() = !isLoading && items.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == items.size

    /** Title reflects the selection while one is active, as it does on the timeline. */
    val title: String get() = if (selection.isActive) "${selection.count} selected" else albumName
}

sealed interface AlbumDetailIntent : UiIntent {

    data class MediaTapped(val item: MediaItem) : AlbumDetailIntent

    data class MediaLongPressed(val item: MediaItem) : AlbumDetailIntent

    data object SelectAll : AlbumDetailIntent

    data object ClearSelection : AlbumDetailIntent

    data object ShareSelection : AlbumDetailIntent

    data object FavoriteSelection : AlbumDetailIntent

    data object DeleteSelection : AlbumDetailIntent

    data object AddToAlbumRequested : AlbumDetailIntent

    data object AddToAlbumDismissed : AlbumDetailIntent

    data class AddedToAlbum(val albumName: String) : AlbumDetailIntent

    data object RenameRequested : AlbumDetailIntent

    data object RenameDismissed : AlbumDetailIntent

    data class RenameConfirmed(val name: String) : AlbumDetailIntent

    data object DeleteAlbumRequested : AlbumDetailIntent

    data object DeleteAlbumDismissed : AlbumDetailIntent

    data object DeleteAlbumConfirmed : AlbumDetailIntent

    data object RemoveFromAlbum : AlbumDetailIntent

    data class DeleteResolved(val ids: List<Long>, val confirmed: Boolean) : AlbumDetailIntent
}

sealed interface AlbumDetailEffect : UiEffect {

    data class OpenViewer(val source: MediaSource, val mediaId: Long) : AlbumDetailEffect

    data class ShareItems(val uris: List<String>, val mimeTypes: List<String>) : AlbumDetailEffect

    data class ConfirmDelete(val ids: List<Long>, val uris: List<String>) : AlbumDetailEffect

    data class ShowMessage(val text: String) : AlbumDetailEffect

    /** The album no longer exists, so there is nothing left to show. */
    data object AlbumDeleted : AlbumDetailEffect
}
