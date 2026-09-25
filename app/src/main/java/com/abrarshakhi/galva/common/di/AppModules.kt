package com.abrarshakhi.galva.common.di

import android.app.ActivityManager
import androidx.room.Room
import com.abrarshakhi.galva.common.main.MainAppViewModel
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.core.media.data.local.GalvaDatabase
import com.abrarshakhi.galva.core.media.data.local.MIGRATION_1_2
import com.abrarshakhi.galva.core.media.data.local.SecureDeleteCallback
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreDataSource
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreReader
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreObserver
import com.abrarshakhi.galva.core.media.data.repository.AlbumRepositoryImpl
import com.abrarshakhi.galva.core.media.data.repository.MediaRepositoryImpl
import com.abrarshakhi.galva.core.media.data.sync.MediaSyncManager
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import com.abrarshakhi.galva.core.media.domain.usecase.AddToAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.CreateAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.DeleteAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.RemoveFromAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.RenameAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.DeleteMediaUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveAlbumsUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveMediaUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveTimelineUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveUserAlbumsUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.SyncMediaUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ToggleFavoriteUseCase
import com.abrarshakhi.galva.core.settings.data.SettingsRepositoryImpl
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.data.VaultImporter
import com.abrarshakhi.galva.core.vault.data.VaultRepositoryImpl
import com.abrarshakhi.galva.core.vault.data.VaultRestorer
import com.abrarshakhi.galva.core.vault.data.VaultSession
import com.abrarshakhi.galva.core.vault.data.crypto.AndroidKeystoreKeyRing
import com.abrarshakhi.galva.core.vault.data.crypto.Argon2idKdf
import com.abrarshakhi.galva.core.vault.data.crypto.HardwareKeyRing
import com.abrarshakhi.galva.core.vault.data.crypto.PassphraseKdf
import com.abrarshakhi.galva.core.vault.data.crypto.RecoveryPhrase
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.abrarshakhi.galva.core.vault.data.store.VaultStore
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import com.abrarshakhi.galva.core.vault.domain.usecase.MoveToSecretsActions
import com.abrarshakhi.galva.core.vault.domain.usecase.RestoreSecretsUseCase
import com.abrarshakhi.galva.core.vault.domain.usecase.UnlockThrottle
import com.abrarshakhi.galva.features.albums.presentation.AddToAlbumViewModel
import com.abrarshakhi.galva.features.albums.presentation.AlbumDetailViewModel
import com.abrarshakhi.galva.features.albums.presentation.AlbumsViewModel
import com.abrarshakhi.galva.features.gallery.presentation.GalleryViewModel
import com.abrarshakhi.galva.features.search.presentation.SearchViewModel
import com.abrarshakhi.galva.features.secrets.presentation.SecretViewerViewModel
import com.abrarshakhi.galva.features.secrets.presentation.SecretsViewModel
import com.abrarshakhi.galva.features.secrets.presentation.VaultSetupViewModel
import com.abrarshakhi.galva.features.secrets.presentation.VaultUnlockViewModel
import com.abrarshakhi.galva.features.settings.presentation.SettingsViewModel
import com.abrarshakhi.galva.features.viewer.presentation.ViewerViewModel
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * One `CoroutineDispatcher` binding is exposed on purpose: everything that touches disk or the
 * content resolver takes it by injection, so a test can substitute a deterministic dispatcher for
 * the whole data layer at once.
 */
private val coreModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single { SnackbarDispatcher() }
}

private val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GalvaDatabase::class.java,
            GalvaDatabase.NAME,
        )
            // No destructive fallback: the media table rebuilds itself, but favourites and album
            // membership are the user's own and a silent wipe would be unrecoverable.
            .addMigrations(MIGRATION_1_2)
            .addCallback(SecureDeleteCallback)
            .build()
    }
    single { get<GalvaDatabase>().mediaDao() }
    single { get<GalvaDatabase>().favoriteDao() }
    single { get<GalvaDatabase>().userAlbumDao() }
}

private val mediaModule = module {
    single<MediaStoreReader> { MediaStoreDataSource(context = androidContext(), dispatcher = get()) }
    single { MediaStoreObserver(context = androidContext()) }
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

    factory { ObserveTimelineUseCase(mediaRepository = get()) }
    factory { ObserveMediaUseCase(mediaRepository = get()) }
    factory { ObserveAlbumsUseCase(albumRepository = get()) }
    factory { ToggleFavoriteUseCase(mediaRepository = get()) }
    factory { DeleteMediaUseCase(mediaRepository = get()) }
    factory { SyncMediaUseCase(mediaRepository = get()) }
    factory { MediaSelectionActions(toggleFavorite = get(), deleteMedia = get()) }
    factory { ObserveUserAlbumsUseCase(albumRepository = get()) }
    factory { CreateAlbumUseCase(albumRepository = get()) }
    factory { AddToAlbumUseCase(albumRepository = get()) }
    factory { RenameAlbumUseCase(albumRepository = get()) }
    factory { DeleteAlbumUseCase(albumRepository = get()) }
    factory { RemoveFromAlbumUseCase(albumRepository = get()) }
}

private val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(context = androidContext()) }
}

/**
 * Everything that holds or handles vault keys is a single: there is exactly one open vault, and the
 * session that holds its keys must be the one every reader and writer sees.
 */
private val vaultModule = module {
    single { VaultCrypto() }
    single { VaultFiles(root = File(androidContext().noBackupFilesDir, "vault")) }
    single<HardwareKeyRing> { AndroidKeystoreKeyRing(androidContext()) }
    single<PassphraseKdf> {
        val activityManager = androidContext().getSystemService(ActivityManager::class.java)
        Argon2idKdf(lowRamDevice = activityManager.isLowRamDevice)
    }
    single { VaultStore(files = get(), keyRing = get(), kdf = get(), crypto = get()) }
    single { VaultSession() }
    single { VaultContent(session = get(), files = get(), crypto = get()) }
    single<VaultRepository> {
        val context = androidContext()
        VaultRepositoryImpl(
            store = get(),
            files = get(),
            session = get(),
            crypto = get(),
            phrases = lazy { RecoveryPhrase.fromAssets(context) },
            encryptor = VaultImporter(context = context, files = get(), crypto = get()),
            exporter = VaultRestorer(context = context),
            dispatcher = get(),
        )
    }
    single { UnlockThrottle() }

    factory { MoveToSecretsActions(vault = get(), selectionActions = get()) }
    factory { RestoreSecretsUseCase(vault = get(), mediaRepository = get()) }
}

private val presentationModule = module {
    viewModel {
        MainAppViewModel(
            syncMedia = get(),
            mediaStoreObserver = get(),
            settingsRepository = get(),
        )
    }
    viewModel {
        GalleryViewModel(
            observeTimeline = get(),
            settingsRepository = get(),
            mediaRepository = get(),
            selectionActions = get(),
            syncMedia = get(),
            moveToSecrets = get(),
        )
    }
    viewModel { AlbumsViewModel(observeAlbums = get(), settingsRepository = get()) }
    viewModel {
        SearchViewModel(
            observeMedia = get(),
            settingsRepository = get(),
            selectionActions = get(),
            moveToSecrets = get(),
        )
    }
    viewModel { SettingsViewModel(settingsRepository = get()) }
    viewModel {
        SecretsViewModel(
            vault = get(),
            settingsRepository = get(),
            restoreSecrets = get(),
            selectionActions = get(),
        )
    }
    viewModel { VaultSetupViewModel(vault = get()) }
    viewModel { VaultUnlockViewModel(vault = get(), throttle = get()) }
    viewModel { (initialId: Long) ->
        SecretViewerViewModel(initialId = initialId, vault = get(), restoreSecrets = get())
    }
    viewModel {
        AddToAlbumViewModel(
            observeUserAlbums = get(),
            createAlbum = get(),
            addToAlbum = get(),
        )
    }

    viewModel { (albumRef: AlbumRef) ->
        AlbumDetailViewModel(
            albumRef = albumRef,
            observeMedia = get(),
            albumRepository = get(),
            settingsRepository = get(),
            selectionActions = get(),
            renameAlbum = get(),
            deleteAlbum = get(),
            removeFromAlbum = get(),
            moveToSecrets = get(),
        )
    }
    viewModel { (source: MediaSource, initialMediaId: Long) ->
        ViewerViewModel(
            source = source,
            initialMediaId = initialMediaId,
            observeMedia = get(),
            selectionActions = get(),
            moveToSecrets = get(),
        )
    }
}

val appModules = listOf(
    coreModule,
    databaseModule,
    mediaModule,
    settingsModule,
    vaultModule,
    presentationModule,
)
