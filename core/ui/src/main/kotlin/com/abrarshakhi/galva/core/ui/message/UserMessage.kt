package com.abrarshakhi.galva.core.ui.message

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong

@Immutable
data class UserMessage(
    val text: String,
    val id: Long = nextId.incrementAndGet(),
)

private val nextId = AtomicLong()

fun List<UserMessage>.withMessage(message: UserMessage?): List<UserMessage> =
    if (message == null) this else this + message

fun List<UserMessage>.withMessage(text: String): List<UserMessage> = this + UserMessage(text)

fun List<UserMessage>.shown(id: Long): List<UserMessage> = filterNot { it.id == id }
