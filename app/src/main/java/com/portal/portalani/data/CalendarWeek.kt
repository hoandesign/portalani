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
    /** English title when AniList provides one; otherwise romaji/native fallback. */
    val englishTitle: String,
    val coverUrl: String,
    val episode: Int,
    /** Unix timestamp (seconds) when the episode airs. */
    val airingAt: Int,
    val format: String?,
    val countryOfOrigin: String? = null,
    val source: String? = null,
    val tags: List<String> = emptyList(),
    val season: String?,
    val seasonYear: Int?,
    val startDateYear: Int? = null,
    val startDateMonth: Int? = null,
    val startDateDay: Int? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val averageScore: Int?,
    val popularity: Int?,
    val listStatus: ListStatus?,
    val genres: List<String> = emptyList(),
) {
  val isOnList: Boolean
    get() = listStatus != null

  /** Best-effort premiere date from start date, falling back to season year/month. */
  fun premiereDate(): LocalDate? {
    startDateYear?.let { year ->
      val month = (startDateMonth ?: 1).coerceIn(1, 12)
      val day = (startDateDay ?: 1).coerceIn(1, 28)
      return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
    val year = seasonYear ?: return null
    val month =
        when (season?.uppercase()) {
          "WINTER" -> 1
          "SPRING" -> 4
          "SUMMER" -> 7
          "FALL" -> 10
          else -> 1
        }
    return LocalDate.of(year, month, 1)
  }

  fun localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
      Instant.ofEpochSecond(airingAt.toLong()).atZone(zone).toLocalDate()

  fun localTimeLabel(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String {
    val time = Instant.ofEpochSecond(airingAt.toLong()).atZone(zone).toLocalTime()
    return time.format(DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale))
  }

  /** Splits [localTimeLabel] into clock text and an optional AM/PM-style period suffix. */
  fun localTimeParts(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): Pair<String, String?> {
    val label = localTimeLabel(zone = zone, locale = locale)
    val match = LOCAL_TIME_PERIOD_REGEX.matchEntire(label.trim()) ?: return label to null
    val period = match.groupValues[2]
    if (!period.any { it.isLetter() }) return label to null
    return match.groupValues[1].trim() to period
  }

  private companion object {
    val LOCAL_TIME_PERIOD_REGEX = Regex("""^(.+?)\s+(\S+)$""")
  }
}

/** Minimal slide for calendar detail open animation while full media loads. */
fun CalendarAiringEntry.toPlaceholderSlide(): AnimeSlide =
    AnimeSlide(
        id = mediaId,
        title = englishTitle,
        nativeTitle = null,
        coverUrl = coverUrl,
        bannerUrl = coverUrl,
        averageScore = averageScore,
        episodes = episodes,
        status = status ?: "RELEASING",
        season = season,
        seasonYear = seasonYear,
        startDateYear = startDateYear ?: seasonYear,
        startDateMonth = startDateMonth,
        startDateDay = startDateDay,
        format = format,
        countryOfOrigin = countryOfOrigin,
        source = source,
        tags = tags,
        studio = null,
        genres = genres,
        description = null,
        siteUrl = "https://anilist.co/anime/$mediaId",
        trailerYoutubeId = null,
        listStatus = listStatus,
        nextAiringEpisode = episode,
        nextAiringAt = airingAt,
    )

object CalendarWeek {
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

  fun mondayInWeek(weekStart: LocalDate): LocalDate = weekDates(weekStart).first { it.dayOfWeek == DayOfWeek.MONDAY }

  fun headerLabel(weekStart: LocalDate, locale: Locale = Locale.getDefault()): String {
    val monday = mondayInWeek(weekStart)
    return "${monday.month.getDisplayName(TextStyle.FULL, locale)} ${monday.year}"
  }

  fun dayOfWeekLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
      date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

  fun matchesFormat(entry: CalendarAiringEntry, formats: Set<FormatFilter>): Boolean =
      formats.matchesMediaFormat(entry.format)

  fun matchesContentFilters(entry: CalendarAiringEntry, filters: LibraryFilters): Boolean =
      filters.matchesCalendarEntry(entry)

  fun matchesHideHentai(entry: CalendarAiringEntry, hideHentai: Boolean): Boolean =
      !hideHentai || !entry.genres.containsHentaiGenre()

  fun sortEntries(entries: List<CalendarAiringEntry>, sort: LibrarySort): List<CalendarAiringEntry> =
      entries.sortedWith(
          compareBy<CalendarAiringEntry> { if (it.isOnList) 0 else 1 }
              .thenBy { it.airingAt }
              .thenByDescending { entry ->
                when (sort) {
                  LibrarySort.SCORE -> entry.averageScore ?: 0
                  LibrarySort.POPULARITY -> entry.popularity ?: 0
                  LibrarySort.TRENDING -> entry.popularity ?: entry.averageScore ?: 0
                  LibrarySort.NEWEST ->
                      entry.premiereDate()?.toEpochDay()?.toInt()
                          ?: ((entry.seasonYear ?: 0) * 10 + seasonOrdinal(entry.season))
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
