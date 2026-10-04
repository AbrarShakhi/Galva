package com.abrarshakhi.galva.core.ui.album

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.key
import com.abrarshakhi.galva.core.ui.component.AlbumListItem
import com.abrarshakhi.galva.core.ui.component.itemCountLabel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToAlbumSheet(
    mediaIds: List<Long>,
    onDismiss: () -> Unit,
    onAdded: (albumName: String) -> Unit,
) {
    val viewModel: AddToAlbumViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    state.addedTo?.let { name ->
        LaunchedEffect(name) {
            onAdded(name)
            viewModel.onAddedHandled()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            item {
                Text(
                    text = "Add to album",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Text(
                    text = itemCountLabel(mediaIds.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            item {
                val height = ButtonDefaults.MediumContainerHeight
                FilledTonalButton(
                    onClick = { creating = true },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                    )
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    Text("New album", style = ButtonDefaults.textStyleFor(height))
                }
            }
            items(state.albums, key = { it.ref.key }) { album ->
                AlbumListItem(
                    album = album,
                    onClick = {
                        val ref = album.ref
                        if (ref is AlbumRef.User) viewModel.addTo(ref, album.name, mediaIds)
                    },
                )
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
