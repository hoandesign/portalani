package com.portal.portalani.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsStoreTest {
  private fun parse(
      strings: Map<String, String> = emptyMap(),
      booleans: Map<String, Boolean> = emptyMap(),
      longs: Map<String, Long> = emptyMap(),
      ints: Map<String, Int> = emptyMap(),
      floats: Map<String, Float> = emptyMap(),
  ): AppSettings =
      parseAppSettings(
          containsKey = { key -> strings.containsKey(key) || booleans.containsKey(key) },
          getString = { key, default -> strings[key] ?: default },
          getBoolean = { key, default -> booleans[key] ?: default },
          getLong = { key, default -> longs[key] ?: default },
          getInt = { key, default -> ints[key] ?: default },
          getFloat = { key, default -> floats[key] ?: default },
      )

  @Test
  fun parse_defaultsWhenEmpty() {
    val settings = parse()
    assertEquals(SourceMode.PERSONAL, settings.sourceMode)
    assertEquals(setOf(ListStatus.CURRENT), settings.listStatuses)
    assertEquals(LibrarySort.POPULARITY, settings.librarySort)
    assertEquals(FrameMode.POSTER_ONLY, settings.frameMode)
    assertFalse(settings.showWeather)
  }

  @Test
  fun parse_corruptSourceMode_fallsBackToLibrary() {
    val settings =
        parse(
            strings = mapOf(SettingsStore.KEY_SOURCE_MODE to "NOT_A_MODE"),
        )
    assertEquals(SourceMode.LIBRARY, settings.sourceMode)
  }

  @Test
  fun parse_legacyUseMyListFalse_usesLibrary() {
    val settings =
        parse(
            booleans = mapOf(SettingsStore.KEY_USE_MY_LIST_LEGACY to false),
        )
    assertEquals(SourceMode.LIBRARY, settings.sourceMode)
  }

  @Test
  fun parse_badListStatusCsv_ignoresInvalidTokens() {
    val settings =
        parse(
            strings =
                mapOf(
                    SettingsStore.KEY_LIST_STATUSES to "CURRENT,NOT_A_STATUS,PLANNING",
                ),
        )
    assertEquals(setOf(ListStatus.CURRENT, ListStatus.PLANNING), settings.listStatuses)
  }

  @Test
  fun parse_corruptLegacyListStatus_usesCurrent() {
    val settings =
        parse(
            strings = mapOf(SettingsStore.KEY_LIST_STATUS to "BROKEN"),
        )
    assertEquals(setOf(ListStatus.CURRENT), settings.listStatuses)
  }

  @Test
  fun parse_corruptFormatFilterLegacy_usesAllFormats() {
    val settings =
        parse(
            strings = mapOf(SettingsStore.KEY_FORMAT_FILTER to "NOT_A_FORMAT"),
        )
    assertEquals(FormatFilter.defaultSelection(), settings.formatFilters)
  }

  @Test
  fun parse_corruptLibrarySort_usesPopularity() {
    val settings =
        parse(
            strings =
                mapOf(
                    SettingsStore.KEY_SOURCE_MODE to SourceMode.LIBRARY.name,
                    SettingsStore.KEY_LIBRARY_SORT to "INVALID",
                ),
        )
    assertEquals(LibrarySort.POPULARITY, settings.librarySort)
  }
}
