package com.abrarshakhi.galva

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.theme.GalvaTheme
import com.abrarshakhi.galva.core.model.AppSettings
import com.abrarshakhi.galva.core.model.ThemeColor
import com.abrarshakhi.galva.core.model.ThemeMode
import com.abrarshakhi.galva.navigation.AppRouteKey
import com.abrarshakhi.galva.ui.AppRoot
import com.abrarshakhi.galva.ui.MainAppViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {

    private val mainAppViewModel: MainAppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { mainAppViewModel.settings.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by mainAppViewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            val darkTheme = current.isDarkTheme()

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            GalvaTheme(
                darkTheme = darkTheme,
                seedColor = ComposeColor(current.themeColor.seedArgb),
                useWallpaperColors = current.themeColor == ThemeColor.WALLPAPER,
                pureBlack = current.pureBlack,
            ) {
                AppRoot(
                    startRoute = AppRouteKey.Gallery,
                    mainAppViewModel = mainAppViewModel,
                )
            }
        }
    }
}

@Composable
private fun AppSettings.isDarkTheme(): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
