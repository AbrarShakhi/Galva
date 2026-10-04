package com.abrarshakhi.galva.core.domain

import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.model.TimelineSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

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
