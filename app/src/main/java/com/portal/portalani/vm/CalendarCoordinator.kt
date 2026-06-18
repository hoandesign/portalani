package com.portal.portalani.vm

import com.portal.portalani.CalendarWeekState
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CalendarWeek
import com.portal.portalani.data.CalendarWeekCache
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.toPlaceholderSlide
import com.portal.portalani.isAniListAuthFailure
import com.portal.portalani.userVisibleError
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

internal class CalendarCoordinator(
    private val scope: CoroutineScope,
    private val client: AniListClientPort,
    private val calendarWeekCache: CalendarWeekCache,
    private val tokens: TokenStore,
    private val getSettings: () -> AppSettings,
    private val isAuthConfigured: () -> Boolean,
    private val onRootState: (UiState) -> Unit,
    private val onSessionExpired: (String) -> Unit,
    private val onUserMessage: (String) -> Unit,
) {
  private val _calendarState = MutableStateFlow<CalendarWeekState?>(null)
  val calendarState: StateFlow<CalendarWeekState?> = _calendarState.asStateFlow()

  private val _calendarLoading = MutableStateFlow(false)
  val calendarLoading: StateFlow<Boolean> = _calendarLoading.asStateFlow()

  private val _calendarDetailSlide = MutableStateFlow<AnimeSlide?>(null)
  val calendarDetailSlide: StateFlow<AnimeSlide?> = _calendarDetailSlide.asStateFlow()

  private val _calendarDetailLoading = MutableStateFlow(false)
  val calendarDetailLoading: StateFlow<Boolean> = _calendarDetailLoading.asStateFlow()

  private var calendarWeekOffset = 0
  private val calendarMemoryCache =
      object : LinkedHashMap<String, CalendarWeekState>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CalendarWeekState>?): Boolean =
            size > 16
      }

  fun resetOffsetAndClearCache() {
    calendarWeekOffset = 0
    clearMemoryCache()
  }

  fun clearMemoryCache() {
    calendarMemoryCache.clear()
  }

  fun shiftWeek(delta: Int) {
    calendarWeekOffset += delta
    navigateToWeek()
  }

  fun goToToday() {
    if (calendarWeekOffset == 0) return
    calendarWeekOffset = 0
    navigateToWeek()
  }

  fun navigateToWeek() {
    val settings = getSettings()
    val weekStart = calendarWeekStart(settings)
    val cacheKey = settings.calendarCacheKey(weekStart.toEpochDay())
    val memCached = calendarMemoryCache[cacheKey]
    if (memCached != null) {
      _calendarState.value = memCached
      _calendarLoading.value = false
      scope.launch { loadWeek(weekStart = weekStart, allowStaleOverlay = false) }
      return
    }

    val diskCached = calendarWeekCache.load(cacheKey) ?: calendarWeekCache.loadStale(cacheKey)
    if (diskCached != null && diskCached.weekStart == weekStart) {
      val fromDisk =
          CalendarWeekState(
              weekStart = weekStart,
              entries = diskCached.entries,
              isCurrentWeek = calendarWeekOffset == 0,
              fromCache = true,
          )
      calendarMemoryCache[cacheKey] = fromDisk
      _calendarState.value = fromDisk
      _calendarLoading.value = false
      scope.launch { loadWeek(weekStart = weekStart, allowStaleOverlay = false) }
      return
    }

    _calendarState.value =
        CalendarWeekState(
            weekStart = weekStart,
            entries = emptyList(),
            isCurrentWeek = calendarWeekOffset == 0,
        )
    _calendarLoading.value = true
    scope.launch { loadWeek(weekStart = weekStart, allowStaleOverlay = true) }
  }

  fun openEntry(entry: CalendarAiringEntry) {
    _calendarDetailSlide.value = entry.toPlaceholderSlide()
    scope.launch {
      _calendarDetailLoading.value = true
      try {
        val slide =
            withContext(Dispatchers.IO) {
              client.fetchMediaById(entry.mediaId, tokens.accessToken())
            }
        if (slide != null && _calendarDetailSlide.value?.id == entry.mediaId) {
          _calendarDetailSlide.value = slide
        } else if (slide == null) {
          onUserMessage("Could not load anime details.")
        }
      } catch (e: IOException) {
        onUserMessage(userVisibleError(e, "Could not load anime details."))
      } catch (e: JSONException) {
        onUserMessage(userVisibleError(e, "Could not load anime details."))
      } finally {
        _calendarDetailLoading.value = false
      }
    }
  }

  fun closeDetail() {
    _calendarDetailSlide.value = null
    _calendarDetailLoading.value = false
  }

  suspend fun loadWeek(
      showLoading: Boolean = false,
      weekStart: LocalDate = calendarWeekStart(),
      allowStaleOverlay: Boolean = showLoading,
  ) {
    val settings = getSettings()
    val token = tokens.accessToken()

    if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
      _calendarState.value = null
      onRootState(
          UiState.NeedsSetup(
              message = "Sign in to show your list schedule on the calendar.",
              canSignIn = isAuthConfigured(),
              canUseLibrary = true,
          ),
      )
      return
    }

    val zone = ZoneId.systemDefault()
    val weekEnd = weekStart.plusDays(6)
    val airingAtGreater = weekStart.atStartOfDay(zone).toEpochSecond().toInt()
    val airingAtLesser = weekEnd.atTime(23, 59, 59).atZone(zone).toEpochSecond().toInt()

    val cacheKey = settings.calendarCacheKey(weekStart.toEpochDay())
    val hasVisibleData = calendarMemoryCache[cacheKey]?.entries?.isNotEmpty() == true
    val showLoadingOverlay = allowStaleOverlay && !hasVisibleData && calendarWeekStart(settings) == weekStart
    if (showLoadingOverlay) {
      _calendarLoading.value = true
    }

    try {
      val raw =
          withContext(Dispatchers.IO) {
            client.fetchAiringSchedules(
                airingAtGreater = airingAtGreater,
                airingAtLesser = airingAtLesser,
                accessToken = token,
            )
          }
      val filtered =
          raw
              .asSequence()
              .filter { CalendarWeek.matchesContentFilters(it, settings.libraryFilters()) }
              .filter { entry ->
                if (settings.sourceMode != SourceMode.PERSONAL) {
                  true
                } else {
                  entry.isOnList && entry.listStatus in settings.listStatuses
                }
              }
              .toList()
      val sorted = CalendarWeek.sortEntries(filtered, settings.librarySort)
      val loaded =
          CalendarWeekState(
              weekStart = weekStart,
              entries = sorted,
              isCurrentWeek = calendarWeekOffset == 0,
          )
      calendarMemoryCache[cacheKey] = loaded
      calendarWeekCache.save(cacheKey, weekStart, sorted)
      if (calendarWeekStart(settings) == weekStart) {
        _calendarState.value = loaded
      }
      onRootState(UiState.Showing(slides = emptyList(), fromCache = false))
      prefetchAdjacentWeeks(settings, weekStart)
    } catch (e: Throwable) {
      when (e) {
        is IOException, is JSONException -> {
          if (isAniListAuthFailure(e)) {
            _calendarState.value = null
            onSessionExpired(userVisibleError(e, "Your AniList sign-in expired. Sign in again."))
            return
          }
          if (_calendarState.value == null) {
            onRootState(
                UiState.Error(userVisibleError(e, "Could not load airing schedule"), canOpenSettings = true),
            )
          } else if (!hasVisibleData) {
            val stale = calendarWeekCache.loadStale(cacheKey)
            if (stale != null && stale.weekStart == weekStart) {
              val fallback =
                  CalendarWeekState(
                      weekStart = weekStart,
                      entries = stale.entries,
                      isCurrentWeek = calendarWeekOffset == 0,
                      fromCache = true,
                  )
              calendarMemoryCache[cacheKey] = fallback
              if (calendarWeekStart(settings) == weekStart) {
                _calendarState.value = fallback
              }
            }
          }
        }
        else -> throw e
      }
    } finally {
      if (calendarWeekStart(settings) == weekStart) {
        _calendarLoading.value = false
      }
    }
  }

  private fun prefetchAdjacentWeeks(settings: AppSettings, weekStart: LocalDate) {
    scope.launch {
      for (delta in listOf(-1L, 1L)) {
        val adjacent = weekStart.plusWeeks(delta)
        val key = settings.calendarCacheKey(adjacent.toEpochDay())
        if (calendarMemoryCache.containsKey(key)) continue
        val diskCached = calendarWeekCache.load(key) ?: calendarWeekCache.loadStale(key)
        if (diskCached != null && diskCached.weekStart == adjacent) {
          calendarMemoryCache[key] =
              CalendarWeekState(
                  weekStart = adjacent,
                  entries = diskCached.entries,
                  isCurrentWeek = calendarWeekOffset == 0 && adjacent == calendarWeekStart(settings),
                  fromCache = true,
              )
          continue
        }
        runCatching { fetchAndStoreWeek(settings, adjacent) }
      }
    }
  }

  private suspend fun fetchAndStoreWeek(settings: AppSettings, weekStart: LocalDate) {
    val token = tokens.accessToken()
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) return

    val zone = ZoneId.systemDefault()
    val weekEnd = weekStart.plusDays(6)
    val airingAtGreater = weekStart.atStartOfDay(zone).toEpochSecond().toInt()
    val airingAtLesser = weekEnd.atTime(23, 59, 59).atZone(zone).toEpochSecond().toInt()
    val cacheKey = settings.calendarCacheKey(weekStart.toEpochDay())

    val raw =
        withContext(Dispatchers.IO) {
          client.fetchAiringSchedules(
              airingAtGreater = airingAtGreater,
              airingAtLesser = airingAtLesser,
              accessToken = token,
          )
        }
    val filtered =
        raw
            .asSequence()
            .filter { CalendarWeek.matchesContentFilters(it, settings.libraryFilters()) }
            .filter { entry ->
              if (settings.sourceMode != SourceMode.PERSONAL) {
                true
              } else {
                entry.isOnList && entry.listStatus in settings.listStatuses
              }
            }
            .toList()
    val sorted = CalendarWeek.sortEntries(filtered, settings.librarySort)
    val loaded =
        CalendarWeekState(
            weekStart = weekStart,
            entries = sorted,
            isCurrentWeek = calendarWeekStart(settings) == weekStart,
        )
    calendarMemoryCache[cacheKey] = loaded
    calendarWeekCache.save(cacheKey, weekStart, sorted)
  }

  private fun calendarWeekStart(settings: AppSettings = getSettings()): LocalDate {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val anchor = CalendarWeek.startOfWeek(today, settings.weekStart)
    return anchor.plusWeeks(calendarWeekOffset.toLong())
  }
}
