package com.abrarshakhi.galva.core.media.domain.model

import java.time.LocalDate

/**
 * One day's worth of media in the timeline, newest day first.
 *
 * The section carries a [date] rather than a formatted label so that "Today" / "Yesterday" /
 * "12 September" stays a presentation concern and remains locale-aware.
 */
data class TimelineSection(
    val date: LocalDate,
    val items: List<MediaItem>,
)
