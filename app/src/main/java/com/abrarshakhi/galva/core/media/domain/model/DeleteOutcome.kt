package com.abrarshakhi.galva.core.media.domain.model

sealed interface DeleteOutcome {

    data class Deleted(val count: Int) : DeleteOutcome

    data class NeedsConsent(val uris: List<String>) : DeleteOutcome

    data class Failed(val reason: String) : DeleteOutcome
}
