package com.abrarshakhi.galva.feature.viewer

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
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionsHost
import com.abrarshakhi.galva.core.ui.component.MediaActionToolbar
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import com.abrarshakhi.galva.core.ui.transition.sharedMediaKey
import com.abrarshakhi.galva.core.ui.transition.thumbnailCacheKey
import com.abrarshakhi.galva.core.ui.util.rememberCaptureTimeFormatter
import com.abrarshakhi.galva.core.ui.viewer.ImmersiveSystemBars
import com.abrarshakhi.galva.core.ui.viewer.VIDEO_SEEK_INCREMENT_MS
import com.abrarshakhi.galva.core.ui.viewer.VideoPage
import com.abrarshakhi.galva.core.ui.viewer.ViewerTopBar
import com.abrarshakhi.galva.core.ui.viewer.ZoomableImage
import com.abrarshakhi.galva.core.ui.viewer.videoControlsPadding
import com.abrarshakhi.galva.core.ui.viewer.viewerToolbarPadding
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.media3.common.MediaItem as Media3Item

@Composable
fun ViewerRoute(source: MediaSource, initialMediaId: Long, onClose: () -> Unit) {
    ViewerScreen(
        viewModel = koinViewModel { parametersOf(source, initialMediaId) },
        onClose = onClose,
    )
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = rememberWindowLayout()
    val context = LocalContext.current
    val formatCaptureTime = rememberCaptureTimeFormatter()

    ImmersiveSystemBars(visible = state.chromeVisible)

    val player = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(VIDEO_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(VIDEO_SEEK_INCREMENT_MS)
            .build()
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.pause() }

    MediaActionsHost(state.actions) { viewModel.onIntent(ViewerIntent.ActionEvent(it)) }
    UserMessagesEffect(state.messages) { viewModel.onIntent(ViewerIntent.MessageShown(it)) }

    if (state.isExhausted) {
        LaunchedEffect(Unit) { onClose() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading) {
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
                .collect { page -> viewModel.onIntent(ViewerIntent.PageSettled(page)) }
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
                    controlsPadding = videoControlsPadding(layout),
                    onTap = { viewModel.onIntent(ViewerIntent.ChromeToggled) },
                )
            } else {
                ZoomableImage(
                    uri = item.uri,
                    onTap = { viewModel.onIntent(ViewerIntent.ChromeToggled) },
                    modifier = Modifier.fillMaxSize(),
                    sharedKey = sharedMediaKey(item.id).takeIf { page == state.currentIndex },
                    placeholderKey = thumbnailCacheKey(item.uri),
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

        MediaActionToolbar(
            visible = state.chromeVisible,
            isFavorite = current?.isFavorite == true,
            onAction = { viewModel.onIntent(ViewerIntent.Perform(it)) },
            layout = layout,
            modifier = Modifier
                .align(toolbarAlignment(layout))
                .viewerToolbarPadding(),
        )
    }
}
