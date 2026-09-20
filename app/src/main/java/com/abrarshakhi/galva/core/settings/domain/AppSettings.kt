package com.abrarshakhi.galva.core.settings.domain

import com.abrarshakhi.galva.core.media.domain.model.AlbumSort

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val galleryColumns: Int = DEFAULT_COLUMNS,
    val albumViewType: AlbumViewType = AlbumViewType.GRID,
    val albumSort: AlbumSort = AlbumSort.RECENT_FIRST,
) {
    companion object {
        const val DEFAULT_COLUMNS = 4
        val COLUMN_RANGE = 2..6
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK,
}

enum class AlbumViewType {
    GRID, LIST,
}
