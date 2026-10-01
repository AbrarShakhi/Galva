package com.abrarshakhi.galva.core.vault

import com.abrarshakhi.galva.core.vault.domain.model.RecoveryWords
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.SecureRandom

class RecoveryPhraseTest {

    private val phrase = bip39()

    // Published BIP-39 test vectors for 256-bit entropy.

    @Test
    fun `all-zero entropy matches the BIP-39 vector`() {
        val words = phrase.encode(ByteArray(32))
        assertEquals(List(23) { "abandon" } + "art", words)
    }

    @Test
    fun `all-7f entropy matches the BIP-39 vector`() {
        val words = phrase.encode(ByteArray(32) { 0x7f })
        val expected = ("legal winner thank year wave sausage worth useful legal winner thank year " +
            "wave sausage worth useful legal winner thank year wave sausage worth title").split(' ')
        assertEquals(expected, words)
    }

    @Test
    fun `all-ff entropy matches the BIP-39 vector`() {
        val words = phrase.encode(ByteArray(32) { 0xff.toByte() })
        assertEquals(List(23) { "zoo" } + "vote", words)
    }

    @Test
    fun `random keys survive a round trip`() {
        val random = SecureRandom()
        repeat(50) {
            val entropy = ByteArray(32).also(random::nextBytes)
            assertArrayEquals(entropy, phrase.decode(phrase.encode(entropy)))
        }
    }

    @Test
    fun `a wrong last word fails the checksum`() {
        // "art" carries the checksum of all-zero entropy; "abandon" carries a checksum of zero.
        assertNull(phrase.decode(List(24) { "abandon" }))
    }

    @Test
    fun `swapping two words is caught`() {
        val words = phrase.encode(ByteArray(32) { it.toByte() }).toMutableList()
        val first = words[0]
        words[0] = words[1]
        words[1] = first
        assertNull(phrase.decode(words))
    }

    @Test
    fun `unknown words and wrong lengths are rejected`() {
        val words = phrase.encode(ByteArray(32))
        assertNull(phrase.decode(words.dropLast(1)))
        assertNull(phrase.decode(words.dropLast(1) + "notaword"))
    }

    @Test
    fun `typed input is split and lower-cased`() {
        assertEquals(listOf("abandon", "ability", "able"), RecoveryWords.parse("  Abandon\nABILITY   able "))
    }
}
