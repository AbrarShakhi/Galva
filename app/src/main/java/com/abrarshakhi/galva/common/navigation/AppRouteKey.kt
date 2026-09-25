package com.abrarshakhi.galva.common.navigation

import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRouteKey : NavKey {

    @Serializable
    data object Gallery : AppRouteKey

    @Serializable
    data object Albums : AppRouteKey

    @Serializable
    data object Search : AppRouteKey

    @Serializable
    data object Secrets : AppRouteKey

    @Serializable
    data class AlbumDetail(val albumRef: AlbumRef) : AppRouteKey

    @Serializable
    data class Viewer(
        val source: MediaSource,
        val initialMediaId: Long,
    ) : AppRouteKey

    @Serializable
    data class SecretViewer(val initialSecretId: Long) : AppRouteKey

    companion object {
        val topLevel: List<AppRouteKey> = listOf(Gallery, Albums, Search, Secrets)
    }
}

val AppRouteKey.isTopLevel: Boolean get() = this in AppRouteKey.topLevel
