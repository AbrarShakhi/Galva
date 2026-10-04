package com.abrarshakhi.galva.core.database.di

import androidx.room.Room
import com.abrarshakhi.galva.core.database.GalvaDatabase
import com.abrarshakhi.galva.core.database.MIGRATION_1_2
import com.abrarshakhi.galva.core.database.SecureDeleteCallback
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), GalvaDatabase::class.java, GalvaDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .addCallback(SecureDeleteCallback)
            .build()
    }
    single { get<GalvaDatabase>().mediaDao() }
    single { get<GalvaDatabase>().favoriteDao() }
    single { get<GalvaDatabase>().userAlbumDao() }
}
