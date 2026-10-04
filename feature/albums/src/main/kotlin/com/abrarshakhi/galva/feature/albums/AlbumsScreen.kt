package com.abrarshakhi.galva.feature.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowSize
import com.abrarshakhi.galva.core.designsystem.theme.GalvaDimens
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.AlbumViewType
import com.abrarshakhi.galva.core.model.key
import com.abrarshakhi.galva.core.ui.appViewModel
import com.abrarshakhi.galva.core.ui.component.AlbumListItem
import com.abrarshakhi.galva.core.ui.component.AlbumTile
import com.abrarshakhi.galva.core.ui.util.albumGridColumns

@Composable
fun AlbumsRoute(onOpenAlbum: (AlbumRef) -> Unit) {
    AlbumsScreen(
        viewModel = appViewModel(),
        onOpenAlbum = onOpenAlbum,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onOpenAlbum: (AlbumRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AlbumsTopBar(
                albumCount = state.visibleAlbums.size,
                viewType = state.viewType,
                sort = state.sort,
                onToggleViewType = { viewModel.onIntent(AlbumsIntent.ViewTypeToggled) },
                onSortSelected = { viewModel.onIntent(AlbumsIntent.SortSelected(it)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        AlbumsContent(
            state = state,
            onAlbumClick = { album -> onOpenAlbum(album.ref) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlbumsContent(
    state: AlbumsUiState,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            ContainedLoadingIndicator(modifier = Modifier.size(72.dp))
        }
        return
    }

    if (state.isEmpty) {
        EmptyState(
            illustration = Illustration.EmptyAlbum,
            title = "No albums",
            message = "Folders containing photos or videos will show up here.",
            modifier = modifier,
        )
        return
    }

    val albums = state.visibleAlbums
    val motion = MaterialTheme.motionScheme
    when (state.viewType) {
        AlbumViewType.GRID -> BoxWithConstraints(modifier = modifier) {
            val windowHeight = rememberWindowSize().height
            val columns = albumGridColumns(
                availableWidth = maxWidth - GalvaDimens.ScreenPadding * 2,
                availableHeight = windowHeight - WINDOW_CHROME_HEIGHT,
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
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = motion.defaultEffectsSpec(),
                            placementSpec = motion.defaultSpatialSpec(),
                            fadeOutSpec = motion.fastEffectsSpec(),
                        ),
                    )
                }
            }
        }

        AlbumViewType.LIST -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = GalvaDimens.ItemSpacing),
        ) {
            items(albums, key = { it.ref.key }) { album ->
                AlbumListItem(
                    album = album,
                    onClick = { onAlbumClick(album) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = motion.defaultEffectsSpec(),
                        placementSpec = motion.defaultSpatialSpec(),
                        fadeOutSpec = motion.fastEffectsSpec(),
                    ),
                )
            }
        }
    }
}

private val WINDOW_CHROME_HEIGHT = 96.dp
