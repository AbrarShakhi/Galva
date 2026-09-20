package com.abrarshakhi.galva.common.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats timeline section headers the way a gallery reads them: relative for the last two days,
 * then day-and-month, with the year only once it stops being obvious.
 */
class TimelineDateFormatter(
    locale: Locale,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val sameYear = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    private val otherYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    fun format(date: LocalDate): String {
        val now = today()
        return when (date) {
            now -> "Today"
            now.minusDays(1) -> "Yesterday"
            else -> if (date.year == now.year) sameYear.format(date) else otherYear.format(date)
        }
    }
}

@Composable
fun rememberTimelineDateFormatter(): TimelineDateFormatter {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    return remember(locale) { TimelineDateFormatter(locale) }
}

/** `1:23` / `1:02:03`, the compact form used on video thumbnails. */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}
