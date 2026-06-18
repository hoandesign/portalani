package com.portal.portalani.vm

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.portal.portalani.AniListOAuthActivity
import com.portal.portalani.UiState
import com.portal.portalani.data.AniListAuthPort
import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.userVisibleError
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

internal class AniListSessionHandler(
    private val application: Application,
    private val scope: CoroutineScope,
    private val tokens: TokenStore,
    private val auth: AniListAuthPort,
    private val client: AniListClientPort,
    private val getState: () -> UiState,
    private val setState: (UiState) -> Unit,
    private val getSettings: () -> AppSettings,
    private val onViewerNameChanged: (String?) -> Unit,
    private val onSignedInChanged: (Boolean) -> Unit,
    private val onLaunchOAuth: (Intent) -> Unit,
    private val onBringAppToFront: () -> Unit,
    private val onOAuthSuccess: suspend () -> Unit,
    private val onCancelSignInReload: suspend () -> Unit,
) {
  private var pendingOAuthState: String? = null
  private var stateBeforeSignIn: UiState? = null

  fun signIn() {
    if (!auth.isConfigured()) {
      setState(
          UiState.NeedsSetup(
              message = missingCredentialsMessage(),
              canSignIn = false,
              canUseLibrary = true,
          ),
      )
      return
    }
    val state = UUID.randomUUID().toString()
    pendingOAuthState = state
    tokens.saveOAuthState(state)
    stateBeforeSignIn = getState().takeUnless { it is UiState.SigningIn }
    setState(UiState.SigningIn)
    val authUrl = auth.buildAuthorizeUrl(state).toString()
    val intent =
        Intent(application, AniListOAuthActivity::class.java)
            .putExtra(AniListOAuthActivity.EXTRA_AUTH_URL, authUrl)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    onLaunchOAuth(intent)
  }

  fun cancelSignIn() {
    if (getState() !is UiState.SigningIn) return
    pendingOAuthState = null
    tokens.clearOAuthState()
    val restore = stateBeforeSignIn?.takeUnless { it is UiState.SigningIn }
    stateBeforeSignIn = null
    if (restore != null) {
      setState(restore)
      return
    }
    val token = tokens.accessToken()
    val settings = getSettings()
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
      setState(
          needsSignIn(
              if (settings.frameMode == FrameMode.CALENDAR) {
                "Sign in to show your list schedule on the calendar."
              } else {
                "Sign in to show anime from your personal AniList."
              },
          ),
      )
    } else {
      scope.launch { onCancelSignInReload() }
    }
  }

  fun handleOAuthCallback(uri: Uri) {
    val callback = auth.parseCallback(uri) ?: return
    if (!callback.error.isNullOrBlank()) {
      stateBeforeSignIn = null
      setState(UiState.Error("AniList sign-in failed: ${callback.error}"))
      pendingOAuthState = null
      tokens.clearOAuthState()
      return
    }
    val expected = pendingOAuthState ?: tokens.peekOAuthState()
    pendingOAuthState = null
    tokens.clearOAuthState()
    if (expected == null || callback.state != expected) {
      stateBeforeSignIn = null
      setState(UiState.Error("Sign-in state mismatch. Please try again."))
      return
    }
    val code = callback.code
    if (code.isNullOrBlank()) {
      setState(UiState.Error("No authorization code received."))
      return
    }
    scope.launch {
      setState(UiState.Loading)
      stateBeforeSignIn = null
      try {
        withContext(Dispatchers.IO) {
          val token = auth.exchangeCode(code)
          val viewer = client.fetchViewer(token)
          tokens.saveViewer(viewer)
        }
        onViewerNameChanged(tokens.viewerName())
        onSignedInChanged(true)
        bringAppToFront()
        onOAuthSuccess()
      } catch (e: IOException) {
        setState(UiState.Error(userVisibleError(e, "Sign-in failed")))
      } catch (e: JSONException) {
        setState(UiState.Error(userVisibleError(e, "Sign-in failed")))
      }
    }
  }

  fun signOut(onAfterSignOut: () -> Unit) {
    clearSessionFromStorage()
    onAfterSignOut()
  }

  fun clearSessionFromStorage() {
    tokens.clear()
    onViewerNameChanged(null)
    onSignedInChanged(false)
  }

  fun needsSignIn(message: String) =
      UiState.NeedsSetup(
          message = message,
          canSignIn = auth.isConfigured(),
          canUseLibrary = true,
      )

  private fun bringAppToFront() {
    onBringAppToFront()
  }

  private fun missingCredentialsMessage(): String =
      "Add your AniList OAuth client to local.properties, then rebuild:\n\n" +
          "ANILIST_CLIENT_ID=...\n" +
          "ANILIST_CLIENT_SECRET=...\n\n" +
          "Redirect URI must be portalani://callback"
}
