package com.abrarshakhi.galva.core.model

sealed interface AddToVaultOutcome {

    data class Added(
        val secretIds: List<Long>,
        val originalIds: List<Long>,
        val originalUris: List<String>,
    ) : AddToVaultOutcome

    data class Failed(val reason: String) : AddToVaultOutcome
}
