package com.abrarshakhi.galva.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

enum class TopLevelDestination(
    val route: AppRouteKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Gallery(AppRouteKey.Gallery, "Photos", Icons.Rounded.Home, Icons.Outlined.Home),
    Albums(AppRouteKey.Albums, "Albums", Icons.Rounded.PhotoAlbum, Icons.Outlined.PhotoAlbum),
    Search(AppRouteKey.Search, "Search", Icons.Rounded.Search, Icons.Outlined.Search),
    Secrets(AppRouteKey.Secrets, "Secrets", Icons.Rounded.Lock, Icons.Outlined.Lock),
}

val TOP_LEVEL_ROUTES: Set<NavKey> = TopLevelDestination.entries.mapTo(LinkedHashSet()) { it.route }

val NavKey.isTopLevel: Boolean get() = this in TOP_LEVEL_ROUTES
