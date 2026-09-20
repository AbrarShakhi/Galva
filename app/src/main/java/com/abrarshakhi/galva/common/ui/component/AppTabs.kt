package com.abrarshakhi.galva.common.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.common.navigation.AppRouteKey
import com.abrarshakhi.galva.common.ui.theme.EaseOutExpo
import com.abrarshakhi.galva.common.ui.theme.LocalGalvaPalette
import com.abrarshakhi.galva.common.ui.theme.NavTransitionMillis
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
    val palette = LocalGalvaPalette.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.navBarContainer,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tabs.forEach { tab ->
                NavPill(
                    tab = tab,
                    selected = current == tab.route,
                    onClick = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@Composable
private fun AppNavigationRail(
    current: AppRouteKey?,
    onTabSelected: (AppRouteKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGalvaPalette.current

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = palette.navBarContainer,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = RailWidth)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tabs.forEach { tab ->
                RailPill(
                    tab = tab,
                    selected = current == tab.route,
                    onClick = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@Composable
private fun NavPill(
    tab: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalGalvaPalette.current
    val animation = tween<Color>(durationMillis = NavTransitionMillis, easing = EaseOutExpo)

    val pillColor by animateColorAsState(
        targetValue = if (selected) palette.navBarActivePill else Color.Transparent,
        animationSpec = animation,
        label = "navPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = animation,
        label = "navPillContent",
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(pillColor)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIcon(tab = tab, selected = selected, tint = contentColor)
        PillLabel(tab = tab, visible = selected, color = contentColor)
    }
}

@Composable
private fun RailPill(
    tab: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalGalvaPalette.current
    val animation = tween<Color>(durationMillis = NavTransitionMillis, easing = EaseOutExpo)

    val pillColor by animateColorAsState(
        targetValue = if (selected) palette.navBarActivePill else Color.Transparent,
        animationSpec = animation,
        label = "railPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = animation,
        label = "railPillContent",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(RailPillCorner))
            .background(pillColor)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .widthIn(min = 56.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PillIcon(tab = tab, selected = selected, tint = contentColor)
        AnimatedVisibility(
            visible = selected,
            enter = expandVertically(tween(NavTransitionMillis, easing = EaseOutExpo)) + fadeIn(
                tween(NavTransitionMillis, easing = EaseOutExpo)
            ),
            exit = shrinkVertically(tween(NavTransitionMillis, easing = EaseOutExpo)) + fadeOut(
                tween(NavTransitionMillis, easing = EaseOutExpo)
            ),
        ) {
            Text(
                text = tab.label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun PillIcon(tab: TabSpec, selected: Boolean, tint: Color) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navIconScale",
    )

    Crossfade(
        targetState = selected,
        animationSpec = tween(NavTransitionMillis, easing = EaseOutExpo),
        label = "navIconGlyph",
        modifier = Modifier.scale(scale),
    ) { isSelected ->
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            // When selected, the adjacent label already names the destination.
            contentDescription = if (isSelected) null else tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PillLabel(tab: TabSpec, visible: Boolean, color: Color) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(
            animationSpec = tween(NavTransitionMillis, easing = EaseOutExpo),
            expandFrom = Alignment.Start,
        ) + fadeIn(tween(NavTransitionMillis, easing = EaseOutExpo)),
        exit = shrinkHorizontally(
            animationSpec = tween(NavTransitionMillis, easing = EaseOutExpo),
            shrinkTowards = Alignment.Start,
        ) + fadeOut(tween(NavTransitionMillis, easing = EaseOutExpo)),
    ) {
        Text(
            text = tab.label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private val RailWidth = 72.dp
private val RailPillCorner = 16.dp
