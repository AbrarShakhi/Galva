package com.abrarshakhi.galva.common.navigation

import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRouteKey : NavKey {

    data object Gallery : AppRouteKey

    @Serializable
    data object Albums : AppRouteKey

    @Serializable
    data object Search : AppRouteKey

    @Serializable
    data class Viewer(
        val source: MediaSource,
        val initialMediaId: Long,
    ) : AppRouteKey

    companion object {
        val topLevel: List<AppRouteKey> = listOf(Gallery, Albums, Search)
    }
}

val AppRouteKey.isTopLevel: Boolean get() = this in AppRouteKey.topLevel
