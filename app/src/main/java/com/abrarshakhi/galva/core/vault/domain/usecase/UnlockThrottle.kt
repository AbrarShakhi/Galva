package com.abrarshakhi.galva.core.vault.domain.usecase

/**
 * Backs off after repeated wrong passphrases.
 *
 * This slows down someone guessing at the unlock screen of a phone left open; it is not a security
 * boundary. Anyone who copies the vault off the device is bounded by Argon2id and the passphrase's
 * strength instead, which is why it lives in memory and resets with the process.
 */
class UnlockThrottle(private val clock: () -> Long = System::currentTimeMillis) {

    private var failures = 0
    private var blockedUntilMs = 0L

    /** Milliseconds until another attempt is allowed; zero when one is allowed now. */
    @Synchronized
    fun waitMs(): Long = (blockedUntilMs - clock()).coerceAtLeast(0L)

    @Synchronized
    fun recordFailure() {
        failures++
        if (failures >= FREE_ATTEMPTS) {
            val doublings = (failures - FREE_ATTEMPTS).coerceAtMost(MAX_DOUBLINGS)
            blockedUntilMs = clock() + BASE_DELAY_MS * (1L shl doublings)
        }
    }

    @Synchronized
    fun recordSuccess() {
        failures = 0
        blockedUntilMs = 0L
    }

    private companion object {
        const val FREE_ATTEMPTS = 5
        const val BASE_DELAY_MS = 30_000L
        const val MAX_DOUBLINGS = 6
    }
}
