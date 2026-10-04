package com.abrarshakhi.galva.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.galva.navigation.TopLevelDestination

@Composable
fun AppNavigationBar(
    selectedRoute: NavKey,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShortNavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination.route == selectedRoute
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, selected) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedRoute: NavKey,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    WideNavigationRail(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination.route == selectedRoute
            WideNavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, selected) },
                label = { Text(destination.label) },
                railExpanded = false,
            )
        }
    }
}

@Composable
private fun DestinationIcon(destination: TopLevelDestination, selected: Boolean) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = null,
    )
}
