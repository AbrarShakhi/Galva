package com.abrarshakhi.galva.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface AlbumRef {

    @Serializable
    data class Device(val bucketId: Long) : AlbumRef

    @Serializable
    data object Favorites : AlbumRef

    @Serializable
    data class User(val id: Long) : AlbumRef
}

val AlbumRef.key: String
    get() = when (this) {
        is AlbumRef.Device -> "device:$bucketId"
        AlbumRef.Favorites -> "favorites"
        is AlbumRef.User -> "user:$id"
    }
