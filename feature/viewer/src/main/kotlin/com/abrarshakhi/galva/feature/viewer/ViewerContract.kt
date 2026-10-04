package com.abrarshakhi.galva.feature.viewer

import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.ui.actions.MediaAction
import com.abrarshakhi.galva.core.ui.actions.MediaActionEvent
import com.abrarshakhi.galva.core.ui.actions.MediaActionsState
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState

data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val chromeVisible: Boolean = true,
    val isLoading: Boolean = true,
    val actions: MediaActionsState = MediaActionsState(),
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val current: MediaItem? get() = items.getOrNull(currentIndex)

    val isExhausted: Boolean get() = !isLoading && items.isEmpty()
}

sealed interface ViewerIntent : UiIntent {

    data class PageSettled(val index: Int) : ViewerIntent

    data object ChromeToggled : ViewerIntent

    data class Perform(val action: MediaAction) : ViewerIntent

    data class ActionEvent(val event: MediaActionEvent) : ViewerIntent

    data class MessageShown(val id: Long) : ViewerIntent
}
