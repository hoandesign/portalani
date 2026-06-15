package com.portal.portalani

import android.content.Context
import android.provider.Settings
import android.service.dreams.DreamService
import android.util.Log
import android.content.Intent

/** Launches the slideshow when Portal idle screensaver starts. */
class AnimeDreamService : DreamService() {
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isInteractive = true
    isFullscreen = true
    isScreenBright = true
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    val launch =
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_DREAM_MODE, true)
    startActivity(launch)
    finish()
  }

  companion object {
    private const val TAG = "AnimeDreamService"
    const val COMPONENT = "com.portal.portalani/com.portal.portalani.AnimeDreamService"
    private const val KEY_COMPONENTS = "screensaver_components"
    private const val KEY_ENABLED = "screensaver_enabled"
    private const val KEY_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep"

    fun setAsDefaultScreensaver(context: Context): Boolean {
      return try {
        val cr = context.contentResolver
        Settings.Secure.putString(cr, KEY_COMPONENTS, COMPONENT)
        Settings.Secure.putInt(cr, KEY_ENABLED, 1)
        Settings.Secure.putInt(cr, KEY_ACTIVATE_ON_SLEEP, 1)
        true
      } catch (e: SecurityException) {
        Log.w(TAG, "WRITE_SECURE_SETTINGS not granted", e)
        false
      }
    }
  }
}
