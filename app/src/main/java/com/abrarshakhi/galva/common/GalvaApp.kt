package com.abrarshakhi.galva.common

import android.app.Application
import com.abrarshakhi.galva.BuildConfig
import com.abrarshakhi.galva.common.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class GalvaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@GalvaApp)
            modules(appModules)
        }
    }
}
