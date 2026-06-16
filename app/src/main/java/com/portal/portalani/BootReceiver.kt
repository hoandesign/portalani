package com.portal.portalani

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    ScreensaverGuard.applyNow(context)
    ScreensaverGuardWorker.schedule(context)
    ScreensaverGuard.scheduleBootReassert(context)
  }
}
