package com.portal.portalani.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.data.FrameMode

enum class SlideshowGuideStep {
  Swipe,
  HoldSettings,
  TapPoster,
  Finished,
}

fun nextSlideshowGuideStep(
    current: SlideshowGuideStep,
    frameMode: FrameMode,
): SlideshowGuideStep =
    when (current) {
      SlideshowGuideStep.Swipe -> SlideshowGuideStep.HoldSettings
      SlideshowGuideStep.HoldSettings ->
          when (frameMode) {
            FrameMode.POSTER_ONLY -> SlideshowGuideStep.TapPoster
            FrameMode.CALENDAR -> SlideshowGuideStep.Finished
            FrameMode.INFORMATIVE -> SlideshowGuideStep.Finished
          }
      SlideshowGuideStep.TapPoster -> SlideshowGuideStep.Finished
      SlideshowGuideStep.Finished -> SlideshowGuideStep.Finished
    }

@Composable
fun SlideshowGuideOverlay(
    step: SlideshowGuideStep,
    frameMode: FrameMode,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
  if (step == SlideshowGuideStep.Finished || !visible) return

  val fade by
      animateFloatAsState(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
          label = "guideFade",
      )
  val pulse =
      rememberInfiniteTransition(label = "guidePulse").animateFloat(
          initialValue = 0.72f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse,
              ),
          label = "guidePulseAlpha",
      )
  val hintAlpha = fade * pulse.value

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .pointerInteropFilter { false },
  ) {
    when (step) {
      SlideshowGuideStep.Swipe -> {
        val swipeLabel =
            if (frameMode == FrameMode.CALENDAR) {
              stringResource(R.string.guide_swipe_week)
            } else {
              stringResource(R.string.guide_swipe)
            }
        GuideHintPill(
            text = swipeLabel,
            leadingIcon = PortalIcons.SwipeBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
            alpha = hintAlpha,
        )
        GuideHintPill(
            text = swipeLabel,
            trailingIcon = PortalIcons.SwipeForward,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            alpha = hintAlpha,
        )
      }
      SlideshowGuideStep.HoldSettings ->
          GuideHintPill(
              text = stringResource(R.string.guide_hold_settings),
              leadingIcon = PortalIcons.Settings,
              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
              alpha = hintAlpha,
          )
      SlideshowGuideStep.TapPoster ->
          if (frameMode == FrameMode.POSTER_ONLY) {
            GuideHintPill(
                text = stringResource(R.string.guide_tap_poster),
                leadingIcon = PortalIcons.TapPoster,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                alpha = hintAlpha,
            )
          }
      SlideshowGuideStep.Finished -> Unit
    }
  }
}

@Composable
private fun GuideHintPill(
    text: String,
    modifier: Modifier = Modifier,
    alpha: Float,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
  Surface(
      modifier = modifier.graphicsLayer { this.alpha = alpha },
      shape = PortalAniShapes.Pill,
      color = Color(0x6605070C),
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      leadingIcon?.let { icon ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PortalAniColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
      }
      Text(
          text = text,
          color = PortalAniColors.TextSecondary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
      )
      trailingIcon?.let { icon ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PortalAniColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
