package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.vault.store.OpenVault

internal class VaultSession {

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
