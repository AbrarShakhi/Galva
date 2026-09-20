package com.abrarshakhi.galva.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.features.albums.presentation.AlbumDetailScreen
import com.abrarshakhi.galva.features.albums.presentation.AlbumsScreen
import com.abrarshakhi.galva.features.gallery.presentation.GalleryScreen
import com.abrarshakhi.galva.features.search.presentation.SearchScreen
import com.abrarshakhi.galva.features.viewer.presentation.ViewerScreen

/**
 * Maps back-stack keys to screens.
 *
 * Both decorators matter here: the saveable-state one keeps each entry's scroll position across
 * navigation, and the ViewModel-store one scopes a detail screen's ViewModel to its entry so it is
 * cleared on pop rather than leaking into the next album the user opens.
 */
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

            entry<AppRouteKey.Albums> {
                AlbumsScreen(
                    onOpenAlbum = { ref -> backStack.navigateTo(AppRouteKey.AlbumDetail(ref)) },
                )
            }

            entry<AppRouteKey.Search> {
                SearchScreen(onOpenViewer = openViewer)
            }

            entry<AppRouteKey.AlbumDetail> { key ->
                AlbumDetailScreen(
                    albumRef = key.albumRef,
                    onBack = backStack::back,
                    onOpenViewer = openViewer,
                )
            }

            entry<AppRouteKey.Viewer> { key ->
                ViewerScreen(
                    source = key.source,
                    initialMediaId = key.initialMediaId,
                    onClose = backStack::back,
                )
            }
        },
    )
}
