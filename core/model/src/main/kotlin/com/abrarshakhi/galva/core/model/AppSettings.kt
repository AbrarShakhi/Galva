package com.abrarshakhi.galva.core.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColor: ThemeColor = ThemeColor.WALLPAPER,
    val pureBlack: Boolean = false,
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
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemeColor(val seedArgb: Long) {
    WALLPAPER(0xFF1DB954),
    EMERALD(0xFF1DB954),
    OCEAN(0xFF3A6FF7),
    VIOLET(0xFF7B4DFF),
    ROSE(0xFFE8457D),
    SUNSET(0xFFF2762E),
    SAND(0xFFB89255),
}

enum class AlbumViewType {
    GRID,
    LIST,
}
