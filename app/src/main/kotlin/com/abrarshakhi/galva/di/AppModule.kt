package com.abrarshakhi.galva.di

import com.abrarshakhi.galva.core.domain.di.domainModule
import com.abrarshakhi.galva.feature.albums.di.albumsModule
import com.abrarshakhi.galva.feature.gallery.di.galleryModule
import com.abrarshakhi.galva.feature.search.di.searchModule
import com.abrarshakhi.galva.feature.secrets.di.secretsModule
import com.abrarshakhi.galva.feature.settings.di.settingsModule
import com.abrarshakhi.galva.feature.viewer.di.viewerModule
import com.abrarshakhi.galva.ui.MainAppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(domainModule)

    viewModel {
        MainAppViewModel(
            syncMedia = get(),
            mediaStoreObserver = get(),
            settingsRepository = get(),
        )
    }
}

val galvaModules = listOf(
    appModule,
    galleryModule,
    albumsModule,
    searchModule,
    viewerModule,
    secretsModule,
    settingsModule,
)
