package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.UiState
import com.portal.portalani.CalendarWeekState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.SourceFilter
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.GeoPlace
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerMode
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SeasonSelection
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.WeatherNow
import com.portal.portalani.data.WeekStart
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun PortalAniApp(
    state: UiState,
    settings: AppSettings,
    weather: WeatherNow? = null,
    calendarState: CalendarWeekState? = null,
    calendarLoading: Boolean = false,
    calendarDetailSlide: AnimeSlide? = null,
    geoStatus: String? = null,
    geoResults: List<GeoPlace> = emptyList(),
    viewerName: String?,
    isSignedIn: Boolean,
    userMessage: String?,
    onSignIn: () -> Unit,
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
) {
  var showSettings by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(userMessage) {
    val message = userMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(message)
    onClearUserMessage()
  }

  BackHandler(enabled = showSettings) { showSettings = false }

  Box(modifier = Modifier.fillMaxSize()) {
    Surface(modifier = Modifier.fillMaxSize(), color = PortalAniColors.Background) {
      when (state) {
        UiState.Loading -> AnimeLoadingScreen(frameMode = settings.frameMode)
        UiState.SigningIn -> AnimeLoadingScreen(message = stringResource(R.string.sign_in_hint))
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
              )
            } else {
              SlideshowScreen(
                  slides = state.slides,
                  fromCache = state.fromCache,
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
  }
}

@Composable
private fun CenterMessage(text: String) {
  Box(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      contentAlignment = Alignment.Center,
  ) {
    Text(text, color = PortalAniColors.TextPrimary, fontSize = 28.sp, textAlign = TextAlign.Center)
  }
}

@Composable
private fun SetupScreen(
    message: String,
    canSignIn: Boolean,
    canUseLibrary: Boolean,
    onSignIn: () -> Unit,
    onUseLibrary: () -> Unit,
    onRetry: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text("Portal Ani", color = PortalAniColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    Text(message, color = PortalAniColors.TextMuted, fontSize = 20.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    if (canSignIn) {
      PortalPrimaryButton(
          text = stringResource(R.string.sign_in),
          onClick = onSignIn,
          modifier = Modifier.width(360.dp),
      )
      Spacer(Modifier.height(16.dp))
    }
    if (canUseLibrary) {
      PortalSecondaryButton(
          text = stringResource(R.string.use_full_library),
          onClick = onUseLibrary,
          modifier = Modifier.width(360.dp),
      )
      Spacer(Modifier.height(16.dp))
    }
    TextButton(onClick = onRetry) {
      Text(stringResource(R.string.retry), fontSize = 20.sp, color = PortalAniColors.TextSecondary)
    }
  }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    canOpenSettings: Boolean = false,
    onOpenSettings: () -> Unit = {},
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(message, color = PortalAniColors.TextPrimary, fontSize = 24.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    if (canOpenSettings) {
      PortalPrimaryButton(
          text = stringResource(R.string.open_settings),
          onClick = onOpenSettings,
          modifier = Modifier.width(320.dp).testTag(PortalTestTags.OPEN_SETTINGS),
      )
      Spacer(Modifier.height(16.dp))
      PortalSecondaryButton(
          text = stringResource(R.string.retry),
          onClick = onRetry,
          modifier = Modifier.width(320.dp),
      )
    } else {
      PortalPrimaryButton(
          text = stringResource(R.string.retry),
          onClick = onRetry,
          modifier = Modifier.width(280.dp),
      )
    }
  }
}

@Composable
private fun SlideshowScreen(
    slides: List<AnimeSlide>,
    fromCache: Boolean,
    orderResetToken: Int,
    shuffle: Boolean,
    intervalMs: Long,
    frameMode: FrameMode,
    showPosterClock: Boolean,
    showWeather: Boolean,
    weather: WeatherNow?,
    settingsOpen: Boolean,
    isSignedIn: Boolean,
    onToggleSettings: () -> Unit,
    onSignIn: () -> Unit,
    onSetUserScore: (Int, Float?) -> Unit,
    onToggleFavourite: (Int) -> Unit,
    onSetAnimeListStatus: (Int, ListStatus) -> Unit,
    onRemoveFromList: (Int) -> Unit,
    onSlideIndexChanged: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    onboardingComplete: Boolean,
    onCompleteOnboarding: () -> Unit,
) {
  var orderedIds by remember(orderResetToken, shuffle) {
    mutableStateOf(buildSlideOrder(slides.map { it.id }, shuffle, orderResetToken))
  }

  LaunchedEffect(orderResetToken, shuffle) {
    orderedIds = buildSlideOrder(slides.map { it.id }, shuffle, orderResetToken)
  }

  LaunchedEffect(slides) {
    val allIds = slides.map { it.id }
    val known = orderedIds.toSet()
    val allSet = allIds.toSet()
    when {
      orderedIds.any { it !in allSet } ->
          orderedIds = buildSlideOrder(allIds, shuffle, orderResetToken)
      else -> {
        val added = allIds.filter { it !in known }
        if (added.isNotEmpty()) {
          val tail = if (shuffle) added.shuffled(Random((orderResetToken to added).hashCode().toLong())) else added
          orderedIds = orderedIds + tail
        }
      }
    }
  }

  val order =
      remember(slides, orderedIds) {
        val byId = slides.associateBy { it.id }
        orderedIds.mapNotNull { byId[it] }
      }

  var index by remember { mutableIntStateOf(0) }
  var navEpoch by remember { mutableIntStateOf(0) }

  LaunchedEffect(orderResetToken) {
    index = 0
  }

  LaunchedEffect(order.size) {
    if (order.isNotEmpty() && index > order.lastIndex) {
      index = order.lastIndex
    }
  }

  var trailerYoutubeId by remember { mutableStateOf<String?>(null) }
  var scoreDialogMediaId by remember { mutableStateOf<Int?>(null) }
  var listDialogMediaId by remember { mutableStateOf<Int?>(null) }
  var showSignInPrompt by remember { mutableStateOf(false) }
  var posterExpanded by remember { mutableStateOf(false) }
  var guideStep by remember(onboardingComplete) {
    mutableStateOf(
        if (onboardingComplete) SlideshowGuideStep.Finished else SlideshowGuideStep.Swipe,
    )
  }
  var appInForeground by remember { mutableStateOf(true) }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  if (order.isEmpty()) return

  val safeIndex = index.coerceIn(0, order.lastIndex)
  val interactionOpen = scoreDialogMediaId != null || listDialogMediaId != null || showSignInPrompt
  val gesturesEnabled = !settingsOpen && !interactionOpen
  val density = LocalDensity.current
  val swipeThresholdPx = with(density) { 64.dp.toPx() }

  val showGuide =
      !onboardingComplete &&
          guideStep != SlideshowGuideStep.Finished &&
          !settingsOpen &&
          !interactionOpen &&
          trailerYoutubeId == null

  fun advanceGuide(expected: SlideshowGuideStep) {
    if (onboardingComplete || guideStep != expected) return
    val next = nextSlideshowGuideStep(guideStep, frameMode)
    guideStep = next
    if (next == SlideshowGuideStep.Finished) {
      onCompleteOnboarding()
    }
  }

  LaunchedEffect(guideStep, onboardingComplete, frameMode, showGuide) {
    if (!showGuide) return@LaunchedEffect
    delay(12_000)
    if (guideStep == SlideshowGuideStep.Finished) return@LaunchedEffect
    val next = nextSlideshowGuideStep(guideStep, frameMode)
    guideStep = next
    if (next == SlideshowGuideStep.Finished) {
      onCompleteOnboarding()
    }
  }

  LaunchedEffect(safeIndex, order.size) {
    onSlideIndexChanged(safeIndex)
  }

  LaunchedEffect(onboardingComplete) {
    if (!onboardingComplete) {
      guideStep = SlideshowGuideStep.Swipe
    }
  }

  LaunchedEffect(safeIndex, frameMode) {
    posterExpanded = false
  }

  BackHandler(enabled = interactionOpen && !settingsOpen) {
    scoreDialogMediaId = null
    listDialogMediaId = null
    showSignInPrompt = false
  }

  fun withSignIn(action: () -> Unit) {
    if (isSignedIn) action() else showSignInPrompt = true
  }

  DisposableEffect(lifecycleOwner) {
    val observer =
        LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_RESUME -> appInForeground = true
            Lifecycle.Event.ON_PAUSE -> appInForeground = false
            else -> Unit
          }
        }
    lifecycleOwner.lifecycle.addObserver(observer)
    appInForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val slideshowPaused =
      trailerYoutubeId != null ||
          settingsOpen ||
          !appInForeground ||
          interactionOpen ||
          (frameMode == FrameMode.POSTER_ONLY && posterExpanded)

  fun restartSlideTimer() {
    navEpoch++
  }

  LaunchedEffect(order.size, intervalMs, safeIndex, slideshowPaused, navEpoch) {
    if (slideshowPaused || order.isEmpty()) return@LaunchedEffect
    delay(intervalMs)
    index = (safeIndex + 1) % order.size
  }

  fun next() {
    if (order.isEmpty()) return
    advanceGuide(SlideshowGuideStep.Swipe)
    index = (index + 1) % order.size
    restartSlideTimer()
  }

  fun previous() {
    if (order.isEmpty()) return
    advanceGuide(SlideshowGuideStep.Swipe)
    index = if (index == 0) order.size - 1 else index - 1
    restartSlideTimer()
  }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .then(
                  if (!gesturesEnabled) {
                    Modifier
                  } else {
                    Modifier.pointerInput(order.size, swipeThresholdPx) {
                          var totalDrag = 0f
                          detectHorizontalDragGestures(
                              onDragStart = { totalDrag = 0f },
                              onHorizontalDrag = { _, amount ->
                                totalDrag += amount
                                onUserInteraction()
                              },
                              onDragEnd = {
                                when {
                                  totalDrag <= -swipeThresholdPx -> next()
                                  totalDrag >= swipeThresholdPx -> previous()
                                }
                                totalDrag = 0f
                              },
                              onDragCancel = { totalDrag = 0f },
                          )
                        }
                        .pointerInput(order.size) {
                          detectTapGestures(
                              onLongPress = { offset ->
                                onUserInteraction()
                                if (offset.x >= size.width * 0.2f && offset.x <= size.width * 0.8f) {
                                  advanceGuide(SlideshowGuideStep.HoldSettings)
                                  onToggleSettings()
                                }
                              },
                              onTap = { offset ->
                                onUserInteraction()
                                if (offset.x < size.width * 0.2f) previous()
                                else if (offset.x > size.width * 0.8f) next()
                              },
                          )
                        }
                  },
              ),
  ) {
    AnimatedSlideHost(
        slideIndex = safeIndex,
        slideCount = order.size,
        modifier = Modifier.fillMaxSize(),
    ) { idx ->
      val slide = order.getOrNull(idx) ?: return@AnimatedSlideHost
      AnimeFrameSlide(
          slide = slide,
          frameMode = frameMode,
          isSignedIn = isSignedIn,
          posterExpanded = frameMode == FrameMode.POSTER_ONLY && posterExpanded,
          onPosterToggle = {
            onUserInteraction()
            advanceGuide(SlideshowGuideStep.TapPoster)
            posterExpanded = !posterExpanded
            restartSlideTimer()
          },
          onPosterLongPress = {
            onUserInteraction()
            advanceGuide(SlideshowGuideStep.HoldSettings)
            onToggleSettings()
          },
          onPlayTrailer =
              slide.trailerYoutubeId?.let { id ->
                { trailerYoutubeId = id }
              },
          onOpenAniList = {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(slide.anilistUrl))
          },
          onTapScore = { withSignIn { scoreDialogMediaId = slide.id } },
          onToggleFavourite = { withSignIn { onToggleFavourite(slide.id) } },
          onEditList = { withSignIn { listDialogMediaId = slide.id } },
      )
    }

    if (showPosterClock && trailerYoutubeId == null) {
      when (frameMode) {
        FrameMode.POSTER_ONLY ->
            AnimatedPosterClock(
                visible = !posterExpanded,
                showWeather = showWeather,
                weather = weather,
                variant = ClockOverlayVariant.Poster,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        FrameMode.INFORMATIVE ->
            AnimatedPosterClock(
                visible = true,
                showWeather = showWeather,
                weather = weather,
                variant = ClockOverlayVariant.Informative,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        FrameMode.CALENDAR -> Unit
      }
    }

    SlideshowGuideOverlay(
        step = guideStep,
        frameMode = frameMode,
        visible = showGuide,
    )

    if (fromCache) {
      Surface(
          color = Color(0x33000000),
          shape = PortalAniShapes.Pill,
          border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
          modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
      ) {
        Text(
            text = stringResource(R.string.cached_feed_hint),
            color = PortalAniColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
      }
    }

    trailerYoutubeId?.let { id ->
      TrailerOverlay(youtubeId = id, onDismiss = { trailerYoutubeId = null })
    }

    scoreDialogMediaId?.let { mediaId ->
      slides.firstOrNull { it.id == mediaId }?.let { slide ->
        ScoreSliderDialog(
            animeTitle = slide.title,
            initialScore = slide.userScore,
            onDismiss = {
              scoreDialogMediaId = null
              restartSlideTimer()
            },
            onSave = { score ->
              onSetUserScore(mediaId, score)
              scoreDialogMediaId = null
              restartSlideTimer()
            },
        )
      }
    }

    listDialogMediaId?.let { mediaId ->
      slides.firstOrNull { it.id == mediaId }?.let { slide ->
        ListStatusDialog(
            animeTitle = slide.title,
            currentStatus = slide.listStatus,
            onDismiss = {
              listDialogMediaId = null
              restartSlideTimer()
            },
            onSelect = { status ->
              onSetAnimeListStatus(mediaId, status)
              listDialogMediaId = null
              restartSlideTimer()
            },
            onRemove =
                if (slide.isOnList) {
                  {
                    onRemoveFromList(mediaId)
                    listDialogMediaId = null
                    restartSlideTimer()
                  }
                } else {
                  null
                },
        )
      }
    }

    if (showSignInPrompt) {
      SignInPromptDialog(
          message = stringResource(R.string.sign_in_for_actions),
          onDismiss = { showSignInPrompt = false },
          onSignIn = {
            showSignInPrompt = false
            onSignIn()
          },
      )
    }
  }
}

@Composable
private fun SettingsPanel(
    settings: AppSettings,
    geoStatus: String?,
    geoResults: List<GeoPlace>,
    viewerName: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    onSetFrameMode: (FrameMode) -> Unit,
    onSetWeekStart: (WeekStart) -> Unit,
    onSetShowPosterClock: (Boolean) -> Unit,
    onSetShowWeather: (Boolean) -> Unit,
    onSetWeatherFahrenheit: (Boolean) -> Unit,
    onSearchLocation: (String) -> Unit,
    onChooseLocation: (GeoPlace) -> Unit,
    onDetectLocation: () -> Unit,
    onClearGeoSearch: () -> Unit,
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
    onSetPowerMode: (PowerMode) -> Unit,
    onSetIdleSleepMinutes: (Int) -> Unit,
    onSetSleepStartMinutes: (Int) -> Unit,
    onSetSleepEndMinutes: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    onResetOnboarding: () -> Unit,
    appVersion: String,
) {
  var statusMenuOpen by remember { mutableStateOf(false) }
  var formatMenuOpen by remember { mutableStateOf(false) }
  var countryMenuOpen by remember { mutableStateOf(false) }
  var sourceMaterialMenuOpen by remember { mutableStateOf(false) }
  var demographicMenuOpen by remember { mutableStateOf(false) }
  var sortMenuOpen by remember { mutableStateOf(false) }
  var seasonMenuOpen by remember { mutableStateOf(false) }
  var sourceMenuOpen by remember { mutableStateOf(false) }
  var frameMenuOpen by remember { mutableStateOf(false) }
  var weekStartMenuOpen by remember { mutableStateOf(false) }
  var intervalMenuOpen by remember { mutableStateOf(false) }
  var powerMenuOpen by remember { mutableStateOf(false) }
  var idleMenuOpen by remember { mutableStateOf(false) }
  var sleepStartMenuOpen by remember { mutableStateOf(false) }
  var sleepEndMenuOpen by remember { mutableStateOf(false) }
  var tempUnitMenuOpen by remember { mutableStateOf(false) }
  var locationDialogOpen by remember { mutableStateOf(false) }
  var placeWhenLocationDialogOpened by remember { mutableStateOf<String?>(null) }
  val intervalSeconds = (settings.intervalMs / 1000L).toInt()
  val tempUnitOptions =
      listOf(
          "celsius" to stringResource(R.string.weather_celsius),
          "fahrenheit" to stringResource(R.string.weather_fahrenheit),
      )
  val sortOptions = LibrarySort.entries.map { sort -> sort.name to sort.label }
  val sourceOptions =
      listOf(
          SourceMode.PERSONAL.name to stringResource(R.string.source_personal),
          SourceMode.LIBRARY.name to stringResource(R.string.source_library),
      )
  val frameOptions =
      listOf(
          FrameMode.INFORMATIVE.name to stringResource(R.string.frame_mode_informative),
          FrameMode.POSTER_ONLY.name to stringResource(R.string.frame_mode_poster),
          FrameMode.CALENDAR.name to stringResource(R.string.frame_mode_calendar),
      )
  val weekStartOptions =
      listOf(
          WeekStart.MONDAY.name to stringResource(R.string.week_start_monday),
          WeekStart.SUNDAY.name to stringResource(R.string.week_start_sunday),
      )
  val intervalOptions = listOf(8, 12, 20, 30).map { seconds -> seconds.toString() to "$seconds s" }
  val powerOptions =
      listOf(
          PowerMode.ALWAYS_ON.name to stringResource(R.string.power_always_on),
          PowerMode.IDLE_SLEEP.name to stringResource(R.string.power_idle_sleep),
          PowerMode.SCHEDULED_SLEEP.name to stringResource(R.string.power_scheduled_sleep),
      )
  val idleOptions =
      PowerPolicy.IDLE_SLEEP_OPTIONS_MINUTES.map { minutes ->
        minutes.toString() to PowerPolicy.formatIdleDuration(minutes)
      }
  val anyMenuOpen =
      statusMenuOpen ||
          formatMenuOpen ||
          countryMenuOpen ||
          sourceMaterialMenuOpen ||
          demographicMenuOpen ||
          sortMenuOpen ||
          seasonMenuOpen ||
          sourceMenuOpen ||
          frameMenuOpen ||
          weekStartMenuOpen ||
          intervalMenuOpen ||
          powerMenuOpen ||
          idleMenuOpen ||
          sleepStartMenuOpen ||
          sleepEndMenuOpen ||
          tempUnitMenuOpen ||
          locationDialogOpen

  LaunchedEffect(locationDialogOpen) {
    if (locationDialogOpen) {
      placeWhenLocationDialogOpened = settings.weatherPlace
    }
  }

  LaunchedEffect(settings.weatherPlace) {
    if (locationDialogOpen && settings.weatherPlace != placeWhenLocationDialogOpened) {
      locationDialogOpen = false
      onClearGeoSearch()
    }
  }

  BackHandler(enabled = anyMenuOpen) {
    statusMenuOpen = false
    formatMenuOpen = false
    countryMenuOpen = false
    sourceMaterialMenuOpen = false
    demographicMenuOpen = false
    sortMenuOpen = false
    seasonMenuOpen = false
    sourceMenuOpen = false
    frameMenuOpen = false
    weekStartMenuOpen = false
    intervalMenuOpen = false
    powerMenuOpen = false
    idleMenuOpen = false
    sleepStartMenuOpen = false
    sleepEndMenuOpen = false
    tempUnitMenuOpen = false
    locationDialogOpen = false
    onClearGeoSearch()
  }

  BackHandler(enabled = !anyMenuOpen) { onDismiss() }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(PortalAniColors.Overlay)
              .clickable(onClick = onDismiss),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        modifier =
            Modifier.padding(horizontal = 48.dp, vertical = 52.dp)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .testTag(PortalTestTags.SETTINGS_SHEET)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
    ) {
      Column(
          modifier =
              Modifier.verticalScroll(rememberScrollState())
                  .padding(horizontal = 36.dp, vertical = 34.dp)
                  .padding(bottom = 12.dp),
      ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            stringResource(R.string.settings),
            color = PortalAniColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        PortalCircleIconButton(
            icon = PortalIcons.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
          stringResource(R.string.screensaver_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 15.sp,
          lineHeight = 21.sp,
      )
      Spacer(Modifier.height(24.dp))

      PortalSettingsSectionHeader(stringResource(R.string.settings_section_slideshow))
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.frame_mode),
            value =
                when (settings.frameMode) {
                  FrameMode.INFORMATIVE -> stringResource(R.string.frame_mode_informative)
                  FrameMode.POSTER_ONLY -> stringResource(R.string.frame_mode_poster)
                  FrameMode.CALENDAR -> stringResource(R.string.frame_mode_calendar)
                },
            onClick = {
              onUserInteraction()
              frameMenuOpen = true
            },
        )
        if (settings.frameMode == FrameMode.CALENDAR) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.week_starts_on),
              value =
                  if (settings.weekStart == WeekStart.MONDAY) {
                    stringResource(R.string.week_start_monday)
                  } else {
                    stringResource(R.string.week_start_sunday)
                  },
              onClick = {
                onUserInteraction()
                weekStartMenuOpen = true
              },
          )
        }
        if (settings.frameMode != FrameMode.CALENDAR) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.interval_seconds),
              value = "$intervalSeconds s",
              onClick = {
                onUserInteraction()
                intervalMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsToggleRow(
              label = stringResource(R.string.shuffle),
              checked = settings.shuffle,
              onCheckedChange = {
                onUserInteraction()
                onSetShuffle(it)
              },
          )
        }
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.settings_section_clock_weather),
          subtitle =
              if (!settings.showPosterClock) {
                stringResource(R.string.weather_requires_clock)
              } else {
                stringResource(R.string.weather_hint)
              },
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsToggleRow(
            label = stringResource(R.string.show_poster_clock),
            checked = settings.showPosterClock,
            onCheckedChange = {
              onUserInteraction()
              onSetShowPosterClock(it)
            },
        )
        PortalSettingsDivider()
        PortalSettingsToggleRow(
            label = stringResource(R.string.show_weather),
            checked = settings.showWeather,
            enabled = settings.showPosterClock,
            onCheckedChange = { enabled ->
              onUserInteraction()
              onSetShowWeather(enabled)
              if (enabled && settings.weatherPlace == null) {
                locationDialogOpen = true
              }
            },
        )
        if (settings.showWeather) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.weather_temperature_unit),
              value =
                  if (settings.weatherFahrenheit) {
                    stringResource(R.string.weather_fahrenheit)
                  } else {
                    stringResource(R.string.weather_celsius)
                  },
              onClick = {
                onUserInteraction()
                tempUnitMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.weather_location),
              value = settings.weatherPlace ?: stringResource(R.string.weather_location_not_set),
              onClick = {
                onUserInteraction()
                locationDialogOpen = true
              },
          )
        }
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.settings_section_content),
          subtitle =
              if (settings.sourceMode == SourceMode.LIBRARY) {
                stringResource(R.string.library_filters)
              } else {
                stringResource(R.string.personal_lists_hint)
              },
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.source_mode),
            value =
                if (settings.sourceMode == SourceMode.PERSONAL) {
                  stringResource(R.string.source_personal)
                } else {
                  stringResource(R.string.source_library)
                },
            onClick = {
              onUserInteraction()
              sourceMenuOpen = true
            },
        )
        PortalSettingsDivider()
        if (settings.sourceMode == SourceMode.PERSONAL) {
          PortalSettingsRow(
              label = stringResource(R.string.list_status),
              value = listStatusesSettingLabel(settings.listStatuses),
              onClick = { statusMenuOpen = true },
          )
          CatalogFilterSettingsRows(
              settings = settings,
              onFormatClick = { formatMenuOpen = true },
              onCountryClick = { countryMenuOpen = true },
              onSourceMaterialClick = { sourceMaterialMenuOpen = true },
              onDemographicClick = { demographicMenuOpen = true },
          )
          if (settings.frameMode == FrameMode.CALENDAR) {
            PortalSettingsDivider()
            PortalSettingsRow(
                label = stringResource(R.string.sort_by),
                value = settings.librarySort.label,
                onClick = { sortMenuOpen = true },
            )
          }
        } else {
          CatalogFilterSettingsRows(
              settings = settings,
              onFormatClick = { formatMenuOpen = true },
              onCountryClick = { countryMenuOpen = true },
              onSourceMaterialClick = { sourceMaterialMenuOpen = true },
              onDemographicClick = { demographicMenuOpen = true },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.sort_by),
              value = settings.librarySort.label,
              onClick = { sortMenuOpen = true },
          )
          PortalSettingsDivider()
          if (settings.frameMode == FrameMode.CALENDAR) {
            PortalSettingsRow(
                label = stringResource(R.string.season_filter),
                value = stringResource(R.string.calendar_season_window),
                onClick = {},
                enabled = false,
                showChevron = false,
            )
          } else {
            PortalSettingsRow(
                label = stringResource(R.string.season_filter),
                value = SeasonSelection.labelFor(settings.seasonKey),
                onClick = { seasonMenuOpen = true },
            )
          }
        }
        PortalSettingsDivider()
        PortalSettingsToggleRow(
            label = stringResource(R.string.hide_hentai_genre),
            checked = settings.hideHentai,
            onCheckedChange = {
              onUserInteraction()
              onSetHideHentai(it)
            },
        )
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(stringResource(R.string.settings_section_account))
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        if (viewerName != null) {
          PortalSettingsRow(
              label = stringResource(R.string.settings_section_account),
              value = viewerName,
              onClick = {},
              enabled = false,
              showChevron = false,
          )
          PortalSettingsDivider()
          PortalSettingsActionRow(
              label = stringResource(R.string.sign_out),
              onClick = onSignOut,
          )
        } else {
          PortalSettingsRow(
              label = stringResource(R.string.settings_section_account),
              value = stringResource(R.string.account_not_signed_in),
              onClick = onSignIn,
          )
        }
        PortalSettingsDivider()
        PortalSettingsRow(
            label = stringResource(R.string.settings_show_guide),
            value = stringResource(R.string.settings_show_guide_action),
            onClick = {
              onUserInteraction()
              onResetOnboarding()
              onDismiss()
            },
        )
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.power_settings),
          subtitle = stringResource(R.string.power_hint),
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.power_mode),
            value =
                when (settings.powerMode) {
                  PowerMode.ALWAYS_ON -> stringResource(R.string.power_always_on)
                  PowerMode.IDLE_SLEEP -> stringResource(R.string.power_idle_sleep)
                  PowerMode.SCHEDULED_SLEEP -> stringResource(R.string.power_scheduled_sleep)
                },
            onClick = {
              onUserInteraction()
              powerMenuOpen = true
            },
        )

        if (settings.powerMode == PowerMode.IDLE_SLEEP) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_idle_after),
              value = PowerPolicy.formatIdleDuration(settings.idleSleepMinutes),
              onClick = {
                onUserInteraction()
                idleMenuOpen = true
              },
          )
        }

        if (settings.powerMode == PowerMode.SCHEDULED_SLEEP) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_sleep_from),
              value = PowerPolicy.formatMinutesOfDay(settings.sleepStartMinutes),
              onClick = {
                onUserInteraction()
                sleepStartMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_wake_at),
              value = PowerPolicy.formatMinutesOfDay(settings.sleepEndMinutes),
              onClick = {
                onUserInteraction()
                sleepEndMenuOpen = true
              },
          )
        }
      }

      if (settings.powerMode == PowerMode.SCHEDULED_SLEEP) {
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.power_scheduled_hint),
            color = PortalAniColors.TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
      }

      Spacer(Modifier.height(20.dp))
      if (appVersion.isNotBlank()) {
        Text(
            text = stringResource(R.string.app_version, appVersion),
            color = PortalAniColors.TextMuted.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
      }
      }
    }
  }

  if (sourceMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.source_mode),
        options = sourceOptions,
        selectedKey = settings.sourceMode.name,
        onDismiss = { sourceMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetSourceMode(SourceMode.valueOf(key))
        },
    )
  }

  if (frameMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.frame_mode),
        options = frameOptions,
        selectedKey = settings.frameMode.name,
        onDismiss = { frameMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetFrameMode(FrameMode.valueOf(key))
        },
    )
  }

  if (weekStartMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.week_starts_on),
        options = weekStartOptions,
        selectedKey = settings.weekStart.name,
        onDismiss = { weekStartMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetWeekStart(WeekStart.valueOf(key))
        },
    )
  }

  if (intervalMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.interval_seconds),
        options = intervalOptions,
        selectedKey = intervalSeconds.toString(),
        onDismiss = { intervalMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetIntervalSeconds(key.toIntOrNull() ?: intervalSeconds)
        },
    )
  }

  if (tempUnitMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.weather_temperature_unit),
        options = tempUnitOptions,
        selectedKey = if (settings.weatherFahrenheit) "fahrenheit" else "celsius",
        onDismiss = { tempUnitMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetWeatherFahrenheit(key == "fahrenheit")
        },
    )
  }

  if (powerMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.power_settings),
        options = powerOptions,
        selectedKey = settings.powerMode.name,
        onDismiss = { powerMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetPowerMode(PowerMode.valueOf(key))
        },
    )
  }

  if (idleMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.power_idle_after),
        options = idleOptions,
        selectedKey = settings.idleSleepMinutes.toString(),
        onDismiss = { idleMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetIdleSleepMinutes(key.toIntOrNull() ?: settings.idleSleepMinutes)
        },
    )
  }

  if (statusMenuOpen) {
    PersonalListStatusesDialog(
        selected = settings.listStatuses,
        onDismiss = { statusMenuOpen = false },
        onApply = { statuses ->
          onUserInteraction()
          onSetListStatuses(statuses)
        },
    )
  }

  if (formatMenuOpen) {
    FormatFiltersDialog(
        selected = settings.formatFilters,
        onDismiss = { formatMenuOpen = false },
        onApply = { formats ->
          onUserInteraction()
          onSetFormatFilters(formats)
        },
    )
  }

  if (countryMenuOpen) {
    CountryFiltersDialog(
        selected = settings.countryFilters,
        onDismiss = { countryMenuOpen = false },
        onApply = { countries ->
          onUserInteraction()
          onSetCountryFilters(countries)
        },
    )
  }

  if (sourceMaterialMenuOpen) {
    SourceFiltersDialog(
        selected = settings.sourceFilters,
        onDismiss = { sourceMaterialMenuOpen = false },
        onApply = { sources ->
          onUserInteraction()
          onSetSourceFilters(sources)
        },
    )
  }

  if (demographicMenuOpen) {
    DemographicFiltersDialog(
        selected = settings.demographicFilters,
        onDismiss = { demographicMenuOpen = false },
        onApply = { demographics ->
          onUserInteraction()
          onSetDemographicFilters(demographics)
        },
    )
  }

  if (sortMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.sort_by),
        options = sortOptions,
        selectedKey = settings.librarySort.name,
        onDismiss = { sortMenuOpen = false },
        onSelect = { key -> onSetLibrarySort(LibrarySort.valueOf(key)) },
    )
  }

  if (seasonMenuOpen) {
    PortalSeasonPickerDialog(
        title = stringResource(R.string.season_filter),
        initialState = SeasonSelection.decode(settings.seasonKey),
        onDismiss = { seasonMenuOpen = false },
        onApply = onSetSeasonKey,
    )
  }

  if (sleepStartMenuOpen) {
    PortalTimePickerDialog(
        title = stringResource(R.string.power_sleep_from),
        selectedMinutes = settings.sleepStartMinutes,
        onDismiss = { sleepStartMenuOpen = false },
        onSelect = onSetSleepStartMinutes,
    )
  }

  if (sleepEndMenuOpen) {
    PortalTimePickerDialog(
        title = stringResource(R.string.power_wake_at),
        selectedMinutes = settings.sleepEndMinutes,
        onDismiss = { sleepEndMenuOpen = false },
        onSelect = onSetSleepEndMinutes,
    )
  }

  if (locationDialogOpen) {
    WeatherLocationDialog(
        currentPlace = settings.weatherPlace,
        geoStatus = geoStatus,
        geoResults = geoResults,
        onDismiss = {
          locationDialogOpen = false
          onClearGeoSearch()
        },
        onDetectLocation = {
          onUserInteraction()
          onDetectLocation()
        },
        onSearchLocation = { query ->
          onUserInteraction()
          onSearchLocation(query)
        },
        onChooseLocation = { place ->
          onUserInteraction()
          onChooseLocation(place)
          locationDialogOpen = false
          onClearGeoSearch()
        },
    )
  }
}

@Composable
private fun CatalogFilterSettingsRows(
    settings: AppSettings,
    onFormatClick: () -> Unit,
    onCountryClick: () -> Unit,
    onSourceMaterialClick: () -> Unit,
    onDemographicClick: () -> Unit,
) {
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.format_filter),
      value = formatFiltersSettingLabel(settings.formatFilters),
      onClick = onFormatClick,
      modifier = Modifier.testTag(PortalTestTags.FORMAT_FILTER_ROW),
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.country_filter),
      value = countryFiltersSettingLabel(settings.countryFilters),
      onClick = onCountryClick,
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.source_material_filter),
      value = sourceFiltersSettingLabel(settings.sourceFilters),
      onClick = onSourceMaterialClick,
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.demographic_filter),
      value = demographicFiltersSettingLabel(settings.demographicFilters),
      onClick = onDemographicClick,
  )
}

private fun buildSlideOrder(ids: List<Int>, shuffle: Boolean, seed: Int): List<Int> =
    if (shuffle) {
      ids.shuffled(Random((seed to ids.size).hashCode().toLong()))
    } else {
      ids
    }
