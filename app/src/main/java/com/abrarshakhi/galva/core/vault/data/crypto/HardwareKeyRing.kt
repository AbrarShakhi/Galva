package com.abrarshakhi.galva.core.vault.data.crypto

/**
 * Keys that never leave this device's secure hardware.
 *
 * The vault's outermost layer is wrapped with one of these, and every destructive change replaces
 * it with a new one. Destroying a key held in hardware is the one kind of deletion on a phone that
 * does not depend on flash storage actually erasing a block — which is what lets "deleted
 * forever" hold even for someone who later learns the passphrase.
 */
interface HardwareKeyRing {

    /** Creates a new key and returns its alias. */
    fun createKey(): String

    fun wrap(alias: String, key: ByteArray, context: ByteArray): ByteArray

    /** @throws KeyUnavailableException when the hardware no longer has [alias]. */
    fun unwrap(alias: String, wrapped: ByteArray, context: ByteArray): ByteArray

    fun delete(alias: String)

    /** Every vault key the hardware holds, including ones an interrupted rotation left behind. */
    fun aliases(): Set<String>
}

class KeyUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
