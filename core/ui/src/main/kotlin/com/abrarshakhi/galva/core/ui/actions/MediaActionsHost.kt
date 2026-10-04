package com.abrarshakhi.galva.core.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.abrarshakhi.galva.core.domain.ShareRequest
import com.abrarshakhi.galva.core.ui.album.AddToAlbumSheet
import com.abrarshakhi.galva.core.ui.media.MediaConsentEffect
import com.abrarshakhi.galva.core.ui.media.MediaSharing
import com.abrarshakhi.galva.core.ui.vault.BlockingProgress
import com.abrarshakhi.galva.core.ui.vault.VaultUnlockSheet

@Composable
fun MediaActionsHost(state: MediaActionsState, onEvent: (MediaActionEvent) -> Unit) {
    MediaConsentEffect(
        request = state.consent,
        onLaunched = { id -> onEvent(MediaActionEvent.ConsentLaunched(id)) },
        onResult = { id, confirmed -> onEvent(MediaActionEvent.ConsentResolved(id, confirmed)) },
    )

    ShareEffect(request = state.share, onLaunched = { onEvent(MediaActionEvent.ShareLaunched) })

    state.addToAlbumIds?.let { ids ->
        AddToAlbumSheet(
            mediaIds = ids,
            onDismiss = { onEvent(MediaActionEvent.AddToAlbumDismissed) },
            onAdded = { name -> onEvent(MediaActionEvent.AddedToAlbum(name)) },
        )
    }

    if (state.unlockingVault) {
        VaultUnlockSheet(
            onDismiss = { onEvent(MediaActionEvent.VaultUnlockDismissed) },
            onUnlocked = { onEvent(MediaActionEvent.VaultUnlocked) },
        )
    }

    state.progress?.let { BlockingProgress(it.label, it.fraction) }
}

@Composable
private fun ShareEffect(request: ShareRequest?, onLaunched: () -> Unit) {
    val context = LocalContext.current
    if (request == null) return
    LaunchedEffect(request) {
        MediaSharing.chooserFor(request.uris, request.mimeTypes)?.let(context::startActivity)
        onLaunched()
    }
}
