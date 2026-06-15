package com.portal.portalani

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal.portalani.ui.PortalAniApp
import com.portal.portalani.ui.PortalAniTheme

class MainActivity : ComponentActivity() {
  private val vm: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enterImmersive()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            moveTaskToBack(true)
          }
        },
    )

    setContent {
      PortalAniTheme {
        val state by vm.state.collectAsStateWithLifecycle()
        val settings by vm.settings.collectAsStateWithLifecycle()
        val viewerName by vm.viewerName.collectAsStateWithLifecycle()
        val isSignedIn by vm.isSignedIn.collectAsStateWithLifecycle()
        val userMessage by vm.userMessage.collectAsStateWithLifecycle()

        PortalAniApp(
          state = state,
          settings = settings,
          viewerName = viewerName,
          isSignedIn = isSignedIn,
          userMessage = userMessage,
          onSignIn = vm::signIn,
          onSignOut = vm::signOut,
          onRetry = vm::refresh,
          onUseLibrary = vm::useLibrary,
          onClearUserMessage = vm::clearUserMessage,
          onSetUserScore = vm::setUserScore,
          onToggleFavourite = vm::toggleFavourite,
          onSetAnimeListStatus = vm::setAnimeListStatus,
          onRemoveFromList = vm::removeFromList,
          onSetShuffle = vm::setShuffle,
          onSetIntervalSeconds = vm::setIntervalSeconds,
          onSetSourceMode = vm::setSourceMode,
          onSetListStatus = vm::setListStatus,
          onSetFormatFilter = vm::setFormatFilter,
          onSetLibrarySort = vm::setLibrarySort,
          onSetSeasonKey = vm::setSeasonKey,
        )
      }
    }

    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) enterImmersive()
  }

  private fun handleIntent(intent: Intent?) {
    val data: Uri = intent?.data ?: return
    if (data.scheme != "portalani") return
    vm.handleOAuthCallback(data)
  }

  private fun enterImmersive() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  companion object {
    const val EXTRA_DREAM_MODE = "dream_mode"
  }
}
