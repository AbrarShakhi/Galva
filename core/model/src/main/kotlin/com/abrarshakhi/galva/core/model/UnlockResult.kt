package com.abrarshakhi.galva.core.model

sealed interface UnlockResult {

    data object Unlocked : UnlockResult

    data object WrongSecret : UnlockResult

    data object InvalidPhrase : UnlockResult

    data class Unavailable(val reason: String) : UnlockResult
}
