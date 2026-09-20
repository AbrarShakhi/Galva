package com.abrarshakhi.galva.common.di

import com.abrarshakhi.galva.common.main.MainAppViewModel
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreObserver
import com.abrarshakhi.galva.core.media.domain.usecase.SyncMediaUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val coreModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single { SnackbarDispatcher() }
}

private val mediaModule = module {
    single { MediaStoreObserver(context = androidContext()) }
    factory { SyncMediaUseCase(mediaRepository = get()) }
}

private val presentationModule = module {
    viewModel {
        MainAppViewModel(
            syncMedia = get(),
            mediaStoreObserver = get(),
            settingsRepository = get(),
        )
    }
}

val appModules = listOf(
    coreModule,
    mediaModule,
    presentationModule,
)
