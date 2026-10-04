package com.abrarshakhi.galva.core.model

data class RestoreOutcome(
    val restoredMediaIds: List<Long>,
    val favoriteMediaIds: List<Long>,
    val failure: String? = null,
)
