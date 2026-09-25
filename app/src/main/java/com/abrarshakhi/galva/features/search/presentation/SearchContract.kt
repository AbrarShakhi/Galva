package com.abrarshakhi.galva.features.search.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.common.ui.selection.SelectionState
import com.abrarshakhi.galva.core.media.domain.model.MediaFilter
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.features.secrets.presentation.MoveProgress

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val results: List<MediaItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val showAddToAlbum: Boolean = false,
    val isSearching: Boolean = false,
    val showVaultUnlock: Boolean = false,
    val moveProgress: MoveProgress? = null,
) : UiState {

    /** A bare filter with no text is still a search — "all videos" is a useful query. */
    val isActive: Boolean get() = query.isNotBlank() || filter != MediaFilter.ALL

    val hasNoResults: Boolean get() = isActive && !isSearching && results.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == results.size
}

sealed interface SearchIntent : UiIntent {

    data class QueryChanged(val query: String) : SearchIntent

    data class FilterChanged(val filter: MediaFilter) : SearchIntent

    data object QueryCleared : SearchIntent

    data class MediaTapped(val item: MediaItem) : SearchIntent

    data class MediaLongPressed(val item: MediaItem) : SearchIntent

    data object SelectAll : SearchIntent

    data object ClearSelection : SearchIntent

    data object ShareSelection : SearchIntent

    data object FavoriteSelection : SearchIntent

    data object DeleteSelection : SearchIntent

    data object AddToAlbumRequested : SearchIntent

    data object AddToAlbumDismissed : SearchIntent

    data class AddedToAlbum(val albumName: String) : SearchIntent

    data class DeleteResolved(val ids: List<Long>, val confirmed: Boolean) : SearchIntent

    data object MoveToSecretsSelection : SearchIntent

    data object VaultUnlocked : SearchIntent

    data object VaultUnlockDismissed : SearchIntent

    data class MoveResolved(val ids: List<Long>, val confirmed: Boolean) : SearchIntent
}

sealed interface SearchEffect : UiEffect {

    data class OpenViewer(val source: MediaSource, val mediaId: Long) : SearchEffect

    data class ShareItems(val uris: List<String>, val mimeTypes: List<String>) : SearchEffect

    data class ConfirmDelete(val ids: List<Long>, val uris: List<String>) : SearchEffect

    data class ConfirmMove(val ids: List<Long>, val uris: List<String>) : SearchEffect

    data class ShowMessage(val text: String) : SearchEffect
}
