package com.abrarshakhi.galva.core.media.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.abrarshakhi.galva.core.media.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {

    @Upsert
    suspend fun favorite(entries: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE mediaId IN (:ids)")
    suspend fun unfavorite(ids: List<Long>)
}
