package com.abrarshakhi.galva.core.vault.store

import com.abrarshakhi.galva.core.vault.crypto.HardwareKeyRing
import com.abrarshakhi.galva.core.vault.crypto.KdfParams
import com.abrarshakhi.galva.core.vault.crypto.KeyUnavailableException
import com.abrarshakhi.galva.core.vault.crypto.PassphraseKdf
import com.abrarshakhi.galva.core.vault.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.crypto.wipe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.security.GeneralSecurityException

@OptIn(ExperimentalSerializationApi::class)
internal class VaultStore(
    private val files: VaultFiles,
    private val keyRing: HardwareKeyRing,
    private val kdf: PassphraseKdf,
    private val crypto: VaultCrypto,
) {

    fun exists(): Boolean = files.containerExists()

    fun create(passphrase: ByteArray, recoveryKey: ByteArray): OpenVault {
        val masterKey = crypto.newKey()
        val params = kdf.newParams(crypto.randomBytes(SALT_BYTES))
        val keyEncryptionKey = kdf.deriveKey(passphrase, params)
        val recoveryWrappingKey = crypto.deriveKey(recoveryKey, VaultContext.RECOVERY_KEY_PURPOSE)
        val keys = try {
            MasterKeys(
                masterKey = masterKey,
                kdf = params,
                byPassphrase = crypto.seal(
                    keyEncryptionKey,
                    masterKey,
                    VaultContext.MasterKeyByPassphrase,
                ),
                byRecovery = crypto.seal(
                    recoveryWrappingKey,
                    masterKey,
                    VaultContext.MasterKeyByRecovery,
                ),
            )
        } finally {
            keyEncryptionKey.wipe()
            recoveryWrappingKey.wipe()
        }
        val index = VaultIndex()
        return OpenVault(commitToNewEpoch(keys, index), keys, index)
    }

    fun openWithPassphrase(passphrase: ByteArray): OpenVault {
        val (epoch, body) = readContainer()
        val masterKey = unwrapOrReject(epoch) {
            val keyEncryptionKey = kdf.deriveKey(passphrase, body.kdf)
            try {
                crypto.open(
                    keyEncryptionKey,
                    body.masterKeyByPassphrase,
                    VaultContext.MasterKeyByPassphrase,
                )
            } finally {
                keyEncryptionKey.wipe()
            }
        }
        return opened(epoch, body, masterKey)
    }

    fun openWithRecoveryKey(recoveryKey: ByteArray): OpenVault {
        val (epoch, body) = readContainer()
        val masterKey = unwrapOrReject(epoch) {
            val wrappingKey = crypto.deriveKey(recoveryKey, VaultContext.RECOVERY_KEY_PURPOSE)
            try {
                crypto.open(wrappingKey, body.masterKeyByRecovery, VaultContext.MasterKeyByRecovery)
            } finally {
                wrappingKey.wipe()
            }
        }
        return opened(epoch, body, masterKey)
    }

    fun write(vault: OpenVault, index: VaultIndex, rotate: Boolean): OpenVault {
        if (!rotate) {
            files.writeContainer(seal(vault.epoch, vault.keys, index))
            return OpenVault(vault.epoch, vault.keys, index)
        }
        val next = commitToNewEpoch(vault.keys, index)
        retire(vault.epoch)
        return OpenVault(next, vault.keys, index)
    }

    fun changePassphrase(vault: OpenVault, newPassphrase: ByteArray): OpenVault {
        val params = kdf.newParams(crypto.randomBytes(SALT_BYTES))
        val keyEncryptionKey = kdf.deriveKey(newPassphrase, params)
        val keys = try {
            vault.keys.copy(
                kdf = params,
                byPassphrase = crypto.seal(
                    keyEncryptionKey,
                    vault.keys.masterKey,
                    VaultContext.MasterKeyByPassphrase,
                ),
            )
        } finally {
            keyEncryptionKey.wipe()
        }
        return write(OpenVault(vault.epoch, keys, vault.index), vault.index, rotate = true)
    }

    fun matchesPassphrase(vault: OpenVault, passphrase: ByteArray): Boolean {
        val keyEncryptionKey = kdf.deriveKey(passphrase, vault.keys.kdf)
        return try {
            val masterKey = crypto.open(
                keyEncryptionKey,
                vault.keys.byPassphrase,
                VaultContext.MasterKeyByPassphrase,
            )
            masterKey.contentEquals(vault.keys.masterKey).also { masterKey.wipe() }
        } catch (wrong: GeneralSecurityException) {
            false
        } finally {
            keyEncryptionKey.wipe()
        }
    }

    fun destroy() {
        keyRing.aliases().forEach { alias -> runCatching { keyRing.delete(alias) } }
        files.deleteEverything()
    }

    fun removeStaleKeys() {
        val current = when (val bytes = files.readContainer()) {
            null -> null
            else -> runCatching { VaultContainer.decode(bytes).alias }.getOrNull() ?: return
        }
        keyRing.aliases()
            .filter { it != current }
            .forEach { alias -> runCatching { keyRing.delete(alias) } }
    }

    private fun readContainer(): Pair<Epoch, VaultBody> {
        val bytes = files.readContainer() ?: throw VaultCorruptedException("There is no vault file")
        val container = VaultContainer.decode(bytes)
        val containerKey = try {
            keyRing.unwrap(
                container.alias,
                container.wrappedKey,
                VaultContext.containerKey(container.alias),
            )
        } catch (unavailable: KeyUnavailableException) {
            throw unavailable
        } catch (error: GeneralSecurityException) {
            throw KeyUnavailableException("The vault's hardware key no longer opens it", error)
        }
        val body = try {
            crypto.open(containerKey, container.sealedBody, container.header)
        } catch (error: GeneralSecurityException) {
            containerKey.wipe()
            throw VaultCorruptedException("The vault file is damaged", error)
        }
        return Epoch(container.alias, containerKey, container.wrappedKey) to
            ProtoBuf.decodeFromByteArray(VaultBody.serializer(), body)
    }

    private inline fun unwrapOrReject(epoch: Epoch, unwrap: () -> ByteArray): ByteArray = try {
        unwrap()
    } catch (wrong: GeneralSecurityException) {
        epoch.containerKey.wipe()
        throw WrongSecretException()
    } catch (error: Exception) {
        epoch.containerKey.wipe()
        throw error
    }

    private fun opened(epoch: Epoch, body: VaultBody, masterKey: ByteArray): OpenVault {
        val index = try {
            ProtoBuf.decodeFromByteArray(
                VaultIndex.serializer(),
                crypto.open(masterKey, body.sealedIndex, VaultContext.Index),
            )
        } catch (error: GeneralSecurityException) {
            epoch.containerKey.wipe()
            masterKey.wipe()
            throw VaultCorruptedException("The vault index is damaged", error)
        }
        val keys = MasterKeys(
            masterKey = masterKey,
            kdf = body.kdf,
            byPassphrase = body.masterKeyByPassphrase,
            byRecovery = body.masterKeyByRecovery,
        )
        return OpenVault(epoch, keys, index)
    }

    private fun commitToNewEpoch(keys: MasterKeys, index: VaultIndex): Epoch {
        val alias = keyRing.createKey()
        val containerKey = crypto.newKey()
        try {
            val epoch = Epoch(
                alias = alias,
                containerKey = containerKey,
                wrappedKey = keyRing.wrap(alias, containerKey, VaultContext.containerKey(alias)),
            )
            files.writeContainer(seal(epoch, keys, index))
            return epoch
        } catch (error: Exception) {
            runCatching { keyRing.delete(alias) }
            containerKey.wipe()
            throw error
        }
    }

    private fun retire(epoch: Epoch) {
        runCatching { keyRing.delete(epoch.alias) }
        epoch.containerKey.wipe()
    }

    private fun seal(epoch: Epoch, keys: MasterKeys, index: VaultIndex): ByteArray {
        val body = VaultBody(
            kdf = keys.kdf,
            masterKeyByPassphrase = keys.byPassphrase,
            masterKeyByRecovery = keys.byRecovery,
            sealedIndex = crypto.seal(
                keys.masterKey,
                ProtoBuf.encodeToByteArray(VaultIndex.serializer(), index),
                VaultContext.Index,
            ),
        )
        val header = VaultContainer.headerOf(epoch.alias, epoch.wrappedKey)
        val sealedBody = crypto.seal(
            epoch.containerKey,
            ProtoBuf.encodeToByteArray(VaultBody.serializer(), body),
            header,
        )
        return VaultContainer(epoch.alias, epoch.wrappedKey, sealedBody).encode()
    }

    private companion object {
        const val SALT_BYTES = 16
    }
}

internal class OpenVault(
    internal val epoch: Epoch,
    internal val keys: MasterKeys,
    val index: VaultIndex,
) {

    private val byId: Map<Long, SecretEntry> by lazy { index.entries.associateBy(SecretEntry::id) }

    fun entry(id: Long): SecretEntry? = byId[id]

    fun wipe() {
        epoch.containerKey.wipe()
        keys.masterKey.wipe()
        index.entries.forEach { it.keyset.wipe() }
    }
}

internal class Epoch(
    val alias: String,
    val containerKey: ByteArray,
    val wrappedKey: ByteArray,
)

internal data class MasterKeys(
    val masterKey: ByteArray,
    val kdf: KdfParams,
    val byPassphrase: ByteArray,
    val byRecovery: ByteArray,
)

internal class WrongSecretException :
    Exception("That passphrase or recovery phrase does not open this vault")
