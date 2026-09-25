package com.abrarshakhi.galva.core.vault.domain.model

/** Vault ids as fixed-width hex, which is also how the vault names its files. */
object VaultIds {

    fun toHex(id: Long): String = java.lang.Long.toHexString(id).padStart(HEX_LENGTH, '0')

    fun fromHex(hex: String): Long? =
        if (hex.length != HEX_LENGTH) null
        else runCatching { java.lang.Long.parseUnsignedLong(hex, 16) }.getOrNull()

    private const val HEX_LENGTH = 16
}
