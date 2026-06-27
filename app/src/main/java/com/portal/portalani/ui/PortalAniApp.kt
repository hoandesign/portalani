package com.portal.portalani.ui

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.portal.portalani.R
import com.portal.portalani.CalendarWeekState
import com.portal.portalani.RelatedAnimeOverlayState
import com.portal.portalani.RelatedMediaDetailState
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.RelatedAnime
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.GeoPlace
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerMode
import com.portal.portalani.data.SourceFilter
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.WeatherNow
import com.portal.portalani.data.WeekStart

@Composable
fun PortalAniApp(
    state: UiState,
    settings: AppSettings,
    weather: WeatherNow? = null,
    calendarState: CalendarWeekState? = null,
    calendarLoading: Boolean = false,
    calendarDetailSlide: AnimeSlide? = null,
    calendarDetailLoading: Boolean = false,
    geoStatus: String? = null,
    geoResults: List<GeoPlace> = emptyList(),
    viewerName: String?,
    isSignedIn: Boolean,
    userMessage: String?,
    onSignIn: () -> Unit,
    onCancelSignIn: () -> Unit = {},
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    onUseLibrary: () -> Unit,
    onClearUserMessage: () -> Unit,
    onSetUserScore: (Int, Float?) -> Unit,
    onToggleFavourite: (Int) -> Unit,
    onSetAnimeListStatus: (Int, ListStatus) -> Unit,
    onRemoveFromList: (Int) -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    onSetFrameMode: (FrameMode) -> Unit,
    onShiftCalendarWeek: (Int) -> Unit = {},
    onGoToCalendarToday: () -> Unit = {},
    onOpenCalendarEntry: (CalendarAiringEntry) -> Unit = {},
    onCloseCalendarDetail: () -> Unit = {},
    onSetWeekStart: (WeekStart) -> Unit = {},
    onSetShowPosterClock: (Boolean) -> Unit,
    onSetShowWeather: (Boolean) -> Unit = {},
    onSetWeatherFahrenheit: (Boolean) -> Unit = {},
    onSearchLocation: (String) -> Unit = {},
    onChooseLocation: (GeoPlace) -> Unit = {},
    onDetectLocation: () -> Unit = {},
    onClearGeoSearch: () -> Unit = {},
    onSetIntervalSeconds: (Int) -> Unit,
    onSetSourceMode: (SourceMode) -> Unit,
    onSetListStatuses: (Set<ListStatus>) -> Unit,
    onSetFormatFilters: (Set<FormatFilter>) -> Unit,
    onSetCountryFilters: (Set<CountryFilter>) -> Unit,
    onSetSourceFilters: (Set<SourceFilter>) -> Unit,
    onSetDemographicFilters: (Set<DemographicFilter>) -> Unit,
    onSetHideHentai: (Boolean) -> Unit,
    onSetLibrarySort: (LibrarySort) -> Unit,
    onSetSeasonKey: (String) -> Unit,
    onSetPowerMode: (PowerMode) -> Unit = {},
    onSetIdleSleepMinutes: (Int) -> Unit = {},
    onSetSleepStartMinutes: (Int) -> Unit = {},
    onSetSleepEndMinutes: (Int) -> Unit = {},
    onSlideIndexChanged: (Int) -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onboardingComplete: Boolean = true,
    onCompleteOnboarding: () -> Unit = {},
    onResetOnboarding: () -> Unit = {},
    appVersion: String = "",
    relatedAnimeOverlay: RelatedAnimeOverlayState? = null,
    relatedMediaDetail: RelatedMediaDetailState? = null,
    onShowRelatedAnime: (Int, String) -> Unit = { _, _ -> },
    onDismissRelatedAnime: () -> Unit = {},
    onOpenRelatedMediaDetail: (RelatedAnime) -> Unit = {},
    onCloseRelatedMediaDetail: () -> Unit = {},
) {
  var showSettings by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  var relatedDetailTrailerId by remember { mutableStateOf<String?>(null) }
  var showRelatedDetailScoreDialog by remember { mutableStateOf(false) }
  var showRelatedDetailListDialog by remember { mutableStateOf(false) }

  LaunchedEffect(userMessage) {
    val message = userMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    onClearUserMessage()
  }

  BackHandler(enabled = showSettings) { showSettings = false }
  BackHandler(enabled = state is UiState.SigningIn && !showSettings) { onCancelSignIn() }

  Box(modifier = Modifier.fillMaxSize()) {
    Surface(modifier = Modifier.fillMaxSize(), color = PortalAniColors.Background) {
      when (state) {
        UiState.Loading -> AnimeLoadingScreen(frameMode = settings.frameMode)
        UiState.SigningIn ->
            SigningInScreen(onCancel = onCancelSignIn)
        is UiState.NeedsSetup ->
            SetupScreen(
                message = state.message,
                canSignIn = state.canSignIn,
                canUseLibrary = state.canUseLibrary,
                onSignIn = onSignIn,
                onUseLibrary = onUseLibrary,
                onRetry = onRetry,
            )
        is UiState.Showing ->
            if (settings.frameMode == FrameMode.CALENDAR) {
              CalendarHostScreen(
                  weekState = calendarState,
                  loading = calendarLoading,
                  weekStartSetting = settings.weekStart,
                  settingsOpen = showSettings,
                  detailSlide = calendarDetailSlide,
                  detailLoading = calendarDetailLoading,
                  isSignedIn = isSignedIn,
                  onToggleSettings = { showSettings = !showSettings },
                  onShiftWeek = onShiftCalendarWeek,
                  onGoToToday = onGoToCalendarToday,
                  onOpenEntry = onOpenCalendarEntry,
                  onCloseDetail = onCloseCalendarDetail,
                  onSetUserScore = onSetUserScore,
                  onToggleFavourite = onToggleFavourite,
                  onSetAnimeListStatus = onSetAnimeListStatus,
                  onRemoveFromList = onRemoveFromList,
                  onUserInteraction = onUserInteraction,
                  onShowRelatedAnime = onShowRelatedAnime,
              )
            } else if (state.slides.isEmpty()) {
              AnimeLoadingScreen(frameMode = settings.frameMode)
            } else {
              SlideshowScreen(
                  slides = state.slides,
                  fromCache = state.fromCache,
                  isRefreshing = state.isRefreshing,
                  orderResetToken = state.orderResetToken,
                  shuffle = settings.shuffle,
                  intervalMs = settings.intervalMs,
                  frameMode = settings.frameMode,
                  showPosterClock = settings.showPosterClock,
                  showWeather = settings.showWeather && settings.showPosterClock,
                  weather = weather,
                  settingsOpen = showSettings,
                  isSignedIn = isSignedIn,
                  onToggleSettings = { showSettings = !showSettings },
                  onSignIn = onSignIn,
                  onSetUserScore = onSetUserScore,
                  onToggleFavourite = onToggleFavourite,
                  onSetAnimeListStatus = onSetAnimeListStatus,
                  onRemoveFromList = onRemoveFromList,
                  onSlideIndexChanged = onSlideIndexChanged,
                  onUserInteraction = onUserInteraction,
                  onShowRelatedAnime = onShowRelatedAnime,
                  onboardingComplete = onboardingComplete,
                  onCompleteOnboarding = onCompleteOnboarding,
              )
            }
        is UiState.Error ->
            ErrorScreen(
                message = state.message,
                canOpenSettings = state.canOpenSettings,
                onRetry = onRetry,
                onOpenSettings = { showSettings = true },
            )
      }
    }

    if (showSettings) {
      SettingsPanel(
          settings = settings,
          geoStatus = geoStatus,
          geoResults = geoResults,
          viewerName = viewerName,
          onDismiss = { showSettings = false },
          onSignIn = onSignIn,
          onSignOut = onSignOut,
          onSetShuffle = onSetShuffle,
          onSetFrameMode = onSetFrameMode,
          onSetWeekStart = onSetWeekStart,
          onSetShowPosterClock = onSetShowPosterClock,
          onSetShowWeather = onSetShowWeather,
          onSetWeatherFahrenheit = onSetWeatherFahrenheit,
          onSearchLocation = onSearchLocation,
          onChooseLocation = onChooseLocation,
          onDetectLocation = onDetectLocation,
          onClearGeoSearch = onClearGeoSearch,
          onSetIntervalSeconds = onSetIntervalSeconds,
          onSetSourceMode = onSetSourceMode,
          onSetListStatuses = onSetListStatuses,
          onSetFormatFilters = onSetFormatFilters,
          onSetCountryFilters = onSetCountryFilters,
          onSetSourceFilters = onSetSourceFilters,
          onSetDemographicFilters = onSetDemographicFilters,
          onSetHideHentai = onSetHideHentai,
          onSetLibrarySort = onSetLibrarySort,
          onSetSeasonKey = onSetSeasonKey,
          onSetPowerMode = onSetPowerMode,
          onSetIdleSleepMinutes = onSetIdleSleepMinutes,
          onSetSleepStartMinutes = onSetSleepStartMinutes,
          onSetSleepEndMinutes = onSetSleepEndMinutes,
          onUserInteraction = onUserInteraction,
          onResetOnboarding = onResetOnboarding,
          appVersion = appVersion,
      )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    )

    relatedAnimeOverlay?.let { overlay ->
      RelatedAnimeCarouselOverlay(
          sourceTitle = overlay.sourceTitle,
          items = overlay.items,
          loading = overlay.loading,
          onDismiss = onDismissRelatedAnime,
          onSelectRelated = onOpenRelatedMediaDetail,
      )
    }

    relatedMediaDetail?.let { detail ->
      val slide = detail.slide
      val withSignIn: (() -> Unit) -> Unit = { action ->
        if (isSignedIn) {
          action()
        } else {
          onSignIn()
        }
      }

      RelatedMediaDetailOverlay(
          slide = slide,
          detailLoading = detail.loading,
          onDismiss = onCloseRelatedMediaDetail,
          onPlayTrailer = slide.trailerYoutubeId?.let { id -> { relatedDetailTrailerId = id } },
          onOpenAniList = {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(slide.anilistUrl))
          },
          onTapScore = { withSignIn { showRelatedDetailScoreDialog = true } },
          onToggleFavourite = { withSignIn { onToggleFavourite(slide.id) } },
          onEditList = { withSignIn { showRelatedDetailListDialog = true } },
          onShowRelated = { onShowRelatedAnime(slide.id, slide.title) },
      )

      relatedDetailTrailerId?.let { id ->
        TrailerOverlay(youtubeId = id, onDismiss = { relatedDetailTrailerId = null })
      }

      if (showRelatedDetailScoreDialog) {
        ScoreSliderDialog(
            animeTitle = slide.title,
            initialScore = slide.userScore,
            onDismiss = { showRelatedDetailScoreDialog = false },
            onSave = { score ->
              onSetUserScore(slide.id, score)
              showRelatedDetailScoreDialog = false
            },
        )
      }

      if (showRelatedDetailListDialog) {
        ListStatusDialog(
            animeTitle = slide.title,
            currentStatus = slide.listStatus,
            onDismiss = { showRelatedDetailListDialog = false },
            onSelect = { status ->
              onSetAnimeListStatus(slide.id, status)
              showRelatedDetailListDialog = false
            },
            onRemove =
                if (slide.isOnList) {
                  {
                    onRemoveFromList(slide.id)
                    showRelatedDetailListDialog = false
                  }
                } else {
                  null
                },
        )
      }
    }
  }
}
