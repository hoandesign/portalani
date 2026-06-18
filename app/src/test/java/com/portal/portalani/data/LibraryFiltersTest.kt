package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFiltersTest {
  @Test
  fun formatFilter_encodeDecode_roundTrip() {
    val selected = setOf(FormatFilter.TV, FormatFilter.MOVIE)
    val encoded = FormatFilter.encodeSelection(selected)
    assertEquals(selected, FormatFilter.decodeSelection(encoded))
  }

  @Test
  fun formatFilter_allSelected_encodesAsAll() {
    assertEquals("ALL", FormatFilter.encodeSelection(FormatFilter.defaultSelection()))
  }

  @Test
  fun formatFilter_decodeEmpty_returnsDefaults() {
    assertEquals(FormatFilter.defaultSelection(), FormatFilter.decodeSelection(null))
    assertEquals(FormatFilter.defaultSelection(), FormatFilter.decodeSelection(""))
  }

  @Test
  fun formatFilter_decodeIgnoresUnknownTokens() {
    val decoded = FormatFilter.decodeSelection("TV,NOT_A_REAL_FORMAT")
    assertEquals(setOf(FormatFilter.TV), decoded)
  }

  @Test
  fun countryFilter_encodeDecode_roundTrip() {
    val selected = setOf(CountryFilter.JP, CountryFilter.US)
    assertEquals(selected, CountryFilter.decodeSelection(CountryFilter.encodeSelection(selected)))
  }

  @Test
  fun sourceFilter_encodeDecode_roundTrip() {
    val selected = setOf(SourceFilter.MANGA, SourceFilter.LIGHT_NOVEL)
    assertEquals(selected, SourceFilter.decodeSelection(SourceFilter.encodeSelection(selected)))
  }

  @Test
  fun demographicFilter_encodeDecode_roundTrip() {
    val selected = setOf(DemographicFilter.SHOUNEN, DemographicFilter.SEINEN)
    assertEquals(selected, DemographicFilter.decodeSelection(DemographicFilter.encodeSelection(selected)))
  }

  @Test
  fun matchesMediaFormat_allSelected_passesAnyFormat() {
    assertTrue(FormatFilter.defaultSelection().matchesMediaFormat("TV"))
    assertTrue(FormatFilter.defaultSelection().matchesMediaFormat(null))
  }

  @Test
  fun matchesMediaFormat_narrowed_requiresMatch() {
    val tvOnly = setOf(FormatFilter.TV)
    assertTrue(tvOnly.matchesMediaFormat("TV"))
    assertFalse(tvOnly.matchesMediaFormat("MOVIE"))
    assertFalse(tvOnly.matchesMediaFormat(null))
  }

  @Test
  fun matchesMediaCountry_narrowed_requiresMatch() {
    val jpOnly = setOf(CountryFilter.JP)
    assertTrue(jpOnly.matchesMediaCountry("JP"))
    assertFalse(jpOnly.matchesMediaCountry("US"))
  }

  @Test
  fun matchesMediaSource_narrowed_requiresMatch() {
    val mangaOnly = setOf(SourceFilter.MANGA)
    assertTrue(mangaOnly.matchesMediaSource("MANGA"))
    assertFalse(mangaOnly.matchesMediaSource("ORIGINAL"))
  }

  @Test
  fun matchesMediaTags_demographicTagMatch() {
    val shounenOnly = setOf(DemographicFilter.SHOUNEN)
    assertTrue(shounenOnly.matchesMediaTags(listOf("Shounen", "Action")))
    assertFalse(shounenOnly.matchesMediaTags(listOf("Seinen")))
    assertFalse(shounenOnly.matchesMediaTags(emptyList()))
  }

  @Test
  fun containsHentaiGenre_caseInsensitive() {
    assertTrue(listOf("Action", "Hentai").containsHentaiGenre())
    assertTrue(listOf("hentai").containsHentaiGenre())
    assertFalse(listOf("Action", "Romance").containsHentaiGenre())
  }

  @Test
  fun matchesSlide_hideHentai_excludesHentaiGenre() {
    val filters = LibraryFilters(hideHentai = true)
    val hentaiSlide = sampleSlide(genres = listOf("Hentai"))
    assertFalse(filters.matchesSlide(hentaiSlide, applySeasonFilter = false))
  }

  @Test
  fun matchesCalendarEntry_hideHentai_excludesHentaiGenre() {
    val filters = LibraryFilters(hideHentai = true)
    val entry = sampleCalendarEntry(genres = listOf("Hentai"))
    assertFalse(filters.matchesCalendarEntry(entry))
  }

  @Test
  fun matchesSlide_seasonYearMismatch_rejected() {
    val filters = LibraryFilters(seasonKey = "season:SPRING:2024")
    val slide = sampleSlide(season = "SPRING", seasonYear = 2023)
    assertFalse(filters.matchesSlide(slide))
  }

  @Test
  fun matchesSlide_seasonMatch_accepted() {
    val filters = LibraryFilters(seasonKey = "season:SPRING:2024")
    val slide = sampleSlide(season = "SPRING", seasonYear = 2024)
    assertTrue(filters.matchesSlide(slide))
  }

  @Test
  fun matchesSlide_applySeasonFilterFalse_skipsSeason() {
    val filters = LibraryFilters(seasonKey = "season:SPRING:2024")
    val slide = sampleSlide(season = "FALL", seasonYear = 2020)
    assertTrue(filters.matchesSlide(slide, applySeasonFilter = false))
  }
}
