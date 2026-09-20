package com.abrarshakhi.galva.core.media.domain.model

/** Progress of mirroring MediaStore into the local index. */
sealed interface SyncState {

    data object Idle : SyncState

    data object Syncing : SyncState

    data class Failed(val reason: String) : SyncState
}
