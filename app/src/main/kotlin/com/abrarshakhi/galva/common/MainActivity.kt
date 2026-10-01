package com.abrarshakhi.galva.common

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.main.AppRoot
import com.abrarshakhi.galva.common.main.MainAppViewModel
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.theme.GalvaTheme
import com.abrarshakhi.galva.core.settings.domain.ThemeMode
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val mainAppViewModel: MainAppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by mainAppViewModel.themeMode.collectAsStateWithLifecycle()
            GalvaTheme(darkTheme = themeMode.resolveDark()) {
                AppRoot(
                    startRoute = AppRouteKey.Gallery,
                    mainAppViewModel = mainAppViewModel,
                )
            }
        }
    }
}

@Composable
private fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
