package com.abrarshakhi.galva.core.data.repository

import com.abrarshakhi.galva.core.model.AlbumSort
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.ThemeColor
import com.abrarshakhi.galva.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setThemeColor(color: ThemeColor)

    suspend fun setPureBlack(enabled: Boolean)

    suspend fun setGalleryColumns(columns: Int)

    suspend fun setAlbumViewType(viewType: AlbumViewType)

    suspend fun setAlbumSort(sort: AlbumSort)
}
