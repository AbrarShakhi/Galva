package com.abrarshakhi.galva.core.settings.domain

import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setGalleryColumns(columns: Int)

    suspend fun setAlbumViewType(viewType: AlbumViewType)

    suspend fun setAlbumSort(sort: AlbumSort)
}
