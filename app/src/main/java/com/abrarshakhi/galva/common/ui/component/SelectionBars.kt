package com.abrarshakhi.galva.common.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abrarshakhi.galva.common.ui.util.ChromeLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            if (!allSelected) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = selectionBarColors(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        // The Scaffold already inset this screen below the status bar.
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun selectionBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)

@Composable
fun SelectionActions(
    layout: ChromeLayout,
    anySelected: Boolean,
    allFavorite: Boolean,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onAddToAlbum: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromAlbum: (() -> Unit)? = null,
    onMoveToSecrets: (() -> Unit)? = null,
) {
    SelectionActionsContainer(layout = layout, modifier = modifier) {
        SelectionAction(
            icon = Icons.Filled.Share,
            label = "Share",
            enabled = anySelected,
            onClick = onShare,
        )
        SelectionAction(
            icon = if (allFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            label = "Favorite",
            enabled = anySelected,
            onClick = onFavorite,
        )
        SelectionAction(
            icon = Icons.Filled.LibraryAdd,
            label = "Add to album",
            enabled = anySelected,
            onClick = onAddToAlbum,
        )
        if (onRemoveFromAlbum != null) {
            SelectionAction(
                icon = Icons.Filled.PlaylistRemove,
                label = "Remove from album",
                enabled = anySelected,
                onClick = onRemoveFromAlbum,
            )
        }
        if (onMoveToSecrets != null) {
            SelectionAction(
                icon = Icons.Filled.Lock,
                label = "Move to Secrets",
                enabled = anySelected,
                onClick = onMoveToSecrets,
            )
        }
        SelectionAction(
            icon = Icons.Filled.Delete,
            label = "Delete",
            enabled = anySelected,
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun SelectionActionsContainer(
    layout: ChromeLayout,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    when (layout) {
        ChromeLayout.BottomBar -> Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                actions()
            }
        }

        ChromeLayout.Rail -> Surface(
            modifier = modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                actions()
            }
        }
    }
}

@Composable
fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = label, tint = tint)
    }
}
