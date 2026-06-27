package com.portal.portalani.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.portal.portalani.R
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.WeatherNow
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
internal fun SlideshowScreen(
    slides: List<AnimeSlide>,
    fromCache: Boolean,
    isRefreshing: Boolean = false,
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
    onShowRelatedAnime: (Int, String) -> Unit,
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

  if (order.isEmpty()) {
    AnimeLoadingScreen(frameMode = frameMode)
    return
  }

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
          onShowRelated = { onShowRelatedAnime(slide.id, slide.title) },
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

    if (fromCache && !isRefreshing) {
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

    if (isRefreshing) {
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .graphicsLayer { alpha = 0.92f },
      ) {
        when (frameMode) {
          FrameMode.POSTER_ONLY -> PosterLoadingSkeleton(modifier = Modifier.fillMaxSize())
          FrameMode.INFORMATIVE -> InformativeLoadingSkeleton(modifier = Modifier.fillMaxSize())
          FrameMode.CALENDAR -> Unit
        }
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

private fun buildSlideOrder(ids: List<Int>, shuffle: Boolean, seed: Int): List<Int> =
    if (shuffle) {
      ids.shuffled(Random((seed to ids.size).hashCode().toLong()))
    } else {
      ids
    }
