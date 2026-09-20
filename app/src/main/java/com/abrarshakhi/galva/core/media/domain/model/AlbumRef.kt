package com.abrarshakhi.galva.core.media.domain.model

import kotlinx.serialization.Serializable

/**
 * Identity of an album.
 *
 * Albums come from three different places and a single `Long` cannot tell them apart. MediaStore's
 * `BUCKET_ID` is a hash of the folder path, so it is free to be negative — which means no sentinel
 * value is safe to reserve for the app's own albums. Making the origin part of the type removes the
 * collision instead of making it unlikely.
 */
@Serializable
sealed interface AlbumRef {

    /** A folder on the device, as MediaStore buckets it. */
    @Serializable
    data class Device(val bucketId: Long) : AlbumRef

    /** The synthetic album backed by the favourites table. */
    @Serializable
    data object Favorites : AlbumRef

    /** An album the user created in this app. Exists only here; nothing is moved on disk. */
    @Serializable
    data class User(val id: Long) : AlbumRef
}

/** Stable identity for list keys and ViewModel scoping, safe to persist. */
val AlbumRef.key: String
    get() = when (this) {
        is AlbumRef.Device -> "device:$bucketId"
        AlbumRef.Favorites -> "favorites"
        is AlbumRef.User -> "user:$id"
    }
