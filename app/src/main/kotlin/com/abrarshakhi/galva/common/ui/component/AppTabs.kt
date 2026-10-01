package com.abrarshakhi.galva.common.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.util.ChromeLayout

private data class TabSpec(
    val route: AppRouteKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val Tabs = listOf(
    TabSpec(AppRouteKey.Gallery, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    TabSpec(AppRouteKey.Albums, "Albums", Icons.Filled.PhotoAlbum, Icons.Outlined.PhotoAlbum),
    TabSpec(AppRouteKey.Search, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    TabSpec(AppRouteKey.Secrets, "Secrets", Icons.Filled.Lock, Icons.Outlined.Lock),
)

@Composable
fun AppTabs(
    layout: ChromeLayout,
    current: AppRouteKey?,
    onTabSelected: (AppRouteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (layout) {
        ChromeLayout.BottomBar -> AppBottomBar(current, onTabSelected, modifier)
        ChromeLayout.Rail -> AppNavigationRail(current, onTabSelected, modifier)
    }
}

@Composable
private fun AppBottomBar(
    current: AppRouteKey?,
    onTabSelected: (AppRouteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        Tabs.forEach { tab ->
            val isSelected = tab.route == current
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) })
        }
    }
}

@Composable
private fun AppNavigationRail(
    current: AppRouteKey?,
    onTabSelected: (AppRouteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        Tabs.forEach { tab ->
            val isSelected = tab.route == current
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) })
        }
    }
}
