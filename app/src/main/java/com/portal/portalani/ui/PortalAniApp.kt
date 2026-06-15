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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerMode
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SeasonSelection
import com.portal.portalani.data.SourceMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun PortalAniApp(
    state: UiState,
    settings: AppSettings,
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
    onSetIntervalSeconds: (Int) -> Unit,
    onSetSourceMode: (SourceMode) -> Unit,
    onSetListStatus: (ListStatus) -> Unit,
    onSetFormatFilter: (FormatFilter) -> Unit,
    onSetLibrarySort: (LibrarySort) -> Unit,
    onSetSeasonKey: (String) -> Unit,
    onSetPowerMode: (PowerMode) -> Unit = {},
    onSetIdleSleepMinutes: (Int) -> Unit = {},
    onSetSleepStartMinutes: (Int) -> Unit = {},
    onSetSleepEndMinutes: (Int) -> Unit = {},
    onSlideIndexChanged: (Int) -> Unit = {},
    onUserInteraction: () -> Unit = {},
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
        UiState.Loading -> CenterMessage(stringResource(R.string.loading))
        UiState.SigningIn -> CenterMessage(stringResource(R.string.sign_in_hint))
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
            SlideshowScreen(
                slides = state.slides,
                fromCache = state.fromCache,
                orderResetToken = state.orderResetToken,
                shuffle = settings.shuffle,
                intervalMs = settings.intervalMs,
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
            )
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
          viewerName = viewerName,
          onDismiss = { showSettings = false },
          onSignIn = onSignIn,
          onSignOut = onSignOut,
          onSetShuffle = onSetShuffle,
          onSetIntervalSeconds = onSetIntervalSeconds,
          onSetSourceMode = onSetSourceMode,
          onSetListStatus = onSetListStatus,
          onSetFormatFilter = onSetFormatFilter,
          onSetLibrarySort = onSetLibrarySort,
          onSetSeasonKey = onSetSeasonKey,
          onSetPowerMode = onSetPowerMode,
          onSetIdleSleepMinutes = onSetIdleSleepMinutes,
          onSetSleepStartMinutes = onSetSleepStartMinutes,
          onSetSleepEndMinutes = onSetSleepEndMinutes,
          onUserInteraction = onUserInteraction,
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
          modifier = Modifier.width(320.dp),
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
  var appInForeground by remember { mutableStateOf(true) }
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  if (order.isEmpty()) return

  val safeIndex = index.coerceIn(0, order.lastIndex)
  val interactionOpen = scoreDialogMediaId != null || listDialogMediaId != null || showSignInPrompt

  LaunchedEffect(safeIndex, order.size) {
    onSlideIndexChanged(safeIndex)
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
      trailerYoutubeId != null || settingsOpen || !appInForeground || interactionOpen

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
    index = (index + 1) % order.size
    restartSlideTimer()
  }

  fun previous() {
    if (order.isEmpty()) return
    index = if (index == 0) order.size - 1 else index - 1
    restartSlideTimer()
  }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .pointerInput(order.size, safeIndex) {
                detectHorizontalDragGestures { _, drag ->
                  onUserInteraction()
                  if (drag < -45f) next()
                  if (drag > 45f) previous()
                }
              }
              .pointerInput(Unit) {
                detectTapGestures { offset ->
                  onUserInteraction()
                  if (offset.x < size.width * 0.2f) previous()
                  else if (offset.x > size.width * 0.8f) next()
                  else onToggleSettings()
                }
              },
  ) {
    AnimatedSlideHost(
        slideIndex = safeIndex,
        slideCount = order.size,
        modifier = Modifier.fillMaxSize(),
    ) { idx ->
      val slide = order.getOrNull(idx) ?: return@AnimatedSlideHost
      AnimeFrameSlide(
          slide = slide,
          isSignedIn = isSignedIn,
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
            title = stringResource(R.string.score_dialog_title, slide.title),
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
            title = stringResource(R.string.list_dialog_title, slide.title),
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
    viewerName: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    onSetIntervalSeconds: (Int) -> Unit,
    onSetSourceMode: (SourceMode) -> Unit,
    onSetListStatus: (ListStatus) -> Unit,
    onSetFormatFilter: (FormatFilter) -> Unit,
    onSetLibrarySort: (LibrarySort) -> Unit,
    onSetSeasonKey: (String) -> Unit,
    onSetPowerMode: (PowerMode) -> Unit,
    onSetIdleSleepMinutes: (Int) -> Unit,
    onSetSleepStartMinutes: (Int) -> Unit,
    onSetSleepEndMinutes: (Int) -> Unit,
    onUserInteraction: () -> Unit,
) {
  var statusMenuOpen by remember { mutableStateOf(false) }
  var formatMenuOpen by remember { mutableStateOf(false) }
  var sortMenuOpen by remember { mutableStateOf(false) }
  var seasonMenuOpen by remember { mutableStateOf(false) }
  var sleepStartMenuOpen by remember { mutableStateOf(false) }
  var sleepEndMenuOpen by remember { mutableStateOf(false) }
  val intervalSeconds = (settings.intervalMs / 1000L).toInt()
  val listStatusOptions = ListStatus.entries.map { status -> status.name to statusLabel(status) }
  val formatOptions = FormatFilter.entries.map { format -> format.name to format.label }
  val sortOptions = LibrarySort.entries.map { sort -> sort.name to sort.label }
  val anyMenuOpen =
      statusMenuOpen || formatMenuOpen || sortMenuOpen || seasonMenuOpen || sleepStartMenuOpen || sleepEndMenuOpen

  BackHandler(enabled = anyMenuOpen) {
    statusMenuOpen = false
    formatMenuOpen = false
    sortMenuOpen = false
    seasonMenuOpen = false
    sleepStartMenuOpen = false
    sleepEndMenuOpen = false
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
                .widthIn(max = 840.dp)
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

      SettingsSectionTitle(stringResource(R.string.source_mode))
      Spacer(Modifier.height(10.dp))
      PortalSegmentedControl(
          options =
              listOf(
                  stringResource(R.string.source_personal),
                  stringResource(R.string.source_library),
              ),
          selectedIndex = if (settings.sourceMode == SourceMode.PERSONAL) 0 else 1,
          onSelect = { index ->
            onSetSourceMode(if (index == 0) SourceMode.PERSONAL else SourceMode.LIBRARY)
          },
      )

      Spacer(Modifier.height(24.dp))

      if (settings.sourceMode == SourceMode.PERSONAL) {
        if (viewerName != null) {
          Text(
              stringResource(R.string.signed_in_as, viewerName),
              color = PortalAniColors.TextSecondary,
              fontSize = 17.sp,
          )
          Spacer(Modifier.height(8.dp))
          TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out), color = PortalAniColors.Accent)
          }
        } else {
          PortalPrimaryButton(
              text = stringResource(R.string.sign_in),
              onClick = onSignIn,
              modifier = Modifier.fillMaxWidth(),
          )
        }
        Spacer(Modifier.height(18.dp))
        PortalFilterField(
            label = stringResource(R.string.list_status),
            value = statusLabel(settings.listStatus),
            onClick = { statusMenuOpen = true },
        )
      } else {
        Text(
            stringResource(R.string.library_filters),
            color = PortalAniColors.TextMuted,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(14.dp))

        PortalFilterField(
            label = stringResource(R.string.format_filter),
            value = settings.formatFilter.label,
            onClick = { formatMenuOpen = true },
        )

        Spacer(Modifier.height(12.dp))
        PortalFilterField(
            label = stringResource(R.string.sort_by),
            value = settings.librarySort.label,
            onClick = { sortMenuOpen = true },
        )

        Spacer(Modifier.height(12.dp))
        PortalFilterField(
            label = stringResource(R.string.season_filter),
            value = SeasonSelection.labelFor(settings.seasonKey),
            onClick = { seasonMenuOpen = true },
        )
      }

      Spacer(Modifier.height(24.dp))
      SettingsSectionTitle(stringResource(R.string.power_settings))
      Spacer(Modifier.height(8.dp))
      Text(
          stringResource(R.string.power_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 15.sp,
          lineHeight = 21.sp,
      )
      Spacer(Modifier.height(12.dp))
      PortalSegmentedControl(
          options =
              listOf(
                  stringResource(R.string.power_always_on),
                  stringResource(R.string.power_idle_sleep),
                  stringResource(R.string.power_scheduled_sleep),
              ),
          selectedIndex =
              when (settings.powerMode) {
                PowerMode.ALWAYS_ON -> 0
                PowerMode.IDLE_SLEEP -> 1
                PowerMode.SCHEDULED_SLEEP -> 2
              },
          onSelect = { index ->
            onUserInteraction()
            onSetPowerMode(
                when (index) {
                  0 -> PowerMode.ALWAYS_ON
                  1 -> PowerMode.IDLE_SLEEP
                  else -> PowerMode.SCHEDULED_SLEEP
                },
            )
          },
      )

      if (settings.powerMode == PowerMode.IDLE_SLEEP) {
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.power_idle_after),
            color = PortalAniColors.TextSecondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PowerPolicy.IDLE_SLEEP_OPTIONS_MINUTES.take(4).forEach { minutes ->
              PortalChoiceChip(
                  label = PowerPolicy.formatIdleDuration(minutes),
                  selected = settings.idleSleepMinutes == minutes,
                  onClick = {
                    onUserInteraction()
                    onSetIdleSleepMinutes(minutes)
                  },
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PowerPolicy.IDLE_SLEEP_OPTIONS_MINUTES.drop(4).forEach { minutes ->
              PortalChoiceChip(
                  label = PowerPolicy.formatIdleDuration(minutes),
                  selected = settings.idleSleepMinutes == minutes,
                  onClick = {
                    onUserInteraction()
                    onSetIdleSleepMinutes(minutes)
                  },
              )
            }
          }
        }
      }

      if (settings.powerMode == PowerMode.SCHEDULED_SLEEP) {
        Spacer(Modifier.height(14.dp))
        PortalFilterField(
            label = stringResource(R.string.power_sleep_from),
            value = PowerPolicy.formatMinutesOfDay(settings.sleepStartMinutes),
            onClick = {
              onUserInteraction()
              sleepStartMenuOpen = true
            },
        )
        Spacer(Modifier.height(12.dp))
        PortalFilterField(
            label = stringResource(R.string.power_wake_at),
            value = PowerPolicy.formatMinutesOfDay(settings.sleepEndMinutes),
            onClick = {
              onUserInteraction()
              sleepEndMenuOpen = true
            },
        )
      }

      Spacer(Modifier.height(24.dp))
      SettingRow(stringResource(R.string.shuffle)) {
        Switch(
            checked = settings.shuffle,
            onCheckedChange = {
              onUserInteraction()
              onSetShuffle(it)
            },
            colors =
                androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PortalAniColors.Accent,
                    uncheckedThumbColor = PortalAniColors.TextSecondary,
                    uncheckedTrackColor = PortalAniColors.SurfaceElevated,
                ),
        )
      }

      Spacer(Modifier.height(12.dp))
      SettingsSectionTitle(stringResource(R.string.interval_seconds))
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(8, 12, 20, 30).forEach { seconds ->
          PortalChoiceChip(
              label = "$seconds s",
              selected = intervalSeconds == seconds,
              onClick = {
                onUserInteraction()
                onSetIntervalSeconds(seconds)
              },
          )
        }
      }

      Spacer(Modifier.height(12.dp))
      }
    }
  }

  if (statusMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.list_status),
        options = listStatusOptions,
        selectedKey = settings.listStatus.name,
        onDismiss = { statusMenuOpen = false },
        onSelect = { key -> onSetListStatus(ListStatus.valueOf(key)) },
    )
  }

  if (formatMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.format_filter),
        options = formatOptions,
        selectedKey = settings.formatFilter.name,
        onDismiss = { formatMenuOpen = false },
        onSelect = { key -> onSetFormatFilter(FormatFilter.valueOf(key)) },
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
}

@Composable
private fun SettingsSectionTitle(text: String) {
  Text(
      text = text,
      color = PortalAniColors.TextPrimary,
      fontSize = 17.sp,
      fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, color = PortalAniColors.TextPrimary, fontSize = 18.sp)
    control()
  }
}

@Composable
private fun statusLabel(status: ListStatus): String =
    when (status) {
      ListStatus.CURRENT -> stringResource(R.string.status_current)
      ListStatus.PLANNING -> stringResource(R.string.status_planning)
      ListStatus.COMPLETED -> stringResource(R.string.status_completed)
      ListStatus.PAUSED -> stringResource(R.string.status_paused)
      ListStatus.DROPPED -> stringResource(R.string.status_dropped)
      ListStatus.REPEATING -> stringResource(R.string.status_repeating)
    }

private fun buildSlideOrder(ids: List<Int>, shuffle: Boolean, seed: Int): List<Int> =
    if (shuffle) {
      ids.shuffled(Random((seed to ids.size).hashCode().toLong()))
    } else {
      ids
    }
