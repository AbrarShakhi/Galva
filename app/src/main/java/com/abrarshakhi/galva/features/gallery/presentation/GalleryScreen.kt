package com.abrarshakhi.galva.features.gallery.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.appViewModel
import com.abrarshakhi.galva.common.ui.component.EmptyState
import com.abrarshakhi.galva.common.ui.component.TimelineGrid
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
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
fun GalleryScreen(
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GalleryViewModel = appViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar: SnackbarDispatcher = koinInject()

    val deleteLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(GalleryIntent.DeleteResolved(ids = ids, confirmed = confirmed))
    }
    val moveLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(GalleryIntent.MoveResolved(ids = ids, confirmed = confirmed))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GalleryEffect.OpenViewer -> onOpenViewer(effect.source, effect.mediaId)

            is GalleryEffect.ShareItems ->
                MediaSharing.chooserFor(effect.uris, effect.mimeTypes)
                    ?.let(context::startActivity)

            is GalleryEffect.ConfirmDelete -> deleteLauncher.request(effect.ids, effect.uris)

            is GalleryEffect.ConfirmMove -> moveLauncher.request(effect.ids, effect.uris)

            is GalleryEffect.ShowMessage -> snackbar.show(effect.text)
        }
    }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(GalleryIntent.ClearSelection)
    }

    if (state.showAddToAlbum) {
        AddToAlbumSheet(
            mediaIds = state.items.selectedBy(state.selection).map(MediaItem::id),
            onDismiss = { viewModel.onIntent(GalleryIntent.AddToAlbumDismissed) },
            onAdded = { name -> viewModel.onIntent(GalleryIntent.AddedToAlbum(name)) },
        )
    }

    if (state.showVaultUnlock) {
        VaultUnlockSheet(
            onDismiss = { viewModel.onIntent(GalleryIntent.VaultUnlockDismissed) },
            onUnlocked = { viewModel.onIntent(GalleryIntent.VaultUnlocked) },
        )
    }

    state.moveProgress?.let { BlockingProgress(it.label) }


    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            state.isEmpty -> EmptyState(
                icon = Icons.Outlined.PhotoLibrary,
                title = "No photos yet",
                message = "Photos and videos on this device will appear here.",
            )

            else -> TimelineGrid(
                sections = state.sections,
                selection = state.selection,
                preferredColumns = state.columns,
                onItemClick = { viewModel.onIntent(GalleryIntent.MediaTapped(it)) },
                onItemLongClick = { viewModel.onIntent(GalleryIntent.MediaLongPressed(it)) },
                onHeaderClick = { viewModel.onIntent(GalleryIntent.SectionTapped(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
