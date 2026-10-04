package com.abrarshakhi.galva.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val commonModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
}
