package com.abrarshakhi.galva.common.main

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.util.ChromeLayout
import com.abrarshakhi.galva.features.gallery.presentation.galleryChrome

data class ChromeScope(
    val backStack: SnapshotStateList<AppRouteKey>,
    val currentRoute: AppRouteKey?,
    val scrollBehavior: TopAppBarScrollBehavior,
    val openDrawer: () -> Unit,
    val switchTab: (AppRouteKey) -> Unit,
    val layout: ChromeLayout,
)

class ScreenChrome(
    val topBar: @Composable (ChromeScope) -> Unit = {},
    val navigation: @Composable (ChromeScope) -> Unit = {},
    val fab: @Composable (ChromeScope) -> Unit = {},
    val immersive: Boolean = false,
)

fun AppRouteKey.chrome(): ScreenChrome = when (this) {
    is AppRouteKey.Gallery -> galleryChrome()
    is AppRouteKey.Albums -> ScreenChrome()
    is AppRouteKey.Search -> ScreenChrome()
    is AppRouteKey.Viewer -> ScreenChrome()
}