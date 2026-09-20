package com.abrarshakhi.galva.features.viewer.presentation

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.ui.rememberMediaDeleteLauncher
import com.abrarshakhi.galva.core.share.MediaSharing
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.media3.common.MediaItem as Media3Item

@OptIn(UnstableApi::class)
@Composable
fun ViewerScreen(
    source: MediaSource,
    initialMediaId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ViewerViewModel = koinViewModel { parametersOf(source, initialMediaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar: SnackbarDispatcher = koinInject()

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    // Playback must not continue behind a locked screen or another app.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }

    val deleteLauncher = rememberMediaDeleteLauncher { confirmed, ids ->
        viewModel.onIntent(ViewerIntent.DeleteResolved(ids, confirmed))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ViewerEffect.ShareItem ->
                MediaSharing.chooserFor(effect.uris, effect.mimeTypes)?.let(context::startActivity)

            is ViewerEffect.ConfirmDelete -> deleteLauncher.request(effect.ids, effect.uris)

            is ViewerEffect.ShowMessage -> snackbar.show(effect.text)

            ViewerEffect.Close -> onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }

        val pagerState = rememberPagerState(
            initialPage = state.currentIndex,
            pageCount = { state.items.size },
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .collect { page -> viewModel.onIntent(ViewerIntent.PageSettled(page)) }
        }

        // Re-anchoring after a delete moves the index in the ViewModel; follow it.
        LaunchedEffect(state.currentIndex) {
            if (pagerState.currentPage != state.currentIndex &&
                state.currentIndex in 0 until pagerState.pageCount
            ) {
                pagerState.scrollToPage(state.currentIndex)
            }
        }

        val currentItem = state.current
        LaunchedEffect(currentItem?.id) {
            val item = currentItem
            if (item != null && item.isVideo) {
                player.setMediaItem(Media3Item.fromUri(item.uri))
                player.prepare()
                player.playWhenReady = true
            } else {
                player.pause()
                player.clearMediaItems()
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> state.items[page].id },
        ) { page ->
            val item = state.items[page]
            if (item.isVideo && page == state.currentIndex) {
                VideoPage(
                    player = player,
                    controlsVisible = state.chromeVisible,
                    onTap = { viewModel.onIntent(ViewerIntent.ChromeToggled) },
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onTap = { viewModel.onIntent(ViewerIntent.ChromeToggled) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ViewerTopBar(
                title = state.current?.displayName.orEmpty(),
                isFavorite = state.current?.isFavorite == true,
                onBack = onClose,
                onFavorite = { viewModel.onIntent(ViewerIntent.FavoriteToggled) },
                onShare = { viewModel.onIntent(ViewerIntent.ShareRequested) },
                onDelete = { viewModel.onIntent(ViewerIntent.DeleteRequested) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    title: String,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites"
                    else "Add to favorites",
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        },
        // Immersive content gets no Scaffold inset, so in landscape the side system bar would sit
        // on top of these actions.
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xCC000000),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}
