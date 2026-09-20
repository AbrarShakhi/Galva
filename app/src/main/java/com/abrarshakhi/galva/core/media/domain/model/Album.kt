package com.abrarshakhi.galva.core.media.domain.model

/**
 * A bucket of media.
 *
 * Device albums mirror a MediaStore bucket (one per folder on disk) and exist because files are in
 * them. Favorites and user albums are app-owned: they are rows in this app's database and change
 * nothing on the filesystem, so a photo can belong to several of them at once.
 */
data class Album(
    val ref: AlbumRef,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
    val lastModifiedMs: Long,
) {
    /** True when the album is this app's own, so it can be added to rather than merely browsed. */
    val isUserCreated: Boolean get() = ref is AlbumRef.User
}
