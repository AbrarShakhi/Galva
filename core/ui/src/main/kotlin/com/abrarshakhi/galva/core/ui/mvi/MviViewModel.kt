package com.abrarshakhi.galva.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MviViewModel<S : UiState, I : UiIntent>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val intents = Channel<I>(Channel.UNLIMITED)

    protected val currentState: S get() = _state.value

    init {
        viewModelScope.launch {
            for (intent in intents) reduce(intent)
        }
    }

    fun onIntent(intent: I) {
        intents.trySend(intent)
    }

    protected abstract suspend fun reduce(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }
}
