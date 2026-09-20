package com.abrarshakhi.galva.features.albums.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.key
import org.koin.androidx.compose.koinViewModel

/**
 * Picks an album for the current selection, or makes a new one.
 *
 * Self-contained on purpose: the screens that show it only supply the ids and hear back when the
 * work is done, so adding this action to a fourth surface costs a flag and a callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToAlbumSheet(
    mediaIds: List<Long>,
    onDismiss: () -> Unit,
    onAdded: (albumName: String) -> Unit,
) {
    val viewModel: AddToAlbumViewModel = koinViewModel()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.added.collect { name -> onAdded(name) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = "Add ${mediaIds.size} to album",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = GalvaDimens.ScreenPadding,
                    end = GalvaDimens.ScreenPadding,
                    bottom = GalvaDimens.ItemSpacing,
                ),
            )

            SheetRow(
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                label = "New album",
                emphasised = true,
                onClick = { creating = true },
            )

            LazyColumn {
                items(albums, key = { it.ref.key }) { album ->
                    SheetRow(
                        icon = {
                            Icon(Icons.Outlined.PhotoAlbum, contentDescription = null)
                        },
                        label = album.name,
                        secondary = album.itemCount.toString(),
                        onClick = {
                            val ref = album.ref
                            if (ref is AlbumRef.User) viewModel.addTo(ref, album.name, mediaIds)
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        AlbumNameDialog(
            title = "New album",
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                viewModel.createAndAdd(name, mediaIds)
            },
        )
    }
}

@Composable
private fun SheetRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    secondary: String? = null,
    emphasised: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = GalvaDimens.ScreenPadding, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(GalvaDimens.ItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.size(24.dp)) { icon() }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (emphasised) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (secondary != null) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
