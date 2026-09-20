package com.abrarshakhi.galva.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base for the unidirectional loop every screen follows.
 *
 * Intents arrive on an unbounded channel and are handled one at a time, so a reducer never
 * observes a half-applied state from a concurrent intent. State is a [StateFlow] because it is a
 * value the UI re-reads on every recomposition; effects are a [Channel] because they must fire
 * exactly once and must not survive a configuration change.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    private val intents = Channel<I>(Channel.UNLIMITED)

    /** Current state, for reducers that need to read before they write. */
    protected val currentState: S get() = _state.value

    init {
        viewModelScope.launch {
            intents.consumeAsFlow().collect { intent -> reduce(intent) }
        }
    }

    fun onIntent(intent: I) {
        intents.trySend(intent)
    }

    protected abstract suspend fun reduce(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }
}
