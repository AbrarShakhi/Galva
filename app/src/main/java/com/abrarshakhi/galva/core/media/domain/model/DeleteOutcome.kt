package com.abrarshakhi.galva.core.media.domain.model

/**
 * Result of asking the data layer to delete media.
 *
 * Scoped storage means an app can rarely delete media it did not create without the user
 * confirming through a system dialog, so [NeedsConsent] is the normal path on API 30+ rather than
 * an error. It hands back plain URI strings; the UI layer turns them into a `MediaStore` delete
 * request because only an Activity can launch one.
 */
sealed interface DeleteOutcome {

    data class Deleted(val count: Int) : DeleteOutcome

    data class NeedsConsent(val uris: List<String>) : DeleteOutcome

    data class Failed(val reason: String) : DeleteOutcome
}
