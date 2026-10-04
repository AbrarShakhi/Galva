package com.abrarshakhi.galva.feature.search

import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.MediaFilter
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaAction
import com.abrarshakhi.galva.core.ui.actions.MediaActionEvent
import com.abrarshakhi.galva.core.ui.actions.MediaActionsState
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.mvi.UiIntent
import com.abrarshakhi.galva.core.ui.mvi.UiState
import com.abrarshakhi.galva.core.ui.selection.SelectionState

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val results: List<MediaItem> = emptyList(),
    val selection: SelectionState = SelectionState(),
    val columns: Int = AppSettings.DEFAULT_COLUMNS,
    val isSearching: Boolean = false,
    val actions: MediaActionsState = MediaActionsState(),
    val messages: List<UserMessage> = emptyList(),
) : UiState {

    val isActive: Boolean get() = query.isNotBlank() || filter != MediaFilter.ALL

    val hasNoResults: Boolean get() = isActive && !isSearching && results.isEmpty()

    val allSelected: Boolean get() = selection.isActive && selection.count == results.size

    val source: MediaSource get() = MediaSource.Query(query, filter)
}

sealed interface SearchIntent : UiIntent {

    data class QueryChanged(val query: String) : SearchIntent

    data class FilterChanged(val filter: MediaFilter) : SearchIntent

    data object QueryCleared : SearchIntent

    data class ToggleSelection(val item: MediaItem) : SearchIntent

    data object SelectAll : SearchIntent

    data object ClearSelection : SearchIntent

    data class Perform(val action: MediaAction) : SearchIntent

    data class ActionEvent(val event: MediaActionEvent) : SearchIntent

    data class MessageShown(val id: Long) : SearchIntent
}
