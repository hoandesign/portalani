package com.portal.portalani.data

import android.content.Context
import androidx.core.content.edit

class TokenStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  fun saveAccessToken(token: String) {
    prefs.edit {
      putString(KEY_TOKEN, token)
      putLong(KEY_SAVED_AT, System.currentTimeMillis())
    }
  }

  fun accessToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

  fun clear() {
    prefs.edit { clear() }
  }

  fun saveViewer(viewer: ViewerProfile) {
    prefs.edit {
      putInt(KEY_VIEWER_ID, viewer.id)
      putString(KEY_VIEWER_NAME, viewer.name)
    }
  }

  fun viewerName(): String? = prefs.getString(KEY_VIEWER_NAME, null)

  fun viewerId(): Int? = prefs.getInt(KEY_VIEWER_ID, 0).takeIf { it > 0 }

  fun saveOAuthState(state: String) {
    prefs.edit { putString(KEY_OAUTH_STATE, state) }
  }

  fun peekOAuthState(): String? = prefs.getString(KEY_OAUTH_STATE, null)?.takeIf { it.isNotBlank() }

  fun clearOAuthState() {
    prefs.edit { remove(KEY_OAUTH_STATE) }
  }

  companion object {
    private const val PREFS = "anilist_auth"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_SAVED_AT = "saved_at"
    private const val KEY_VIEWER_ID = "viewer_id"
    private const val KEY_VIEWER_NAME = "viewer_name"
    private const val KEY_OAUTH_STATE = "oauth_state"
  }
}

class SettingsStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  fun load(): AppSettings =
      parseAppSettings(
          containsKey = prefs::contains,
          getString = prefs::getString,
          getBoolean = prefs::getBoolean,
          getLong = prefs::getLong,
          getInt = prefs::getInt,
          getFloat = prefs::getFloat,
      )

  fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

  fun setOnboardingComplete() {
    prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, true) }
  }

  fun clearOnboardingComplete() {
    prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, false) }
  }

  fun save(settings: AppSettings) {
    prefs.edit {
      putBoolean(KEY_SHUFFLE, settings.shuffle)
      putLong(KEY_INTERVAL_MS, settings.intervalMs)
      putString(KEY_SOURCE_MODE, settings.sourceMode.name)
      putString(
          KEY_LIST_STATUSES,
          settings.listStatuses.sortedBy { it.ordinal }.joinToString(",") { it.name },
      )
      putString(KEY_FORMAT_FILTERS, FormatFilter.encodeSelection(settings.formatFilters))
      putString(KEY_COUNTRY_FILTERS, CountryFilter.encodeSelection(settings.countryFilters))
      putString(KEY_SOURCE_FILTERS, SourceFilter.encodeSelection(settings.sourceFilters))
      putString(KEY_DEMOGRAPHIC_FILTERS, DemographicFilter.encodeSelection(settings.demographicFilters))
      putString(KEY_LIBRARY_SORT, settings.librarySort.name)
      putString(KEY_SEASON_KEY, settings.seasonKey)
      putString(KEY_FRAME_MODE, settings.frameMode.name)
      putString(KEY_WEEK_START, settings.weekStart.name)
      putBoolean(KEY_SHOW_POSTER_CLOCK, settings.showPosterClock)
      putBoolean(KEY_SHOW_WEATHER, settings.showWeather)
      putBoolean(KEY_WEATHER_FAHRENHEIT, settings.weatherFahrenheit)
      if (settings.weatherPlace != null && settings.weatherLat != null && settings.weatherLon != null) {
        putFloat(KEY_WEATHER_LAT, settings.weatherLat.toFloat())
        putFloat(KEY_WEATHER_LON, settings.weatherLon.toFloat())
        putString(KEY_WEATHER_PLACE, settings.weatherPlace)
      } else {
        remove(KEY_WEATHER_LAT)
        remove(KEY_WEATHER_LON)
        remove(KEY_WEATHER_PLACE)
      }
      putString(KEY_POWER_MODE, settings.powerMode.name)
      putInt(KEY_IDLE_SLEEP_MINUTES, settings.idleSleepMinutes)
      putInt(KEY_SLEEP_START_MINUTES, settings.sleepStartMinutes)
      putInt(KEY_SLEEP_END_MINUTES, settings.sleepEndMinutes)
      putBoolean(KEY_HIDE_HENTAI, settings.hideHentai)
    }
  }

  companion object {
    internal const val PREFS = "portalani_settings"
    internal const val KEY_SHUFFLE = "shuffle"
    internal const val KEY_INTERVAL_MS = "interval_ms"
    internal const val KEY_USE_MY_LIST_LEGACY = "use_my_list"
    internal const val KEY_SOURCE_MODE = "source_mode"
    internal const val KEY_LIST_STATUS = "list_status"
    internal const val KEY_LIST_STATUSES = "list_statuses"
    internal const val KEY_FORMAT_FILTER = "format_filter"
    internal const val KEY_FORMAT_FILTERS = "format_filters"
    internal const val KEY_COUNTRY_FILTERS = "country_filters"
    internal const val KEY_SOURCE_FILTERS = "source_filters"
    internal const val KEY_DEMOGRAPHIC_FILTERS = "demographic_filters"
    internal const val KEY_LIBRARY_SORT = "library_sort"
    internal const val KEY_SEASON_KEY = "season_key"
    internal const val KEY_FRAME_MODE = "frame_mode"
    internal const val KEY_WEEK_START = "week_start"
    internal const val KEY_SHOW_POSTER_CLOCK = "show_poster_clock"
    internal const val KEY_SHOW_WEATHER = "show_weather"
    internal const val KEY_WEATHER_FAHRENHEIT = "weather_fahrenheit"
    internal const val KEY_WEATHER_LAT = "weather_lat"
    internal const val KEY_WEATHER_LON = "weather_lon"
    internal const val KEY_WEATHER_PLACE = "weather_place"
    internal const val KEY_POWER_MODE = "power_mode"
    internal const val KEY_IDLE_SLEEP_MINUTES = "idle_sleep_minutes"
    internal const val KEY_SLEEP_START_MINUTES = "sleep_start_minutes"
    internal const val KEY_SLEEP_END_MINUTES = "sleep_end_minutes"
    internal const val KEY_HIDE_HENTAI = "hide_hentai"
    internal const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
  }
}
