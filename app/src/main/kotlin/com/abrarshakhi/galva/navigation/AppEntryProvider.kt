package com.abrarshakhi.galva.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.abrarshakhi.galva.feature.albums.AlbumDetailRoute
import com.abrarshakhi.galva.feature.albums.AlbumsRoute
import com.abrarshakhi.galva.feature.gallery.GalleryRoute
import com.abrarshakhi.galva.feature.search.SearchRoute
import com.abrarshakhi.galva.feature.secrets.SecretViewerRoute
import com.abrarshakhi.galva.feature.secrets.SecretsRoute
import com.abrarshakhi.galva.feature.settings.DocumentRoute
import com.abrarshakhi.galva.feature.settings.SettingsRoute
import com.abrarshakhi.galva.feature.viewer.ViewerRoute

fun appEntryProvider(
    navigator: Navigator,
    transitions: NavTransitions,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    val topLevel = topLevelMetadata()
    val immersive = transitions.immersiveMetadata()

    entry<AppRouteKey.Gallery>(metadata = topLevel) {
        GalleryRoute(
            onOpenViewer = { source, mediaId ->
                navigator.navigate(AppRouteKey.Viewer(source, mediaId))
            },
            onOpenSettings = { navigator.navigate(AppRouteKey.Settings) },
        )
    }

    entry<AppRouteKey.Albums>(metadata = topLevel) {
        AlbumsRoute(onOpenAlbum = { ref -> navigator.navigate(AppRouteKey.AlbumDetail(ref)) })
    }

    entry<AppRouteKey.Search>(metadata = topLevel) {
        SearchRoute(
            onOpenViewer = { source, mediaId ->
                navigator.navigate(AppRouteKey.Viewer(source, mediaId))
            },
        )
    }

    entry<AppRouteKey.Secrets>(metadata = topLevel) {
        SecretsRoute(onOpenViewer = { id -> navigator.navigate(AppRouteKey.SecretViewer(id)) })
    }

    entry<AppRouteKey.AlbumDetail> { key ->
        AlbumDetailRoute(
            albumRef = key.albumRef,
            onBack = navigator::goBack,
            onOpenViewer = { source, mediaId ->
                navigator.navigate(AppRouteKey.Viewer(source, mediaId))
            },
        )
    }

    entry<AppRouteKey.Viewer>(metadata = immersive) { key ->
        ViewerRoute(
            source = key.source,
            initialMediaId = key.initialMediaId,
            onClose = navigator::goBack,
        )
    }

    entry<AppRouteKey.SecretViewer>(metadata = immersive) { key ->
        SecretViewerRoute(initialSecretId = key.initialSecretId, onClose = navigator::goBack)
    }

    entry<AppRouteKey.Settings> {
        SettingsRoute(
            onBack = navigator::goBack,
            onOpenDocument = { navigator.navigate(AppRouteKey.Document(it)) },
        )
    }

    entry<AppRouteKey.Document> { key ->
        DocumentRoute(document = key.document, onBack = navigator::goBack)
    }
}
