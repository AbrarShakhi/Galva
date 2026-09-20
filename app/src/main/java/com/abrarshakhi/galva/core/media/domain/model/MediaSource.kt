package com.abrarshakhi.galva.core.media.domain.model

import kotlinx.serialization.Serializable

/**
 * Identifies a queryable collection of media.
 *
 * This is what a screen passes down to the data layer instead of a materialised list, which is why
 * it is [Serializable]: the viewer receives a source plus a starting id through the navigation back
 * stack and re-resolves the list itself, rather than having thousands of items serialised into a
 * navigation key.
 */
@Serializable
sealed interface MediaSource {

    @Serializable
    data object AllMedia : MediaSource

    @Serializable
    data class Album(val ref: AlbumRef) : MediaSource

    @Serializable
    data class Query(val text: String, val filter: MediaFilter = MediaFilter.ALL) : MediaSource
}

enum class MediaFilter {
    ALL,
    IMAGES,
    VIDEOS,
    FAVORITES,
}
