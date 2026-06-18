package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonSelectionTest {
  @Test
  fun resolve_anyKey_returnsEmptyParams() {
    val params = SeasonSelection.resolve(SeasonSelection.ANY_KEY)
    assertNull(params.season)
    assertNull(params.seasonYear)
  }

  @Test
  fun resolve_seasonSpring2024() {
    val params = SeasonSelection.resolve("season:SPRING:2024")
    assertEquals("SPRING", params.season)
    assertEquals(2024, params.seasonYear)
  }

  @Test
  fun resolve_seasonOnlyWinter() {
    val params = SeasonSelection.resolve("season-only:WINTER")
    assertEquals("WINTER", params.season)
    assertNull(params.seasonYear)
  }

  @Test
  fun resolve_year2020() {
    val params = SeasonSelection.resolve("year:2020")
    assertNull(params.season)
    assertEquals(2020, params.seasonYear)
  }

  @Test
  fun resolve_currentKey_returnsSeasonAndYear() {
    val params = SeasonSelection.resolve(SeasonSelection.CURRENT_KEY)
    assertNotNull(params.season)
    assertNotNull(params.seasonYear)
  }

  @Test
  fun resolve_previousKey_returnsSeasonAndYear() {
    val params = SeasonSelection.resolve(SeasonSelection.PREVIOUS_KEY)
    assertNotNull(params.season)
    assertNotNull(params.seasonYear)
  }

  @Test
  fun resolve_invalidKey_returnsEmptyParams() {
    val params = SeasonSelection.resolve("not-a-real-key")
    assertNull(params.season)
    assertNull(params.seasonYear)
  }

  @Test
  fun decode_encode_roundTrip_stableKeys() {
    val keys =
        listOf(
            SeasonSelection.ANY_KEY,
            SeasonSelection.CURRENT_KEY,
            SeasonSelection.PREVIOUS_KEY,
            "season:SPRING:2024",
            "season-only:WINTER",
            "year:2020",
        )
    for (key in keys) {
      val state = SeasonSelection.decode(key)
      assertEquals(key, SeasonSelection.encode(state))
    }
  }

  @Test
  fun encode_decode_explicitYearState() {
    val state =
        SeasonPickerState(
            season = SeasonPickerSeason.SPRING,
            year = SeasonPickerYear.Specific(2024),
        )
    val key = SeasonSelection.encode(state)
    assertEquals("season:SPRING:2024", key)
    val decoded = SeasonSelection.decode(key)
    assertEquals(SeasonPickerSeason.SPRING, decoded.season)
    assertEquals(SeasonPickerYear.Specific(2024), decoded.year)
  }

  @Test
  fun resolve_encode_consistentForExplicitYear() {
    val state =
        SeasonPickerState(
            season = SeasonPickerSeason.FALL,
            year = SeasonPickerYear.Specific(2022),
        )
    val key = SeasonSelection.encode(state)
    val params = SeasonSelection.resolve(key)
    assertEquals("FALL", params.season)
    assertEquals(2022, params.seasonYear)
  }

  @Test
  fun decode_invalidKey_returnsDefaultPickerState() {
    val state = SeasonSelection.decode("garbage")
    assertEquals(SeasonPickerSeason.ANY, state.season)
    assertEquals(SeasonPickerYear.Any, state.year)
  }

  @Test
  fun normalizePickerState_convertsThisYearToSpecific() {
    val state = SeasonPickerState(SeasonPickerSeason.ANY, SeasonPickerYear.ThisYear)
    val normalized = SeasonSelection.normalizePickerState(state)
    assertTrue(normalized.year is SeasonPickerYear.Specific)
  }

  @Test
  fun yearColumnOptions_includesSpecificYears() {
    val options = SeasonSelection.yearColumnOptions(nowYear = 2026)
    assertTrue(options.first() is SeasonPickerYear.Any)
    assertTrue(options.any { it is SeasonPickerYear.Specific && it.year == 2026 })
    assertTrue(options.any { it is SeasonPickerYear.Specific && it.year == SeasonSelection.MIN_YEAR })
  }
}
