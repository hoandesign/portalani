package com.portal.portalani

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal.portalani.BuildConfig
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.ScreensaverGuard
import com.portal.portalani.ui.PortalAniApp
import com.portal.portalani.ui.PortalAniTheme
import com.portal.portalani.ui.PowerScreenEffect

class MainActivity : ComponentActivity() {
  private val vm: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enterImmersive()

    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            if (vm.state.value is UiState.SigningIn) {
              vm.cancelSignIn()
            } else {
              moveTaskToBack(true)
            }
          }
        },
    )

    val launchedFromDream = intent.getBooleanExtra(EXTRA_DREAM_MODE, false)

    setContent {
      PortalAniTheme {
        val state by vm.state.collectAsStateWithLifecycle()
        val settings by vm.settings.collectAsStateWithLifecycle()
        val calendarState by vm.calendarState.collectAsStateWithLifecycle()
        val calendarLoading by vm.calendarLoading.collectAsStateWithLifecycle()
        val calendarDetailSlide by vm.calendarDetailSlide.collectAsStateWithLifecycle()
        val calendarDetailLoading by vm.calendarDetailLoading.collectAsStateWithLifecycle()
        val viewerName by vm.viewerName.collectAsStateWithLifecycle()
        val isSignedIn by vm.isSignedIn.collectAsStateWithLifecycle()
        val userMessage by vm.userMessage.collectAsStateWithLifecycle()
        val onboardingComplete by vm.onboardingComplete.collectAsStateWithLifecycle()
        val weather by vm.weather.collectAsStateWithLifecycle()
        val geoStatus by vm.geoStatus.collectAsStateWithLifecycle()
        val geoResults by vm.geoResults.collectAsStateWithLifecycle()
        var lastUserInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        val slideshowAllowed = PowerPolicy.shouldRunSlideshow(settings)

        val locationLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
              if (grants.values.any { it }) vm.detectLocation()
            }

        LaunchedEffect(launchedFromDream, settings.powerMode, settings.sleepStartMinutes, settings.sleepEndMinutes) {
          if (launchedFromDream && !slideshowAllowed) {
            finish()
          }
        }

        PowerScreenEffect(
            settings = settings,
            lastUserInteractionMs = lastUserInteractionMs,
        )

        PortalAniApp(
            state = state,
            settings = settings,
            weather = weather,
            calendarState = calendarState,
            calendarLoading = calendarLoading,
            calendarDetailSlide = calendarDetailSlide,
            calendarDetailLoading = calendarDetailLoading,
            geoStatus = geoStatus,
            geoResults = geoResults,
            viewerName = viewerName,
            isSignedIn = isSignedIn,
            userMessage = userMessage,
            onSignIn = vm::signIn,
            onCancelSignIn = vm::cancelSignIn,
            onSignOut = vm::signOut,
            onRetry = vm::refresh,
            onUseLibrary = vm::useLibrary,
            onClearUserMessage = vm::clearUserMessage,
            onSetUserScore = vm::setUserScore,
            onToggleFavourite = vm::toggleFavourite,
            onSetAnimeListStatus = vm::setAnimeListStatus,
            onRemoveFromList = vm::removeFromList,
            onSetShuffle = vm::setShuffle,
            onSetFrameMode = vm::setFrameMode,
            onShiftCalendarWeek = vm::shiftCalendarWeek,
            onGoToCalendarToday = vm::goToCalendarToday,
            onOpenCalendarEntry = vm::openCalendarEntry,
            onCloseCalendarDetail = vm::closeCalendarDetail,
            onSetWeekStart = vm::setWeekStart,
            onSetShowPosterClock = vm::setShowPosterClock,
            onSetShowWeather = vm::setShowWeather,
            onSetWeatherFahrenheit = vm::setWeatherFahrenheit,
            onSearchLocation = vm::searchLocation,
            onChooseLocation = vm::chooseLocation,
            onDetectLocation = {
              if (vm.hasLocationPermission()) {
                vm.detectLocation()
              } else {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ),
                )
              }
            },
            onClearGeoSearch = vm::clearGeoSearch,
            onSetIntervalSeconds = vm::setIntervalSeconds,
            onSetSourceMode = vm::setSourceMode,
            onSetListStatuses = vm::setListStatuses,
            onSetFormatFilters = vm::setFormatFilters,
            onSetCountryFilters = vm::setCountryFilters,
            onSetSourceFilters = vm::setSourceFilters,
            onSetDemographicFilters = vm::setDemographicFilters,
            onSetHideHentai = vm::setHideHentai,
            onSetLibrarySort = vm::setLibrarySort,
            onSetSeasonKey = vm::setSeasonKey,
            onSetPowerMode = vm::setPowerMode,
            onSetIdleSleepMinutes = vm::setIdleSleepMinutes,
            onSetSleepStartMinutes = vm::setSleepStartMinutes,
            onSetSleepEndMinutes = vm::setSleepEndMinutes,
            onSlideIndexChanged = vm::onSlideIndexChanged,
            onUserInteraction = { lastUserInteractionMs = System.currentTimeMillis() },
            onboardingComplete = onboardingComplete,
            onCompleteOnboarding = vm::completeOnboarding,
            onResetOnboarding = vm::resetOnboarding,
            appVersion = BuildConfig.VERSION_NAME,
        )
      }
    }

    handleIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    ScreensaverGuard.applyNow(this)
  }

  override fun onStop() {
    super.onStop()
    // Re-assert after backgrounding so idle screensaver still launches Portal Ani.
    ScreensaverGuard.applyNow(this)
    ScreensaverGuard.scheduleBackgroundReassert(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) enterImmersive()
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(EXTRA_OAUTH_CANCELLED, false) == true) {
      vm.cancelSignIn()
      setIntent(intent.apply { removeExtra(EXTRA_OAUTH_CANCELLED) })
      return
    }
    val data: Uri = intent?.data ?: return
    if (data.scheme != "portalani") return
    vm.handleOAuthCallback(data)
  }

  private fun enterImmersive() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  companion object {
    const val EXTRA_DREAM_MODE = "dream_mode"
    const val EXTRA_OAUTH_CANCELLED = "oauth_cancelled"
  }
}
