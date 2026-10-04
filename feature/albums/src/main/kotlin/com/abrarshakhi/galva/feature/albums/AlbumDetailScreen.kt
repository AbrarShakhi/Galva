package com.abrarshakhi.galva.feature.albums

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.AdaptiveTopAppBar
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.model.key
import com.abrarshakhi.galva.core.ui.actions.MediaActionsHost
import com.abrarshakhi.galva.core.ui.album.AlbumNameDialog
import com.abrarshakhi.galva.core.ui.component.MediaActionToolbar
import com.abrarshakhi.galva.core.ui.component.MediaGrid
import com.abrarshakhi.galva.core.ui.component.SelectionTopBar
import com.abrarshakhi.galva.core.ui.component.ToolbarAction
import com.abrarshakhi.galva.core.ui.component.itemCountLabel
import com.abrarshakhi.galva.core.ui.component.rememberToolbarAwarePadding
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import com.abrarshakhi.galva.core.ui.selection.selectedBy
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AlbumDetailRoute(
    albumRef: AlbumRef,
    onBack: () -> Unit,
    onOpenViewer: (MediaSource, Long) -> Unit,
) {
    AlbumDetailScreen(
        viewModel = koinViewModel(key = "album-${albumRef.key}") { parametersOf(albumRef) },
        onBack = onBack,
        onOpenViewer = onOpenViewer,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onBack: () -> Unit,
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = rememberWindowLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val motion = MaterialTheme.motionScheme

    MediaActionsHost(state.actions) { viewModel.onIntent(AlbumDetailIntent.ActionEvent(it)) }
    UserMessagesEffect(state.messages) { viewModel.onIntent(AlbumDetailIntent.MessageShown(it)) }

    if (state.isDeleted) {
        LaunchedEffect(Unit) { onBack() }
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = state.selection.isActive,
                transitionSpec = {
                    fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                label = "albumTopBar",
            ) { selecting ->
                if (selecting) {
                    SelectionTopBar(
                        count = state.selection.count,
                        allSelected = state.allSelected,
                        onClear = { viewModel.onIntent(AlbumDetailIntent.ClearSelection) },
                        onSelectAll = { viewModel.onIntent(AlbumDetailIntent.SelectAll) },
                    )
                } else {
                    AlbumTopBar(
                        name = state.albumName,
                        itemCount = state.items.size,
                        canManage = state.canManage,
                        onBack = onBack,
                        onRename = { viewModel.onIntent(AlbumDetailIntent.RenameRequested) },
                        onDelete = { viewModel.onIntent(AlbumDetailIntent.DeleteAlbumRequested) },
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                state.isLoading -> ContainedLoadingIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                )

                state.isEmpty -> EmptyState(
                    illustration = Illustration.EmptyAlbum,
                    title = "Nothing here yet",
                    message = "Add photos from your timeline with the album button.",
                )

                else -> MediaGrid(
                    items = state.items,
                    selection = state.selection,
                    preferredColumns = state.columns,
                    onItemClick = { item ->
                        if (state.selection.isActive) {
                            viewModel.onIntent(AlbumDetailIntent.ToggleSelection(item))
                        } else {
                            onOpenViewer(state.source, item.id)
                        }
                    },
                    onItemLongClick = { viewModel.onIntent(AlbumDetailIntent.ToggleSelection(it)) },
                    contentPadding = rememberToolbarAwarePadding(
                        toolbarVisible = state.selection.isActive,
                        layout = layout,
                        extraBottom = padding.calculateBottomPadding(),
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val selected = state.items.selectedBy(state.selection)
            MediaActionToolbar(
                visible = state.selection.isActive,
                isFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onAction = { viewModel.onIntent(AlbumDetailIntent.Perform(it)) },
                layout = layout,
                extraActions = if (state.canManage) {
                    listOf(
                        ToolbarAction(Icons.Rounded.PlaylistRemove, "Remove from album") {
                            viewModel.onIntent(AlbumDetailIntent.RemoveFromAlbum)
                        },
                    )
                } else {
                    emptyList()
                },
                modifier = Modifier
                    .align(toolbarAlignment(layout))
                    .padding(bottom = padding.calculateBottomPadding())
                    .padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlbumTopBar(
    name: String,
    itemCount: Int,
    canManage: Boolean,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    AdaptiveTopAppBar(
        title = { Text(name, maxLines = 1) },
        subtitle = { Text(itemCountLabel(itemCount)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (canManage) AlbumOverflowMenu(onRename = onRename, onDelete = onDelete)
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun AlbumOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Album options")
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.large,
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete album", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
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
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text("Delete \"$albumName\"?") },
        text = { Text("The photos in it stay on your device.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
