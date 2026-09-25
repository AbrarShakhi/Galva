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
import com.abrarshakhi.galva.features.secrets.presentation.SecretViewerScreen
import com.abrarshakhi.galva.features.secrets.presentation.SecretsScreen
import com.abrarshakhi.galva.features.viewer.presentation.ViewerScreen

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

            entry<AppRouteKey.Secrets> {
                SecretsScreen(
                    onOpenViewer = { id -> backStack.navigateTo(AppRouteKey.SecretViewer(id)) },
                )
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

            entry<AppRouteKey.SecretViewer> { key ->
                SecretViewerScreen(
                    initialId = key.initialSecretId,
                    onClose = backStack::back,
                )
            }
        },
    )
}
