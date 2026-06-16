package com.portal.portalani.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Slow Ken-Burns drift. Pauses while poster detail is open (flip) but keeps the last position so the
 * background does not snap back to center.
 */
@Composable
fun rememberPosterParallaxOffsets(enabled: Boolean, slideId: Int): Pair<Float, Float> {
  val driftX = remember(slideId) { Animatable(-24f) }
  val driftY = remember(slideId) { Animatable(-14f) }

  LaunchedEffect(slideId) {
    driftX.snapTo(-24f)
    driftY.snapTo(-14f)
  }

  LaunchedEffect(slideId, enabled) {
    if (!enabled) return@LaunchedEffect
    while (true) {
      driftX.animateTo(24f, tween(28_000, easing = FastOutSlowInEasing))
      driftX.animateTo(-24f, tween(28_000, easing = FastOutSlowInEasing))
    }
  }

  LaunchedEffect(slideId, enabled) {
    if (!enabled) return@LaunchedEffect
    while (true) {
      driftY.animateTo(14f, tween(34_000, easing = FastOutSlowInEasing))
      driftY.animateTo(-14f, tween(34_000, easing = FastOutSlowInEasing))
    }
  }

  return driftX.value to driftY.value
}
