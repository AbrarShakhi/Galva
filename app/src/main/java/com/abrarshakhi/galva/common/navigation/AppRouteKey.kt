package com.abrarshakhi.galva.common.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRouteKey : NavKey {

    @Serializable
    data object Gallery : AppRouteKey

    @Serializable
    data object Albums : AppRouteKey

    @Serializable
    data object Search : AppRouteKey

    companion object {
        val topLevel: List<AppRouteKey> = listOf(Gallery, Albums, Search)
    }
}

val AppRouteKey.isTopLevel: Boolean get() = this in AppRouteKey.topLevel
