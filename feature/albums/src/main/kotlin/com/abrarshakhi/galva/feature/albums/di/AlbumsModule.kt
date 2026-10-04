package com.abrarshakhi.galva.feature.albums.di

import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.ui.di.coreUiModule
import com.abrarshakhi.galva.feature.albums.AlbumDetailViewModel
import com.abrarshakhi.galva.feature.albums.AlbumsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val albumsModule = module {
    includes(coreUiModule)

    viewModel { AlbumsViewModel(observeAlbums = get(), settingsRepository = get()) }
    viewModel { (albumRef: AlbumRef) ->
        AlbumDetailViewModel(
            albumRef = albumRef,
            observeMedia = get(),
            albumRepository = get(),
            settingsRepository = get(),
            renameAlbum = get(),
            deleteAlbum = get(),
            removeFromAlbum = get(),
            mediaActions = get(),
        )
    }
}
