package com.portal.portalani.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.PowerPolicy
import kotlinx.coroutines.delay

@Composable
fun PowerScreenEffect(
    settings: AppSettings,
    lastUserInteractionMs: Long,
    enabled: Boolean = true,
) {
  val view = LocalView.current
  LaunchedEffect(
      enabled,
      settings.powerMode,
      settings.idleSleepMinutes,
      settings.sleepStartMinutes,
      settings.sleepEndMinutes,
      lastUserInteractionMs,
  ) {
    val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
    if (!enabled) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      return@LaunchedEffect
    }
    while (true) {
      val keepOn = PowerPolicy.shouldKeepScreenOn(settings, lastUserInteractionMs)
      if (keepOn) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      delay(15_000)
    }
  }
}
