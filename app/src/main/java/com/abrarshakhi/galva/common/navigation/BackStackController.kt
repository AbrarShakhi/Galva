package com.abrarshakhi.galva.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.Json

fun <T> SnapshotStateList<T>.currentRoute(): T? = lastOrNull()

/**
 * Replaces the whole stack with [destination].
 *
 * This is the root-swap tab model: switching tabs is not a push, so the back stack never grows a
 * trail of visited tabs.
 */
fun <T> SnapshotStateList<T>.switchTabTo(destination: T) {
    if (size == 1 && lastOrNull() == destination) return
    clear()
    add(destination)
}

fun <T> SnapshotStateList<T>.back() {
    if (size > 1) removeLastOrNull()
}

fun <T> SnapshotStateList<T>.navigateTo(destination: T) {
    add(destination)
}

/**
 * Persists the stack across process death by encoding each key.
 *
 * Polymorphic encoding through the sealed [AppRouteKey] hierarchy is what lets routes carry
 * payloads — an album id, a viewer source — and still come back intact.
 */
val AppRouteBackStackSaver: Saver<SnapshotStateList<AppRouteKey>, Any> = listSaver(
    save = { stack -> stack.map { Json.encodeToString<AppRouteKey>(it) } },
    restore = { saved ->
        val routes = saved.mapNotNull { encoded ->
            runCatching { Json.decodeFromString<AppRouteKey>(encoded) }.getOrNull()
        }
        mutableStateListOf<AppRouteKey>().apply {
            addAll(routes.ifEmpty { listOf(AppRouteKey.Gallery) })
        }
    },
)

@Composable
fun rememberAppBackStack(start: AppRouteKey): SnapshotStateList<AppRouteKey> =
    rememberSaveable(saver = AppRouteBackStackSaver) { mutableStateListOf(start) }
