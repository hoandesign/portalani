package com.portal.portalani.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

/** Fires [onLongPress] without stealing short taps from child clickables. */
internal fun Modifier.longPressWithoutConsumingTaps(onLongPress: () -> Unit): Modifier =
    pointerInput(onLongPress) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        val start = down.position
        val slop = viewConfiguration.touchSlop
        val deadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
        var fired = false
        while (true) {
          val event = awaitPointerEvent(pass = PointerEventPass.Initial)
          val change = event.changes.firstOrNull { it.id == pointerId } ?: break
          if (!change.pressed) break
          val moved = hypot(change.position.x - start.x, change.position.y - start.y)
          if (moved > slop) break
          if (!fired && change.uptimeMillis >= deadline) {
            fired = true
            onLongPress()
          }
        }
      }
    }
