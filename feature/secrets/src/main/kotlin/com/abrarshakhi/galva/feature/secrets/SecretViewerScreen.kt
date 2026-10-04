package com.abrarshakhi.galva.feature.secrets

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import com.abrarshakhi.galva.core.ui.transition.sharedMediaKey
import com.abrarshakhi.galva.core.ui.transition.thumbnailCacheKey
import com.abrarshakhi.galva.core.ui.util.rememberCaptureTimeFormatter
import com.abrarshakhi.galva.core.ui.vault.BlockingProgress
import com.abrarshakhi.galva.core.ui.vault.SecureWindow
import com.abrarshakhi.galva.core.ui.viewer.ImmersiveSystemBars
import com.abrarshakhi.galva.core.ui.viewer.VIDEO_SEEK_INCREMENT_MS
import com.abrarshakhi.galva.core.ui.viewer.VideoPage
import com.abrarshakhi.galva.core.ui.viewer.ViewerTopBar
import com.abrarshakhi.galva.core.ui.viewer.ZoomableImage
import com.abrarshakhi.galva.core.ui.viewer.videoControlsPadding
import com.abrarshakhi.galva.core.ui.viewer.viewerToolbarPadding
import com.abrarshakhi.galva.core.vault.VaultUri
import com.abrarshakhi.galva.core.vault.media.VaultMedia
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import androidx.media3.common.MediaItem as Media3Item

@Composable
fun SecretViewerRoute(initialSecretId: Long, onClose: () -> Unit) {
    SecretViewerScreen(
        viewModel = koinViewModel { parametersOf(initialSecretId) },
        onClose = onClose,
    )
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SecretViewerScreen(
    viewModel: SecretViewerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureWindow()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = rememberWindowLayout()
    val context = LocalContext.current
    val vaultMedia: VaultMedia = koinInject()
    val formatCaptureTime = rememberCaptureTimeFormatter()

    ImmersiveSystemBars(visible = state.chromeVisible)

    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(vaultMedia.dataSourceFactory()))
            .setSeekBackIncrementMs(VIDEO_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(VIDEO_SEEK_INCREMENT_MS)
            .build()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }

    UserMessagesEffect(state.messages) { viewModel.onIntent(SecretViewerIntent.MessageShown(it)) }

    if (state.isExhausted) {
        LaunchedEffect(Unit) { onClose() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading || state.items.isEmpty()) {
            ContainedLoadingIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
            )
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
                    controlsPadding = videoControlsPadding(layout),
                    onTap = { viewModel.onIntent(SecretViewerIntent.ChromeToggled) },
                )
            } else {
                ZoomableImage(
                    uri = if (item.isVideo) VaultUri.thumbnail(item.id) else VaultUri.full(item.id),
                    onTap = { viewModel.onIntent(SecretViewerIntent.ChromeToggled) },
                    modifier = Modifier.fillMaxSize(),
                    sharedKey = sharedMediaKey(item.id, SECRET_NAMESPACE)
                        .takeIf { page == state.currentIndex },
                    placeholderKey = thumbnailCacheKey(VaultUri.thumbnail(item.id)),
                )
            }
        }

        val current = state.current
        ViewerTopBar(
            visible = state.chromeVisible,
            title = current?.displayName.orEmpty(),
            subtitle = current?.let { formatCaptureTime(it.dateTakenMs) }.orEmpty(),
            onBack = onClose,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        SecretActionToolbar(
            visible = state.chromeVisible,
            onRestore = { viewModel.onIntent(SecretViewerIntent.RestoreRequested) },
            onDelete = { viewModel.onIntent(SecretViewerIntent.DeleteRequested) },
            layout = layout,
            modifier = Modifier
                .align(toolbarAlignment(layout))
                .viewerToolbarPadding(),
        )
    }

    if (state.confirmingDelete) {
        DeleteForGoodDialog(
            title = "Delete for good?",
            onConfirm = { viewModel.onIntent(SecretViewerIntent.DeleteConfirmed) },
            onDismiss = { viewModel.onIntent(SecretViewerIntent.DeleteDismissed) },
        )
    }

    state.work?.let { BlockingProgress(it) }
}
