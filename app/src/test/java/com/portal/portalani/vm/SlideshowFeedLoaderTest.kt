package com.portal.portalani.vm

import android.app.Application
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AnimeSlideCache.Companion.DEFAULT_MAX_AGE_MS
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.ViewerProfile
import com.portal.portalani.data.sampleSlide
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class SlideshowFeedLoaderTest {
  @get:Rule
  val rules: RuleChain = RuleChain.outerRule(InstantTaskExecutorRule()).around(CoroutineTestRule())

  private lateinit var app: Application
  private var rootState: UiState? = null
  private lateinit var testScope: CoroutineScope

  @Before
  fun setUp() {
    app = RuntimeEnvironment.getApplication()
    rootState = null
    testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  }

  private fun createLoader(
      settings: AppSettings = AppSettings(),
      fakeClient: FakeAniListClient = FakeAniListClient(),
      tokens: TokenStore = TokenStore(app),
  ): SlideshowFeedLoader {
    SettingsStore(app).save(settings)
    return SlideshowFeedLoader(
        scope = testScope,
        client = fakeClient,
        slideCache = AnimeSlideCache(app),
        tokens = tokens,
        getSettings = { settings },
        isAuthConfigured = { true },
        getCurrentShowing = { rootState as? UiState.Showing },
        onRootState = { rootState = it },
        onSessionExpired = { rootState = UiState.NeedsSetup(it, canSignIn = true, canUseLibrary = true) },
    )
  }

  private fun awaitShowing(timeoutMs: Long = 3000): UiState.Showing {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val state = rootState as? UiState.Showing
      if (state != null && state.slides.isNotEmpty()) return state
      Thread.sleep(10)
    }
    error("Timed out waiting for Showing, last=$rootState")
  }

  @Test
  fun libraryRefresh_showsSlidesFromClient() {
    val slide = sampleSlide(id = 99)
    val loader =
        createLoader(
            fakeClient =
                FakeAniListClient(
                    libraryPagesResult = FetchBatchResult(listOf(slide), 2, false),
                ),
        )

    runBlocking { loader.loadSlides(showLoading = true) }

    val state = awaitShowing()
    assertEquals(1, state.slides.size)
    assertEquals(99, state.slides.first().id)
    assertFalse(state.fromCache)
  }

  @Test
  fun personalWithToken_networkError_showsErrorNotNeedsSetup() {
    val tokens =
        TokenStore(app).apply {
          saveAccessToken("test-token")
          saveViewer(ViewerProfile(id = 7, name = "Hoan"))
        }
    val fakeClient =
        FakeAniListClient().apply {
          viewerListPagesError = IOException("network down")
        }
    val loader =
        createLoader(
            settings = AppSettings(sourceMode = SourceMode.PERSONAL),
            fakeClient = fakeClient,
            tokens = tokens,
        )

    runBlocking { loader.loadSlides(showLoading = true) }

    val state = rootState as UiState.Error
    assertTrue(state.message.contains("network down"))
    assertEquals("test-token", tokens.accessToken())
  }

  @Test
  fun staleCacheOnNetworkFailure_showsCachedSlides() {
    val cachedSlide = sampleSlide(id = 77)
    val settings = AppSettings()
    val cache = AnimeSlideCache(app)
    val cacheKey = settings.cacheKey()
    cache.save(cacheKey, listOf(cachedSlide))
    val cacheFile =
        File(app.filesDir, "feeds").listFiles()?.firstOrNull { it.extension == "json" }
            ?: error("cache file missing")
    cacheFile.setLastModified(System.currentTimeMillis() - DEFAULT_MAX_AGE_MS - 60_000L)

    val fakeClient = FakeAniListClient()
    fakeClient.libraryPagesError = IOException("network down")
    val loader = createLoader(settings = settings, fakeClient = fakeClient)

    runBlocking { loader.loadSlides(showLoading = true) }

    val state = awaitShowing()
    assertEquals(77, state.slides.first().id)
    assertTrue(state.fromCache)
  }
}
