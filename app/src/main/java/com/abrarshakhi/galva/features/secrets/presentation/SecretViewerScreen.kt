package com.abrarshakhi.galva.features.secrets.presentation

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.abrarshakhi.galva.common.mvi.CollectEffects
import com.abrarshakhi.galva.common.ui.snackbar.SnackbarDispatcher
import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.domain.model.VaultUri
import com.abrarshakhi.galva.core.vault.ui.SecureWindow
import com.abrarshakhi.galva.core.vault.ui.VaultDataSource
import com.abrarshakhi.galva.features.viewer.presentation.VideoPage
import com.abrarshakhi.galva.features.viewer.presentation.ZoomableImage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.media3.common.MediaItem as Media3Item

/**
 * Full-screen viewer for the vault, built from the gallery viewer's pages.
 *
 * Its player reads through [VaultDataSource] and nothing else, so every byte it plays was
 * decrypted in memory a moment before.
 */
@OptIn(UnstableApi::class)
@Composable
fun SecretViewerScreen(
    initialId: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureWindow()

    val viewModel: SecretViewerViewModel = koinViewModel { parametersOf(initialId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val content: VaultContent = koinInject()
    val snackbar: SnackbarDispatcher = koinInject()

    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(VaultDataSource.Factory(content)))
            .build()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SecretViewerEffect.ShowMessage -> snackbar.show(effect.text)
            SecretViewerEffect.Close -> onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading || state.items.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }

        val pagerState = rememberPagerState(
            initialPage = state.currentIndex,
            pageCount = { state.items.size },
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .collect { page -> viewModel.onIntent(SecretViewerIntent.PageSettled(page)) }
        }

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
                player.setMediaItem(Media3Item.fromUri(VaultUri.blob(item.id)))
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
                    onTap = { viewModel.onIntent(SecretViewerIntent.ChromeToggled) },
                )
            } else {
                ZoomableImage(
                    uri = if (item.isVideo) VaultUri.thumbnail(item.id) else VaultUri.full(item.id),
                    onTap = { viewModel.onIntent(SecretViewerIntent.ChromeToggled) },
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
            SecretViewerTopBar(
                title = state.current?.displayName.orEmpty(),
                onBack = onClose,
                onRestore = { viewModel.onIntent(SecretViewerIntent.RestoreRequested) },
                onDelete = { viewModel.onIntent(SecretViewerIntent.DeleteRequested) },
            )
        }
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(SecretViewerIntent.DeleteDismissed) },
            title = { Text("Delete for good?") },
            text = { Text(DELETE_FOR_GOOD_WARNING) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(SecretViewerIntent.DeleteConfirmed) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(SecretViewerIntent.DeleteDismissed) }) {
                    Text("Cancel")
                }
            },
        )
    }

    state.work?.let { BlockingProgress(it) }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretViewerTopBar(
    title: String,
    onBack: () -> Unit,
    onRestore: () -> Unit,
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
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.LockOpen, contentDescription = "Restore to gallery")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteForever, contentDescription = "Delete for good")
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
