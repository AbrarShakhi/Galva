package com.abrarshakhi.galva.core.vault.crypto

internal interface HardwareKeyRing {

    fun createKey(): String

    fun wrap(alias: String, key: ByteArray, context: ByteArray): ByteArray

    fun unwrap(alias: String, wrapped: ByteArray, context: ByteArray): ByteArray

    fun delete(alias: String)

    fun aliases(): Set<String>
}

internal class KeyUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
