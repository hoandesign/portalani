package com.portal.portalani.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.data.FrameMode

@Composable
fun AnimeLoadingScreen(
    frameMode: FrameMode = FrameMode.POSTER_ONLY,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
  when (frameMode) {
    FrameMode.CALENDAR -> CalendarLoadingSkeleton(modifier = modifier, showHeader = true)
    FrameMode.POSTER_ONLY -> PosterLoadingSkeleton(modifier = modifier)
    FrameMode.INFORMATIVE -> InformativeLoadingSkeleton(modifier = modifier)
  }

  if (!message.isNullOrBlank()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
      Text(
          text = message,
          color = PortalAniColors.TextSecondary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(bottom = 36.dp),
      )
    }
  }
}

@Composable
fun CalendarLoadingSkeleton(
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
  val shimmer = rememberShimmerBrush()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(PortalAniColors.Background),
  ) {
    if (showHeader) {
      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(vertical = CalendarLayout.ScreenVerticalPadding),
          contentAlignment = Alignment.Center,
      ) {
        ShimmerBlock(brush = shimmer, modifier = Modifier.width(240.dp).height(28.dp), shape = PortalAniShapes.Pill)
      }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .weight(1f)
                .padding(horizontal = CalendarLayout.ScreenHorizontalPadding)
                .padding(bottom = CalendarLayout.ScreenVerticalPadding),
    ) {
      if (showHeader) {
        Spacer(Modifier.height(CalendarLayout.HeaderBottomSpacing))
      }

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(CalendarLayout.ColumnSpacing),
      ) {
        repeat(7) {
          ShimmerBlock(
              brush = shimmer,
              modifier = Modifier.weight(1f).height(54.dp),
              shape = RoundedCornerShape(CalendarLayout.DayHeaderCornerRadius),
          )
        }
      }

      Spacer(Modifier.height(CalendarLayout.DayHeaderBottomSpacing))

      BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
        val columnWidth = CalendarLayout.columnWidth(maxWidth)
        val posterHeight = CalendarLayout.posterHeight(columnWidth)
        val cardShape = RoundedCornerShape(CalendarLayout.CardCornerRadius)
        val gridShape = RoundedCornerShape(CalendarLayout.GridCornerRadius)

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .clip(gridShape)
                    .background(Color(0x2205070C)),
        ) {
          CalendarWeekGridSkeleton(
              modifier = Modifier.fillMaxSize(),
              posterHeight = posterHeight,
              cardShape = cardShape,
          )
        }
      }
    }
  }
}

@Composable
fun CalendarWeekGridSkeleton(
    modifier: Modifier = Modifier,
    posterHeight: Dp? = null,
    cardShape: RoundedCornerShape = RoundedCornerShape(CalendarLayout.CardCornerRadius),
) {
  val shimmer = rememberShimmerBrush()

  BoxWithConstraints(modifier = modifier) {
    val resolvedPosterHeight = posterHeight ?: CalendarLayout.posterHeight(CalendarLayout.columnWidth(maxWidth))
    val columnCounts = CalendarLayout.SkeletonPosterCountsPerColumn

    Row(
        modifier = Modifier.fillMaxSize().padding(vertical = CalendarLayout.GridVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(CalendarLayout.ColumnSpacing),
    ) {
      columnCounts.forEach { posterCount ->
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(CalendarLayout.CardSpacing),
        ) {
          repeat(posterCount) {
            ShimmerBlock(
                brush = shimmer,
                modifier = Modifier.fillMaxWidth().height(resolvedPosterHeight),
                shape = cardShape,
            )
          }
        }
      }
    }
  }
}

@Composable
fun PosterLoadingSkeleton(modifier: Modifier = Modifier) {
  val shimmer = rememberShimmerBrush()

  Box(
      modifier = modifier.fillMaxSize().background(PortalAniColors.Background),
      contentAlignment = Alignment.Center,
  ) {
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    horizontal = FrameViewerInsets.horizontal,
                    vertical = FrameViewerInsets.vertical,
                ),
    ) {
      val collapsedPoster = PosterLayout.fitIn(maxWidth = maxWidth, maxHeight = maxHeight)

      ShimmerBlock(
          brush = shimmer,
          modifier =
              Modifier.align(Alignment.Center)
                  .width(collapsedPoster.width)
                  .height(collapsedPoster.height),
          shape = PortalAniShapes.Poster,
      )
    }
  }
}

@Composable
fun InformativeLoadingSkeleton(modifier: Modifier = Modifier) {
  val shimmer = rememberShimmerBrush()

  Box(
      modifier = modifier.fillMaxSize().background(PortalAniColors.Background),
      contentAlignment = Alignment.Center,
  ) {
    Row(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    horizontal = FrameViewerInsets.horizontal,
                    vertical = FrameViewerInsets.vertical,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
      ShimmerBlock(
          brush = shimmer,
          modifier =
              Modifier.fillMaxHeight(0.98f)
                  .aspectRatio(PosterLayout.AspectRatio)
                  .clip(PortalAniShapes.Poster),
      )

      Column(
          modifier = Modifier.weight(1f).widthIn(max = 760.dp),
          verticalArrangement = Arrangement.Center,
      ) {
        ShimmerBlock(brush = shimmer, modifier = Modifier.fillMaxWidth(0.72f).height(34.dp))
        Spacer(Modifier.height(12.dp))
        ShimmerBlock(brush = shimmer, modifier = Modifier.fillMaxWidth(0.48f).height(20.dp))
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ShimmerBlock(brush = shimmer, modifier = Modifier.width(88.dp).height(36.dp), shape = PortalAniShapes.Pill)
          ShimmerBlock(brush = shimmer, modifier = Modifier.width(220.dp).height(36.dp), shape = PortalAniShapes.Pill)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ShimmerBlock(brush = shimmer, modifier = Modifier.width(168.dp).height(64.dp), shape = PortalAniShapes.Chip)
          ShimmerBlock(brush = shimmer, modifier = Modifier.width(168.dp).height(64.dp), shape = PortalAniShapes.Chip)
        }
        Spacer(Modifier.height(18.dp))
        repeat(3) { index ->
          ShimmerBlock(
              brush = shimmer,
              modifier = Modifier.fillMaxWidth(if (index == 2) 0.62f else 1f).height(16.dp),
          )
          if (index < 2) Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          repeat(4) {
            ShimmerBlock(brush = shimmer, modifier = Modifier.width(72.dp).height(30.dp), shape = PortalAniShapes.Pill)
          }
        }
      }
    }
  }
}

@Composable
fun CalendarGridRefreshOverlay(
    modifier: Modifier = Modifier,
    posterHeight: Dp? = null,
) {
  val shimmer = rememberShimmerBrush()

  Box(
      modifier =
          modifier
              .background(Color(0xAA0B0D12))
              .graphicsLayer { alpha = 0.92f },
  ) {
    CalendarWeekGridSkeleton(
        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.42f },
        posterHeight = posterHeight,
    )
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(shimmer)
                .graphicsLayer { alpha = 0.18f },
    )
  }
}

@Composable
fun InformativePanelLoadingSkeleton(modifier: Modifier = Modifier) {
  val shimmer = rememberShimmerBrush()

  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.Center,
  ) {
    ShimmerBlock(brush = shimmer, modifier = Modifier.fillMaxWidth(0.72f).height(34.dp))
    Spacer(Modifier.height(12.dp))
    ShimmerBlock(brush = shimmer, modifier = Modifier.fillMaxWidth(0.48f).height(20.dp))
    Spacer(Modifier.height(22.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ShimmerBlock(brush = shimmer, modifier = Modifier.width(88.dp).height(36.dp), shape = PortalAniShapes.Pill)
      ShimmerBlock(brush = shimmer, modifier = Modifier.width(220.dp).height(36.dp), shape = PortalAniShapes.Pill)
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      ShimmerBlock(brush = shimmer, modifier = Modifier.width(168.dp).height(64.dp), shape = PortalAniShapes.Chip)
      ShimmerBlock(brush = shimmer, modifier = Modifier.width(168.dp).height(64.dp), shape = PortalAniShapes.Chip)
    }
    Spacer(Modifier.height(18.dp))
    repeat(3) { index ->
      ShimmerBlock(
          brush = shimmer,
          modifier = Modifier.fillMaxWidth(if (index == 2) 0.62f else 1f).height(16.dp),
      )
      if (index < 2) Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      repeat(4) {
        ShimmerBlock(brush = shimmer, modifier = Modifier.width(72.dp).height(30.dp), shape = PortalAniShapes.Pill)
      }
    }
  }
}

@Composable
internal fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = PortalAniShapes.Chip,
) {
  Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
internal fun rememberShimmerBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "shimmer")
  val offset by transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
      label = "shimmerOffset",
  )
  val base = Color(0xFF1A1F29)
  val highlight = Color(0xFF4E5F7A)
  return Brush.linearGradient(
      colors = listOf(base, highlight, base),
      start = Offset(offset * 1200f - 400f, 0f),
      end = Offset(offset * 1200f + 200f, 80f),
  )
}
