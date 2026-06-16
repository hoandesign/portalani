package com.portal.portalani.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal fun isForwardSlideTransition(fromIndex: Int, toIndex: Int, slideCount: Int): Boolean {
  if (slideCount <= 1 || fromIndex == toIndex) return true
  if (fromIndex == slideCount - 1 && toIndex == 0) return true
  if (fromIndex == 0 && toIndex == slideCount - 1) return false
  return toIndex > fromIndex
}

internal fun AnimatedContentTransitionScope<Int>.animeSlideTransition(
    slideCount: Int,
): ContentTransform {
  val forward = isForwardSlideTransition(initialState, targetState, slideCount)
  val direction = if (forward) 1 else -1
  val enterMs = 950
  val exitMs = 650
  val easing = FastOutSlowInEasing

  val enter =
      fadeIn(animationSpec = tween(enterMs, easing = easing)) +
          slideInHorizontally(
              animationSpec = tween(enterMs, easing = easing),
              initialOffsetX = { fullWidth -> (fullWidth * 0.14f * direction).toInt() },
          ) +
          scaleIn(
              initialScale = 0.94f,
              animationSpec = tween(enterMs, easing = easing),
          )

  val exit =
      fadeOut(animationSpec = tween(exitMs, easing = easing)) +
          slideOutHorizontally(
              animationSpec = tween(exitMs, easing = easing),
              targetOffsetX = { fullWidth -> (-fullWidth * 0.1f * direction).toInt() },
          ) +
          scaleOut(
              targetScale = 1.035f,
              animationSpec = tween(exitMs, easing = easing),
          )

  return enter togetherWith exit using SizeTransform(clip = false) { _, _ -> tween(0) }
}

@Composable
internal fun AnimatedSlideHost(
    slideIndex: Int,
    slideCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
  AnimatedContent(
      targetState = slideIndex,
      modifier = modifier,
      contentKey = { it },
      transitionSpec = { animeSlideTransition(slideCount) },
      label = "anime-slide",
  ) { idx ->
    content(idx)
  }
}
