package com.portal.portalani.data

/** Reads persisted app settings from raw preference values (JVM-testable). */
internal fun parseAppSettings(
    containsKey: (String) -> Boolean,
    getString: (String, String?) -> String?,
    getBoolean: (String, Boolean) -> Boolean,
    getLong: (String, Long) -> Long,
    getInt: (String, Int) -> Int,
    getFloat: (String, Float) -> Float,
): AppSettings {
  val sourceMode =
      when {
        containsKey(SettingsStore.KEY_SOURCE_MODE) ->
            enumValueOrNull<SourceMode>(getString(SettingsStore.KEY_SOURCE_MODE, null))
                ?: SourceMode.LIBRARY
        getBoolean(SettingsStore.KEY_USE_MY_LIST_LEGACY, true) -> SourceMode.PERSONAL
        else -> SourceMode.LIBRARY
      }

  val listStatuses =
      getString(SettingsStore.KEY_LIST_STATUSES, null)
          ?.split(',')
          ?.mapNotNull { token ->
            token.trim().takeIf { it.isNotEmpty() }?.let { enumValueOrNull<ListStatus>(it) }
          }
          ?.toSet()
          ?.takeIf { it.isNotEmpty() }
          ?: setOf(
              enumValueOrNull<ListStatus>(
                  getString(SettingsStore.KEY_LIST_STATUS, ListStatus.CURRENT.name),
              ) ?: ListStatus.CURRENT,
          )

  val weatherPlace = getString(SettingsStore.KEY_WEATHER_PLACE, null)?.ifBlank { null }
  val weatherLat =
      if (weatherPlace != null) {
        getFloat(SettingsStore.KEY_WEATHER_LAT, Float.NaN).toDouble().takeIf { !it.isNaN() }
      } else {
        null
      }
  val weatherLon =
      if (weatherPlace != null) {
        getFloat(SettingsStore.KEY_WEATHER_LON, Float.NaN).toDouble().takeIf { !it.isNaN() }
      } else {
        null
      }

  val formatFilters =
      if (containsKey(SettingsStore.KEY_FORMAT_FILTERS)) {
        FormatFilter.decodeSelection(getString(SettingsStore.KEY_FORMAT_FILTERS, null))
      } else {
        FormatFilter.fromLegacy(
            enumValueOrNull<FormatFilter>(
                getString(SettingsStore.KEY_FORMAT_FILTER, FormatFilter.ALL.name),
            ) ?: FormatFilter.ALL,
        )
      }

  return AppSettings(
      shuffle = getBoolean(SettingsStore.KEY_SHUFFLE, true),
      intervalMs = getLong(SettingsStore.KEY_INTERVAL_MS, 12_000L),
      sourceMode = sourceMode,
      listStatuses = listStatuses,
      formatFilters = formatFilters,
      countryFilters = CountryFilter.decodeSelection(getString(SettingsStore.KEY_COUNTRY_FILTERS, null)),
      sourceFilters = SourceFilter.decodeSelection(getString(SettingsStore.KEY_SOURCE_FILTERS, null)),
      demographicFilters =
          DemographicFilter.decodeSelection(getString(SettingsStore.KEY_DEMOGRAPHIC_FILTERS, null)),
      librarySort =
          enumValueOrNull<LibrarySort>(
              getString(SettingsStore.KEY_LIBRARY_SORT, LibrarySort.POPULARITY.name),
          ) ?: LibrarySort.POPULARITY,
      seasonKey = getString(SettingsStore.KEY_SEASON_KEY, SeasonSelection.ANY_KEY)!!,
      frameMode =
          enumValueOrNull<FrameMode>(
              getString(SettingsStore.KEY_FRAME_MODE, FrameMode.POSTER_ONLY.name),
          ) ?: FrameMode.POSTER_ONLY,
      showPosterClock = getBoolean(SettingsStore.KEY_SHOW_POSTER_CLOCK, true),
      showWeather = getBoolean(SettingsStore.KEY_SHOW_WEATHER, false),
      weatherFahrenheit =
          if (containsKey(SettingsStore.KEY_WEATHER_FAHRENHEIT)) {
            getBoolean(SettingsStore.KEY_WEATHER_FAHRENHEIT, false)
          } else {
            java.util.Locale.getDefault().country == "US"
          },
      weatherLat = weatherLat,
      weatherLon = weatherLon,
      weatherPlace = weatherPlace,
      weekStart =
          enumValueOrNull<WeekStart>(getString(SettingsStore.KEY_WEEK_START, WeekStart.MONDAY.name))
              ?: WeekStart.MONDAY,
      powerMode =
          enumValueOrNull<PowerMode>(
              getString(SettingsStore.KEY_POWER_MODE, PowerMode.ALWAYS_ON.name),
          ) ?: PowerMode.ALWAYS_ON,
      idleSleepMinutes = getInt(SettingsStore.KEY_IDLE_SLEEP_MINUTES, PowerPolicy.DEFAULT_IDLE_SLEEP_MINUTES),
      sleepStartMinutes = getInt(SettingsStore.KEY_SLEEP_START_MINUTES, PowerPolicy.DEFAULT_SLEEP_START_MINUTES),
      sleepEndMinutes = getInt(SettingsStore.KEY_SLEEP_END_MINUTES, PowerPolicy.DEFAULT_SLEEP_END_MINUTES),
      hideHentai = getBoolean(SettingsStore.KEY_HIDE_HENTAI, true),
  )
}
