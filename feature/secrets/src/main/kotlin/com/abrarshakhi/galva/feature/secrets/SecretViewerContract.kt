package com.abrarshakhi.galva.feature.secrets

import com.abrarshakhi.galva.core.model.SecretItem
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState

data class SecretViewerUiState(
    val items: List<SecretItem> = emptyList(),
    val currentIndex: Int = 0,
    val chromeVisible: Boolean = true,
    val isLoading: Boolean = true,
    val work: String? = null,
    val confirmingDelete: Boolean = false,
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val current: SecretItem? get() = items.getOrNull(currentIndex)

    val isExhausted: Boolean get() = !isLoading && items.isEmpty()
}

sealed interface SecretViewerIntent : UiIntent {

    data class PageSettled(val index: Int) : SecretViewerIntent

    data object ChromeToggled : SecretViewerIntent

    data object RestoreRequested : SecretViewerIntent

    data object DeleteRequested : SecretViewerIntent

    data object DeleteConfirmed : SecretViewerIntent

    data object DeleteDismissed : SecretViewerIntent

    data class MessageShown(val id: Long) : SecretViewerIntent
}
