package com.abrarshakhi.galva.core.mediastore.di

import com.abrarshakhi.galva.core.common.di.commonModule
import com.abrarshakhi.galva.core.mediastore.MediaStoreDataSource
import com.abrarshakhi.galva.core.mediastore.MediaStoreObserver
import com.abrarshakhi.galva.core.mediastore.MediaStoreReader
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mediaStoreModule = module {
    includes(commonModule)
    single<MediaStoreReader> { MediaStoreDataSource(context = androidContext(), dispatcher = get()) }
    single { MediaStoreObserver(context = androidContext()) }
}
