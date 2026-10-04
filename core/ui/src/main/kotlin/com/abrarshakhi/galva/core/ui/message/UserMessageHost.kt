package com.abrarshakhi.galva.core.ui.message

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class UserMessageHost internal constructor(
    val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(message: UserMessage) {
        scope.launch { snackbarHostState.showSnackbar(message.text) }
    }
}

val LocalUserMessageHost = staticCompositionLocalOf<UserMessageHost> {
    error("No UserMessageHost provided")
}

@Composable
fun rememberUserMessageHost(): UserMessageHost {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, scope) { UserMessageHost(snackbarHostState, scope) }
}

@Composable
fun UserMessagesEffect(messages: List<UserMessage>, onShown: (id: Long) -> Unit) {
    val host = LocalUserMessageHost.current
    val currentOnShown by rememberUpdatedState(onShown)
    val message = messages.firstOrNull() ?: return
    LaunchedEffect(message.id) {
        host.show(message)
        currentOnShown(message.id)
    }
}
