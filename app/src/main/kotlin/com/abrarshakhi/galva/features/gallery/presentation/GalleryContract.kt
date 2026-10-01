package com.abrarshakhi.galva.features.gallery.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.common.ui.selection.SelectionState
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.TimelineSection
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.features.secrets.presentation.MoveProgress

data class GalleryUiState(
    val sections: List<TimelineSection> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val showAddToAlbum: Boolean = false,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val showVaultUnlock: Boolean = false,
    val moveProgress: MoveProgress? = null,
) : UiState {

    val items: List<MediaItem> get() = sections.flatMap(TimelineSection::items)

    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()

    val allSelected: Boolean
        get() = selection.isActive && selection.count == items.size
}

sealed interface GalleryIntent : UiIntent {

    data object Refresh : GalleryIntent

    data class MediaTapped(val item: MediaItem) : GalleryIntent

    data class MediaLongPressed(val item: MediaItem) : GalleryIntent

    /** Tapping a day header selects or clears that whole day. */
    data class SectionTapped(val section: TimelineSection) : GalleryIntent

    data object SelectAll : GalleryIntent

    data object ClearSelection : GalleryIntent

    data object ShareSelection : GalleryIntent

    data object FavoriteSelection : GalleryIntent

    data object DeleteSelection : GalleryIntent

    data object AddToAlbumRequested : GalleryIntent

    data object AddToAlbumDismissed : GalleryIntent

    data class AddedToAlbum(val albumName: String) : GalleryIntent

    /** Reported back by the UI once the system delete dialog has been answered. */
    data class DeleteResolved(val ids: List<Long>, val confirmed: Boolean) : GalleryIntent

    data object MoveToSecretsSelection : GalleryIntent

    data object VaultUnlocked : GalleryIntent

    data object VaultUnlockDismissed : GalleryIntent

    /** The delete dialog for a move's originals was answered. */
    data class MoveResolved(val ids: List<Long>, val confirmed: Boolean) : GalleryIntent
}

sealed interface GalleryEffect : UiEffect {

    data class OpenViewer(val source: MediaSource, val mediaId: Long) : GalleryEffect

    data class ShareItems(val uris: List<String>, val mimeTypes: List<String>) : GalleryEffect

    data class ConfirmDelete(val ids: List<Long>, val uris: List<String>) : GalleryEffect

    /** The items are safely in Secrets; the originals now go through the system delete dialog. */
    data class ConfirmMove(val ids: List<Long>, val uris: List<String>) : GalleryEffect

    data class ShowMessage(val text: String) : GalleryEffect
}
