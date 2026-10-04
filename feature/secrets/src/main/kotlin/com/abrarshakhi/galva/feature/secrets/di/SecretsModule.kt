package com.abrarshakhi.galva.feature.secrets.di

import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.secrets.SecretViewerViewModel
import com.abrarshakhi.galva.feature.secrets.SecretsViewModel
import com.abrarshakhi.galva.feature.secrets.VaultSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val secretsModule = module {
    includes(coreUiModule)

    viewModel {
        SecretsViewModel(
            vault = get(),
            settingsRepository = get(),
            restoreSecrets = get(),
            selectionActions = get(),
        )
    }
    viewModel { VaultSetupViewModel(vault = get()) }
    viewModel { (initialId: Long) ->
        SecretViewerViewModel(initialId = initialId, vault = get(), restoreSecrets = get())
    }
}
