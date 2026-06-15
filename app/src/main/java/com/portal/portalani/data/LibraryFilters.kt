package com.portal.portalani.data

import java.util.Calendar

enum class SourceMode {
  PERSONAL,
  LIBRARY,
}

/** Anime release formats from AniList `MediaFormat`. */
enum class FormatFilter(val apiValue: String?, val label: String) {
  ALL(null, "All formats"),
  TV("TV", "TV series"),
  MOVIE("MOVIE", "Movies"),
  TV_SHORT("TV_SHORT", "TV shorts"),
  OVA("OVA", "OVA"),
  ONA("ONA", "ONA"),
  SPECIAL("SPECIAL", "Specials"),
}

enum class LibrarySort(val apiSort: String, val label: String) {
  POPULARITY("POPULARITY_DESC", "Most popular"),
  SCORE("SCORE_DESC", "Top score"),
  TRENDING("TRENDING_DESC", "Trending now"),
  NEWEST("START_DATE_DESC", "Newest"),
}

enum class AnimeSeason(val apiValue: String, val label: String) {
  WINTER("WINTER", "Winter"),
  SPRING("SPRING", "Spring"),
  SUMMER("SUMMER", "Summer"),
  FALL("FALL", "Fall"),
}

data class LibrarySeasonParams(
    val season: String? = null,
    val seasonYear: Int? = null,
)

data class LibraryFilters(
    val format: FormatFilter = FormatFilter.ALL,
    val sort: LibrarySort = LibrarySort.POPULARITY,
    val seasonKey: String = SeasonSelection.ANY_KEY,
) {
  fun resolvedSeason(): LibrarySeasonParams = SeasonSelection.resolve(seasonKey)
}

object SeasonSelection {
  const val ANY_KEY = "any"
  const val CURRENT_KEY = "current"
  const val PREVIOUS_KEY = "previous"
  const val THIS_YEAR_KEY = "year:this"
  const val LAST_YEAR_KEY = "year:last"

  fun resolve(key: String): LibrarySeasonParams {
    when (key) {
      ANY_KEY -> return LibrarySeasonParams()
      CURRENT_KEY -> {
        val (season, year) = currentSeason()
        return LibrarySeasonParams(season = season.apiValue, seasonYear = year)
      }
      PREVIOUS_KEY -> {
        val (season, year) = previousSeason()
        return LibrarySeasonParams(season = season.apiValue, seasonYear = year)
      }
      THIS_YEAR_KEY -> return LibrarySeasonParams(seasonYear = Calendar.getInstance().get(Calendar.YEAR))
      LAST_YEAR_KEY ->
          return LibrarySeasonParams(seasonYear = Calendar.getInstance().get(Calendar.YEAR) - 1)
    }
    if (key.startsWith("season:")) {
      val parts = key.split(":")
      if (parts.size == 3) {
        val season = AnimeSeason.valueOf(parts[1])
        val year = parts[2].toIntOrNull() ?: return LibrarySeasonParams()
        return LibrarySeasonParams(season = season.apiValue, seasonYear = year)
      }
    }
    return LibrarySeasonParams()
  }

  fun labelFor(key: String): String {
    when (key) {
      ANY_KEY -> return "Any season"
      CURRENT_KEY -> {
        val (season, year) = currentSeason()
        return "This season (${season.label} $year)"
      }
      PREVIOUS_KEY -> {
        val (season, year) = previousSeason()
        return "Last season (${season.label} $year)"
      }
      THIS_YEAR_KEY -> return "This year (${Calendar.getInstance().get(Calendar.YEAR)})"
      LAST_YEAR_KEY -> return "Last year (${Calendar.getInstance().get(Calendar.YEAR) - 1})"
    }
    if (key.startsWith("season:")) {
      val parts = key.split(":")
      if (parts.size == 3) {
        val season = runCatching { AnimeSeason.valueOf(parts[1]) }.getOrNull() ?: return key
        val year = parts[2]
        return "${season.label} $year"
      }
    }
    return key
  }

  fun allOptions(): List<Pair<String, String>> {
    val presets =
        listOf(
            ANY_KEY to labelFor(ANY_KEY),
            CURRENT_KEY to labelFor(CURRENT_KEY),
            PREVIOUS_KEY to labelFor(PREVIOUS_KEY),
            THIS_YEAR_KEY to labelFor(THIS_YEAR_KEY),
            LAST_YEAR_KEY to labelFor(LAST_YEAR_KEY),
        )
    return presets + recentSeasonKeys(16).map { key -> key to labelFor(key) }
  }

  fun recentSeasonKeys(count: Int): List<String> {
    var (season, year) = currentSeason()
    val keys = mutableListOf<String>()
    repeat(count) {
      keys += "season:${season.name}:$year"
      val prev = stepPrevious(season, year)
      season = prev.first
      year = prev.second
    }
    return keys
  }

  private fun currentSeason(): Pair<AnimeSeason, Int> {
    val cal = Calendar.getInstance()
    return seasonForMonth(cal.get(Calendar.MONTH) + 1) to cal.get(Calendar.YEAR)
  }

  private fun previousSeason(): Pair<AnimeSeason, Int> {
    val (season, year) = currentSeason()
    return stepPrevious(season, year)
  }

  private fun stepPrevious(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> =
      when (season) {
        AnimeSeason.WINTER -> AnimeSeason.FALL to year - 1
        AnimeSeason.SPRING -> AnimeSeason.WINTER to year
        AnimeSeason.SUMMER -> AnimeSeason.SPRING to year
        AnimeSeason.FALL -> AnimeSeason.SUMMER to year
      }

  private fun seasonForMonth(month: Int): AnimeSeason =
      when (month) {
        in 1..3 -> AnimeSeason.WINTER
        in 4..6 -> AnimeSeason.SPRING
        in 7..9 -> AnimeSeason.SUMMER
        else -> AnimeSeason.FALL
      }
}
