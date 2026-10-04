package com.abrarshakhi.galva.core.model

object RecoveryWords {

    const val COUNT = 24

    fun parse(input: CharSequence): List<String> =
        input.trim().split(Regex("\\s+")).filter(String::isNotEmpty).map(String::lowercase)
}
