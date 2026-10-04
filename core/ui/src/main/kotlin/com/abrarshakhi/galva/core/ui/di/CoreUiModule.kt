package com.abrarshakhi.galva.core.ui.di

import com.abrarshakhi.galva.core.domain.di.domainModule
import com.abrarshakhi.galva.core.ui.actions.MediaActionsStateHolder
import com.abrarshakhi.galva.core.ui.album.AddToAlbumViewModel
import com.abrarshakhi.galva.core.ui.vault.VaultUnlockViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreUiModule = module {
    includes(domainModule)

    factory { MediaActionsStateHolder(selectionActions = get(), moveToSecrets = get()) }
    viewModel {
        AddToAlbumViewModel(
            observeUserAlbums = get(),
            createAlbum = get(),
            addToAlbum = get(),
        )
    }
    viewModel { VaultUnlockViewModel(vault = get(), throttle = get()) }
}
