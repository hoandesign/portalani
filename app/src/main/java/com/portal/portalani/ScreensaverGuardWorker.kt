package com.portal.portalani

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Re-applies screensaver registration after reboot or system updates. */
class ScreensaverGuardWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    ScreensaverGuard.applyNow(applicationContext)
    return Result.success()
  }

  companion object {
    fun schedule(context: Context) {
      ScreensaverGuard.ensureScheduled(context)
    }
  }
}
