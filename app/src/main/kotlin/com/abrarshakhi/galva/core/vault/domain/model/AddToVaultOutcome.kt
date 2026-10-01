package com.abrarshakhi.galva.core.vault.domain.model

/** Result of encrypting gallery items into the vault, before their originals are touched. */
sealed interface AddToVaultOutcome {

    /**
     * The items are encrypted and committed to the vault. The originals are still in the gallery
     * until the system delete dialog is confirmed.
     */
    data class Added(
        val secretIds: List<Long>,
        val originalIds: List<Long>,
        val originalUris: List<String>,
    ) : AddToVaultOutcome

    data class Failed(val reason: String) : AddToVaultOutcome
}
