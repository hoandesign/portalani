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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R

@Composable
fun AnimeLoadingScreen(
    message: String = stringResource(R.string.loading),
    modifier: Modifier = Modifier,
) {
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
                  .aspectRatio(2f / 3f)
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

    LoadingStatusLabel(
        message = message,
        shimmer = shimmer,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
    )
  }
}

@Composable
private fun LoadingStatusLabel(
    message: String,
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "loadingPulse")
  val alpha by transition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
      label = "loadingAlpha",
  )

  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ShimmerBlock(
        brush = shimmer,
        modifier = Modifier.width(140.dp).height(10.dp),
        shape = PortalAniShapes.Pill,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = message,
        color = PortalAniColors.TextSecondary.copy(alpha = alpha),
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = PortalAniShapes.Chip,
) {
  Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
private fun rememberShimmerBrush(): Brush {
  val transition = rememberInfiniteTransition(label = "shimmer")
  val offset by transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
      label = "shimmerOffset",
  )
  val base = Color(0xFF1A1F29)
  val highlight = Color(0xFF2E3648)
  return Brush.linearGradient(
      colors = listOf(base, highlight, base),
      start = Offset(offset * 1200f - 400f, 0f),
      end = Offset(offset * 1200f + 200f, 80f),
  )
}
