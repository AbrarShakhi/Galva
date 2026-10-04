package com.abrarshakhi.galva.feature.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionsHost
import com.abrarshakhi.galva.core.ui.appViewModel
import com.abrarshakhi.galva.core.ui.component.MediaActionToolbar
import com.abrarshakhi.galva.core.ui.component.SelectionTopBar
import com.abrarshakhi.galva.core.ui.component.TimelineGrid
import com.abrarshakhi.galva.core.ui.component.rememberToolbarAwarePadding
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect

@Composable
fun GalleryRoute(
    onOpenViewer: (MediaSource, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    GalleryScreen(
        viewModel = appViewModel(),
        onOpenViewer = onOpenViewer,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenViewer: (MediaSource, Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = rememberWindowLayout()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val motion = MaterialTheme.motionScheme

    MediaActionsHost(state.actions) { viewModel.onIntent(GalleryIntent.ActionEvent(it)) }
    UserMessagesEffect(state.messages) { viewModel.onIntent(GalleryIntent.MessageShown(it)) }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(GalleryIntent.ClearSelection)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = state.selection.isActive,
                transitionSpec = {
                    fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                label = "galleryTopBar",
            ) { selecting ->
                if (selecting) {
                    SelectionTopBar(
                        count = state.selection.count,
                        allSelected = state.allSelected,
                        onClear = { viewModel.onIntent(GalleryIntent.ClearSelection) },
                        onSelectAll = { viewModel.onIntent(GalleryIntent.SelectAll) },
                    )
                } else {
                    GalleryTopBar(
                        itemCount = state.items.size,
                        isSyncing = state.isSyncing && !state.isRefreshing,
                        onOpenSettings = onOpenSettings,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onIntent(GalleryIntent.Refresh) },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullState,
                    isRefreshing = state.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            when {
                state.isLoading -> ContainedLoadingIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                )

                state.isEmpty -> EmptyState(
                    illustration = Illustration.EmptyGallery,
                    title = "No photos yet",
                    message = "Photos and videos on this device will appear here.",
                )

                else -> TimelineGrid(
                    sections = state.sections,
                    selection = state.selection,
                    preferredColumns = state.columns,
                    onItemClick = { item ->
                        if (state.selection.isActive) {
                            viewModel.onIntent(GalleryIntent.ToggleSelection(item))
                        } else {
                            onOpenViewer(MediaSource.AllMedia, item.id)
                        }
                    },
                    onItemLongClick = { viewModel.onIntent(GalleryIntent.ToggleSelection(it)) },
                    onHeaderClick = { viewModel.onIntent(GalleryIntent.ToggleSection(it)) },
                    contentPadding = rememberToolbarAwarePadding(state.selection.isActive, layout),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val selected = state.selectedItems
            MediaActionToolbar(
                visible = state.selection.isActive,
                isFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onAction = { viewModel.onIntent(GalleryIntent.Perform(it)) },
                layout = layout,
                modifier = Modifier
                    .align(toolbarAlignment(layout))
                    .padding(16.dp),
            )
        }
    }
}
