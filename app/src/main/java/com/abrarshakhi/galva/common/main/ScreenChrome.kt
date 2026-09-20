package com.abrarshakhi.galva.common.main

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.util.ChromeLayout
import com.abrarshakhi.galva.features.albums.presentation.albumsChrome
import com.abrarshakhi.galva.features.gallery.presentation.galleryChrome
import com.abrarshakhi.galva.features.search.presentation.searchChrome

/** What a screen's chrome is given to work with. */
data class ChromeScope(
    val backStack: SnapshotStateList<AppRouteKey>,
    val currentRoute: AppRouteKey?,
    val scrollBehavior: TopAppBarScrollBehavior,
    val openDrawer: () -> Unit,
    val switchTab: (AppRouteKey) -> Unit,
    /** Whether this window puts the tab navigation along the bottom or down the leading edge. */
    val layout: ChromeLayout,
)

/**
 * Bars the shared [AppRoot] Scaffold renders on a screen's behalf.
 *
 * Not a data class: the fields are composable lambdas, for which equality is meaningless.
 */
class ScreenChrome(
    val topBar: @Composable (ChromeScope) -> Unit = {},
    /**
     * Tab navigation, or whatever replaces it. Rendered as a bottom bar or as a leading rail
     * depending on [ChromeScope.layout]; the chrome supplies the content, not the placement.
     */
    val navigation: @Composable (ChromeScope) -> Unit = {},
    val fab: @Composable (ChromeScope) -> Unit = {},
    /**
     * Draw behind the system bars instead of inside the Scaffold's safe-drawing insets. The
     * viewer needs this: a photo letterboxed away from the screen edges is not full screen.
     */
    val immersive: Boolean = false,
)

/**
 * Only the bottom-bar tabs put their chrome here.
 *
 * Pushed screens return empty chrome and draw their own bar inside the navigation entry, because
 * their titles and actions come from an entry-scoped ViewModel that the shared Scaffold — which
 * lives outside `NavDisplay` — cannot reach.
 */
fun AppRouteKey.chrome(): ScreenChrome = when (this) {
    AppRouteKey.Gallery -> galleryChrome()
    AppRouteKey.Albums -> albumsChrome()
    AppRouteKey.Search -> searchChrome()
    is AppRouteKey.AlbumDetail -> ScreenChrome()
    is AppRouteKey.Viewer -> ScreenChrome(immersive = true)
}
