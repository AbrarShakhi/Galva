package com.abrarshakhi.galva.common.navigation

import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRouteKey : NavKey {

    /** The day-grouped photo wall. The app's start destination. */
    @Serializable
    data object Gallery : AppRouteKey

    @Serializable
    data object Albums : AppRouteKey

    @Serializable
    data object Search : AppRouteKey

    @Serializable
    data class AlbumDetail(val albumRef: AlbumRef) : AppRouteKey

    /**
     * Full-screen viewer.
     *
     * It carries the [source] it is paging through rather than a materialised list, so opening a
     * photo out of a 20,000-item library does not serialise that library into the back stack.
     */
    @Serializable
    data class Viewer(
        val source: MediaSource,
        val initialMediaId: Long,
    ) : AppRouteKey

    companion object {
        /** Destinations reachable from the bottom bar, in display order. */
        val topLevel: List<AppRouteKey> = listOf(Gallery, Albums, Search)
    }
}

val AppRouteKey.isTopLevel: Boolean get() = this in AppRouteKey.topLevel
