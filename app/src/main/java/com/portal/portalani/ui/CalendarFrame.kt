package com.portal.portalani.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.portal.portalani.R
import com.portal.portalani.CalendarWeekState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CalendarWeek
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.WeekStart
import com.portal.portalani.data.toPlaceholderSlide
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CalendarHostScreen(
    weekState: CalendarWeekState?,
    loading: Boolean,
    weekStartSetting: WeekStart,
    settingsOpen: Boolean,
    detailSlide: AnimeSlide?,
    detailLoading: Boolean = false,
    isSignedIn: Boolean,
    onToggleSettings: () -> Unit,
    onShiftWeek: (Int) -> Unit,
    onGoToToday: () -> Unit,
    onOpenEntry: (CalendarAiringEntry) -> Unit,
    onCloseDetail: () -> Unit,
    onSetUserScore: (Int, Float?) -> Unit,
    onToggleFavourite: (Int) -> Unit,
    onSetAnimeListStatus: (Int, ListStatus) -> Unit,
    onRemoveFromList: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val swipeThresholdPx = with(density) { 64.dp.toPx() }
  val weekStart = weekState?.weekStart ?: CalendarWeek.startOfWeek(LocalDate.now(), weekStartSetting)
  val entries = weekState?.entries.orEmpty()
  var openingEntry by remember { mutableStateOf<CalendarAiringEntry?>(null) }
  var openingBounds by remember { mutableStateOf<Rect?>(null) }
  var posterExpanded by remember { mutableStateOf(false) }
  var detailClosing by remember { mutableStateOf(false) }
  val detailOverlayVisible = openingEntry != null || detailSlide != null
  var weekTransitionDirection by remember { mutableIntStateOf(0) }

  var trailerYoutubeId by remember { mutableStateOf<String?>(null) }
  var scoreDialogMediaId by remember { mutableStateOf<Int?>(null) }
  var listDialogMediaId by remember { mutableStateOf<Int?>(null) }
  var showSignInPrompt by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val interactionOpen = scoreDialogMediaId != null || listDialogMediaId != null || showSignInPrompt
  val gesturesEnabled = !settingsOpen && !interactionOpen && !detailOverlayVisible

  LaunchedEffect(openingEntry?.scheduleId) {
    val entry = openingEntry ?: return@LaunchedEffect
    posterExpanded = false
    withFrameNanos { }
    if (openingEntry?.scheduleId == entry.scheduleId) {
      posterExpanded = true
    }
  }

  LaunchedEffect(detailSlide?.id) {
    if (detailSlide != null && openingEntry?.mediaId == detailSlide.id) {
      posterExpanded = true
    }
  }

  LaunchedEffect(posterExpanded, detailClosing) {
    if (detailClosing && !posterExpanded) {
      delay(720)
      if (detailClosing && !posterExpanded) {
        detailClosing = false
        openingEntry = null
        openingBounds = null
        onCloseDetail()
      }
    }
  }

  fun shiftWeek(delta: Int) {
    weekTransitionDirection = delta
    onShiftWeek(delta)
  }

  fun goToToday() {
    weekTransitionDirection = 0
    onGoToToday()
  }

  fun withSignIn(action: () -> Unit) {
    if (isSignedIn) action() else showSignInPrompt = true
  }

  fun openSettings() {
    onUserInteraction()
    onToggleSettings()
  }

  BackHandler(enabled = detailOverlayVisible && !settingsOpen && !interactionOpen) {
    if (posterExpanded) {
      detailClosing = true
      posterExpanded = false
    }
  }

  val expandProgress by
      animateFloatAsState(
          targetValue = if (posterExpanded) 1f else 0f,
          animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
          label = "calendarDetailExpand",
      )

  var detailContentUnlocked by remember { mutableStateOf(false) }
  LaunchedEffect(expandProgress) {
    if (expandProgress >= 0.98f) {
      detailContentUnlocked = true
    }
  }
  LaunchedEffect(openingEntry?.scheduleId) {
    detailContentUnlocked = false
  }

  val posterSlide = detailSlide ?: openingEntry?.toPlaceholderSlide()
  val entryForInfo = openingEntry
  val infoSlide =
      when {
        detailContentUnlocked && detailSlide != null -> detailSlide
        entryForInfo != null -> entryForInfo.toPlaceholderSlide()
        else -> detailSlide
      }

  val calendarAlpha = if (detailOverlayVisible) (1f - expandProgress).coerceIn(0f, 1f) else 1f

  Box(modifier = modifier.fillMaxSize()) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = calendarAlpha }
                .then(
                    if (!gesturesEnabled) {
                      Modifier
                    } else {
                      Modifier.pointerInput(weekStart, swipeThresholdPx) {
                            var totalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onHorizontalDrag = { _, amount ->
                                  totalDrag += amount
                                  onUserInteraction()
                                },
                                onDragEnd = {
                                  when {
                                    totalDrag <= -swipeThresholdPx -> shiftWeek(1)
                                    totalDrag >= swipeThresholdPx -> shiftWeek(-1)
                                  }
                                  totalDrag = 0f
                                },
                                onDragCancel = { totalDrag = 0f },
                            )
                          }
                          .pointerInput(weekStart) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                  onUserInteraction()
                                  if (offset.x >= size.width * 0.2f && offset.x <= size.width * 0.8f) {
                                    openSettings()
                                  }
                                },
                                onTap = { offset ->
                                  onUserInteraction()
                                  if (offset.x < size.width * 0.2f) shiftWeek(-1)
                                  else if (offset.x > size.width * 0.8f) shiftWeek(1)
                                },
                            )
                          }
                    },
                ),
    ) {
      CalendarFrameScreen(
          weekStart = weekStart,
          entries = entries,
          loading = loading,
          weekTransitionDirection = weekTransitionDirection,
          hiddenScheduleId = if (detailOverlayVisible) openingEntry?.scheduleId else null,
          entryClicksEnabled = !detailOverlayVisible,
          onGoToToday = {
            onUserInteraction()
            goToToday()
          },
          onEntryClick = { entry, bounds ->
            onUserInteraction()
            detailClosing = false
            openingBounds = bounds
            openingEntry = entry
            posterExpanded = false
            onOpenEntry(entry)
          },
          onLongPressOpenSettings = ::openSettings,
      )
    }

    val bounds = openingBounds
    if (posterSlide != null && bounds != null && infoSlide != null) {
      CalendarDetailOverlay(
          slide = posterSlide,
          infoSlide = infoSlide,
          detailLoading = detailLoading,
          sourceBounds = bounds,
          expandProgress = expandProgress,
          onPosterToggle = {
            if (posterExpanded) {
              detailClosing = true
              posterExpanded = false
            } else {
              posterExpanded = true
            }
          },
          onLongPressOpenSettings = ::openSettings,
          onPlayTrailer = posterSlide.trailerYoutubeId?.let { id -> { trailerYoutubeId = id } },
          onOpenAniList = {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, Uri.parse(posterSlide.anilistUrl))
          },
          onTapScore = { withSignIn { scoreDialogMediaId = posterSlide.id } },
          onToggleFavourite = { withSignIn { onToggleFavourite(posterSlide.id) } },
          onEditList = { withSignIn { listDialogMediaId = posterSlide.id } },
          modifier = Modifier.fillMaxSize(),
      )
    }

    trailerYoutubeId?.let { id ->
      TrailerOverlay(youtubeId = id, onDismiss = { trailerYoutubeId = null })
    }

    scoreDialogMediaId?.let { mediaId ->
      posterSlide?.takeIf { it.id == mediaId }?.let { slide ->
        ScoreSliderDialog(
            animeTitle = slide.title,
            initialScore = slide.userScore,
            onDismiss = { scoreDialogMediaId = null },
            onSave = { score ->
              onSetUserScore(mediaId, score)
              scoreDialogMediaId = null
            },
        )
      }
    }

    listDialogMediaId?.let { mediaId ->
      posterSlide?.takeIf { it.id == mediaId }?.let { slide ->
        ListStatusDialog(
            animeTitle = slide.title,
            currentStatus = slide.listStatus,
            onDismiss = { listDialogMediaId = null },
            onSelect = { status ->
              onSetAnimeListStatus(mediaId, status)
              listDialogMediaId = null
            },
            onRemove =
                if (slide.isOnList) {
                  {
                    onRemoveFromList(mediaId)
                    listDialogMediaId = null
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
            onToggleSettings()
          },
      )
    }
  }
}

@Composable
fun CalendarFrameScreen(
    weekStart: LocalDate,
    entries: List<CalendarAiringEntry>,
    loading: Boolean,
    weekTransitionDirection: Int,
    hiddenScheduleId: Int? = null,
    entryClicksEnabled: Boolean = true,
    onGoToToday: () -> Unit,
    onEntryClick: (CalendarAiringEntry, Rect) -> Unit,
    onLongPressOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val locale = remember { Locale.getDefault() }
  val zone = remember { java.time.ZoneId.systemDefault() }
  val header = remember(weekStart, locale) { CalendarWeek.headerLabel(weekStart, locale) }
  val context = LocalContext.current
  val showGridSkeleton = loading && entries.isEmpty()
  val showRefreshOverlay = loading && entries.isNotEmpty()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(PortalAniColors.Background),
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .pointerInput(onLongPressOpenSettings) {
                  detectTapGestures(onLongPress = { onLongPressOpenSettings() })
                }
                .padding(vertical = CalendarLayout.ScreenVerticalPadding),
    ) {
      Text(
          text = header,
          color = PortalAniColors.TextPrimary,
          fontSize = 26.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.fillMaxWidth().align(Alignment.Center),
          textAlign = TextAlign.Center,
      )
      OutlinedButton(
          onClick = onGoToToday,
          modifier =
              Modifier.align(Alignment.CenterEnd)
                  .padding(end = CalendarLayout.ScreenHorizontalPadding)
                  .height(36.dp),
          shape = PortalAniShapes.Pill,
          border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.BorderStrong),
          colors =
              ButtonDefaults.outlinedButtonColors(
                  contentColor = PortalAniColors.TextPrimary,
                  containerColor = Color(0x14FFFFFF),
              ),
      ) {
        Text(
            text = stringResource(R.string.calendar_today),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
      }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .weight(1f)
                .padding(horizontal = CalendarLayout.ScreenHorizontalPadding)
                .padding(bottom = CalendarLayout.ScreenVerticalPadding),
    ) {
      Spacer(Modifier.height(CalendarLayout.HeaderBottomSpacing))

      AnimatedContent(
          targetState = weekStart,
          transitionSpec = {
          if (weekTransitionDirection == 0) {
            fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))
          } else {
            val enterOffset = { width: Int -> width / 4 * weekTransitionDirection }
            val exitOffset = { width: Int -> -width / 4 * weekTransitionDirection }
            (slideInHorizontally(
                    animationSpec = tween(420, easing = FastOutSlowInEasing),
                    initialOffsetX = enterOffset,
                ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(420, easing = FastOutSlowInEasing),
                    targetOffsetX = exitOffset,
                ) + fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)))
          }
        },
        label = "calendarWeek",
    ) { animatedWeekStart ->
      val weekDates = remember(animatedWeekStart) { CalendarWeek.weekDates(animatedWeekStart) }
      val grouped =
          remember(entries, animatedWeekStart) {
            CalendarWeek.groupByDay(entries, animatedWeekStart, zone)
          }

      Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CalendarLayout.ColumnSpacing),
        ) {
          weekDates.forEach { date ->
            CalendarDayHeader(
                dayNumber = date.dayOfMonth,
                dayLabel = CalendarWeek.dayOfWeekLabel(date, locale),
                isToday = date == LocalDate.now(zone),
                onLongPressOpenSettings = onLongPressOpenSettings,
                modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(Modifier.height(CalendarLayout.DayHeaderBottomSpacing))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
          val columnWidth = CalendarLayout.columnWidth(maxWidth)
          val posterHeight = CalendarLayout.posterHeight(columnWidth)
          val gridShape = RoundedCornerShape(CalendarLayout.GridCornerRadius)

          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .clip(gridShape)
                      .background(Color(0x2205070C)),
          ) {
            when {
              showGridSkeleton -> {
                CalendarWeekGridSkeleton(
                    modifier = Modifier.fillMaxSize(),
                    posterHeight = posterHeight,
                )
              }
              entries.isEmpty() -> {
                Text(
                    text = stringResource(R.string.calendar_empty_week),
                    color = PortalAniColors.TextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                            .pointerInput(onLongPressOpenSettings) {
                              detectTapGestures(onLongPress = { onLongPressOpenSettings() })
                            },
                )
              }
              else -> {
                val scrollState = rememberScrollState()
                Box(Modifier.fillMaxSize()) {
                  Row(
                      modifier =
                          Modifier.fillMaxSize()
                              .verticalScroll(scrollState)
                              .padding(vertical = CalendarLayout.GridVerticalPadding),
                      horizontalArrangement = Arrangement.spacedBy(CalendarLayout.ColumnSpacing),
                  ) {
                    grouped.forEach { dayEntries ->
                      Column(
                          modifier = Modifier.weight(1f),
                          verticalArrangement = Arrangement.spacedBy(CalendarLayout.CardSpacing),
                      ) {
                        dayEntries.forEach { entry ->
                          CalendarAiringCard(
                              entry = entry,
                              posterHeight = posterHeight,
                              context = context,
                              hidden = hiddenScheduleId == entry.scheduleId,
                              clicksEnabled = entryClicksEnabled,
                              onClick = { bounds -> onEntryClick(entry, bounds) },
                              onLongPressOpenSettings = onLongPressOpenSettings,
                          )
                        }
                      }
                    }
                  }
                  if (showRefreshOverlay) {
                    CalendarGridRefreshOverlay(
                        modifier = Modifier.fillMaxSize(),
                        posterHeight = posterHeight,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayHeader(
    dayNumber: Int,
    dayLabel: String,
    isToday: Boolean,
    onLongPressOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(CalendarLayout.DayHeaderCornerRadius)
  Column(
      modifier =
          modifier
              .clip(shape)
              .background(Color(0x2205070C))
              .border(1.dp, PortalAniColors.Border, shape)
              .combinedClickable(
                  indication = null,
                  interactionSource = remember { MutableInteractionSource() },
                  onClick = {},
                  onLongClick = onLongPressOpenSettings,
              )
              .padding(vertical = 6.dp, horizontal = 2.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = dayNumber.toString(),
        color = if (isToday) PortalAniColors.Score else PortalAniColors.TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        text = dayLabel,
        color = PortalAniColors.TextMuted,
        fontSize = CalendarLayout.dayOfWeekFontSize,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarAiringCard(
    entry: CalendarAiringEntry,
    posterHeight: Dp,
    context: android.content.Context,
    hidden: Boolean = false,
    clicksEnabled: Boolean = true,
    onClick: (Rect) -> Unit,
    onLongPressOpenSettings: () -> Unit,
) {
  val locale = remember { Locale.getDefault() }
  val density = LocalDensity.current
  val overlayHeight = CalendarLayout.posterOverlayHeight(posterHeight)
  val titleFontSize = CalendarLayout.posterTitleFontSize(posterHeight)
  val timeFontSize = CalendarLayout.posterTimeFontSize(posterHeight)
  val amPmFontSize = CalendarLayout.posterTimeAmPmFontSize(timeFontSize)
  val (clockLabel, periodLabel) = entry.localTimeParts(locale = locale)
  val cardShape = RoundedCornerShape(CalendarLayout.CardCornerRadius)
  var cardBounds by remember(entry.scheduleId) { mutableStateOf(Rect.Zero) }
  val thumbHeightPx = with(density) { posterHeight.roundToPx() }
  val thumbWidthPx = (thumbHeightPx * PosterLayout.AspectRatio).roundToInt()
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(posterHeight)
              .onGloballyPositioned { cardBounds = it.boundsInRoot() }
              .alpha(if (hidden) 0f else 1f)
              .clip(cardShape)
              .combinedClickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  enabled = clicksEnabled && !hidden,
                  onClick = { onClick(cardBounds) },
                  onLongClick = onLongPressOpenSettings,
              ),
  ) {
    AsyncImage(
        model =
            calendarCoverImageRequest(
                context = context,
                coverUrl = entry.coverUrl,
                mediaId = entry.mediaId,
                widthPx = thumbWidthPx,
                heightPx = thumbHeightPx,
            ),
        contentDescription = entry.englishTitle,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )

    Box(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(overlayHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.32f to Color(0x88000000),
                        1f to Color(0xE8000000),
                    ),
                ),
    )

    Column(
        modifier =
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      Text(
          text =
              buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = PortalAniColors.Cyan,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                  append(stringResource(R.string.calendar_episode_prefix, entry.episode))
                }
                append(CalendarLayout.EpisodeTitleSeparator)
                withStyle(
                    SpanStyle(
                        color = PortalAniColors.TextPrimary,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                  append(entry.englishTitle)
                }
              },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = titleFontSize * 1.12f,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 10f)),
      )
      Text(
          text =
              buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = PortalAniColors.Cyan,
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                  append(clockLabel)
                }
                if (periodLabel != null) {
                  append(" ")
                  withStyle(
                      SpanStyle(
                          color = PortalAniColors.Cyan,
                          fontSize = amPmFontSize,
                          fontWeight = FontWeight.Medium,
                      ),
                  ) {
                    append(periodLabel)
                  }
                }
              },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          lineHeight = timeFontSize * 1.05f,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 8f)),
      )
    }
  }
}
