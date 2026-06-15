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

  fun load(): AppSettings {
    val sourceMode =
        if (prefs.contains(KEY_SOURCE_MODE)) {
          SourceMode.valueOf(prefs.getString(KEY_SOURCE_MODE, SourceMode.LIBRARY.name)!!)
        } else if (prefs.getBoolean(KEY_USE_MY_LIST_LEGACY, true)) {
          SourceMode.PERSONAL
        } else {
          SourceMode.LIBRARY
        }
    return AppSettings(
        shuffle = prefs.getBoolean(KEY_SHUFFLE, true),
        intervalMs = prefs.getLong(KEY_INTERVAL_MS, 12_000L),
        sourceMode = sourceMode,
        listStatus = ListStatus.valueOf(prefs.getString(KEY_LIST_STATUS, ListStatus.CURRENT.name)!!),
        formatFilter =
            FormatFilter.valueOf(prefs.getString(KEY_FORMAT_FILTER, FormatFilter.ALL.name)!!),
        librarySort =
            LibrarySort.valueOf(prefs.getString(KEY_LIBRARY_SORT, LibrarySort.POPULARITY.name)!!),
        seasonKey = prefs.getString(KEY_SEASON_KEY, SeasonSelection.ANY_KEY)!!,
    )
  }

  fun save(settings: AppSettings) {
    prefs.edit {
      putBoolean(KEY_SHUFFLE, settings.shuffle)
      putLong(KEY_INTERVAL_MS, settings.intervalMs)
      putString(KEY_SOURCE_MODE, settings.sourceMode.name)
      putString(KEY_LIST_STATUS, settings.listStatus.name)
      putString(KEY_FORMAT_FILTER, settings.formatFilter.name)
      putString(KEY_LIBRARY_SORT, settings.librarySort.name)
      putString(KEY_SEASON_KEY, settings.seasonKey)
    }
  }

  companion object {
    private const val PREFS = "portalani_settings"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_INTERVAL_MS = "interval_ms"
    private const val KEY_USE_MY_LIST_LEGACY = "use_my_list"
    private const val KEY_SOURCE_MODE = "source_mode"
    private const val KEY_LIST_STATUS = "list_status"
    private const val KEY_FORMAT_FILTER = "format_filter"
    private const val KEY_LIBRARY_SORT = "library_sort"
    private const val KEY_SEASON_KEY = "season_key"
  }
}
