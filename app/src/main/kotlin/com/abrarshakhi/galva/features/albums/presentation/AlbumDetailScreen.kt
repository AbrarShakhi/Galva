package com.abrarshakhi.galva.features.albums.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.component.DetailTopBar
import com.abrarshakhi.galva.common.ui.component.EmptyState
import com.abrarshakhi.galva.common.ui.component.MediaGrid
import com.abrarshakhi.galva.common.ui.component.SelectionActions
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.key
import com.abrarshakhi.galva.core.media.ui.rememberMediaDeleteLauncher
import com.abrarshakhi.galva.core.share.MediaSharing
import com.abrarshakhi.galva.common.ui.selection.selectedBy
import com.abrarshakhi.galva.common.ui.util.ChromeLayout
import com.abrarshakhi.galva.common.ui.util.rememberChromeLayout
import com.abrarshakhi.galva.features.secrets.presentation.BlockingProgress
import com.abrarshakhi.galva.features.secrets.presentation.VaultUnlockSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * A pushed screen, so it draws its own top bar instead of going through [ScreenChrome]: the title
 * comes from this entry's ViewModel, which the shared Scaffold cannot reach.
 */
@Composable
fun AlbumDetailScreen(
    albumRef: AlbumRef,
    onBack: () -> Unit,
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlbumDetailViewModel =
        koinViewModel(key = "album-${albumRef.key}") { parametersOf(albumRef) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar: SnackbarDispatcher = koinInject()

    val deleteLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(AlbumDetailIntent.DeleteResolved(ids, confirmed))
    }
    val moveLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(AlbumDetailIntent.MoveResolved(ids, confirmed))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is AlbumDetailEffect.OpenViewer -> onOpenViewer(effect.source, effect.mediaId)

            is AlbumDetailEffect.ShareItems ->
                MediaSharing.chooserFor(effect.uris, effect.mimeTypes)?.let(context::startActivity)

            is AlbumDetailEffect.ConfirmDelete -> deleteLauncher.request(effect.ids, effect.uris)

            is AlbumDetailEffect.ConfirmMove -> moveLauncher.request(effect.ids, effect.uris)

            is AlbumDetailEffect.ShowMessage -> snackbar.show(effect.text)

            AlbumDetailEffect.AlbumDeleted -> onBack()
        }
    }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(AlbumDetailIntent.ClearSelection)
    }

    if (state.showRenameDialog) {
        AlbumNameDialog(
            title = "Rename album",
            confirmLabel = "Rename",
            initialName = state.albumName,
            onDismiss = { viewModel.onIntent(AlbumDetailIntent.RenameDismissed) },
            onConfirm = { name -> viewModel.onIntent(AlbumDetailIntent.RenameConfirmed(name)) },
        )
    }

    if (state.showDeleteAlbumDialog) {
        DeleteAlbumDialog(
            albumName = state.albumName,
            onDismiss = { viewModel.onIntent(AlbumDetailIntent.DeleteAlbumDismissed) },
            onConfirm = { viewModel.onIntent(AlbumDetailIntent.DeleteAlbumConfirmed) },
        )
    }

    if (state.showAddToAlbum) {
        AddToAlbumSheet(
            mediaIds = state.items.selectedBy(state.selection).map(MediaItem::id),
            onDismiss = { viewModel.onIntent(AlbumDetailIntent.AddToAlbumDismissed) },
            onAdded = { name -> viewModel.onIntent(AlbumDetailIntent.AddedToAlbum(name)) },
        )
    }

    if (state.showVaultUnlock) {
        VaultUnlockSheet(
            onDismiss = { viewModel.onIntent(AlbumDetailIntent.VaultUnlockDismissed) },
            onUnlocked = { viewModel.onIntent(AlbumDetailIntent.VaultUnlocked) },
        )
    }

    state.moveProgress?.let { BlockingProgress(it.label) }


    val layout = rememberChromeLayout()
    val selected = state.items.selectedBy(state.selection)

    // Mirrors the shell: in a landscape window the selection actions take the leading edge rather
    // than stealing another band of height from the grid.
    Row(modifier = modifier.fillMaxSize()) {
        if (layout == ChromeLayout.Rail && state.selection.isActive) {
            SelectionActions(
                layout = ChromeLayout.Rail,
                anySelected = selected.isNotEmpty(),
                allFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onShare = { viewModel.onIntent(AlbumDetailIntent.ShareSelection) },
                onFavorite = { viewModel.onIntent(AlbumDetailIntent.FavoriteSelection) },
                onAddToAlbum = { viewModel.onIntent(AlbumDetailIntent.AddToAlbumRequested) },
                onRemoveFromAlbum = if (state.canManage) {
                    { viewModel.onIntent(AlbumDetailIntent.RemoveFromAlbum) }
                } else {
                    null
                },
                onDelete = { viewModel.onIntent(AlbumDetailIntent.DeleteSelection) },
                onMoveToSecrets = { viewModel.onIntent(AlbumDetailIntent.MoveToSecretsSelection) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
        DetailTopBar(
            title = state.title,
            onBack = {
                if (state.selection.isActive) viewModel.onIntent(AlbumDetailIntent.ClearSelection)
                else onBack()
            },
            actions = {
                if (state.selection.isActive && !state.allSelected) {
                    IconButton(onClick = { viewModel.onIntent(AlbumDetailIntent.SelectAll) }) {
                        Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                    }
                }
                // Device folders are not ours to rename or delete.
                if (state.canManage && !state.selection.isActive) {
                    AlbumOverflowMenu(
                        onRename = { viewModel.onIntent(AlbumDetailIntent.RenameRequested) },
                        onDelete = { viewModel.onIntent(AlbumDetailIntent.DeleteAlbumRequested) },
                    )
                }
            },
        )

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Outlined.PhotoLibrary,
                title = "Nothing here",
                modifier = Modifier.weight(1f),
            )
        } else {
            MediaGrid(
                items = state.items,
                selection = state.selection,
                preferredColumns = state.columns,
                onItemClick = { viewModel.onIntent(AlbumDetailIntent.MediaTapped(it)) },
                onItemLongClick = { viewModel.onIntent(AlbumDetailIntent.MediaLongPressed(it)) },
                modifier = Modifier.weight(1f),
            )
        }

        if (layout == ChromeLayout.BottomBar && state.selection.isActive) {
            SelectionActions(
                layout = ChromeLayout.BottomBar,
                anySelected = selected.isNotEmpty(),
                allFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onShare = { viewModel.onIntent(AlbumDetailIntent.ShareSelection) },
                onFavorite = { viewModel.onIntent(AlbumDetailIntent.FavoriteSelection) },
                onAddToAlbum = { viewModel.onIntent(AlbumDetailIntent.AddToAlbumRequested) },
                onRemoveFromAlbum = if (state.canManage) {
                    { viewModel.onIntent(AlbumDetailIntent.RemoveFromAlbum) }
                } else {
                    null
                },
                onDelete = { viewModel.onIntent(AlbumDetailIntent.DeleteSelection) },
                onMoveToSecrets = { viewModel.onIntent(AlbumDetailIntent.MoveToSecretsSelection) },
            )
        }
        }
    }
}

@Composable
private fun AlbumOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Album options")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
                open = false
                onRename()
            },
        )
        DropdownMenuItem(
            text = { Text("Delete album") },
            onClick = {
                open = false
                onDelete()
            },
        )
    }
}

@Composable
private fun DeleteAlbumDialog(
    albumName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"$albumName\"?") },
        // Worth stating plainly: this album is only a grouping, so nothing is lost with it.
        text = { Text("The photos in it stay on your device.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
