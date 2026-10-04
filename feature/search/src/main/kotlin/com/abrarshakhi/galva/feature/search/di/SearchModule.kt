package com.abrarshakhi.galva.feature.search.di

import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule = module {
    includes(coreUiModule)

    viewModel {
        SearchViewModel(
            observeMedia = get(),
            settingsRepository = get(),
            mediaActions = get(),
        )
    }
}
