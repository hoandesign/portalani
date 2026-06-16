package com.portal.portalani

import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SettingsStore
import android.service.dreams.DreamService
import android.content.Intent

/** Launches the slideshow when Portal idle screensaver starts. */
class AnimeDreamService : DreamService() {
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    val settings = SettingsStore(this).load()
    val active = PowerPolicy.shouldRunSlideshow(settings)
    isInteractive = active
    isFullscreen = active
    isScreenBright = active
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    val settings = SettingsStore(this).load()
    if (!PowerPolicy.shouldRunSlideshow(settings)) {
      return
    }
    val launch =
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_DREAM_MODE, true)
    startActivity(launch)
    finish()
  }

  companion object {
    const val COMPONENT = "com.portal.portalani/com.portal.portalani.AnimeDreamService"
  }
}
