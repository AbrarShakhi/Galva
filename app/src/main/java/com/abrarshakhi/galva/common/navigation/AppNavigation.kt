package com.abrarshakhi.galva.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.features.gallery.presentation.GalleryScreen

@Composable
fun AppNavigation(
    backStack: SnapshotStateList<AppRouteKey>,
    modifier: Modifier = Modifier,
) {
    val openViewer: (MediaSource, Long) -> Unit = { source, mediaId ->
        backStack.navigateTo(AppRouteKey.Viewer(source, mediaId))
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRouteKey.Gallery> {
                GalleryScreen(onOpenViewer = openViewer)
            }
        })
}
