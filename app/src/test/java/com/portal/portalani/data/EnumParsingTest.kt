package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnumParsingTest {
  @Test
  fun enumValueOrNull_blankOrUnknown_returnsNull() {
    assertNull(enumValueOrNull<ListStatus>(null))
    assertNull(enumValueOrNull<ListStatus>(""))
    assertNull(enumValueOrNull<ListStatus>("null"))
    assertNull(enumValueOrNull<ListStatus>("NOT_A_STATUS"))
  }

  @Test
  fun enumValueOrNull_validName_returnsEnum() {
    assertEquals(ListStatus.CURRENT, enumValueOrNull<ListStatus>("CURRENT"))
  }

  @Test
  fun decodeCommaSeparatedEnumSelection_ignoresUnknownTokens() {
    val decoded = FormatFilter.decodeSelection("TV,NOT_A_REAL_FORMAT")
    assertEquals(setOf(FormatFilter.TV), FormatFilter.normalizeSelection(decoded))
  }
}
