package com.abrarshakhi.galva.core.vault.di

import android.app.ActivityManager
import com.abrarshakhi.galva.core.common.di.commonModule
import com.abrarshakhi.galva.core.vault.VaultRepository
import com.abrarshakhi.galva.core.vault.crypto.AndroidKeystoreKeyRing
import com.abrarshakhi.galva.core.vault.crypto.Argon2idKdf
import com.abrarshakhi.galva.core.vault.crypto.HardwareKeyRing
import com.abrarshakhi.galva.core.vault.crypto.PassphraseKdf
import com.abrarshakhi.galva.core.vault.crypto.RecoveryPhrase
import com.abrarshakhi.galva.core.vault.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.data.VaultImporter
import com.abrarshakhi.galva.core.vault.data.VaultRepositoryImpl
import com.abrarshakhi.galva.core.vault.data.VaultRestorer
import com.abrarshakhi.galva.core.vault.data.VaultSession
import com.abrarshakhi.galva.core.vault.media.VaultMedia
import com.abrarshakhi.galva.core.vault.store.VaultFiles
import com.abrarshakhi.galva.core.vault.store.VaultStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val vaultModule = module {
    includes(commonModule)

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
    single { VaultMedia(content = get()) }
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
}
