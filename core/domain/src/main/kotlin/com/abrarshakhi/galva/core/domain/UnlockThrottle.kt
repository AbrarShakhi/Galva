package com.abrarshakhi.galva.core.domain

class UnlockThrottle(private val clock: () -> Long = System::currentTimeMillis) {

    private var failures = 0
    private var blockedUntilMs = 0L

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
