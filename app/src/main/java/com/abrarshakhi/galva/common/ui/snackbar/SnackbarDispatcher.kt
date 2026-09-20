package com.abrarshakhi.galva.common.ui.snackbar

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class SnackbarMessage(
    val text: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)


class SnackbarDispatcher {

    private val _messages = Channel<SnackbarMessage>(Channel.BUFFERED)
    val messages: Flow<SnackbarMessage> = _messages.receiveAsFlow()

    fun show(text: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        _messages.trySend(SnackbarMessage(text, duration))
    }

    fun showError(text: String) = show(text, SnackbarDuration.Long)
}