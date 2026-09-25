package com.abrarshakhi.galva.core.vault.data.store

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * The layout of `vault.bin`:
 *
 * ```
 * "GLVV" | version:u8 | alias:u16 + UTF-8 | wrapped container key:u16 + bytes | sealed body
 * ```
 *
 * The header is plaintext because it has to be read before anything can be decrypted. It holds
 * nothing secret — the alias names a hardware key, and the container key is useless without that
 * hardware — but it is authenticated: the header is the associated data of the sealed body.
 */
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

        /** @throws VaultCorruptedException when [bytes] is not a container this version reads. */
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

class VaultCorruptedException(message: String, cause: Throwable? = null) : Exception(message, cause)
