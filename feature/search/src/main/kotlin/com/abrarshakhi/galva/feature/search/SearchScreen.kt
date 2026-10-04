package com.abrarshakhi.galva.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.designsystem.layout.rememberWindowLayout
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionsHost
import com.abrarshakhi.galva.core.ui.appViewModel
import com.abrarshakhi.galva.core.ui.component.MediaActionToolbar
import com.abrarshakhi.galva.core.ui.component.MediaGrid
import com.abrarshakhi.galva.core.ui.component.SelectionTopBar
import com.abrarshakhi.galva.core.ui.component.rememberToolbarAwarePadding
import com.abrarshakhi.galva.core.ui.component.toolbarAlignment
import com.abrarshakhi.galva.core.ui.message.UserMessagesEffect
import com.abrarshakhi.galva.core.ui.selection.selectedBy

@Composable
fun SearchRoute(onOpenViewer: (MediaSource, Long) -> Unit) {
    SearchScreen(
        viewModel = appViewModel(),
        onOpenViewer = onOpenViewer,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = rememberWindowLayout()
    val motion = MaterialTheme.motionScheme

    MediaActionsHost(state.actions) { viewModel.onIntent(SearchIntent.ActionEvent(it)) }
    UserMessagesEffect(state.messages) { viewModel.onIntent(SearchIntent.MessageShown(it)) }

    BackHandler(enabled = state.selection.isActive) {
        viewModel.onIntent(SearchIntent.ClearSelection)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AnimatedContent(
                targetState = state.selection.isActive,
                transitionSpec = {
                    fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
                },
                label = "searchTopBar",
            ) { selecting ->
                if (selecting) {
                    SelectionTopBar(
                        count = state.selection.count,
                        allSelected = state.allSelected,
                        onClear = { viewModel.onIntent(SearchIntent.ClearSelection) },
                        onSelectAll = { viewModel.onIntent(SearchIntent.SelectAll) },
                    )
                } else {
                    SearchHeader(
                        query = state.query,
                        filter = state.filter,
                        onQueryChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
                        onClear = { viewModel.onIntent(SearchIntent.QueryCleared) },
                        onFilterSelected = { viewModel.onIntent(SearchIntent.FilterChanged(it)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                !state.isActive -> EmptyState(
                    illustration = Illustration.Search,
                    title = "Search your library",
                    message = "Find photos and videos by file name or folder.",
                )

                state.hasNoResults -> EmptyState(
                    illustration = Illustration.Search,
                    title = "No matches",
                    message = "Try a different name or filter.",
                )

                else -> MediaGrid(
                    items = state.results,
                    selection = state.selection,
                    preferredColumns = state.columns,
                    onItemClick = { item ->
                        if (state.selection.isActive) {
                            viewModel.onIntent(SearchIntent.ToggleSelection(item))
                        } else {
                            onOpenViewer(state.source, item.id)
                        }
                    },
                    onItemLongClick = { viewModel.onIntent(SearchIntent.ToggleSelection(it)) },
                    contentPadding = rememberToolbarAwarePadding(state.selection.isActive, layout),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val selected = state.results.selectedBy(state.selection)
            MediaActionToolbar(
                visible = state.selection.isActive,
                isFavorite = selected.isNotEmpty() && selected.all { it.isFavorite },
                onAction = { viewModel.onIntent(SearchIntent.Perform(it)) },
                layout = layout,
                modifier = Modifier
                    .align(toolbarAlignment(layout))
                    .padding(16.dp),
            )
        }
    }
}
