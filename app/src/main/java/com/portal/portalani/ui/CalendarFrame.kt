package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.portal.portalani.R
import com.portal.portalani.CalendarWeekState
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.CalendarWeek
import com.portal.portalani.data.WeekStart
import java.time.LocalDate
import java.util.Locale

private val CalendarEntryHeight = 80.dp
private val CalendarVisibleRows = 3.5f

@Composable
fun CalendarHostScreen(
    weekState: CalendarWeekState?,
    loading: Boolean,
    weekStartSetting: WeekStart,
    settingsOpen: Boolean,
    onToggleSettings: () -> Unit,
    onShiftWeek: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val swipeThresholdPx = with(density) { 64.dp.toPx() }
  val weekStart = weekState?.weekStart ?: CalendarWeek.startOfWeek(LocalDate.now(), weekStartSetting)
  val entries = weekState?.entries.orEmpty()

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .then(
                  if (settingsOpen) {
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
                                  totalDrag <= -swipeThresholdPx -> onShiftWeek(1)
                                  totalDrag >= swipeThresholdPx -> onShiftWeek(-1)
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
                                  onToggleSettings()
                                }
                              },
                              onTap = { offset ->
                                onUserInteraction()
                                if (offset.x < size.width * 0.2f) onShiftWeek(-1)
                                else if (offset.x > size.width * 0.8f) onShiftWeek(1)
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
    )
  }
}

@Composable
fun CalendarFrameScreen(
    weekStart: LocalDate,
    entries: List<CalendarAiringEntry>,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
  val locale = remember { Locale.getDefault() }
  val zone = remember { java.time.ZoneId.systemDefault() }
  val weekDates = remember(weekStart) { CalendarWeek.weekDates(weekStart) }
  val grouped = remember(entries, weekStart) { CalendarWeek.groupByDay(entries, weekStart, zone) }
  val header = remember(weekStart, locale) { CalendarWeek.headerLabel(weekStart, locale) }
  val context = LocalContext.current

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(PortalAniColors.Background)
              .padding(horizontal = FrameViewerInsets.horizontal, vertical = FrameViewerInsets.vertical),
  ) {
    Text(
        text = header,
        color = PortalAniColors.TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      weekDates.forEachIndexed { index, date ->
        CalendarDayHeader(
            dayNumber = date.dayOfMonth,
            dayLabel = CalendarWeek.dayOfWeekLabel(date, locale),
            isToday = date == LocalDate.now(zone),
            modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(Modifier.height(10.dp))

    val scrollState = rememberScrollState()
    val scrollHeight = CalendarEntryHeight * CalendarVisibleRows

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .weight(1f)
                .heightIn(min = scrollHeight, max = scrollHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x3305070C))
                .border(1.dp, PortalAniColors.Border, RoundedCornerShape(12.dp)),
    ) {
      if (loading && entries.isEmpty()) {
        Text(
            text = stringResource(R.string.calendar_loading),
            color = PortalAniColors.TextMuted,
            modifier = Modifier.align(Alignment.Center),
        )
      } else if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.calendar_empty_week),
            color = PortalAniColors.TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
        )
      } else {
        Row(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          grouped.forEach { dayEntries ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              dayEntries.forEach { entry ->
                CalendarAiringCard(
                    entry = entry,
                    context = context,
                )
              }
            }
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.calendar_swipe_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CalendarDayHeader(
    dayNumber: Int,
    dayLabel: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
  val bg = if (isToday) Color(0x44E8A0FF) else Color(0x2205070C)
  val borderColor = if (isToday) PortalAniColors.Accent.copy(alpha = 0.55f) else PortalAniColors.Border
  Column(
      modifier =
          modifier
              .clip(RoundedCornerShape(10.dp))
              .background(bg)
              .border(1.dp, borderColor, RoundedCornerShape(10.dp))
              .padding(vertical = 8.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = dayNumber.toString(),
        color = PortalAniColors.TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        text = dayLabel,
        color = PortalAniColors.TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun CalendarAiringCard(
    entry: CalendarAiringEntry,
    context: android.content.Context,
) {
  val listTint = if (entry.isOnList) PortalAniColors.Accent.copy(alpha = 0.18f) else Color(0x1805070C)
  Surface(
      color = listTint,
      shape = RoundedCornerShape(10.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
      modifier = Modifier.fillMaxWidth().height(CalendarEntryHeight - 8.dp),
  ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      AsyncImage(
          model =
              ImageRequest.Builder(context)
                  .data(entry.coverUrl)
                  .crossfade(true)
                  .memoryCacheKey("cal-cover-${entry.mediaId}")
                  .build(),
          contentDescription = entry.title,
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.size(width = 36.dp, height = 52.dp)
                  .clip(PortalAniShapes.Poster)
                  .border(1.dp, Color.White.copy(alpha = 0.12f), PortalAniShapes.Poster),
      )
      Text(
          text = entry.title,
          color = PortalAniColors.TextPrimary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
      )
      Text(
          text = stringResource(R.string.calendar_episode_time, entry.episode, entry.localTimeLabel()),
          color = PortalAniColors.TextMuted,
          fontSize = 9.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
      )
    }
  }
}
