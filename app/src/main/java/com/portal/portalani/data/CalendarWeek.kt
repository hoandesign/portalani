package com.portal.portalani.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class WeekStart {
  MONDAY,
  SUNDAY,
  ;

  fun label(): String =
      when (this) {
        MONDAY -> "Monday"
        SUNDAY -> "Sunday"
      }
}

/** One scheduled episode in the weekly calendar grid. */
data class CalendarAiringEntry(
    val scheduleId: Int,
    val mediaId: Int,
    val title: String,
    val coverUrl: String,
    val episode: Int,
    /** Unix timestamp (seconds) when the episode airs. */
    val airingAt: Int,
    val format: String?,
    val season: String?,
    val seasonYear: Int?,
    val averageScore: Int?,
    val popularity: Int?,
    val listStatus: ListStatus?,
) {
  val isOnList: Boolean
    get() = listStatus != null

  fun localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
      Instant.ofEpochSecond(airingAt.toLong()).atZone(zone).toLocalDate()

  fun localTimeLabel(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
    val time = Instant.ofEpochSecond(airingAt.toLong()).atZone(zone).toLocalTime()
    return time.format(DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale))
  }
}

object CalendarWeek {
  private const val SEASON_WINDOW_COUNT = 4

  fun startOfWeek(date: LocalDate, weekStart: WeekStart): LocalDate =
      when (weekStart) {
        WeekStart.MONDAY -> date.with(DayOfWeek.MONDAY)
        WeekStart.SUNDAY ->
            if (date.dayOfWeek == DayOfWeek.SUNDAY) date else date.minusDays(date.dayOfWeek.value.toLong())
      }

  fun weekDates(weekStart: LocalDate): List<LocalDate> = (0..6).map { weekStart.plusDays(it.toLong()) }

  fun dayIndex(date: LocalDate, weekStart: LocalDate): Int {
    val days = ChronoDaysBetween(weekStart, date)
    return days.toInt().coerceIn(0, 6)
  }

  private fun ChronoDaysBetween(start: LocalDate, end: LocalDate): Long =
      java.time.temporal.ChronoUnit.DAYS.between(start, end)

  fun headerLabel(weekStart: LocalDate, locale: Locale = Locale.getDefault()): String {
    val weekEnd = weekStart.plusDays(6)
    val monthStyle = TextStyle.FULL
    return if (weekStart.month == weekEnd.month && weekStart.year == weekEnd.year) {
      "${weekStart.month.getDisplayName(monthStyle, locale)} ${weekStart.year}"
    } else {
      val startMonth = weekStart.month.getDisplayName(TextStyle.SHORT, locale)
      val endMonth = weekEnd.month.getDisplayName(TextStyle.SHORT, locale)
      "$startMonth ${weekStart.dayOfMonth} – $endMonth ${weekEnd.dayOfMonth}, ${weekEnd.year}"
    }
  }

  fun dayOfWeekLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
      date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

  fun seasonWindow(): List<LibrarySeasonParams> = SeasonSelection.seasonWindowForward(SEASON_WINDOW_COUNT)

  fun matchesSeasonWindow(season: String?, seasonYear: Int?): Boolean {
    if (season.isNullOrBlank() || seasonYear == null || seasonYear <= 0) return false
    return seasonWindow().any { params ->
      params.season.equals(season, ignoreCase = true) && params.seasonYear == seasonYear
    }
  }

  fun matchesFormat(entry: CalendarAiringEntry, filter: FormatFilter): Boolean {
    val api = filter.apiValue ?: return true
    return entry.format.equals(api, ignoreCase = true)
  }

  fun sortEntries(entries: List<CalendarAiringEntry>, sort: LibrarySort): List<CalendarAiringEntry> =
      entries.sortedWith(
          compareBy<CalendarAiringEntry> { if (it.isOnList) 0 else 1 }
              .thenBy { it.airingAt }
              .thenByDescending { entry ->
                when (sort) {
                  LibrarySort.SCORE -> entry.averageScore ?: 0
                  LibrarySort.POPULARITY -> entry.popularity ?: 0
                  LibrarySort.TRENDING -> entry.popularity ?: entry.averageScore ?: 0
                  LibrarySort.NEWEST -> (entry.seasonYear ?: 0) * 10 + seasonOrdinal(entry.season)
                }
              },
      )

  private fun seasonOrdinal(season: String?): Int =
      when (season?.uppercase()) {
        "WINTER" -> 1
        "SPRING" -> 2
        "SUMMER" -> 3
        "FALL" -> 4
        else -> 0
      }

  fun groupByDay(
      entries: List<CalendarAiringEntry>,
      weekStart: LocalDate,
      zone: ZoneId = ZoneId.systemDefault(),
  ): List<List<CalendarAiringEntry>> {
    val buckets = List(7) { mutableListOf<CalendarAiringEntry>() }
    for (entry in entries) {
      val index = dayIndex(entry.localDate(zone), weekStart)
      buckets[index] += entry
    }
    return buckets
  }
}
