package com.portal.portalani.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/** Calendar grid spacing and sizing — posters use [PosterLayout] (460 × 610). */
object CalendarLayout {
  val ColumnSpacing = 6.dp
  val CardSpacing = 8.dp
  val GridCornerRadius = 12.dp
  val CardCornerRadius = 8.dp
  val DayHeaderCornerRadius = 10.dp

  /** Screen edge inset matches column gutter so the week grid uses full width. */
  val ScreenHorizontalPadding = ColumnSpacing
  val ScreenVerticalPadding = 8.dp
  val HeaderBottomSpacing = 10.dp
  val DayHeaderBottomSpacing = 8.dp
  val GridVerticalPadding = 6.dp

  /** Poster placeholder count per weekday column in loading skeleton (Sun–Sat). */
  val SkeletonPosterCountsPerColumn = listOf(2, 3, 2, 1, 2, 3, 2)

  val screenPadding =
      PaddingValues(
          horizontal = ScreenHorizontalPadding,
          vertical = ScreenVerticalPadding,
      )

  fun posterHeight(columnWidth: Dp): Dp = PosterLayout.heightForWidth(columnWidth)

  fun columnWidth(gridWidth: Dp, columnCount: Int = 7): Dp =
      (gridWidth - ColumnSpacing * (columnCount - 1)) / columnCount

  fun posterOverlayHeight(posterHeight: Dp): Dp = (posterHeight * 0.4f).coerceIn(96.dp, 148.dp)

  fun posterTitleFontSize(posterHeight: Dp): TextUnit =
      (posterHeight.value * 0.05f).coerceIn(13f, 16f).sp

  val dayOfWeekFontSize = 13.sp

  fun posterTimeFontSize(posterHeight: Dp): TextUnit =
      (posterHeight.value * 0.1f).coerceIn(22f, 32f).sp

  fun posterTimeAmPmFontSize(timeFontSize: TextUnit): TextUnit = timeFontSize * 0.58f
}

fun Modifier.calendarScreenPadding(): Modifier = padding(CalendarLayout.screenPadding)
