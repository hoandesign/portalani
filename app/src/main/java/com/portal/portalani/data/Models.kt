package com.portal.portalani.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class FrameMode {
  INFORMATIVE,
  POSTER_ONLY,
  CALENDAR,
}

enum class ListStatus(val apiValue: String) {
  CURRENT("CURRENT"),
  PLANNING("PLANNING"),
  COMPLETED("COMPLETED"),
  PAUSED("PAUSED"),
  DROPPED("DROPPED"),
  REPEATING("REPEATING"),
}

enum class RankScope {
  ALL_TIME,
  YEAR,
  SEASON,
}

enum class RankType {
  RATED,
  POPULAR,
}

data class MediaRanking(
    val rank: Int,
    val type: RankType,
    val scope: RankScope,
    val year: Int? = null,
    val season: String? = null,
) {
  companion object {
    const val MAX_VISIBLE = 2

    /** Up to [max] rankings: timeframe, then rank number, then type. */
    fun pickTop(entries: List<MediaRanking>, max: Int = MAX_VISIBLE): List<MediaRanking> =
        entries
            .sortedWith(
                compareBy<MediaRanking> { it.scope.ordinal }
                    .thenBy { it.rank }
                    .thenBy { it.type.ordinal },
            )
            .take(max)
  }
}

data class AnimeSlide(
    val id: Int,
    val title: String,
    val nativeTitle: String?,
    /** Portrait cover art — shown on the left. */
    val coverUrl: String,
    /** Wide banner — blurred parallax background. */
    val bannerUrl: String,
    val averageScore: Int?,
    val episodes: Int?,
    val status: String?,
    val season: String?,
    val seasonYear: Int?,
    /** Fallback when seasonYear is unset (movies, older entries). */
    val startDateYear: Int?,
    val startDateMonth: Int? = null,
    val startDateDay: Int? = null,
    val format: String?,
    val studio: String?,
    val genres: List<String>,
    val description: String?,
    val rankings: List<MediaRanking> = emptyList(),
    val siteUrl: String,
    val trailerYoutubeId: String?,
    /** Authenticated viewer fields — null when signed out or unknown. */
    val listEntryId: Int? = null,
    val listStatus: ListStatus? = null,
    /** User score on a 0–10 scale; null means not rated. */
    val userScore: Float? = null,
    val isFavourite: Boolean = false,
    /** Next scheduled episode number; only set for currently releasing anime. */
    val nextAiringEpisode: Int? = null,
    /** Unix timestamp (seconds) when the next episode airs. */
    val nextAiringAt: Int? = null,
) {
  val anilistUrl: String
    get() = siteUrl.ifBlank { "https://anilist.co/anime/$id" }

  val isOnList: Boolean
    get() = listEntryId != null && listStatus != null

  val isReleasing: Boolean
    get() = status.equals("RELEASING", ignoreCase = true)

  val isNotYetReleased: Boolean
    get() = status.equals("NOT_YET_RELEASED", ignoreCase = true)

  /** Best display/filter year: season year first, then start date year. */
  fun airingYear(): Int? = seasonYear ?: startDateYear

  /**
   * Human-readable schedule label for releasing or upcoming anime, e.g. "Ep 5 Today",
   * "Ep 1 Tomorrow", "Premieres in 14 days", or "Premieres Spring 2026".
   */
  fun nextAiringLabel(nowMillis: Long = System.currentTimeMillis()): String? {
    if (!isReleasing && !isNotYetReleased) return null

    episodeScheduleLabel(nowMillis)?.let { return it }
    if (isNotYetReleased) {
      premiereDateLabel(nowMillis)?.let { return it }
      seasonPremiereLabel()?.let { return it }
    }
    return null
  }

  private fun episodeScheduleLabel(nowMillis: Long): String? {
    val episode = nextAiringEpisode ?: return null
    val airingAt = nextAiringAt ?: return null
    val whenText = whenTextFromEpoch(airingAt.toLong(), nowMillis) ?: return null
    return "Ep $episode $whenText"
  }

  private fun premiereDateLabel(nowMillis: Long): String? {
    val year = startDateYear ?: return null
    val month = startDateMonth ?: return null
    val day = startDateDay ?: return null
    val whenText =
        whenTextFromLocalDate(LocalDate.of(year, month, day), nowMillis) ?: return null
    return "Premieres $whenText"
  }

  private fun seasonPremiereLabel(): String? {
    val year = seasonYear ?: startDateYear ?: return null
    val seasonName =
        when (season?.uppercase()) {
          "WINTER" -> "Winter"
          "SPRING" -> "Spring"
          "SUMMER" -> "Summer"
          "FALL" -> "Fall"
          else -> return null
        }
    return "Premieres $seasonName $year"
  }

  private fun whenTextFromEpoch(epochSeconds: Long, nowMillis: Long): String? =
      whenTextFromLocalDate(
          Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate(),
          nowMillis,
      )

  private fun whenTextFromLocalDate(targetDate: LocalDate, nowMillis: Long): String? {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val daysUntil = ChronoUnit.DAYS.between(today, targetDate).toInt()
    if (daysUntil < 0) return null
    return when (daysUntil) {
      0 -> "Today"
      1 -> "Tomorrow"
      else -> "in $daysUntil days"
    }
  }

  fun withUserState(
      listEntryId: Int? = this.listEntryId,
      listStatus: ListStatus? = this.listStatus,
      userScore: Float? = this.userScore,
      isFavourite: Boolean = this.isFavourite,
  ): AnimeSlide =
      copy(
          listEntryId = listEntryId,
          listStatus = listStatus,
          userScore = userScore,
          isFavourite = isFavourite,
      )
}

data class AppSettings(
    val shuffle: Boolean = true,
    val intervalMs: Long = 12_000L,
    val sourceMode: SourceMode = SourceMode.LIBRARY,
    val listStatuses: Set<ListStatus> = setOf(ListStatus.CURRENT),
    val formatFilter: FormatFilter = FormatFilter.ALL,
    val librarySort: LibrarySort = LibrarySort.POPULARITY,
    val seasonKey: String = SeasonSelection.ANY_KEY,
    val frameMode: FrameMode = FrameMode.POSTER_ONLY,
    val showPosterClock: Boolean = true,
    val showWeather: Boolean = false,
    val weatherFahrenheit: Boolean = java.util.Locale.getDefault().country == "US",
    val weatherLat: Double? = null,
    val weatherLon: Double? = null,
    val weatherPlace: String? = null,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val powerMode: PowerMode = PowerMode.ALWAYS_ON,
    val idleSleepMinutes: Int = PowerPolicy.DEFAULT_IDLE_SLEEP_MINUTES,
    val sleepStartMinutes: Int = PowerPolicy.DEFAULT_SLEEP_START_MINUTES,
    val sleepEndMinutes: Int = PowerPolicy.DEFAULT_SLEEP_END_MINUTES,
) {
  fun libraryFilters(): LibraryFilters =
      LibraryFilters(
          format = formatFilter,
          sort = librarySort,
          seasonKey = seasonKey,
      )

  fun cacheKey(): String =
      when (sourceMode) {
        SourceMode.PERSONAL ->
            "personal_" +
                listStatuses
                    .sortedBy { it.ordinal }
                    .joinToString("_") { it.name }
        SourceMode.LIBRARY -> "library_${formatFilter.name}_${librarySort.name}_$seasonKey"
      }

  fun calendarCacheKey(weekStartEpochDay: Long): String =
      "calendar_${sourceMode.name}_${formatFilter.name}_${librarySort.name}_" +
          listStatuses.sortedBy { it.ordinal }.joinToString("_") { it.name } +
          "_$weekStartEpochDay"
}

data class ViewerProfile(
    val id: Int,
    val name: String,
)
