package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.common.mvi.UiEffect
import com.abrarshakhi.galva.common.mvi.UiIntent
import com.abrarshakhi.galva.common.mvi.UiState
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem

data class SecretViewerUiState(
    val items: List<SecretItem> = emptyList(),
    val currentIndex: Int = 0,
    val chromeVisible: Boolean = true,
    val isLoading: Boolean = true,
    val work: String? = null,
    val confirmingDelete: Boolean = false,
) : UiState {

    val current: SecretItem? get() = items.getOrNull(currentIndex)
}

sealed interface SecretViewerIntent : UiIntent {

    data class PageSettled(val index: Int) : SecretViewerIntent

    data object ChromeToggled : SecretViewerIntent

    data object RestoreRequested : SecretViewerIntent

    data object DeleteRequested : SecretViewerIntent

    data object DeleteConfirmed : SecretViewerIntent

    data object DeleteDismissed : SecretViewerIntent
}

sealed interface SecretViewerEffect : UiEffect {

    data class ShowMessage(val text: String) : SecretViewerEffect

    /** The vault locked, or nothing is left in it. */
    data object Close : SecretViewerEffect
}
