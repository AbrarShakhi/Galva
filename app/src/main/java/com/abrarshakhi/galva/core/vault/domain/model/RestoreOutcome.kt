package com.abrarshakhi.galva.core.vault.domain.model

/**
 * What a restore managed. [restoredMediaIds] are the new gallery ids, and [favoriteMediaIds] the
 * ones that were favourites before they were hidden. [failure] is set when the restore stopped
 * early; everything listed before it was restored and has left the vault.
 */
data class RestoreOutcome(
    val restoredMediaIds: List<Long>,
    val favoriteMediaIds: List<Long>,
    val failure: String? = null,
)
