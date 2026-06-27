package com.portal.portalani.vm

import android.app.Application
import com.portal.portalani.MainViewModel
import com.portal.portalani.MainViewModelDeps
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.AniListHttpException
import com.portal.portalani.data.CalendarWeekCache
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.ViewerProfile
import com.portal.portalani.data.WeatherClient
import com.portal.portalani.data.sampleCalendarEntry
import com.portal.portalani.data.sampleSlide
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MainViewModelTest {
  @get:Rule
  val rules: RuleChain =
      RuleChain.outerRule(InstantTaskExecutorRule()).around(CoroutineTestRule())

  private lateinit var app: Application

  @Before
  fun setUp() {
    app = RuntimeEnvironment.getApplication()
  }

  private fun createViewModel(
      settings: AppSettings = AppSettings(),
      fakeClient: FakeAniListClient = FakeAniListClient(),
      fakeAuth: FakeAniListAuth = FakeAniListAuth(),
      tokens: TokenStore = TokenStore(app),
  ): MainViewModel {
    val settingsStore = SettingsStore(app)
    settingsStore.save(settings)
    val http = OkHttpClient()
    val deps =
        MainViewModelDeps(
            tokens = tokens,
            settingsStore = settingsStore,
            slideCache = AnimeSlideCache(app),
            calendarWeekCache = CalendarWeekCache(app),
            client = fakeClient,
            auth = fakeAuth,
            weatherClient = WeatherClient(http),
            http = http,
        )
    return MainViewModel(app, deps, runBootstrap = false)
  }

  private fun <T : UiState> awaitState(vm: MainViewModel, klass: Class<T>, timeoutMs: Long = 3000): T {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val state = vm.state.value
      if (klass.isInstance(state)) {
        @Suppress("UNCHECKED_CAST")
        return state as T
      }
      Thread.sleep(10)
    }
    error("Timed out waiting for ${klass.simpleName}, last=${vm.state.value}")
  }

  private fun awaitCalendarState(vm: MainViewModel, timeoutMs: Long = 3000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (vm.calendarState.value?.entries?.isNotEmpty() == true) return
      Thread.sleep(10)
    }
    error("Timed out waiting for calendar entries, last=${vm.calendarState.value}")
  }

  private fun awaitOrderResetTokenGreater(vm: MainViewModel, before: Int, timeoutMs: Long = 3000): Int {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val state = vm.state.value as? UiState.Showing
      if (state != null && state.orderResetToken > before) return state.orderResetToken
      Thread.sleep(10)
    }
    error("Timed out waiting for orderResetToken > $before, last=${vm.state.value}")
  }

  private fun awaitCalendarDetail(vm: MainViewModel, mediaId: Int, timeoutMs: Long = 3000): AnimeSlide {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val slide = vm.calendarDetailSlide.value
      if (slide?.id == mediaId && vm.calendarDetailLoading.value == false) return slide
      Thread.sleep(10)
    }
    error("Timed out waiting for calendar detail $mediaId, last=${vm.calendarDetailSlide.value}")
  }

  private fun awaitCalendarDetailField(
      vm: MainViewModel,
      mediaId: Int,
      timeoutMs: Long = 3000,
      predicate: (AnimeSlide) -> Boolean,
  ): AnimeSlide {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val slide = vm.calendarDetailSlide.value
      if (slide?.id == mediaId && predicate(slide)) return slide
      Thread.sleep(10)
    }
    error("Timed out waiting for calendar detail update on $mediaId, last=${vm.calendarDetailSlide.value}")
  }

  @Test
  fun libraryRefresh_showsSlidesFromClient() {
    val slide = sampleSlide(id = 99)
    val fakeClient =
        FakeAniListClient(
            libraryPagesResult = FetchBatchResult(listOf(slide), 2, false),
        )
    val vm = createViewModel(fakeClient = fakeClient)

    vm.refresh()

    val state = awaitState(vm, UiState.Showing::class.java)
    assertEquals(1, state.slides.size)
    assertEquals(99, state.slides.first().id)
  }

  @Test
  fun personalWithoutToken_needsSetup() {
    val vm =
        createViewModel(
            settings = AppSettings(sourceMode = SourceMode.PERSONAL),
        )

    vm.refresh()

    val state = awaitState(vm, UiState.NeedsSetup::class.java)
    assertTrue(state.message.contains("Sign in"))
    assertTrue(state.canUseLibrary)
  }

  @Test
  fun personalWithToken_networkError_showsErrorAndKeepsSession() {
    TokenStore(app).apply {
      saveAccessToken("test-token")
      saveViewer(ViewerProfile(id = 7, name = "Hoan"))
    }
    val fakeClient =
        FakeAniListClient().apply {
          viewerListPagesError = IOException("network down")
        }
    val vm =
        createViewModel(
            settings = AppSettings(sourceMode = SourceMode.PERSONAL),
            fakeClient = fakeClient,
        )

    vm.refresh()

    val state = awaitState(vm, UiState.Error::class.java)
    assertTrue(state.message.contains("network down"))
    assertTrue(vm.isSignedIn.value)
    assertEquals("test-token", TokenStore(app).accessToken())
  }

  @Test
  fun personalWithToken_authFailure_clearsSession() {
    TokenStore(app).apply {
      saveAccessToken("expired-token")
      saveViewer(ViewerProfile(id = 7, name = "Hoan"))
    }
    val fakeClient =
        FakeAniListClient().apply {
          viewerListPagesError = AniListHttpException(401, "Unauthorized")
        }
    val vm =
        createViewModel(
            settings = AppSettings(sourceMode = SourceMode.PERSONAL),
            fakeClient = fakeClient,
        )

    vm.refresh()

    val state = awaitState(vm, UiState.NeedsSetup::class.java)
    assertTrue(state.message.contains("expired"))
    assertFalse(vm.isSignedIn.value)
    assertNull(TokenStore(app).accessToken())
  }

  @Test
  fun formatFilterChange_bumpsOrderResetToken() {
    val slide = sampleSlide()
    val fakeClient =
        FakeAniListClient(
            libraryPagesResult = FetchBatchResult(listOf(slide), 2, false),
        )
    val vm = createViewModel(fakeClient = fakeClient)

    vm.refresh()
    val before = awaitState(vm, UiState.Showing::class.java).orderResetToken

    vm.setFormatFilters(setOf(FormatFilter.TV))

    val after = awaitOrderResetTokenGreater(vm, before)
    assertTrue(after > before)
  }

  @Test
  fun calendarPersonalWithoutToken_needsSetup() {
    val vm =
        createViewModel(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.PERSONAL,
                ),
        )

    vm.refresh()

    val state = awaitState(vm, UiState.NeedsSetup::class.java)
    assertTrue(state.message.contains("calendar"))
    assertTrue(state.canUseLibrary)
  }

  @Test
  fun calendarWeekShiftBack_usesMemoryCache() {
    val fakeClient =
        FakeAniListClient(
            airingSchedules = listOf(sampleCalendarEntry(mediaId = 42)),
        )
    val vm =
        createViewModel(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.LIBRARY,
                ),
            fakeClient = fakeClient,
        )

    vm.refresh()
    awaitCalendarState(vm)

    val callsAfterInitialLoad = fakeClient.fetchAiringSchedulesCalls
    vm.shiftCalendarWeek(1)
    vm.shiftCalendarWeek(-1)

    assertNotNull(vm.calendarState.value)
    assertTrue(vm.calendarState.value!!.entries.isNotEmpty())
    assertTrue(fakeClient.fetchAiringSchedulesCalls <= callsAfterInitialLoad + 4)
  }

  @Test
  fun libraryRefresh_ioFailure_setsErrorState() {
    val fakeClient = FakeAniListClient()
    fakeClient.libraryPagesError = IOException("network down")
    val vm = createViewModel(fakeClient = fakeClient)

    vm.refresh()

    val state = awaitState(vm, UiState.Error::class.java)
    assertTrue(state.message!!.contains("network down"))
  }

  @Test
  fun calendarDetailToggleFavourite_updatesDetailSlide() {
    val entry = sampleCalendarEntry(mediaId = 42)
    val detailSlide = sampleSlide(id = 42).copy(listEntryId = 7, listStatus = ListStatus.CURRENT, isFavourite = false)
    val tokens =
        TokenStore(app).apply {
          saveAccessToken("test-token")
          saveViewer(ViewerProfile(id = 7, name = "Hoan"))
        }
    val fakeClient =
        FakeAniListClient(
            airingSchedules = listOf(entry),
            mediaById = detailSlide,
        )
    val vm =
        createViewModel(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.LIBRARY,
                ),
            fakeClient = fakeClient,
            tokens = tokens,
        )

    vm.refresh()
    awaitCalendarState(vm)
    vm.openCalendarEntry(entry)
    awaitCalendarDetail(vm, mediaId = 42)

    vm.toggleFavourite(42)

    assertTrue(awaitCalendarDetailField(vm, mediaId = 42) { it.isFavourite }.isFavourite)
  }

  @Test
  fun calendarDetailSetUserScore_updatesDetailSlide() {
    val entry = sampleCalendarEntry(mediaId = 42)
    val detailSlide =
        sampleSlide(id = 42).copy(listEntryId = 7, listStatus = ListStatus.CURRENT, userScore = null)
    val tokens =
        TokenStore(app).apply {
          saveAccessToken("test-token")
          saveViewer(ViewerProfile(id = 7, name = "Hoan"))
        }
    val fakeClient =
        FakeAniListClient(
            airingSchedules = listOf(entry),
            mediaById = detailSlide,
        )
    val vm =
        createViewModel(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.LIBRARY,
                ),
            fakeClient = fakeClient,
            tokens = tokens,
        )

    vm.refresh()
    awaitCalendarState(vm)
    vm.openCalendarEntry(entry)
    awaitCalendarDetail(vm, mediaId = 42)

    vm.setUserScore(42, 8.5f)

    assertEquals(8.5f, awaitCalendarDetailField(vm, mediaId = 42) { it.userScore == 8.5f }.userScore)
  }

  @Test
  fun goToCalendarToday_atCurrentWeek_isNoOp() {
    val fakeClient =
        FakeAniListClient(
            airingSchedules = listOf(sampleCalendarEntry()),
        )
    val vm =
        createViewModel(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.LIBRARY,
                ),
            fakeClient = fakeClient,
        )

    vm.refresh()
    awaitCalendarState(vm)
    val callsAfterLoad = fakeClient.fetchAiringSchedulesCalls

    vm.goToCalendarToday()

    assertEquals(callsAfterLoad, fakeClient.fetchAiringSchedulesCalls)
  }
}
