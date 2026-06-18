package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryFlagTest {
  @Test
  fun isoCountryFlagEmoji_jp_returnsFlag() {
    assertEquals("🇯🇵", isoCountryFlagEmoji("JP"))
  }

  @Test
  fun isoCountryFlagEmoji_lowercaseWorks() {
    assertEquals("🇯🇵", isoCountryFlagEmoji("jp"))
  }

  @Test
  fun isoCountryFlagEmoji_invalidLength_returnsEmpty() {
    assertEquals("", isoCountryFlagEmoji("JPN"))
    assertEquals("", isoCountryFlagEmoji("J"))
  }

  @Test
  fun isoCountryFlagEmoji_nonAlpha_returnsEmpty() {
    assertEquals("", isoCountryFlagEmoji("J1"))
  }
}
