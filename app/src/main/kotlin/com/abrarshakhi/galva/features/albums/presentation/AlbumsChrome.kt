package com.abrarshakhi.galva.features.albums.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.main.ScreenChrome
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.AppTabs
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType

fun albumsChrome(): ScreenChrome = ScreenChrome(
    topBar = { scope ->
        val viewModel: AlbumsViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        AlbumsTopBar(
            viewType = state.viewType,
            sort = state.sort,
            onToggleViewType = { viewModel.onIntent(AlbumsIntent.ViewTypeToggled) },
            onSortSelected = { viewModel.onIntent(AlbumsIntent.SortSelected(it)) },
            scrollBehavior = scope.scrollBehavior,
        )
    },
    navigation = { scope ->
        AppTabs(
            layout = scope.layout,
            current = scope.currentRoute,
            onTabSelected = scope.switchTab,
        )
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumsTopBar(
    viewType: AlbumViewType,
    sort: AlbumSort,
    onToggleViewType: () -> Unit,
    onSortSelected: (AlbumSort) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Albums") },
        actions = {
            IconButton(onClick = onToggleViewType) {
                Icon(
                    imageVector = if (viewType == AlbumViewType.GRID) Icons.AutoMirrored.Filled.List
                    else Icons.Filled.GridView,
                    contentDescription = if (viewType == AlbumViewType.GRID) "Show as list"
                    else "Show as grid",
                )
            }
            IconButton(onClick = { sortMenuOpen = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort albums")
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        leadingIcon = {
                            RadioButton(selected = option.sort == sort, onClick = null)
                        },
                        onClick = {
                            onSortSelected(option.sort)
                            sortMenuOpen = false
                        },
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

private enum class SortOption(val sort: AlbumSort, val label: String) {
    Recent(AlbumSort.RECENT_FIRST, "Last updated"),
    Oldest(AlbumSort.OLDEST_FIRST, "Oldest first"),
    NameAsc(AlbumSort.NAME_ASC, "Name (A–Z)"),
    NameDesc(AlbumSort.NAME_DESC, "Name (Z–A)"),
}
