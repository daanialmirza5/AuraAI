package com.aura.ai.core.memory.extraction

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * A deliberately small local date-phrase resolver — "today," "tomorrow," and weekday names
 * ("Monday" through "Sunday," always resolved to their *next upcoming* occurrence, matching how
 * "next Monday" is used in casual speech). This is not a general NLP date parser; it exists
 * specifically to bridge the brief's own example — "my exam is next Monday" recognized as both
 * a Goal memory and a calendar action — without needing a connected AI provider to do it.
 */
internal object DateExpressionParser {
    private val WEEKDAYS =
        mapOf(
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
        )

    fun parseEpochMillis(
        text: String,
        today: LocalDate = LocalDate.now(),
    ): Long? {
        val lower = text.lowercase()
        val resolved =
            when {
                lower.contains("tomorrow") -> today.plusDays(1)
                lower.contains("today") -> today
                else ->
                    WEEKDAYS.entries
                        .firstOrNull { (name, _) -> lower.contains(name) }
                        ?.let { (_, dayOfWeek) -> nextOccurrenceOf(today, dayOfWeek) }
            } ?: return null

        return resolved.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun nextOccurrenceOf(
        from: LocalDate,
        target: DayOfWeek,
    ): LocalDate {
        var candidate = from.plusDays(1)
        while (candidate.dayOfWeek != target) candidate = candidate.plusDays(1)
        return candidate
    }
}
