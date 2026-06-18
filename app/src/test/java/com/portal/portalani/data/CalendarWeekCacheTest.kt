package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarWeekCacheTest {
  @Test
  fun roundTrip_preservesEntryFields() {
    val original =
        sampleCalendarEntry(
            scheduleId = 55,
            mediaId = 99,
            listStatus = ListStatus.PLANNING,
            genres = listOf("Action"),
            tags = listOf("Shounen", "School"),
        )
    val restored = original.toCacheJson().toCalendarAiringEntryOrNull()
    assertNotNull(restored)
    assertEquals(original.scheduleId, restored!!.scheduleId)
    assertEquals(original.mediaId, restored.mediaId)
    assertEquals(original.episode, restored.episode)
    assertEquals(original.airingAt, restored.airingAt)
    assertEquals(original.listStatus, restored.listStatus)
    assertEquals(original.genres, restored.genres)
    assertEquals(original.tags, restored.tags)
  }

  @Test
  fun parse_missingCover_returnsNull() {
    val json = sampleCalendarEntry().toCacheJson().put("coverUrl", "")
    assertNull(json.toCalendarAiringEntryOrNull())
  }

  @Test
  fun parse_missingEpisode_returnsNull() {
    val json = sampleCalendarEntry().toCacheJson().put("episode", 0)
    assertNull(json.toCalendarAiringEntryOrNull())
  }

  @Test
  fun parse_missingAiringAt_returnsNull() {
    val json = sampleCalendarEntry().toCacheJson().put("airingAt", 0)
    assertNull(json.toCalendarAiringEntryOrNull())
  }
}
