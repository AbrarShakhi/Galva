package com.abrarshakhi.galva.features.viewer.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.features.secrets.presentation.MoveProgress

data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val chromeVisible: Boolean = true,
    val isLoading: Boolean = true,
    val showVaultUnlock: Boolean = false,
    val moveProgress: MoveProgress? = null,
) : UiState {

    val current: MediaItem? get() = items.getOrNull(currentIndex)
}

sealed interface ViewerIntent : UiIntent {

    data class PageSettled(val index: Int) : ViewerIntent

    data object ChromeToggled : ViewerIntent

    data object FavoriteToggled : ViewerIntent

    data object ShareRequested : ViewerIntent

    data object DeleteRequested : ViewerIntent

    data class DeleteResolved(val ids: List<Long>, val confirmed: Boolean) : ViewerIntent

    data object MoveToSecretsRequested : ViewerIntent

    data object VaultUnlocked : ViewerIntent

    data object VaultUnlockDismissed : ViewerIntent

    data class MoveResolved(val ids: List<Long>, val confirmed: Boolean) : ViewerIntent
}

sealed interface ViewerEffect : UiEffect {

    data class ShareItem(val uris: List<String>, val mimeTypes: List<String>) : ViewerEffect

    data class ConfirmDelete(val ids: List<Long>, val uris: List<String>) : ViewerEffect

    data class ConfirmMove(val ids: List<Long>, val uris: List<String>) : ViewerEffect

    data class ShowMessage(val text: String) : ViewerEffect

    /** The last item in the collection was removed, so there is nothing left to show. */
    data object Close : ViewerEffect
}
