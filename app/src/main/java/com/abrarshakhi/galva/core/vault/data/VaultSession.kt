package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.vault.data.store.OpenVault

/**
 * The open vault, held only in memory.
 *
 * Nothing that can decrypt the vault is ever written anywhere but here. [close] overwrites the keys
 * in place, so a heap dump taken after the vault locks finds zeroes where they were.
 */
class VaultSession {

    @Volatile
    var current: OpenVault? = null
        private set

    fun open(vault: OpenVault) {
        current = vault
    }

    fun close() {
        val vault = current ?: return
        current = null
        vault.wipe()
    }
}
