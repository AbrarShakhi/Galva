package com.abrarshakhi.galva.navigation

import androidx.navigation3.runtime.NavKey
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.AppDocument
import com.abrarshakhi.galva.core.model.MediaSource
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
    data class Viewer(val source: MediaSource, val initialMediaId: Long) : AppRouteKey

    @Serializable
    data class SecretViewer(val initialSecretId: Long) : AppRouteKey

    @Serializable
    data object Settings : AppRouteKey

    @Serializable
    data class Document(val document: AppDocument) : AppRouteKey
}
