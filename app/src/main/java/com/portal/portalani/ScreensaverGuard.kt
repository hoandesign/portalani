package com.portal.portalani

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SettingsStore
import java.util.concurrent.TimeUnit

/**
 * Keeps [AnimeDreamService] registered as the Portal screensaver.
 *
 * Portal's launcher rewrites `screensaver_components` to its own dream on boot. We re-apply
 * ours after boot (immediate + delayed passes) and on a 15-minute periodic worker. If another
 * screensaver app (e.g. portal-gphotos) is installed, whichever app last re-asserted wins —
 * open Portal Ani after the other app to switch back.
 */
object ScreensaverGuard {
  private const val TAG = "ScreensaverGuard"
  private const val KEY_COMPONENTS = "screensaver_components"
  private const val KEY_ENABLED = "screensaver_enabled"
  private const val KEY_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep"
  private const val WORK_NAME = "screensaver_guard"

  fun applyNow(context: Context): Boolean {
    val settings = SettingsStore(context).load()
    return applyPowerPolicy(context, settings)
  }

  /**
   * Re-assert only [AnimeDreamService] in `screensaver_components` when the slideshow should run.
   * Used by the periodic worker (same shape as portal-gphotos) so we can win races without
   * re-reading quiet-hours policy on every tick.
   */
  fun reassertComponentIfActive(context: Context): Boolean {
    val settings = SettingsStore(context).load()
    if (!PowerPolicy.shouldRunSlideshow(settings)) return true
    return reassertComponent(context)
  }

  private fun reassertComponent(context: Context): Boolean {
    return try {
      val cr = context.contentResolver
      val current = Settings.Secure.getString(cr, KEY_COMPONENTS)
      if (current != AnimeDreamService.COMPONENT) {
        Settings.Secure.putString(cr, KEY_COMPONENTS, AnimeDreamService.COMPONENT)
        Log.i(TAG, "re-asserted screensaver_components (was: $current)")
      }
      true
    } catch (e: SecurityException) {
      Log.w(
          TAG,
          "WRITE_SECURE_SETTINGS not granted — run: adb shell pm grant " +
              "com.portal.portalani android.permission.WRITE_SECURE_SETTINGS",
          e,
      )
      false
    }
  }

  /** Enable/disable the dream and re-assert our component when the slideshow should run. */
  fun applyPowerPolicy(context: Context, settings: AppSettings): Boolean {
    return try {
      val cr = context.contentResolver
      if (PowerPolicy.shouldRunSlideshow(settings)) {
        Settings.Secure.putInt(cr, KEY_ENABLED, 1)
        Settings.Secure.putInt(cr, KEY_ACTIVATE_ON_SLEEP, 1)
        reassertComponent(context)
      } else {
        Settings.Secure.putInt(cr, KEY_ENABLED, 0)
        Log.i(TAG, "quiet-hours policy: screensaver disabled")
      }
      true
    } catch (e: SecurityException) {
      Log.w(
          TAG,
          "WRITE_SECURE_SETTINGS not granted — run: adb shell pm grant " +
              "com.portal.portalani android.permission.WRITE_SECURE_SETTINGS",
          e,
      )
      false
    }
  }

  fun ensureScheduled(context: Context) {
    val request = PeriodicWorkRequestBuilder<ScreensaverGuardWorker>(5, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
  }

  /** Delayed passes after boot — the launcher usually wins the immediate race. */
  fun scheduleBootReassert(context: Context) {
    scheduleDelayedReassert(context, "${WORK_NAME}_boot")
  }

  /**
   * Delayed passes after the app backgrounds. Another screensaver app (e.g. portal-gphotos) may
   * overwrite `screensaver_components` on its own schedule — these land after we leave foreground.
   */
  fun scheduleBackgroundReassert(context: Context) {
    scheduleDelayedReassert(context, "${WORK_NAME}_bg")
  }

  private fun scheduleDelayedReassert(context: Context, namePrefix: String) {
    val wm = WorkManager.getInstance(context)
    listOf(15L, 60L, 180L).forEachIndexed { index, delaySec ->
      val request =
          OneTimeWorkRequestBuilder<ScreensaverGuardWorker>()
              .setInitialDelay(delaySec, TimeUnit.SECONDS)
              .build()
      wm.enqueueUniqueWork("${namePrefix}_$index", ExistingWorkPolicy.REPLACE, request)
    }
  }
}
