package com.abrarshakhi.galva.features.albums.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.AlbumListItem
import com.abrarshakhi.galva.common.ui.component.AlbumTile
import com.abrarshakhi.galva.common.ui.component.EmptyState
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.common.ui.util.albumGridColumns
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.key
import com.abrarshakhi.galva.core.settings.domain.AlbumViewType

@Composable
fun AlbumsScreen(
    onOpenAlbum: (AlbumRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlbumsViewModel = appViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is AlbumsEffect.OpenAlbum -> onOpenAlbum(effect.albumRef)
        }
    }

    if (state.isEmpty) {
        EmptyState(
            icon = Icons.Outlined.PhotoAlbum,
            title = "No albums",
            message = "Folders containing photos or videos will show up here.",
            modifier = modifier,
        )
        return
    }

    val albums = state.visibleAlbums
    when (state.viewType) {
        // Measured rather than assumed: the column count depends on the height actually left
        // over after the app bar and navigation bar have taken theirs.
        AlbumViewType.GRID -> BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val columns = albumGridColumns(
                availableWidth = maxWidth - GalvaDimens.ScreenPadding * 2,
                availableHeight = maxHeight,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = GalvaDimens.ScreenPadding,
                    end = GalvaDimens.ScreenPadding,
                    top = GalvaDimens.ItemSpacing,
                    bottom = GalvaDimens.ItemSpacing,
                ),
                horizontalArrangement = Arrangement.spacedBy(GalvaDimens.AlbumGridSpacing),
                verticalArrangement = Arrangement.spacedBy(GalvaDimens.AlbumGridSpacing),
            ) {
                items(albums, key = { it.ref.key }) { album ->
                    AlbumTile(
                        album = album,
                        onClick = { viewModel.onIntent(AlbumsIntent.AlbumTapped(album)) },
                    )
                }
            }
        }

        AlbumViewType.LIST -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = GalvaDimens.ItemSpacing),
        ) {
            items(albums, key = { it.ref.key }) { album ->
                AlbumListItem(
                    album = album,
                    onClick = { viewModel.onIntent(AlbumsIntent.AlbumTapped(album)) },
                )
            }
        }
    }
}
