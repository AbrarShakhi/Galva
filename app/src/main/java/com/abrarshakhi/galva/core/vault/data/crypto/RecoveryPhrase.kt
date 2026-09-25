package com.abrarshakhi.galva.core.vault.data.crypto

import android.content.Context
import com.abrarshakhi.galva.core.vault.domain.model.RecoveryWords
import java.security.MessageDigest

/**
 * The 256-bit recovery key as 24 English words, per BIP-39.
 *
 * Words survive being written on paper far better than hex. The 8-bit checksum means a mistyped
 * or misremembered word is caught before any decryption is attempted, so the user is told "a word
 * is wrong" rather than "this key does not open the vault".
 */
class RecoveryPhrase(private val wordList: List<String>) {

    private val indexOf: Map<String, Int> =
        wordList.withIndex().associate { (index, word) -> word to index }

    init {
        require(wordList.size == WORD_LIST_SIZE && indexOf.size == WORD_LIST_SIZE) {
            "A BIP-39 word list has exactly $WORD_LIST_SIZE distinct words"
        }
    }

    fun encode(entropy: ByteArray): List<String> {
        require(entropy.size == ENTROPY_BYTES) { "Recovery keys are $ENTROPY_BYTES bytes" }
        val bits = entropy + checksumOf(entropy)
        return List(WORD_COUNT) { word -> wordList[readBits(bits, word * BITS_PER_WORD)] }
    }

    /**
     * The key [words] encode, or null when a word is unknown, one is missing, or the checksum
     * fails.
     */
    fun decode(words: List<String>): ByteArray? {
        if (words.size != WORD_COUNT) return null
        val bits = ByteArray(ENTROPY_BYTES + 1)
        words.forEachIndexed { position, word ->
            val index = indexOf[word] ?: return null
            writeBits(bits, position * BITS_PER_WORD, index)
        }
        val entropy = bits.copyOf(ENTROPY_BYTES)
        return entropy.takeIf { checksumOf(it) == bits[ENTROPY_BYTES] }
    }

    private fun checksumOf(entropy: ByteArray): Byte =
        MessageDigest.getInstance("SHA-256").digest(entropy)[0]

    private fun readBits(data: ByteArray, offset: Int): Int {
        var value = 0
        for (bit in offset until offset + BITS_PER_WORD) {
            value = (value shl 1) or ((data[bit / 8].toInt() shr (7 - bit % 8)) and 1)
        }
        return value
    }

    private fun writeBits(data: ByteArray, offset: Int, value: Int) {
        for (i in 0 until BITS_PER_WORD) {
            if ((value shr (BITS_PER_WORD - 1 - i)) and 1 == 1) {
                val bit = offset + i
                data[bit / 8] = (data[bit / 8].toInt() or (1 shl (7 - bit % 8))).toByte()
            }
        }
    }

    companion object {
        const val WORD_COUNT = RecoveryWords.COUNT
        const val ENTROPY_BYTES = 32
        private const val BITS_PER_WORD = 11
        private const val WORD_LIST_SIZE = 2048

        /** The English list ships in assets, byte-for-byte as published with BIP-39. */
        fun fromAssets(context: Context): RecoveryPhrase = RecoveryPhrase(
            context.assets.open("bip39-english.txt").bufferedReader().useLines { lines ->
                lines.map(String::trim).filter(String::isNotEmpty).toList()
            },
        )
    }
}
