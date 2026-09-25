package com.abrarshakhi.galva.features.gallery.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.main.ScreenChrome
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.AppTabs
import com.abrarshakhi.galva.common.ui.component.SelectionActions
import com.abrarshakhi.galva.common.ui.component.SelectionTopBar
import com.abrarshakhi.galva.common.ui.selection.selectedBy

/**
 * Chrome for the Home tab.
 *
 * Both bars swap wholesale once a selection exists — the same trade Ente makes — so the actions
 * that apply to the selection sit where the navigation was, within thumb reach.
 */
fun galleryChrome(): ScreenChrome = ScreenChrome(
    topBar = { scope ->
        val viewModel: GalleryViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            SelectionTopBar(
                count = state.selection.count,
                allSelected = state.allSelected,
                onClear = { viewModel.onIntent(GalleryIntent.ClearSelection) },
                onSelectAll = { viewModel.onIntent(GalleryIntent.SelectAll) },
                scrollBehavior = scope.scrollBehavior,
            )
        } else {
            GalleryTopBar(
                isSyncing = state.isSyncing,
                onOpenDrawer = scope.openDrawer,
                scrollBehavior = scope.scrollBehavior,
            )
        }
    },
    navigation = { scope ->
        val viewModel: GalleryViewModel = appViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.selection.isActive) {
            val selected = state.items.selectedBy(state.selection)
            SelectionActions(
                layout = scope.layout,
                anySelected = selected.isNotEmpty(),
                allFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onShare = { viewModel.onIntent(GalleryIntent.ShareSelection) },
                onFavorite = { viewModel.onIntent(GalleryIntent.FavoriteSelection) },
                onAddToAlbum = { viewModel.onIntent(GalleryIntent.AddToAlbumRequested) },
                onDelete = { viewModel.onIntent(GalleryIntent.DeleteSelection) },
                onMoveToSecrets = { viewModel.onIntent(GalleryIntent.MoveToSecretsSelection) },
            )
        } else {
            AppTabs(
                layout = scope.layout,
                current = scope.currentRoute,
                onTabSelected = scope.switchTab,
            )
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    isSyncing: Boolean,
    onOpenDrawer: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = { Text("Galva") },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = "Open settings")
            }
        },
        actions = {
            if (isSyncing) {
                LinearProgressIndicator(modifier = Modifier.width(48.dp))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
