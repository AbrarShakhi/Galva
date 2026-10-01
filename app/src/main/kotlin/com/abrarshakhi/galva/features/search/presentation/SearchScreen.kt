package com.abrarshakhi.galva.features.search.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.EmptyState
import com.abrarshakhi.galva.common.ui.component.MediaGrid
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.core.media.domain.model.MediaFilter
import com.abrarshakhi.galva.common.ui.selection.selectedBy
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.features.albums.presentation.AddToAlbumSheet
import com.abrarshakhi.galva.core.media.ui.rememberMediaDeleteLauncher
import com.abrarshakhi.galva.core.share.MediaSharing
import com.abrarshakhi.galva.features.secrets.presentation.BlockingProgress
import com.abrarshakhi.galva.features.secrets.presentation.VaultUnlockSheet
import org.koin.compose.koinInject

@Composable
fun SearchScreen(
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = appViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar: SnackbarDispatcher = koinInject()

    val deleteLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(SearchIntent.DeleteResolved(ids, confirmed))
    }
    val moveLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(SearchIntent.MoveResolved(ids, confirmed))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SearchEffect.OpenViewer -> onOpenViewer(effect.source, effect.mediaId)

            is SearchEffect.ShareItems ->
                MediaSharing.chooserFor(effect.uris, effect.mimeTypes)?.let(context::startActivity)

            is SearchEffect.ConfirmDelete -> deleteLauncher.request(effect.ids, effect.uris)

            is SearchEffect.ConfirmMove -> moveLauncher.request(effect.ids, effect.uris)

            is SearchEffect.ShowMessage -> snackbar.show(effect.text)
        }
    }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(SearchIntent.ClearSelection)
    }

    if (state.showAddToAlbum) {
        AddToAlbumSheet(
            mediaIds = state.results.selectedBy(state.selection).map(MediaItem::id),
            onDismiss = { viewModel.onIntent(SearchIntent.AddToAlbumDismissed) },
            onAdded = { name -> viewModel.onIntent(SearchIntent.AddedToAlbum(name)) },
        )
    }

    if (state.showVaultUnlock) {
        VaultUnlockSheet(
            onDismiss = { viewModel.onIntent(SearchIntent.VaultUnlockDismissed) },
            onUnlocked = { viewModel.onIntent(SearchIntent.VaultUnlocked) },
        )
    }

    state.moveProgress?.let { BlockingProgress(it.label) }


    Column(modifier = modifier.fillMaxSize()) {
        FilterRow(
            selected = state.filter,
            onSelect = { viewModel.onIntent(SearchIntent.FilterChanged(it)) },
        )

        when {
            !state.isActive -> EmptyState(
                icon = Icons.Outlined.Search,
                title = "Search your library",
                message = "Find photos and videos by file name or folder.",
                modifier = Modifier.weight(1f),
            )

            state.hasNoResults -> EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "No matches",
                message = "Try a different name or filter.",
                modifier = Modifier.weight(1f),
            )

            else -> MediaGrid(
                items = state.results,
                selection = state.selection,
                preferredColumns = state.columns,
                onItemClick = { viewModel.onIntent(SearchIntent.MediaTapped(it)) },
                onItemLongClick = { viewModel.onIntent(SearchIntent.MediaLongPressed(it)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FilterRow(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GalvaDimens.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterOption.entries.forEach { option ->
            FilterChip(
                selected = option.filter == selected,
                onClick = { onSelect(option.filter) },
                label = { Text(option.label) },
            )
        }
    }
}

private enum class FilterOption(val filter: MediaFilter, val label: String) {
    All(MediaFilter.ALL, "All"),
    Photos(MediaFilter.IMAGES, "Photos"),
    Videos(MediaFilter.VIDEOS, "Videos"),
    Favorites(MediaFilter.FAVORITES, "Favorites"),
}
