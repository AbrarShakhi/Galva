package com.abrarshakhi.galva.common.main

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.util.ChromeLayout

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
    AppRouteKey.Gallery -> ScreenChrome()
    AppRouteKey.Albums -> ScreenChrome()
    AppRouteKey.Search -> ScreenChrome()
}