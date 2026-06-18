package com.portal.portalani.vm

import android.app.Application
import android.content.Intent
import android.net.Uri
import com.portal.portalani.AniListOAuthActivity
import com.portal.portalani.UiState
import com.portal.portalani.data.AniListAuthPort
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.ViewerProfile
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class AniListSessionHandlerTest {
  @get:Rule
  val rules: RuleChain = RuleChain.outerRule(InstantTaskExecutorRule()).around(CoroutineTestRule())

  private lateinit var app: Application
  private lateinit var testScope: CoroutineScope
  private var rootState: UiState = UiState.Loading
  private var settings: AppSettings = AppSettings()
  private var viewerName: String? = null
  private var signedIn: Boolean = false
  private var launchedOAuth: Intent? = null
  private var broughtToFront = false
  private var oauthSuccessCalls = 0

  @Before
  fun setUp() {
    app = RuntimeEnvironment.getApplication()
    testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    rootState = UiState.Loading
    settings = AppSettings()
    viewerName = null
    signedIn = false
    launchedOAuth = null
    broughtToFront = false
    oauthSuccessCalls = 0
    SettingsStore(app).save(settings)
    TokenStore(app).clear()
  }

  private fun createHandler(
      fakeAuth: AniListAuthPort = FakeAniListAuth(),
      fakeClient: FakeAniListClient = FakeAniListClient(),
      initialState: UiState = UiState.Loading,
  ): AniListSessionHandler {
    rootState = initialState
    val tokens = TokenStore(app)
    return AniListSessionHandler(
        application = app,
        scope = testScope,
        tokens = tokens,
        auth = fakeAuth,
        client = fakeClient,
        getState = { rootState },
        setState = { rootState = it },
        getSettings = { settings },
        onViewerNameChanged = { viewerName = it },
        onSignedInChanged = { signedIn = it },
        onLaunchOAuth = { launchedOAuth = it },
        onBringAppToFront = { broughtToFront = true },
        onOAuthSuccess = { oauthSuccessCalls++ },
        onCancelSignInReload = {},
    )
  }

  private fun <T : UiState> awaitState(klass: Class<T>, timeoutMs: Long = 3000): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (klass.isInstance(rootState)) {
        @Suppress("UNCHECKED_CAST")
        return rootState as T
      }
      Thread.sleep(10)
    }
    error("Timed out waiting for ${klass.simpleName}, last=$rootState")
  }

  @Test
  fun signIn_notConfigured_showsNeedsSetupWithoutLaunchingOAuth() {
    val handler = createHandler(fakeAuth = FakeAniListAuth(configured = false))
    handler.signIn()
    val state = rootState as UiState.NeedsSetup
    assertFalse(state.canSignIn)
    assertTrue(state.message.contains("local.properties"))
    assertNull(launchedOAuth)
  }

  @Test
  fun signIn_configured_setsSigningInAndLaunchesOAuth() {
    val handler = createHandler(initialState = UiState.NeedsSetup("Sign in", canSignIn = true, canUseLibrary = true))
    handler.signIn()
    assertTrue(rootState is UiState.SigningIn)
    assertNotNull(launchedOAuth)
    assertTrue(launchedOAuth!!.getStringExtra(AniListOAuthActivity.EXTRA_AUTH_URL)?.contains("anilist.co") == true)
  }

  @Test
  fun cancelSignIn_restoresNeedsSetup() {
    settings = AppSettings(sourceMode = SourceMode.PERSONAL)
    val setup = UiState.NeedsSetup("Sign in to show anime from your personal AniList.", canSignIn = true, canUseLibrary = true)
    val handler = createHandler(initialState = setup)
    handler.signIn()
    assertTrue(rootState is UiState.SigningIn)
    handler.cancelSignIn()
    val restored = awaitState(UiState.NeedsSetup::class.java)
    assertEquals(setup.message, restored.message)
    assertTrue(restored.canSignIn)
    assertNull(TokenStore(app).peekOAuthState())
  }

  @Test
  fun oauthCallback_error_setsErrorState() {
    val fakeAuth = FakeAniListAuth()
    fakeAuth.setCallback(error = "access_denied")
    val handler = createHandler(fakeAuth = fakeAuth)
    handler.handleOAuthCallback(Uri.parse("portalani://callback?error=access_denied"))
    val state = awaitState(UiState.Error::class.java)
    assertTrue(state.message.contains("access_denied"))
  }

  @Test
  fun oauthCallback_stateMismatch_setsErrorState() {
    val fakeAuth = FakeAniListAuth()
    val handler = createHandler(fakeAuth = fakeAuth, initialState = UiState.Loading)
    handler.signIn()
    fakeAuth.setCallback(code = "auth-code", state = "wrong-state")
    handler.handleOAuthCallback(Uri.parse("portalani://callback?code=auth-code&state=wrong-state"))
    val state = awaitState(UiState.Error::class.java)
    assertTrue(state.message.contains("state mismatch"))
  }

  @Test
  fun oauthCallback_success_savesSessionAndInvokesCallbacks() {
    val fakeAuth = FakeAniListAuth()
    val fakeClient = FakeAniListClient(viewerProfile = ViewerProfile(id = 3, name = "TestUser"))
    val handler = createHandler(fakeAuth = fakeAuth, fakeClient = fakeClient)
    handler.signIn()
    val savedState = TokenStore(app).peekOAuthState()
    fakeAuth.setCallback(code = "good-code", state = savedState)
    handler.handleOAuthCallback(Uri.parse("portalani://callback?code=good-code&state=$savedState"))
    awaitState(UiState.Loading::class.java)
    Thread.sleep(100)
    assertTrue(signedIn)
    assertEquals("TestUser", viewerName)
    assertTrue(broughtToFront)
    assertEquals(1, oauthSuccessCalls)
  }

  @Test
  fun oauthCallback_exchangeFailure_setsErrorState() {
    val delegate = FakeAniListAuth()
    val throwingAuth =
        object : AniListAuthPort {
          override fun isConfigured(): Boolean = delegate.isConfigured()
          override fun buildAuthorizeUrl(state: String) = delegate.buildAuthorizeUrl(state)
          override fun parseCallback(uri: Uri) = delegate.parseCallback(uri)
          override fun exchangeCode(code: String): String = throw IOException("network down")
        }
    val handler = createHandler(fakeAuth = throwingAuth)
    handler.signIn()
    val savedState = TokenStore(app).peekOAuthState()
    delegate.setCallback(code = "good-code", state = savedState)
    handler.handleOAuthCallback(Uri.parse("portalani://callback?code=good-code&state=$savedState"))
    val state = awaitState(UiState.Error::class.java)
    assertTrue(state.message.contains("network down"))
    assertFalse(signedIn)
  }

  @Test
  fun clearSessionFromStorage_clearsTokensAndFlags() {
    TokenStore(app).apply {
      saveAccessToken("token")
      saveViewer(ViewerProfile(id = 1, name = "Hoan"))
    }
    signedIn = true
    viewerName = "Hoan"
    val handler = createHandler()
    handler.clearSessionFromStorage()
    assertNull(TokenStore(app).accessToken())
    assertFalse(signedIn)
    assertNull(viewerName)
  }

  @Test
  fun cancelSignIn_personalWithoutToken_usesCalendarMessage() {
    settings = AppSettings(sourceMode = SourceMode.PERSONAL, frameMode = FrameMode.CALENDAR)
    val handler = createHandler(initialState = UiState.SigningIn)
    handler.cancelSignIn()
    val state = awaitState(UiState.NeedsSetup::class.java)
    assertTrue(state.message.contains("calendar"))
  }
}
