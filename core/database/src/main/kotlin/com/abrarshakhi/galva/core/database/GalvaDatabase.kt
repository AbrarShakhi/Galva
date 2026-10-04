package com.abrarshakhi.galva.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abrarshakhi.galva.core.database.dao.FavoriteDao
import com.abrarshakhi.galva.core.database.dao.MediaDao
import com.abrarshakhi.galva.core.database.dao.UserAlbumDao
import com.abrarshakhi.galva.core.database.entity.FavoriteEntity
import com.abrarshakhi.galva.core.database.entity.MediaEntity
import com.abrarshakhi.galva.core.database.entity.UserAlbumEntity
import com.abrarshakhi.galva.core.database.entity.UserAlbumMemberEntity

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
