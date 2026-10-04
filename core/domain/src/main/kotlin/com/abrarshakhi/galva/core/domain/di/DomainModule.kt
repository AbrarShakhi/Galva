package com.abrarshakhi.galva.core.domain.di

import com.abrarshakhi.galva.core.data.di.dataModule
import com.abrarshakhi.galva.core.domain.AddToAlbumUseCase
import com.abrarshakhi.galva.core.domain.CreateAlbumUseCase
import com.abrarshakhi.galva.core.domain.DeleteAlbumUseCase
import com.abrarshakhi.galva.core.domain.DeleteMediaUseCase
import com.abrarshakhi.galva.core.domain.MediaSelectionActions
import com.abrarshakhi.galva.core.domain.MoveToSecretsActions
import com.abrarshakhi.galva.core.domain.ObserveAlbumsUseCase
import com.abrarshakhi.galva.core.domain.ObserveMediaUseCase
import com.abrarshakhi.galva.core.domain.ObserveTimelineUseCase
import com.abrarshakhi.galva.core.domain.ObserveUserAlbumsUseCase
import com.abrarshakhi.galva.core.domain.RemoveFromAlbumUseCase
import com.abrarshakhi.galva.core.domain.RenameAlbumUseCase
import com.abrarshakhi.galva.core.domain.RestoreSecretsUseCase
import com.abrarshakhi.galva.core.domain.SyncMediaUseCase
import com.abrarshakhi.galva.core.domain.ToggleFavoriteUseCase
import com.abrarshakhi.galva.core.domain.UnlockThrottle
import com.abrarshakhi.galva.core.vault.di.vaultModule
import org.koin.dsl.module

val domainModule = module {
    includes(dataModule, vaultModule)

    factory { ObserveTimelineUseCase(mediaRepository = get()) }
    factory { ObserveMediaUseCase(mediaRepository = get()) }
    factory { ObserveAlbumsUseCase(albumRepository = get()) }
    factory { ObserveUserAlbumsUseCase(albumRepository = get()) }
    factory { ToggleFavoriteUseCase(mediaRepository = get()) }
    factory { DeleteMediaUseCase(mediaRepository = get()) }
    factory { SyncMediaUseCase(mediaRepository = get()) }
    factory { CreateAlbumUseCase(albumRepository = get()) }
    factory { AddToAlbumUseCase(albumRepository = get()) }
    factory { RenameAlbumUseCase(albumRepository = get()) }
    factory { DeleteAlbumUseCase(albumRepository = get()) }
    factory { RemoveFromAlbumUseCase(albumRepository = get()) }
    factory { MediaSelectionActions(toggleFavorite = get(), deleteMedia = get()) }
    factory { MoveToSecretsActions(vault = get(), selectionActions = get()) }
    factory { RestoreSecretsUseCase(vault = get(), mediaRepository = get()) }
    single { UnlockThrottle() }
}
