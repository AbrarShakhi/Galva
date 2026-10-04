package com.abrarshakhi.galva.core.model

import java.time.LocalDate

data class TimelineSection(
    val date: LocalDate,
    val items: List<MediaItem>,
)
