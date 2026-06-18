package com.portal.portalani.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarWeekTest {
  private val thursday = LocalDate.of(2026, 6, 18)

  @Test
  fun startOfWeek_mondayAnchor() {
    assertEquals(LocalDate.of(2026, 6, 15), CalendarWeek.startOfWeek(thursday, WeekStart.MONDAY))
  }

  @Test
  fun startOfWeek_sundayAnchor() {
    assertEquals(LocalDate.of(2026, 6, 14), CalendarWeek.startOfWeek(thursday, WeekStart.SUNDAY))
  }

  @Test
  fun startOfWeek_sundayWhenDateIsSunday() {
    val sunday = LocalDate.of(2026, 6, 14)
    assertEquals(sunday, CalendarWeek.startOfWeek(sunday, WeekStart.SUNDAY))
  }

  @Test
  fun dayIndex_placesThursdayOnCorrectColumn() {
    val weekStart = LocalDate.of(2026, 6, 15)
    assertEquals(3, CalendarWeek.dayIndex(thursday, weekStart))
  }

  @Test
  fun groupByDay_bucketsEntryByLocalDate() {
    val zone = ZoneId.of("UTC")
    val weekStart = LocalDate.of(2026, 6, 15)
    val airingAt = thursday.atStartOfDay(zone).plusHours(20).toEpochSecond().toInt()
    val entry = sampleCalendarEntry(airingAt = airingAt)
    val buckets = CalendarWeek.groupByDay(listOf(entry), weekStart, zone)
    assertEquals(1, buckets[3].size)
    assertEquals(entry, buckets[3].first())
  }

  @Test
  fun sortEntries_listAnimeFirst() {
    val onList = sampleCalendarEntry(scheduleId = 1, listStatus = ListStatus.CURRENT, airingAt = 100)
    val offList = sampleCalendarEntry(scheduleId = 2, listStatus = null, airingAt = 50)
    val sorted = CalendarWeek.sortEntries(listOf(offList, onList), LibrarySort.POPULARITY)
    assertEquals(onList, sorted.first())
  }

  @Test
  fun sortEntries_byScoreDescending() {
    val low = sampleCalendarEntry(scheduleId = 1, averageScore = 60, airingAt = 100)
    val high = sampleCalendarEntry(scheduleId = 2, averageScore = 90, airingAt = 100)
    val sorted = CalendarWeek.sortEntries(listOf(low, high), LibrarySort.SCORE)
    assertEquals(high, sorted.first())
  }

  @Test
  fun matchesHideHentai_blocksWhenEnabled() {
    val entry = sampleCalendarEntry(genres = listOf("Hentai"))
    assertFalse(CalendarWeek.matchesHideHentai(entry, hideHentai = true))
    assertTrue(CalendarWeek.matchesHideHentai(entry, hideHentai = false))
  }

  @Test
  fun matchesContentFilters_respectsLibraryFilters() {
    val filters = LibraryFilters(formats = setOf(FormatFilter.MOVIE))
    val tvEntry = sampleCalendarEntry(format = "TV")
    assertFalse(CalendarWeek.matchesContentFilters(tvEntry, filters))
  }

  @Test
  fun localDate_usesZoneId() {
    val zone = ZoneId.of("America/New_York")
    val date = LocalDate.of(2026, 6, 18)
    val epoch = date.atTime(4, 0).atZone(zone).toEpochSecond().toInt()
    assertEquals(date, sampleCalendarEntry(airingAt = epoch).localDate(zone))
  }
}
