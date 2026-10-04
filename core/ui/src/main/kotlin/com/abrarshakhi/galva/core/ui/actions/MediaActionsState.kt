package com.abrarshakhi.galva.core.ui.actions

import androidx.compose.runtime.Immutable
import com.abrarshakhi.galva.core.domain.ShareRequest
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.vault.MoveProgress
import java.util.concurrent.atomic.AtomicLong

enum class MediaAction {
    Share,
    Favorite,
    AddToAlbum,
    MoveToSecrets,
    Delete,
}

@Immutable
data class ConsentRequest(
    val uris: List<String>,
    val launched: Boolean = false,
    val id: Long = nextRequestId.incrementAndGet(),
)

private val nextRequestId = AtomicLong()

@Immutable
data class MediaActionsState(
    val consent: ConsentRequest? = null,
    val share: ShareRequest? = null,
    val addToAlbumIds: List<Long>? = null,
    val unlockingVault: Boolean = false,
    val progress: MoveProgress? = null,
)

sealed interface MediaActionEvent {

    data class ConsentLaunched(val requestId: Long) : MediaActionEvent

    data class ConsentResolved(val requestId: Long, val confirmed: Boolean) : MediaActionEvent

    data object ShareLaunched : MediaActionEvent

    data object AddToAlbumDismissed : MediaActionEvent

    data class AddedToAlbum(val albumName: String) : MediaActionEvent

    data object VaultUnlocked : MediaActionEvent

    data object VaultUnlockDismissed : MediaActionEvent
}

data class MediaActionResult(
    val message: UserMessage? = null,
    val completed: Boolean = false,
) {
    companion object {
        val None = MediaActionResult()
    }
}
