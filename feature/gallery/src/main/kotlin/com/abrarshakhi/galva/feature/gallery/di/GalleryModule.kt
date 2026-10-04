package com.abrarshakhi.galva.feature.gallery.di

import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.gallery.GalleryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val galleryModule = module {
    includes(coreUiModule)

    viewModel {
        GalleryViewModel(
            observeTimeline = get(),
            settingsRepository = get(),
            mediaRepository = get(),
            syncMedia = get(),
            mediaActions = get(),
        )
    }
}
