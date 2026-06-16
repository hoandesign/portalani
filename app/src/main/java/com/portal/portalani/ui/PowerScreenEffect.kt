package com.portal.portalani.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import com.portal.portalani.ScreensaverGuard
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.PowerPolicy
import kotlinx.coroutines.delay

@Composable
fun PowerScreenEffect(
    settings: AppSettings,
    lastUserInteractionMs: Long,
) {
  val view = LocalView.current
  LaunchedEffect(
      settings.powerMode,
      settings.idleSleepMinutes,
      settings.sleepStartMinutes,
      settings.sleepEndMinutes,
  ) {
    val activity = view.context as? Activity ?: return@LaunchedEffect
    ScreensaverGuard.applyPowerPolicy(activity, settings)
  }

  LaunchedEffect(
      settings.powerMode,
      settings.idleSleepMinutes,
      settings.sleepStartMinutes,
      settings.sleepEndMinutes,
      lastUserInteractionMs,
  ) {
    val activity = view.context as? Activity ?: return@LaunchedEffect
    val window = activity.window
    var wasKeepingOn = PowerPolicy.shouldKeepScreenOn(settings, lastUserInteractionMs)
    if (wasKeepingOn) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    while (true) {
      delay(15_000)
      val keepOn = PowerPolicy.shouldKeepScreenOn(settings, lastUserInteractionMs)
      if (keepOn) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (wasKeepingOn) {
          activity.moveTaskToBack(true)
        }
      }
      wasKeepingOn = keepOn
    }
  }
}
