package com.abrarshakhi.galva.core.vault.store

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

internal class VaultContainer(
    val alias: String,
    val wrappedKey: ByteArray,
    val sealedBody: ByteArray,
) {

    val header: ByteArray get() = headerOf(alias, wrappedKey)

    fun encode(): ByteArray = header + sealedBody

    companion object {

        private val MAGIC = "GLVV".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1

        fun headerOf(alias: String, wrappedKey: ByteArray): ByteArray {
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use { out ->
                out.write(MAGIC)
                out.writeByte(VERSION)
                out.writeUTF(alias)
                out.writeShort(wrappedKey.size)
                out.write(wrappedKey)
            }
            return bytes.toByteArray()
        }

        fun decode(bytes: ByteArray): VaultContainer = try {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            val magic = ByteArray(MAGIC.size).also(input::readFully)
            if (!magic.contentEquals(MAGIC)) throw VaultCorruptedException("Not a vault file")
            val version = input.readUnsignedByte()
            if (version != VERSION) {
                throw VaultCorruptedException("Unsupported vault version $version")
            }
            val alias = input.readUTF()
            val wrappedKey = ByteArray(input.readUnsignedShort()).also(input::readFully)
            val headerSize = headerOf(alias, wrappedKey).size
            VaultContainer(alias, wrappedKey, bytes.copyOfRange(headerSize, bytes.size))
        } catch (truncated: IOException) {
            throw VaultCorruptedException("The vault file is truncated", truncated)
        }
    }
}

internal class VaultCorruptedException(message: String, cause: Throwable? = null) : Exception(message, cause)
