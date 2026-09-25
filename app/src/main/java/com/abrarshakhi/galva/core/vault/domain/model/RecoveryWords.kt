package com.abrarshakhi.galva.core.vault.domain.model

/** The recovery phrase as the user handles it: 24 words, written down in order. */
object RecoveryWords {

    const val COUNT = 24

    /** Splits whatever the user typed or pasted into lower-case words. */
    fun parse(input: CharSequence): List<String> =
        input.trim().split(Regex("\\s+")).filter(String::isNotEmpty).map(String::lowercase)
}
