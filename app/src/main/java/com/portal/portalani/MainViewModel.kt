package com.portal.portalani

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.portal.portalani.data.AniListAuthPort
import com.portal.portalani.data.AniListClient
import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CalendarWeekCache
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.SourceFilter
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerMode
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.GeoPlace
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.WeatherClient
import com.portal.portalani.data.WeatherNow
import com.portal.portalani.data.WeekStart
import com.portal.portalani.vm.CalendarCoordinator
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import okhttp3.OkHttpClient
import org.json.JSONObject

sealed interface UiState {
  data object Loading : UiState

  data class NeedsSetup(
      val message: String,
      val canSignIn: Boolean,
      val canUseLibrary: Boolean,
  ) : UiState

  data object SigningIn : UiState

  data class Showing(
      val slides: List<AnimeSlide>,
      val fromCache: Boolean = false,
      /** Bumps on full feed reload; slideshow order resets when this changes. */
      val orderResetToken: Int = 0,
  ) : UiState

  data class Error(
      val message: String,
      val canOpenSettings: Boolean = false,
  ) : UiState
}

class MainViewModel internal constructor(
    app: Application,
    deps: MainViewModelDeps,
    runBootstrap: Boolean,
) : AndroidViewModel(app) {
  private val tokens = deps.tokens
  private val settingsStore = deps.settingsStore
  private val slideCache = deps.slideCache
  private val calendarWeekCache = deps.calendarWeekCache
  private val auth: AniListAuthPort = deps.auth
  private val client: AniListClientPort = deps.client
  private val weatherClient = deps.weatherClient
  private val http = deps.http

  private val _state = MutableStateFlow<UiState>(UiState.Loading)
  val state: StateFlow<UiState> = _state.asStateFlow()

  private val _settings = MutableStateFlow(settingsStore.load())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  private val _viewerName = MutableStateFlow(tokens.viewerName())
  val viewerName: StateFlow<String?> = _viewerName.asStateFlow()

  private val _isSignedIn = MutableStateFlow(!tokens.accessToken().isNullOrBlank())
  val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  private val calendar =
      CalendarCoordinator(
          scope = viewModelScope,
          client = client,
          calendarWeekCache = calendarWeekCache,
          tokens = tokens,
          getSettings = { _settings.value },
          isAuthConfigured = { auth.isConfigured() },
          onRootState = { _state.value = it },
          onSessionExpired = { message ->
            clearSessionFromStorage()
            _state.value = needsSignIn(message)
          },
          onUserMessage = { _userMessage.value = it },
      )

  private val _onboardingComplete = MutableStateFlow(settingsStore.isOnboardingComplete())
  val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

  private val _weather = MutableStateFlow<WeatherNow?>(null)
  val weather: StateFlow<WeatherNow?> = _weather.asStateFlow()

  private val _geoStatus = MutableStateFlow<String?>(null)
  val geoStatus: StateFlow<String?> = _geoStatus.asStateFlow()

  private val _geoResults = MutableStateFlow<List<GeoPlace>>(emptyList())
  val geoResults: StateFlow<List<GeoPlace>> = _geoResults.asStateFlow()

  val calendarState = calendar.calendarState
  val calendarLoading = calendar.calendarLoading
  val calendarDetailSlide = calendar.calendarDetailSlide
  val calendarDetailLoading = calendar.calendarDetailLoading

  private var pendingOAuthState: String? = null
  private var stateBeforeSignIn: UiState? = null

  private var feedNextPage = 1
  private var feedHasMore = false
  private var feedLoadingMore = false
  private var feedListPages: Map<ListStatus, Pair<Int, Boolean>> = emptyMap()
  private var orderResetToken = 0
  private var activeFeedKey: String? = null

  init {
    if (runBootstrap) {
      val application = getApplication<Application>()
      ScreensaverGuardWorker.schedule(application)
      ScreensaverGuard.applyNow(application)
      refresh()
      weatherLoop()
    }
  }

  constructor(app: Application) : this(app, MainViewModelDeps.live(app), runBootstrap = true)

  fun refresh(forceReload: Boolean = false) {
    viewModelScope.launch {
      if (_settings.value.frameMode == FrameMode.CALENDAR) {
        if (forceReload) {
          calendar.resetOffsetAndClearCache()
        }
        if (calendar.calendarState.value == null || forceReload) {
          calendar.navigateToWeek()
        } else {
          calendar.loadWeek(showLoading = forceReload)
        }
      } else {
        loadSlides(showLoading = forceReload || _state.value !is UiState.Showing)
      }
    }
  }

  fun shiftCalendarWeek(delta: Int) {
    calendar.shiftWeek(delta)
  }

  fun goToCalendarToday() {
    calendar.goToToday()
  }

  fun openCalendarEntry(entry: CalendarAiringEntry) {
    calendar.openEntry(entry)
  }

  fun closeCalendarDetail() {
    calendar.closeDetail()
  }

  fun setWeekStart(weekStart: WeekStart) {
    updateSettings(_settings.value.copy(weekStart = weekStart))
    if (_settings.value.frameMode == FrameMode.CALENDAR) {
      calendar.resetOffsetAndClearCache()
      calendar.navigateToWeek()
    }
  }

  fun signIn() {
    if (!auth.isConfigured()) {
      _state.value =
          UiState.NeedsSetup(
              message = missingCredentialsMessage(),
              canSignIn = false,
              canUseLibrary = true,
          )
      return
    }
    val state = UUID.randomUUID().toString()
    pendingOAuthState = state
    tokens.saveOAuthState(state)
    stateBeforeSignIn = _state.value.takeUnless { it is UiState.SigningIn }
    _state.value = UiState.SigningIn
    val authUrl = auth.buildAuthorizeUrl(state).toString()
    val ctx = getApplication<Application>()
    val intent =
        Intent(ctx, AniListOAuthActivity::class.java)
            .putExtra(AniListOAuthActivity.EXTRA_AUTH_URL, authUrl)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(intent)
  }

  fun cancelSignIn() {
    if (_state.value !is UiState.SigningIn) return
    pendingOAuthState = null
    tokens.clearOAuthState()
    val restore = stateBeforeSignIn?.takeUnless { it is UiState.SigningIn }
    stateBeforeSignIn = null
    if (restore != null) {
      _state.value = restore
      return
    }
    val token = tokens.accessToken()
    val settings = _settings.value
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
      _state.value =
          needsSignIn(
              if (settings.frameMode == FrameMode.CALENDAR) {
                "Sign in to show your list schedule on the calendar."
              } else {
                "Sign in to show anime from your personal AniList."
              },
          )
    } else {
      viewModelScope.launch { loadSlides(showLoading = false) }
    }
  }

  fun handleOAuthCallback(uri: android.net.Uri) {
    val callback = auth.parseCallback(uri) ?: return
    if (!callback.error.isNullOrBlank()) {
      stateBeforeSignIn = null
      _state.value = UiState.Error("AniList sign-in failed: ${callback.error}")
      pendingOAuthState = null
      tokens.clearOAuthState()
      return
    }
    val expected = pendingOAuthState ?: tokens.peekOAuthState()
    pendingOAuthState = null
    tokens.clearOAuthState()
    if (expected == null || callback.state != expected) {
      stateBeforeSignIn = null
      _state.value = UiState.Error("Sign-in state mismatch. Please try again.")
      return
    }
    val code = callback.code
    if (code.isNullOrBlank()) {
      _state.value = UiState.Error("No authorization code received.")
      return
    }
    viewModelScope.launch {
      _state.value = UiState.Loading
      stateBeforeSignIn = null
      try {
        withContext(Dispatchers.IO) {
          val token = auth.exchangeCode(code)
          val viewer = client.fetchViewer(token)
          tokens.saveViewer(viewer)
        }
        _viewerName.value = tokens.viewerName()
        _isSignedIn.value = true
        updateSettings(_settings.value.copy(sourceMode = SourceMode.PERSONAL))
        bringAppToFront()
        loadSlides(showLoading = true)
      } catch (e: IOException) {
        _state.value = UiState.Error(userVisibleError(e, "Sign-in failed"))
      } catch (e: JSONException) {
        _state.value = UiState.Error(userVisibleError(e, "Sign-in failed"))
      }
    }
  }

  fun signOut() {
    clearSessionFromStorage()
    if (_settings.value.sourceMode == SourceMode.PERSONAL) {
      updateSettings(_settings.value.copy(sourceMode = SourceMode.LIBRARY))
    }
    refresh(forceReload = true)
  }

  private fun clearSessionFromStorage() {
    tokens.clear()
    _viewerName.value = null
    _isSignedIn.value = false
  }

  private fun needsSignIn(message: String) =
      UiState.NeedsSetup(
          message = message,
          canSignIn = auth.isConfigured(),
          canUseLibrary = true,
      )

  fun useLibrary() {
    updateSettings(_settings.value.copy(sourceMode = SourceMode.LIBRARY))
    refresh(forceReload = true)
  }

  fun setSourceMode(mode: SourceMode) {
    val isCalendarMode = _settings.value.frameMode == FrameMode.CALENDAR
    updateSettings(_settings.value.copy(sourceMode = mode))
    if (isCalendarMode) {
      calendar.resetOffsetAndClearCache()
      calendar.navigateToWeek()
    } else {
      refresh(forceReload = true)
    }
  }

  fun setShuffle(enabled: Boolean) {
    updateSettings(_settings.value.copy(shuffle = enabled))
  }

  fun completeOnboarding() {
    if (_onboardingComplete.value) return
    settingsStore.setOnboardingComplete()
    _onboardingComplete.value = true
  }

  fun resetOnboarding() {
    settingsStore.clearOnboardingComplete()
    _onboardingComplete.value = false
  }

  fun setFrameMode(mode: FrameMode) {
    val leavingCalendar = _settings.value.frameMode == FrameMode.CALENDAR && mode != FrameMode.CALENDAR
    updateSettings(_settings.value.copy(frameMode = mode))
    if (mode == FrameMode.CALENDAR) {
      calendar.resetOffsetAndClearCache()
      calendar.navigateToWeek()
    } else if (leavingCalendar) {
      refresh(forceReload = true)
    }
  }

  fun setShowPosterClock(enabled: Boolean) {
    updateSettings(
        _settings.value.copy(
            showPosterClock = enabled,
            showWeather = if (enabled) _settings.value.showWeather else false,
        ),
    )
    if (!enabled) {
      _geoResults.value = emptyList()
      _geoStatus.value = null
    }
  }

  fun setShowWeather(enabled: Boolean) {
    if (enabled && !_settings.value.showPosterClock) return
    updateSettings(_settings.value.copy(showWeather = enabled))
    if (enabled) {
      refreshWeather()
    } else {
      clearGeoSearch()
    }
  }

  fun clearGeoSearch() {
    _geoResults.value = emptyList()
    _geoStatus.value = null
  }

  fun setWeatherFahrenheit(fahrenheit: Boolean) {
    updateSettings(_settings.value.copy(weatherFahrenheit = fahrenheit))
    refreshWeather()
  }

  fun searchLocation(query: String) {
    if (query.isBlank()) return
    viewModelScope.launch {
      _geoStatus.value = "Searching…"
      _geoResults.value = emptyList()
      val results =
          withContext(Dispatchers.IO) {
            runCatching { weatherClient.geocode(query) }.getOrDefault(emptyList())
          }
      when {
        results.isEmpty() -> _geoStatus.value = "Couldn't find that place"
        results.size == 1 -> {
          _geoStatus.value = null
          chooseLocation(results.first())
        }
        else -> {
          _geoStatus.value = null
          _geoResults.value = results
        }
      }
    }
  }

  fun chooseLocation(place: GeoPlace) {
    updateSettings(
        _settings.value.copy(
            weatherLat = place.lat,
            weatherLon = place.lon,
            weatherPlace = place.label,
        ),
    )
    _geoResults.value = emptyList()
    _geoStatus.value = null
    refreshWeather()
  }

  fun hasLocationPermission(): Boolean {
    val ctx = getApplication<Application>()
    return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
  }

  @Suppress("MissingPermission")
  fun detectLocation() {
    if (!hasLocationPermission()) {
      _geoStatus.value = "Location permission not granted"
      return
    }
    val ctx = getApplication<Application>()
    val lm =
        ctx.getSystemService(LocationManager::class.java)
            ?: run {
              _geoStatus.value = "Location services unavailable"
              return
            }
    _geoStatus.value = "Getting location…"
    _geoResults.value = emptyList()

    val last: Location? =
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    if (last != null) {
      applyDetectedLocation(last.latitude, last.longitude)
      return
    }

    val provider =
        when {
          lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
          lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
          else -> {
            _geoStatus.value = "No location provider available"
            return
          }
        }
    val listener =
        object : LocationListener {
          override fun onLocationChanged(loc: Location) {
            lm.removeUpdates(this)
            applyDetectedLocation(loc.latitude, loc.longitude)
          }

          override fun onProviderDisabled(provider: String) {
            lm.removeUpdates(this)
            _geoStatus.value = "Location provider disabled"
          }

          @Deprecated("Required on API <29", ReplaceWith(""))
          override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    @Suppress("DEPRECATION")
    lm.requestSingleUpdate(provider, listener, null)
  }

  private fun applyDetectedLocation(lat: Double, lon: Double) {
    viewModelScope.launch {
      val label =
          withContext(Dispatchers.IO) {
            runCatching {
                  val url =
                      "https://nominatim.openstreetmap.org/reverse" +
                          "?lat=$lat&lon=$lon&format=json&zoom=10"
                  val req =
                      okhttp3.Request.Builder()
                          .url(url)
                          .header("User-Agent", "PortalAni/1.0")
                          .build()
                  http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val addr =
                        JSONObject(resp.body?.string().orEmpty()).optJSONObject("address")
                            ?: return@runCatching null
                    listOfNotNull(
                            addr.optString("city").ifBlank { null }
                                ?: addr.optString("town").ifBlank { null }
                                ?: addr.optString("village").ifBlank { null },
                            addr.optString("state").ifBlank { null },
                            addr.optString("country_code").uppercase().ifBlank { null },
                        )
                        .joinToString(", ")
                        .ifBlank { null }
                  }
                }
                .getOrNull()
          }
      chooseLocation(GeoPlace(lat, lon, label ?: "%.2f, %.2f".format(lat, lon)))
      _geoStatus.value = null
    }
  }

  private fun refreshWeather() {
    val s = _settings.value
    val lat = s.weatherLat
    val lon = s.weatherLon
    if (!s.showWeather || lat == null || lon == null) return
    viewModelScope.launch {
      val w =
          withContext(Dispatchers.IO) {
            runCatching { weatherClient.current(lat, lon, s.weatherFahrenheit) }.getOrNull()
          }
      if (w != null) _weather.value = w
    }
  }

  private fun weatherLoop() {
    viewModelScope.launch {
      while (true) {
        refreshWeather()
        delay(30 * 60 * 1000L)
      }
    }
  }

  fun setIntervalSeconds(seconds: Int) {
    updateSettings(_settings.value.copy(intervalMs = seconds.coerceIn(5, 120) * 1000L))
  }

  fun setListStatuses(statuses: Set<ListStatus>) {
    if (statuses.isEmpty()) return
    val isCalendarMode = _settings.value.frameMode == FrameMode.CALENDAR
    updateSettings(_settings.value.copy(listStatuses = statuses))
    if (isCalendarMode) {
      calendar.clearMemoryCache()
      calendar.navigateToWeek()
    } else {
      refresh(forceReload = true)
    }
  }

  fun setFormatFilters(formats: Set<FormatFilter>) {
    if (formats.isEmpty()) return
    applyContentFilterChange { it.copy(formatFilters = FormatFilter.normalizeSelection(formats)) }
  }

  fun setCountryFilters(countries: Set<CountryFilter>) {
    if (countries.isEmpty()) return
    applyContentFilterChange { it.copy(countryFilters = CountryFilter.normalizeSelection(countries)) }
  }

  fun setSourceFilters(sources: Set<SourceFilter>) {
    if (sources.isEmpty()) return
    applyContentFilterChange { it.copy(sourceFilters = SourceFilter.normalizeSelection(sources)) }
  }

  fun setDemographicFilters(demographics: Set<DemographicFilter>) {
    if (demographics.isEmpty()) return
    applyContentFilterChange {
      it.copy(demographicFilters = DemographicFilter.normalizeSelection(demographics))
    }
  }

  private fun applyContentFilterChange(transform: (AppSettings) -> AppSettings) {
    val isCalendarMode = _settings.value.frameMode == FrameMode.CALENDAR
    updateSettings(transform(_settings.value))
    if (isCalendarMode) {
      calendar.clearMemoryCache()
      calendar.navigateToWeek()
    } else {
      refresh(forceReload = true)
    }
  }

  fun setHideHentai(enabled: Boolean) {
    val isCalendarMode = _settings.value.frameMode == FrameMode.CALENDAR
    updateSettings(_settings.value.copy(hideHentai = enabled))
    if (isCalendarMode) {
      calendar.clearMemoryCache()
      calendar.navigateToWeek()
    } else {
      refresh(forceReload = true)
    }
  }

  fun setLibrarySort(sort: LibrarySort) {
    val isCalendarMode = _settings.value.frameMode == FrameMode.CALENDAR
    updateSettings(_settings.value.copy(librarySort = sort))
    if (isCalendarMode) {
      calendar.clearMemoryCache()
      calendar.navigateToWeek()
    } else {
      refresh(forceReload = true)
    }
  }

  fun setSeasonKey(key: String) {
    updateSettings(_settings.value.copy(seasonKey = key))
    refresh(forceReload = true)
  }

  fun setPowerMode(mode: PowerMode) {
    updateSettings(_settings.value.copy(powerMode = mode))
    ScreensaverGuard.applyPowerPolicy(getApplication(), _settings.value)
  }

  fun setIdleSleepMinutes(minutes: Int) {
    val allowed = PowerPolicy.IDLE_SLEEP_OPTIONS_MINUTES
    val next = allowed.minByOrNull { kotlin.math.abs(it - minutes) } ?: PowerPolicy.DEFAULT_IDLE_SLEEP_MINUTES
    updateSettings(_settings.value.copy(idleSleepMinutes = next))
  }

  fun setSleepStartMinutes(minutes: Int) {
    updateSettings(_settings.value.copy(sleepStartMinutes = minutes.coerceIn(0, 24 * 60 - 1)))
    ScreensaverGuard.applyPowerPolicy(getApplication(), _settings.value)
  }

  fun setSleepEndMinutes(minutes: Int) {
    updateSettings(_settings.value.copy(sleepEndMinutes = minutes.coerceIn(0, 24 * 60 - 1)))
    ScreensaverGuard.applyPowerPolicy(getApplication(), _settings.value)
  }

  fun onSlideIndexChanged(index: Int) {
    val showing = _state.value as? UiState.Showing ?: return
    if (!feedHasMore || feedLoadingMore) return
    if (showing.slides.isEmpty()) return
    if (index < showing.slides.lastIndex - LOAD_MORE_THRESHOLD) return
    loadMoreSlides()
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun setUserScore(mediaId: Int, score: Float?) {
    performUserAction(mediaId, "Could not save score") { token, slide ->
      val update =
          client.saveMediaListEntry(
              accessToken = token,
              mediaId = mediaId,
              listEntryId = slide.listEntryId,
              status = slide.listStatus ?: ListStatus.PLANNING,
              score = score,
          )
      slide.withUserState(
          listEntryId = update.listEntryId,
          listStatus = update.listStatus ?: slide.listStatus ?: ListStatus.PLANNING,
          userScore = update.userScore,
      )
    }
  }

  fun toggleFavourite(mediaId: Int) {
    performUserAction(mediaId, "Could not update favourite") { token, slide ->
      val isFavourite = client.toggleFavourite(token, mediaId)
      slide.withUserState(isFavourite = isFavourite)
    }
  }

  fun setAnimeListStatus(mediaId: Int, status: ListStatus) {
    performUserAction(mediaId, "Could not update list") { token, slide ->
      val update =
          client.saveMediaListEntry(
              accessToken = token,
              mediaId = mediaId,
              listEntryId = slide.listEntryId,
              status = status,
              score = slide.userScore,
          )
      slide.withUserState(
          listEntryId = update.listEntryId,
          listStatus = update.listStatus ?: status,
          userScore = update.userScore ?: slide.userScore,
      )
    }
  }

  fun removeFromList(mediaId: Int) {
    val slide = (_state.value as? UiState.Showing)?.slides?.firstOrNull { it.id == mediaId }
    if (slide?.listEntryId == null) {
      _userMessage.value = "This anime is not on your list."
      return
    }
    performUserAction(mediaId, "Could not remove from list") { token, current ->
      client.deleteMediaListEntry(token, current.listEntryId!!)
      current.withUserState(listEntryId = null, listStatus = null)
    }
  }

  private fun performUserAction(
      mediaId: Int,
      failureMessage: String,
      transform: suspend (String, AnimeSlide) -> AnimeSlide,
  ) {
    val token = tokens.accessToken()
    if (token.isNullOrBlank()) {
      _userMessage.value = "Sign in to manage your AniList."
      return
    }
    val current = _state.value as? UiState.Showing ?: return
    val slide = current.slides.firstOrNull { it.id == mediaId } ?: return

    viewModelScope.launch {
      try {
        val updated = withContext(Dispatchers.IO) { transform(token, slide) }
        updateSlideInState(mediaId, updated)
        persistCurrentSlides()
      } catch (e: IOException) {
        _userMessage.value = userVisibleError(e, failureMessage)
      } catch (e: JSONException) {
        _userMessage.value = userVisibleError(e, failureMessage)
      }
    }
  }

  private fun updateSlideInState(mediaId: Int, updated: AnimeSlide) {
    val current = _state.value as? UiState.Showing ?: return
    _state.value = current.copy(slides = current.slides.map { if (it.id == mediaId) updated else it })
  }

  private suspend fun persistCurrentSlides() {
    val current = _state.value as? UiState.Showing ?: return
    withContext(Dispatchers.IO) {
      slideCache.save(_settings.value.cacheKey(), current.slides)
    }
  }

  private fun filterSlidesForSettings(slides: List<AnimeSlide>, settings: AppSettings): List<AnimeSlide> {
    val filters = settings.libraryFilters()
    val applySeasonFilter = settings.sourceMode == SourceMode.LIBRARY
    return slides.filter { filters.matchesSlide(it, applySeasonFilter = applySeasonFilter) }
  }

  private fun updateSettings(next: AppSettings) {
    settingsStore.save(next)
    _settings.value = next
  }

  private suspend fun loadSlides(showLoading: Boolean) {
    if (_settings.value.frameMode == FrameMode.CALENDAR) {
      calendar.loadWeek(showLoading)
      return
    }
    val settings = _settings.value
    val token = tokens.accessToken()
    val cacheKey = settings.cacheKey()
    val previousFeedKey = activeFeedKey
    val feedKeyChanged = previousFeedKey != null && previousFeedKey != cacheKey

    resetFeedPagination(cacheKey)

    val cachedFresh =
        withContext(Dispatchers.IO) { slideCache.load(cacheKey) }?.let { filterSlidesForSettings(it, settings) }
    val cachedStale =
        cachedFresh
            ?: withContext(Dispatchers.IO) { slideCache.loadStale(cacheKey) }
                ?.let { filterSlidesForSettings(it, settings) }

    when {
      feedKeyChanged && cachedFresh == null -> _state.value = UiState.Loading
      showLoading && cachedFresh == null -> _state.value = UiState.Loading
      cachedFresh != null -> _state.value = showingState(cachedFresh, fromCache = true)
    }

    try {
      val batch =
          withContext(Dispatchers.IO) {
            when (settings.sourceMode) {
              SourceMode.PERSONAL -> {
                if (token == null) return@withContext null
                val userId =
                    tokens.viewerId()
                        ?: client.fetchViewer(token).also { tokens.saveViewer(it) }.id
                fetchPersonalSlides(
                    accessToken = token,
                    userId = userId,
                    statuses = settings.listStatuses,
                    initial = true,
                )
              }
              SourceMode.LIBRARY ->
                  client.fetchLibraryPages(
                      filters = settings.libraryFilters(),
                      startPage = 1,
                      pageCount = AniListClient.DEFAULT_INITIAL_PAGES,
                      accessToken = token,
                  )
            }
          }
      val rawSlides = batch?.slides.orEmpty()
      val slides = filterSlidesForSettings(rawSlides, settings)
      if (slides.isEmpty()) {
        if (cachedStale != null) {
          _state.value = showingState(cachedStale, fromCache = true)
          return
        }
        if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
          _state.value =
              UiState.NeedsSetup(
                  message = "Sign in to show anime from your personal AniList.",
                  canSignIn = auth.isConfigured(),
                  canUseLibrary = true,
              )
        } else {
          val message =
              if (settings.sourceMode == SourceMode.PERSONAL) {
                personalListEmptyMessage(settings.listStatuses, filtersExcludedAll = rawSlides.isNotEmpty())
              } else {
                "No anime found for these filters. Try broader filters in Settings."
              }
          _state.value = UiState.Error(message = message, canOpenSettings = true)
        }
        return
      }
      if (batch != null) {
        feedNextPage = batch.nextPage
        feedHasMore = batch.hasMore
      }
      withContext(Dispatchers.IO) { slideCache.save(cacheKey, slides) }
      _state.value = showingState(slides, fromCache = false)
    } catch (e: Throwable) {
      when (e) {
        is IOException, is JSONException -> {
          if (cachedStale != null) {
            _state.value = showingState(cachedStale, fromCache = true)
            return
          }
          if (isAniListAuthFailure(e)) {
            clearSessionFromStorage()
            _state.value =
                needsSignIn(userVisibleError(e, "Your AniList sign-in expired. Sign in again."))
            return
          }
          if (settings.sourceMode == SourceMode.PERSONAL && token != null) {
            _state.value =
                UiState.Error(
                    userVisibleError(
                        e,
                        "Could not load your list. Check your connection and try again.",
                    ),
                    canOpenSettings = true,
                )
          } else {
            _state.value = UiState.Error(userVisibleError(e, "Could not load anime"))
          }
        }
        else -> throw e
      }
    }
  }

  private fun loadMoreSlides() {
    if (feedLoadingMore || !feedHasMore) return
    val settings = _settings.value
    val token = tokens.accessToken()
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) return

    feedLoadingMore = true
    viewModelScope.launch {
      try {
        val batch =
            withContext(Dispatchers.IO) {
              when (settings.sourceMode) {
                SourceMode.PERSONAL -> {
                  val accessToken = token ?: return@withContext FetchBatchResult(emptyList(), feedNextPage, false)
                  val userId =
                      tokens.viewerId()
                          ?: client.fetchViewer(accessToken).also { tokens.saveViewer(it) }.id
                  fetchPersonalSlides(
                      accessToken = accessToken,
                      userId = userId,
                      statuses = settings.listStatuses,
                      initial = false,
                  )
                }
                SourceMode.LIBRARY ->
                    client.fetchLibraryPages(
                        filters = settings.libraryFilters(),
                        startPage = feedNextPage,
                        pageCount = AniListClient.DEFAULT_LOAD_MORE_PAGES,
                        accessToken = token,
                    )
              }
            }
        feedNextPage = batch.nextPage
        feedHasMore = batch.hasMore
        val filtered = filterSlidesForSettings(batch.slides, settings)
        if (filtered.isNotEmpty()) {
          appendSlides(filtered)
          persistCurrentSlides()
        }
      } catch (e: Throwable) {
        when (e) {
          is IOException, is JSONException -> Unit // Keep current feed; try again on a later slide.
          else -> throw e
        }
      } finally {
        feedLoadingMore = false
      }
    }
  }

  private fun resetFeedPagination(cacheKey: String) {
    if (activeFeedKey != cacheKey) {
      activeFeedKey = cacheKey
      orderResetToken++
      feedListPages = emptyMap()
    }
    feedNextPage = 1
    feedHasMore = false
    feedLoadingMore = false
  }

  private suspend fun fetchPersonalSlides(
      accessToken: String,
      userId: Int,
      statuses: Set<ListStatus>,
      initial: Boolean,
  ): FetchBatchResult {
    if (statuses.isEmpty()) return FetchBatchResult(emptyList(), 1, false)

    val merged = mutableListOf<AnimeSlide>()
    val updatedPages = feedListPages.toMutableMap()
    var anyHasMore = false

    for (status in statuses.sortedBy { it.ordinal }) {
      if (!initial) {
        val hasMore = updatedPages[status]?.second == true
        if (!hasMore) continue
      }

      val startPage = if (initial) 1 else updatedPages[status]?.first ?: 1
      val pageCount =
          if (initial) {
            AniListClient.DEFAULT_INITIAL_PAGES
          } else {
            AniListClient.DEFAULT_LOAD_MORE_PAGES
          }
      val batch =
          client.fetchViewerListPages(
              accessToken = accessToken,
              userId = userId,
              status = status,
              startPage = startPage,
              pageCount = pageCount,
          )
      merged += batch.slides
      updatedPages[status] = batch.nextPage to batch.hasMore
      if (batch.hasMore) anyHasMore = true
    }

    feedListPages = updatedPages
    feedHasMore = anyHasMore
    return FetchBatchResult(merged, 1, anyHasMore)
  }

  private fun showingState(slides: List<AnimeSlide>, fromCache: Boolean): UiState.Showing =
      UiState.Showing(
          slides = slides,
          fromCache = fromCache,
          orderResetToken = orderResetToken,
      )

  private fun appendSlides(batch: List<AnimeSlide>) {
    val current = _state.value as? UiState.Showing ?: return
    val existingIds = current.slides.asSequence().map { it.id }.toSet()
    val merged = current.slides + batch.filter { it.id !in existingIds }
    if (merged.size == current.slides.size) return
    _state.value = current.copy(slides = merged)
  }

  private companion object {
    const val LOAD_MORE_THRESHOLD = 8
  }

  private fun bringAppToFront() {
    val ctx = getApplication<Application>()
    val intent =
        Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    ctx.startActivity(intent)
  }

  private fun personalListEmptyMessage(statuses: Set<ListStatus>, filtersExcludedAll: Boolean = false): String {
    if (filtersExcludedAll) {
      return "Nothing on your lists matches the current filters. Try broader filters in Settings, or switch to Full library."
    }
    if (statuses.isEmpty()) {
      return "Choose at least one list in Settings."
    }
    if (statuses.size > 1) {
      return "Your selected lists are empty on AniList. Try other lists in Settings, or add anime on anilist.co."
    }
    val status = statuses.first()
    val label =
        when (status) {
          ListStatus.CURRENT -> "Currently watching"
          ListStatus.PLANNING -> "Planning"
          ListStatus.COMPLETED -> "Completed"
          ListStatus.PAUSED -> "Paused"
          ListStatus.DROPPED -> "Dropped"
          ListStatus.REPEATING -> "Rewatching"
        }
    return "Your \"$label\" list is empty on AniList. Try another list status in Settings, or add anime on anilist.co."
  }

  private fun missingCredentialsMessage(): String =
      "Add your AniList OAuth client to local.properties, then rebuild:\n\n" +
          "ANILIST_CLIENT_ID=...\n" +
          "ANILIST_CLIENT_SECRET=...\n\n" +
          "Redirect URI must be portalani://callback"
}
