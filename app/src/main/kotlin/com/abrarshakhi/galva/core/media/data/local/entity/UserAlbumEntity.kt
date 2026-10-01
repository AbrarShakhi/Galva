package com.abrarshakhi.galva.core.media.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An album the user created. Purely app-owned; nothing corresponding exists on the filesystem. */
@Entity(tableName = "user_albums")
data class UserAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
)
