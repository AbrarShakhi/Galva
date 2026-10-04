package com.abrarshakhi.galva.core.designsystem.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalvaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = GalvaBrandColor,
    useWallpaperColors: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val seed = if (useWallpaperColors && supportsWallpaperColors()) {
        Color(LocalContext.current.getColor(android.R.color.system_accent1_500))
    } else {
        seedColor
    }
    DynamicMaterialExpressiveTheme(
        seedColor = seed,
        motionScheme = MotionScheme.expressive(),
        isDark = darkTheme,
        isAmoled = pureBlack && darkTheme,
        style = PaletteStyle.Expressive,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        animate = true,
        content = content,
    )
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsWallpaperColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val GalvaBrandColor = Color(0xFF1DB954)
