package com.portal.portalani.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.portal.portalani.data.AnimeSlide
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDetailOverlay(
    slide: AnimeSlide,
    sourceBounds: Rect,
    expandProgress: Float,
    onPosterToggle: () -> Unit,
    onLongPressOpenSettings: () -> Unit,
    onPlayTrailer: (() -> Unit)?,
    onOpenAniList: (() -> Unit)?,
    onTapScore: (() -> Unit)?,
    onToggleFavourite: (() -> Unit)?,
    onEditList: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  var targetBounds by remember(slide.id) { mutableStateOf<Rect?>(null) }

  Box(modifier = modifier.fillMaxSize().longPressWithoutConsumingTaps(onLongPressOpenSettings)) {
    // Absorb taps on empty overlay areas so they don't reach the calendar grid underneath.
    Box(
        modifier =
            Modifier.fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    )

    if (expandProgress > 0.02f) {
      Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = expandProgress.coerceIn(0f, 1f) }) {
        SlideParallaxBackground(
            slide = slide,
            context = context,
            enter = 1f,
            bgScale = 1.22f,
            bgAlpha = 0.62f,
            bgDriftX = 0f,
            bgDriftY = 0f,
            posterOnly = false,
        )
      }
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
                .padding(FrameViewerInsets.detailContentPadding),
    ) {
      val collapsedPoster = PosterLayout.fitIn(maxWidth = maxWidth, maxHeight = maxHeight)
      val targetPosterHeight = collapsedPoster.height * FrameViewerInsets.expandedPosterHeightFraction
      val targetPosterWidth = PosterLayout.widthForHeight(targetPosterHeight)

      Box(
          modifier =
              Modifier.align(Alignment.CenterStart)
                  .size(targetPosterWidth, targetPosterHeight)
                  .graphicsLayer { alpha = 0f }
                  .onGloballyPositioned { targetBounds = it.boundsInRoot() },
      )

      if (expandProgress > 0.04f) {
        val infoAlpha = ((expandProgress - 0.22f) / 0.78f).coerceIn(0f, 1f)
        AnimeInfoPanel(
            slide = slide,
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .width(
                        (maxWidth - targetPosterWidth - FrameViewerInsets.posterInfoGap)
                            .coerceAtLeast(280.dp),
                    )
                    .fillMaxHeight()
                    .graphicsLayer {
                      alpha = infoAlpha
                      translationX = (1f - expandProgress) * 56f
                      translationY = (1f - expandProgress) * 18f
                    },
            onPlayTrailer = onPlayTrailer,
            onOpenAniList = onOpenAniList,
            onTapScore = onTapScore,
            onToggleFavourite = onToggleFavourite,
            onEditList = onEditList,
        )
      }
    }

    val destination = targetBounds
    if (destination != null) {
      val progress = expandProgress.coerceIn(0f, 1f)
      val currentLeft = sourceBounds.left + (destination.left - sourceBounds.left) * progress
      val currentTop = sourceBounds.top + (destination.top - sourceBounds.top) * progress
      val currentWidth = sourceBounds.width + (destination.width - sourceBounds.width) * progress
      val currentHeight = sourceBounds.height + (destination.height - sourceBounds.height) * progress
      val cornerRadius = lerp(CalendarLayout.CardCornerRadius, 12.dp, progress)
      val posterShape = RoundedCornerShape(cornerRadius)
      val shadowElevation = lerp(8.dp, 24.dp, progress)
      val thumbWidthPx = remember(slide.id) { sourceBounds.width.roundToInt().coerceAtLeast(1) }
      val thumbHeightPx = remember(slide.id) { sourceBounds.height.roundToInt().coerceAtLeast(1) }
      val (detailWidthPx, detailHeightPx) =
          remember(slide.id, destination) {
            detailCoverSizePx(destination.width, destination.height)
          }

      Box(
          modifier =
              Modifier.offset { IntOffset(currentLeft.roundToInt(), currentTop.roundToInt()) }
                  .size(
                      width = with(density) { currentWidth.toDp() },
                      height = with(density) { currentHeight.toDp() },
                  )
                  .shadow(
                      elevation = shadowElevation,
                      shape = posterShape,
                      clip = true,
                      ambientColor = Color.Black,
                      spotColor = Color.Black,
                  )
                  .clip(posterShape)
                  .border(1.5.dp, Color.White.copy(alpha = 0.12f + (0.08f * progress)), posterShape)
                  .combinedClickable(
                      indication = null,
                      interactionSource = remember(slide.id) { MutableInteractionSource() },
                      onClick = onPosterToggle,
                      onLongClick = onLongPressOpenSettings,
                  ),
      ) {
        CalendarDetailPosterImage(
            slide = slide,
            thumbWidthPx = thumbWidthPx,
            thumbHeightPx = thumbHeightPx,
            detailWidthPx = detailWidthPx,
            detailHeightPx = detailHeightPx,
            modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
