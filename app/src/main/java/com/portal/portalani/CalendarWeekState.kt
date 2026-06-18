package com.portal.portalani

import com.portal.portalani.data.CalendarAiringEntry
import java.time.LocalDate

data class CalendarWeekState(
    val weekStart: LocalDate,
    val entries: List<CalendarAiringEntry>,
    val isCurrentWeek: Boolean = false,
    val fromCache: Boolean = false,
)
