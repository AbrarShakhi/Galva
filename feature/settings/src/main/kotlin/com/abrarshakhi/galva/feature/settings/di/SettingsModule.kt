package com.abrarshakhi.galva.feature.settings.di

import com.abrarshakhi.galva.core.model.AppDocument
import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.settings.DocumentViewModel
import com.abrarshakhi.galva.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    includes(coreUiModule)

    viewModel { SettingsViewModel(settingsRepository = get()) }
    viewModel { (document: AppDocument) -> DocumentViewModel(document = document, repository = get()) }
}
