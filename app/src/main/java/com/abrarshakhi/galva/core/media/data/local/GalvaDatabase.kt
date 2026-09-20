package com.abrarshakhi.galva.core.media.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abrarshakhi.galva.core.media.data.local.dao.FavoriteDao
import com.abrarshakhi.galva.core.media.data.local.dao.MediaDao
import com.abrarshakhi.galva.core.media.data.local.dao.UserAlbumDao
import com.abrarshakhi.galva.core.media.data.local.entity.FavoriteEntity
import com.abrarshakhi.galva.core.media.data.local.entity.MediaEntity
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumEntity
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumMemberEntity

@Database(
    entities = [
        MediaEntity::class,
        FavoriteEntity::class,
        UserAlbumEntity::class,
        UserAlbumMemberEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GalvaDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun userAlbumDao(): UserAlbumDao

    companion object {
        const val NAME = "galva.db"
    }
}
