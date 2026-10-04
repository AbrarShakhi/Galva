package com.abrarshakhi.galva.feature.viewer

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.domain.ObserveMediaUseCase
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionResult
import com.abrarshakhi.galva.core.ui.actions.MediaActionsStateHolder
import com.abrarshakhi.galva.core.ui.message.shown
import com.abrarshakhi.galva.core.ui.message.withMessage
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ViewerViewModel(
    source: MediaSource,
    initialMediaId: Long,
    observeMedia: ObserveMediaUseCase,
    private val mediaActions: MediaActionsStateHolder,
) : MviViewModel<ViewerUiState, ViewerIntent>(ViewerUiState()) {

    private var anchorId: Long = initialMediaId

    init {
        observeMedia(source)
            .onEach { items ->
                if (items.isEmpty()) {
                    setState { copy(items = items, isLoading = false) }
                    return@onEach
                }
                val index = items.indexOfFirst { it.id == anchorId }
                    .takeIf { it >= 0 }
                    ?: currentState.currentIndex.coerceIn(items.indices)
                anchorId = items[index].id
                setState { copy(items = items, currentIndex = index, isLoading = false) }
            }
            .launchIn(viewModelScope)

        mediaActions.state
            .onEach { actions -> setState { copy(actions = actions) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: ViewerIntent) {
        when (intent) {
            is ViewerIntent.PageSettled -> {
                val item = currentState.items.getOrNull(intent.index) ?: return
                anchorId = item.id
                setState { copy(currentIndex = intent.index) }
            }

            ViewerIntent.ChromeToggled -> setState { copy(chromeVisible = !chromeVisible) }

            is ViewerIntent.Perform -> apply(
                mediaActions.perform(
                    action = intent.action,
                    items = listOfNotNull(currentState.current),
                    onRemoving = ::anchorPastRemoval,
                ),
            )

            is ViewerIntent.ActionEvent ->
                apply(mediaActions.handle(intent.event, onRemoving = ::anchorPastRemoval))

            is ViewerIntent.MessageShown ->
                setState { copy(messages = messages.shown(intent.id)) }
        }
    }

    private fun apply(result: MediaActionResult) {
        setState { copy(messages = messages.withMessage(result.message)) }
    }

    private fun anchorPastRemoval(ids: List<Long>) {
        val items = currentState.items
        val removed = ids.toSet()
        val nextAnchor = items.drop(currentState.currentIndex + 1).firstOrNull { it.id !in removed }
            ?: items.take(currentState.currentIndex).lastOrNull { it.id !in removed }
        anchorId = nextAnchor?.id ?: NO_ANCHOR
    }

    private companion object {
        const val NO_ANCHOR = -1L
    }
}
