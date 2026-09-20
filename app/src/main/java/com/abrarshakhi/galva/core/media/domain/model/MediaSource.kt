package com.abrarshakhi.galva.core.media.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaSource {

    @Serializable
    data object AllMedia : MediaSource

    @Serializable
    data class Query(val text: String, val filter: MediaFilter = MediaFilter.ALL) : MediaSource
}

enum class MediaFilter {
    ALL, IMAGES, VIDEOS, FAVORITES,
}
