package com.abrarshakhi.galva.core.vault.domain.model

/** How an attempt to open the vault went. */
sealed interface UnlockResult {

    data object Unlocked : UnlockResult

    data object WrongSecret : UnlockResult

    /** The words are not a valid recovery phrase: a typo, or a word that is not on the list. */
    data object InvalidPhrase : UnlockResult

    /**
     * The vault can no longer be opened on this device, whatever the secret. The usual cause is
     * that the phone's secure hardware no longer holds the vault's outer key.
     */
    data class Unavailable(val reason: String) : UnlockResult
}
