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
  ;

  companion object {
    val selectable: List<FormatFilter> = entries.filter { it != ALL }

    fun defaultSelection(): Set<FormatFilter> = selectable.toSet()

    fun normalizeSelection(selected: Set<FormatFilter>): Set<FormatFilter> {
      val concrete = selected.filter { it != ALL }.toSet()
      return if (concrete.isEmpty()) defaultSelection() else concrete
    }

    fun isAllSelected(selected: Set<FormatFilter>): Boolean =
        normalizeSelection(selected).containsAll(selectable)

    fun decodeSelection(raw: String?): Set<FormatFilter> {
      if (raw.isNullOrBlank() || raw == ALL.name) return defaultSelection()
      val parsed =
          raw.split(',')
              .mapNotNull { token ->
                token.trim().takeIf { it.isNotEmpty() }?.let {
                  runCatching { valueOf(it) }.getOrNull()?.takeIf { f -> f != ALL }
                }
              }
              .toSet()
      return normalizeSelection(parsed)
    }

    fun encodeSelection(selected: Set<FormatFilter>): String =
        if (isAllSelected(selected)) ALL.name
        else normalizeSelection(selected).sortedBy { it.ordinal }.joinToString(",") { it.name }

    /** Migrate a single-format preference from older builds. */
    fun fromLegacy(single: FormatFilter): Set<FormatFilter> =
        if (single == ALL) defaultSelection() else setOf(single)
  }
}

fun Set<FormatFilter>.matchesMediaFormat(mediaFormat: String?): Boolean {
  val normalized = FormatFilter.normalizeSelection(this)
  if (FormatFilter.isAllSelected(normalized)) return true
  val format = mediaFormat?.takeIf { it.isNotBlank() } ?: return false
  return normalized.any { it.apiValue.equals(format, ignoreCase = true) }
}

/** ISO 3166-1 alpha-2 country codes from AniList `countryOfOrigin`. */
enum class CountryFilter(val apiValue: String, val label: String) {
  ALL("", "All countries"),
  JP("JP", "Japan"),
  CN("CN", "China"),
  KR("KR", "South Korea"),
  TW("TW", "Taiwan"),
  US("US", "United States"),
  TH("TH", "Thailand"),
  FR("FR", "France"),
  DE("DE", "Germany"),
  GB("GB", "United Kingdom"),
  IT("IT", "Italy"),
  ES("ES", "Spain"),
  ;

  companion object {
    val selectable: List<CountryFilter> = entries.filter { it != ALL }

    fun defaultSelection(): Set<CountryFilter> = selectable.toSet()

    fun normalizeSelection(selected: Set<CountryFilter>): Set<CountryFilter> {
      val concrete = selected.filter { it != ALL }.toSet()
      return if (concrete.isEmpty()) defaultSelection() else concrete
    }

    fun isAllSelected(selected: Set<CountryFilter>): Boolean =
        normalizeSelection(selected).containsAll(selectable)

    fun decodeSelection(raw: String?): Set<CountryFilter> {
      if (raw.isNullOrBlank() || raw == ALL.name) return defaultSelection()
      val parsed =
          raw.split(',')
              .mapNotNull { token ->
                token.trim().takeIf { it.isNotEmpty() }?.let {
                  runCatching { valueOf(it) }.getOrNull()?.takeIf { f -> f != ALL }
                }
              }
              .toSet()
      return normalizeSelection(parsed)
    }

    fun encodeSelection(selected: Set<CountryFilter>): String =
        if (isAllSelected(selected)) ALL.name
        else normalizeSelection(selected).sortedBy { it.ordinal }.joinToString(",") { it.name }
  }
}

fun Set<CountryFilter>.matchesMediaCountry(countryOfOrigin: String?): Boolean {
  val normalized = CountryFilter.normalizeSelection(this)
  if (CountryFilter.isAllSelected(normalized)) return true
  val country = countryOfOrigin?.takeIf { it.isNotBlank() } ?: return false
  return normalized.any { it.apiValue.equals(country, ignoreCase = true) }
}

/** Adaptation source from AniList `MediaSource`. */
enum class SourceFilter(val apiValue: String, val label: String) {
  ALL("", "All sources"),
  ORIGINAL("ORIGINAL", "Original"),
  MANGA("MANGA", "Manga"),
  LIGHT_NOVEL("LIGHT_NOVEL", "Light novel"),
  VISUAL_NOVEL("VISUAL_NOVEL", "Visual novel"),
  VIDEO_GAME("VIDEO_GAME", "Video game"),
  NOVEL("NOVEL", "Novel"),
  WEB_NOVEL("WEB_NOVEL", "Web novel"),
  ANIME("ANIME", "Anime"),
  LIVE_ACTION("LIVE_ACTION", "Live action"),
  DOUJINSHI("DOUJINSHI", "Doujinshi"),
  COMIC("COMIC", "Comic"),
  GAME("GAME", "Game"),
  MULTIMEDIA_PROJECT("MULTIMEDIA_PROJECT", "Multimedia project"),
  PICTURE_BOOK("PICTURE_BOOK", "Picture book"),
  OTHER("OTHER", "Other"),
  ;

  companion object {
    val selectable: List<SourceFilter> = entries.filter { it != ALL }

    fun defaultSelection(): Set<SourceFilter> = selectable.toSet()

    fun normalizeSelection(selected: Set<SourceFilter>): Set<SourceFilter> {
      val concrete = selected.filter { it != ALL }.toSet()
      return if (concrete.isEmpty()) defaultSelection() else concrete
    }

    fun isAllSelected(selected: Set<SourceFilter>): Boolean =
        normalizeSelection(selected).containsAll(selectable)

    fun decodeSelection(raw: String?): Set<SourceFilter> {
      if (raw.isNullOrBlank() || raw == ALL.name) return defaultSelection()
      val parsed =
          raw.split(',')
              .mapNotNull { token ->
                token.trim().takeIf { it.isNotEmpty() }?.let {
                  runCatching { valueOf(it) }.getOrNull()?.takeIf { f -> f != ALL }
                }
              }
              .toSet()
      return normalizeSelection(parsed)
    }

    fun encodeSelection(selected: Set<SourceFilter>): String =
        if (isAllSelected(selected)) ALL.name
        else normalizeSelection(selected).sortedBy { it.ordinal }.joinToString(",") { it.name }
  }
}

fun Set<SourceFilter>.matchesMediaSource(source: String?): Boolean {
  val normalized = SourceFilter.normalizeSelection(this)
  if (SourceFilter.isAllSelected(normalized)) return true
  val value = source?.takeIf { it.isNotBlank() } ?: return false
  return normalized.any { it.apiValue.equals(value, ignoreCase = true) }
}

/** Demographic audience tags from AniList (tag category Demographic). */
enum class DemographicFilter(val tagName: String, val label: String) {
  ALL("", "All demographics"),
  SHOUNEN("Shounen", "Shounen"),
  SHOUJO("Shoujo", "Shoujo"),
  SEINEN("Seinen", "Seinen"),
  JOSEI("Josei", "Josei"),
  KIDS("Kids", "Kids"),
  ;

  companion object {
    val selectable: List<DemographicFilter> = entries.filter { it != ALL }

    fun defaultSelection(): Set<DemographicFilter> = selectable.toSet()

    fun normalizeSelection(selected: Set<DemographicFilter>): Set<DemographicFilter> {
      val concrete = selected.filter { it != ALL }.toSet()
      return if (concrete.isEmpty()) defaultSelection() else concrete
    }

    fun isAllSelected(selected: Set<DemographicFilter>): Boolean =
        normalizeSelection(selected).containsAll(selectable)

    fun decodeSelection(raw: String?): Set<DemographicFilter> {
      if (raw.isNullOrBlank() || raw == ALL.name) return defaultSelection()
      val parsed =
          raw.split(',')
              .mapNotNull { token ->
                token.trim().takeIf { it.isNotEmpty() }?.let {
                  runCatching { valueOf(it) }.getOrNull()?.takeIf { f -> f != ALL }
                }
              }
              .toSet()
      return normalizeSelection(parsed)
    }

    fun encodeSelection(selected: Set<DemographicFilter>): String =
        if (isAllSelected(selected)) ALL.name
        else normalizeSelection(selected).sortedBy { it.ordinal }.joinToString(",") { it.name }
  }
}

fun Set<DemographicFilter>.matchesMediaTags(tags: List<String>): Boolean {
  val normalized = DemographicFilter.normalizeSelection(this)
  if (DemographicFilter.isAllSelected(normalized)) return true
  if (tags.isEmpty()) return false
  val tagNames = tags.map { it.lowercase() }.toSet()
  return normalized.any { demo -> demo.tagName.lowercase() in tagNames }
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
    val formats: Set<FormatFilter> = FormatFilter.defaultSelection(),
    val countries: Set<CountryFilter> = CountryFilter.defaultSelection(),
    val sources: Set<SourceFilter> = SourceFilter.defaultSelection(),
    val demographics: Set<DemographicFilter> = DemographicFilter.defaultSelection(),
    val sort: LibrarySort = LibrarySort.POPULARITY,
    val seasonKey: String = SeasonSelection.ANY_KEY,
    val hideHentai: Boolean = true,
) {
  fun resolvedSeason(): LibrarySeasonParams = SeasonSelection.resolve(seasonKey)

  fun formatApiValues(): List<String>? {
    if (FormatFilter.isAllSelected(formats)) return null
    return FormatFilter.normalizeSelection(formats).mapNotNull { it.apiValue }
  }

  fun sourceApiValues(): List<String>? {
    if (SourceFilter.isAllSelected(sources)) return null
    return SourceFilter.normalizeSelection(sources).map { it.apiValue }
  }

  fun demographicTagApiValues(): List<String>? {
    if (DemographicFilter.isAllSelected(demographics)) return null
    return DemographicFilter.normalizeSelection(demographics).map { it.tagName }
  }

  fun matchesCalendarEntry(entry: CalendarAiringEntry): Boolean {
    if (hideHentai && entry.genres.containsHentaiGenre()) return false
    if (!formats.matchesMediaFormat(entry.format)) return false
    if (!countries.matchesMediaCountry(entry.countryOfOrigin)) return false
    if (!sources.matchesMediaSource(entry.source)) return false
    if (!demographics.matchesMediaTags(entry.tags)) return false
    return true
  }

  /** Client-side guard when API returns entries with missing or mismatched year metadata. */
  fun matchesSlide(slide: AnimeSlide, applySeasonFilter: Boolean = true): Boolean {
    if (hideHentai && slide.genres.containsHentaiGenre()) return false
    if (!formats.matchesMediaFormat(slide.format)) return false
    if (!countries.matchesMediaCountry(slide.countryOfOrigin)) return false
    if (!sources.matchesMediaSource(slide.source)) return false
    if (!demographics.matchesMediaTags(slide.tags)) return false
    if (!applySeasonFilter) return true
    val params = resolvedSeason()
    val targetYear = params.seasonYear ?: return true
    val slideYear = slide.airingYear() ?: return false
    if (slideYear != targetYear) return false
    val targetSeason = params.season ?: return true
    val slideSeason = slide.season ?: return true
    return slideSeason.equals(targetSeason, ignoreCase = true)
  }
}

const val HENTAI_GENRE = "Hentai"

fun List<String>.containsHentaiGenre(): Boolean = any { it.equals(HENTAI_GENRE, ignoreCase = true) }

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
      THIS_YEAR_KEY ->
          return SeasonPickerState(
              SeasonPickerSeason.ANY,
              SeasonPickerYear.Specific(Calendar.getInstance().get(Calendar.YEAR)),
          )
      LAST_YEAR_KEY ->
          return SeasonPickerState(
              SeasonPickerSeason.ANY,
              SeasonPickerYear.Specific(Calendar.getInstance().get(Calendar.YEAR) - 1),
          )
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
    val maxYear = nowYear + 2
    val years = (maxYear downTo MIN_YEAR).map { SeasonPickerYear.Specific(it) }
    return listOf(SeasonPickerYear.Any) + years
  }

  /** Current season plus the next [count] - 1 seasons (4 seasons by default). */
  fun seasonWindowForward(count: Int): List<LibrarySeasonParams> {
    var (season, year) = currentSeason()
    val result = mutableListOf<LibrarySeasonParams>()
    repeat(count) {
      result.add(LibrarySeasonParams(season = season.apiValue, seasonYear = year))
      val next = stepNext(season, year)
      season = next.first
      year = next.second
    }
    return result
  }

  private fun stepNext(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> =
      when (season) {
        AnimeSeason.WINTER -> AnimeSeason.SPRING to year
        AnimeSeason.SPRING -> AnimeSeason.SUMMER to year
        AnimeSeason.SUMMER -> AnimeSeason.FALL to year
        AnimeSeason.FALL -> AnimeSeason.WINTER to year + 1
      }

  fun normalizePickerState(state: SeasonPickerState): SeasonPickerState {
    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    val normalizedYear =
        when (val year = state.year) {
          SeasonPickerYear.ThisYear -> SeasonPickerYear.Specific(nowYear)
          SeasonPickerYear.LastYear -> SeasonPickerYear.Specific(nowYear - 1)
          else -> year
        }
    return if (normalizedYear == state.year) state else state.copy(year = normalizedYear)
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
