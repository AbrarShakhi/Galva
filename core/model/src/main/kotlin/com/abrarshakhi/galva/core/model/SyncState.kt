package com.abrarshakhi.galva.core.model

sealed interface SyncState {

    data object Idle : SyncState

    data object Syncing : SyncState

    data class Failed(val reason: String) : SyncState
}
