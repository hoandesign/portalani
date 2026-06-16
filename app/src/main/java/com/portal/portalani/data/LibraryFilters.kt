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
) {
  /** Year-only filters (no season) must use startDate — AniList ignores seasonYear without season. */
  val usesStartDateRange: Boolean
    get() = seasonYear != null && season == null

  fun startDateGreater(): Int? = seasonYear?.takeIf { usesStartDateRange }?.let { year -> year * 10_000 + 101 }

  fun startDateLesser(): Int? = seasonYear?.takeIf { usesStartDateRange }?.let { year -> year * 10_000 + 1231 }
}

data class LibraryFilters(
    val format: FormatFilter = FormatFilter.ALL,
    val sort: LibrarySort = LibrarySort.POPULARITY,
    val seasonKey: String = SeasonSelection.ANY_KEY,
) {
  fun resolvedSeason(): LibrarySeasonParams = SeasonSelection.resolve(seasonKey)

  /** Client-side guard when API returns entries with missing or mismatched year metadata. */
  fun matchesSlide(slide: AnimeSlide): Boolean {
    val params = resolvedSeason()
    val targetYear = params.seasonYear ?: return true
    val slideYear = slide.airingYear() ?: return false
    if (slideYear != targetYear) return false
    val targetSeason = params.season ?: return true
    val slideSeason = slide.season ?: return true
    return slideSeason.equals(targetSeason, ignoreCase = true)
  }
}

enum class SeasonPickerSeason {
  ANY,
  CURRENT,
  PREVIOUS,
  WINTER,
  SPRING,
  SUMMER,
  FALL,
  ;

  val label: String
    get() =
        when (this) {
          ANY -> "All seasons"
          CURRENT -> "This season"
          PREVIOUS -> "Last season"
          WINTER -> AnimeSeason.WINTER.label
          SPRING -> AnimeSeason.SPRING.label
          SUMMER -> AnimeSeason.SUMMER.label
          FALL -> AnimeSeason.FALL.label
        }

  val ignoresYear: Boolean
    get() = this == CURRENT || this == PREVIOUS
}

sealed class SeasonPickerYear {
  object Any : SeasonPickerYear()

  object ThisYear : SeasonPickerYear()

  object LastYear : SeasonPickerYear()

  data class Specific(val year: Int) : SeasonPickerYear()

  fun label(nowYear: Int = Calendar.getInstance().get(Calendar.YEAR)): String =
      when (this) {
        Any -> "All years"
        ThisYear -> "This year ($nowYear)"
        LastYear -> "Last year (${nowYear - 1})"
        is Specific -> year.toString()
      }
}

data class SeasonPickerState(
    val season: SeasonPickerSeason,
    val year: SeasonPickerYear,
)

object SeasonSelection {
  const val ANY_KEY = "any"
  const val CURRENT_KEY = "current"
  const val PREVIOUS_KEY = "previous"
  const val THIS_YEAR_KEY = "year:this"
  const val LAST_YEAR_KEY = "year:last"
  const val MIN_YEAR = 1940

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
    if (key.startsWith("year:")) {
      val yearToken = key.removePrefix("year:")
      yearToken.toIntOrNull()?.let { return LibrarySeasonParams(seasonYear = it) }
    }
    if (key.startsWith("season-only:")) {
      val seasonName = key.removePrefix("season-only:")
      val season = runCatching { AnimeSeason.valueOf(seasonName) }.getOrNull() ?: return LibrarySeasonParams()
      return LibrarySeasonParams(season = season.apiValue)
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

  fun labelFor(key: String): String = labelFor(decode(key))

  fun labelFor(state: SeasonPickerState): String {
    if (state.season.ignoresYear) {
      return when (state.season) {
        SeasonPickerSeason.CURRENT -> {
          val (season, year) = currentSeason()
          "This season (${season.label} $year)"
        }
        SeasonPickerSeason.PREVIOUS -> {
          val (season, year) = previousSeason()
          "Last season (${season.label} $year)"
        }
        else -> state.season.label
      }
    }
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    return when {
      state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Any -> "Any season"
      state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.ThisYear ->
          "This year ($nowYear)"
      state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.LastYear ->
          "Last year (${nowYear - 1})"
      state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Specific ->
          "Year ${state.year.year}"
      state.season != SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Any -> state.season.label
      state.season != SeasonPickerSeason.ANY && state.year is SeasonPickerYear.ThisYear ->
          "${state.season.label} $nowYear"
      state.season != SeasonPickerSeason.ANY && state.year is SeasonPickerYear.LastYear ->
          "${state.season.label} ${nowYear - 1}"
      state.season != SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Specific ->
          "${state.season.label} ${state.year.year}"
      else -> "Any season"
    }
  }

  fun decode(key: String): SeasonPickerState {
    when (key) {
      ANY_KEY -> return SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.Any)
      CURRENT_KEY -> return SeasonPickerState(SeasonPickerSeason.CURRENT, SeasonPickerYear.Any)
      PREVIOUS_KEY -> return SeasonPickerState(SeasonPickerSeason.PREVIOUS, SeasonPickerYear.Any)
      THIS_YEAR_KEY -> return SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.ThisYear)
      LAST_YEAR_KEY -> return SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.LastYear)
    }
    if (key.startsWith("year:")) {
      val yearToken = key.removePrefix("year:")
      yearToken.toIntOrNull()?.let {
        return SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.Specific(it))
      }
    }
    if (key.startsWith("season-only:")) {
      val seasonName = key.removePrefix("season-only:")
      val season = seasonPickerSeasonForName(seasonName) ?: return SeasonPickerState()
      return SeasonPickerState(season, SeasonPickerYear.Any)
    }
    if (key.startsWith("season:")) {
      val parts = key.split(":")
      if (parts.size == 3) {
        val season = seasonPickerSeasonForName(parts[1]) ?: return SeasonPickerState()
        val year = parts[2].toIntOrNull() ?: return SeasonPickerState()
        return SeasonPickerState(season, SeasonPickerYear.Specific(year))
      }
    }
    return SeasonPickerState()
  }

  fun encode(state: SeasonPickerState): String {
    if (state.season == SeasonPickerSeason.CURRENT) return CURRENT_KEY
    if (state.season == SeasonPickerSeason.PREVIOUS) return PREVIOUS_KEY
    if (state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Any) return ANY_KEY
    if (state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.ThisYear) return THIS_YEAR_KEY
    if (state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.LastYear) return LAST_YEAR_KEY
    if (state.season == SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Specific) {
      return "year:${state.year.year}"
    }
    if (state.season != SeasonPickerSeason.ANY && state.year is SeasonPickerYear.Any) {
      return "season-only:${animeSeasonForPicker(state.season).name}"
    }
    val year =
        when (val pickerYear = state.year) {
          is SeasonPickerYear.Specific -> pickerYear.year
          SeasonPickerYear.ThisYear -> Calendar.getInstance().get(Calendar.YEAR)
          SeasonPickerYear.LastYear -> Calendar.getInstance().get(Calendar.YEAR) - 1
          SeasonPickerYear.Any -> return "season-only:${animeSeasonForPicker(state.season).name}"
        }
    return "season:${animeSeasonForPicker(state.season).name}:$year"
  }

  fun seasonColumnOptions(): List<SeasonPickerSeason> =
      listOf(
          SeasonPickerSeason.ANY,
          SeasonPickerSeason.CURRENT,
          SeasonPickerSeason.PREVIOUS,
          SeasonPickerSeason.WINTER,
          SeasonPickerSeason.SPRING,
          SeasonPickerSeason.SUMMER,
          SeasonPickerSeason.FALL,
      )

  fun yearColumnOptions(nowYear: Int = Calendar.getInstance().get(Calendar.YEAR)): List<SeasonPickerYear> {
    val years = (nowYear downTo MIN_YEAR).map { SeasonPickerYear.Specific(it) }
    return listOf(SeasonPickerYear.Any, SeasonPickerYear.ThisYear, SeasonPickerYear.LastYear) + years
  }

  private fun SeasonPickerState(): SeasonPickerState =
      SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.Any)

  private fun seasonPickerSeasonForName(name: String): SeasonPickerSeason? =
      when (name) {
        AnimeSeason.WINTER.name -> SeasonPickerSeason.WINTER
        AnimeSeason.SPRING.name -> SeasonPickerSeason.SPRING
        AnimeSeason.SUMMER.name -> SeasonPickerSeason.SUMMER
        AnimeSeason.FALL.name -> SeasonPickerSeason.FALL
        else -> null
      }

  private fun animeSeasonForPicker(season: SeasonPickerSeason): AnimeSeason =
      when (season) {
        SeasonPickerSeason.WINTER -> AnimeSeason.WINTER
        SeasonPickerSeason.SPRING -> AnimeSeason.SPRING
        SeasonPickerSeason.SUMMER -> AnimeSeason.SUMMER
        SeasonPickerSeason.FALL -> AnimeSeason.FALL
        else -> error("Not a calendar season: $season")
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
