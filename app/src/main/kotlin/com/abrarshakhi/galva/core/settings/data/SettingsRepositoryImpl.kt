package com.abrarshakhi.galva.core.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType
import com.abrarshakhi.galva.core.settings.domain.AppSettings
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.settings.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Enum preferences are stored by name rather than ordinal so reordering an enum cannot silently
 * reinterpret a value the user already chose. Unknown names fall back to the default.
 */
class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.ThemeMode].toEnum(ThemeMode.SYSTEM),
            galleryColumns = prefs[Keys.GalleryColumns]
                ?.coerceIn(AppSettings.COLUMN_RANGE)
                ?: AppSettings.DEFAULT_COLUMNS,
            albumViewType = prefs[Keys.AlbumViewType].toEnum(AlbumViewType.GRID),
            albumSort = prefs[Keys.AlbumSort].toEnum(AlbumSort.RECENT_FIRST),
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.ThemeMode] = mode.name }

    override suspend fun setGalleryColumns(columns: Int) = edit {
        it[Keys.GalleryColumns] = columns.coerceIn(AppSettings.COLUMN_RANGE)
    }

    override suspend fun setAlbumViewType(viewType: AlbumViewType) = edit {
        it[Keys.AlbumViewType] = viewType.name
    }

    override suspend fun setAlbumSort(sort: AlbumSort) = edit { it[Keys.AlbumSort] = sort.name }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit(block)
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(fallback: E): E =
        this?.let { name -> runCatching { enumValueOf<E>(name) }.getOrNull() } ?: fallback

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val GalleryColumns = intPreferencesKey("gallery_columns")
        val AlbumViewType = stringPreferencesKey("album_view_type")
        val AlbumSort = stringPreferencesKey("album_sort")
    }
}
