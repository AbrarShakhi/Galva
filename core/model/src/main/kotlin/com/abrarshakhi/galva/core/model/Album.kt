package com.abrarshakhi.galva.core.model

data class Album(
    val ref: AlbumRef,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
    val lastModifiedMs: Long,
) {
    val isUserCreated: Boolean get() = ref is AlbumRef.User
}
