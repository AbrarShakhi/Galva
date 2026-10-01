package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.TimelineSection
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Turns a flat, newest-first media list into day-grouped sections for the timeline grid.
 *
 * Grouping lives here rather than in the repository so the data layer stays a plain query surface
 * and the day boundary is computed against the device's current time zone at collection time.
 */
class ObserveTimelineUseCase(
    private val mediaRepository: MediaRepository,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault,
) {

    operator fun invoke(source: MediaSource): Flow<List<TimelineSection>> =
        mediaRepository.observeMedia(source).map { items ->
            val zone = zoneId()
            items
                .groupBy { item ->
                    Instant.ofEpochMilli(item.dateTakenMs).atZone(zone).toLocalDate()
                }
                .map { (date, media) -> TimelineSection(date = date, items = media) }
                .sortedByDescending(TimelineSection::date)
        }
}
