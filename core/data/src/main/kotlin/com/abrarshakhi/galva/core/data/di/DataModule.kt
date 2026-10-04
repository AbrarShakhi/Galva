package com.abrarshakhi.galva.core.data.di

import com.abrarshakhi.galva.core.common.di.commonModule
import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.data.repository.AlbumRepositoryImpl
import com.abrarshakhi.galva.core.data.repository.AssetDocumentRepository
import com.abrarshakhi.galva.core.data.repository.DocumentRepository
import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.data.repository.MediaRepositoryImpl
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.data.repository.SettingsRepositoryImpl
import com.abrarshakhi.galva.core.data.sync.MediaSyncManager
import com.abrarshakhi.galva.core.database.di.databaseModule
import com.abrarshakhi.galva.core.mediastore.di.mediaStoreModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    includes(commonModule, databaseModule, mediaStoreModule)

    single { MediaSyncManager(dataSource = get(), mediaDao = get(), dispatcher = get()) }
    single<MediaRepository> {
        MediaRepositoryImpl(
            mediaDao = get(),
            favoriteDao = get(),
            userAlbumDao = get(),
            syncManager = get(),
            dispatcher = get(),
        )
    }
    single<AlbumRepository> {
        AlbumRepositoryImpl(mediaDao = get(), userAlbumDao = get(), dispatcher = get())
    }
    single<SettingsRepository> { SettingsRepositoryImpl(context = androidContext()) }
    single<DocumentRepository> {
        AssetDocumentRepository(context = androidContext(), dispatcher = get())
    }
}
