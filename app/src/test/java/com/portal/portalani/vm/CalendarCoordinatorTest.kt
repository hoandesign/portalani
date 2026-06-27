package com.portal.portalani.vm

import android.app.Application
import com.portal.portalani.UiState
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CalendarWeekCache
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.data.sampleCalendarEntry
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
class CalendarCoordinatorTest {
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

  private fun createCoordinator(
      settings: AppSettings = AppSettings(sourceMode = SourceMode.LIBRARY, frameMode = FrameMode.CALENDAR),
      fakeClient: FakeAniListClient = FakeAniListClient(airingSchedules = listOf(sampleCalendarEntry(mediaId = 7))),
  ): CalendarCoordinator {
    SettingsStore(app).save(settings)
    return CalendarCoordinator(
        scope = testScope,
        client = fakeClient,
        calendarWeekCache = CalendarWeekCache(app),
        tokens = TokenStore(app),
        getSettings = { settings },
        isAuthConfigured = { true },
        onRootState = { rootState = it },
        onSessionExpired = { rootState = UiState.NeedsSetup(it, canSignIn = true, canUseLibrary = true) },
        onUserMessage = {},
    )
  }

  private fun awaitCalendarEntries(coordinator: CalendarCoordinator, timeoutMs: Long = 3000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (coordinator.calendarState.value?.entries?.isNotEmpty() == true) return
      Thread.sleep(10)
    }
    error("Timed out waiting for calendar entries, last=${coordinator.calendarState.value}")
  }

  @Test
  fun loadWeek_libraryMode_populatesCalendarState() {
    val coordinator = createCoordinator()

    coordinator.navigateToWeek()
    awaitCalendarEntries(coordinator)

    assertTrue(rootState is UiState.Showing)
  }

  @Test
  fun personalWithoutToken_setsNeedsSetup() {
    val coordinator =
        createCoordinator(
            settings =
                AppSettings(
                    frameMode = FrameMode.CALENDAR,
                    sourceMode = SourceMode.PERSONAL,
                ),
        )

    coordinator.navigateToWeek()
    Thread.sleep(50)

    val state = rootState as UiState.NeedsSetup
    assertTrue(state.message.contains("calendar"))
    assertTrue(state.canUseLibrary)
  }

  private fun awaitCalendarLoadingFalse(coordinator: CalendarCoordinator, timeoutMs: Long = 3000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (!coordinator.calendarLoading.value) return
      Thread.sleep(10)
    }
    error("Timed out waiting for calendar loading to finish")
  }

  @Test
  fun navigateToWeek_memoryCacheHit_setsLoadingDuringRefresh() {
    val coordinator = createCoordinator()

    coordinator.navigateToWeek()
    awaitCalendarEntries(coordinator)
    awaitCalendarLoadingFalse(coordinator)

    coordinator.shiftWeek(1)
    awaitCalendarEntries(coordinator)
    awaitCalendarLoadingFalse(coordinator)

    coordinator.shiftWeek(-1)
    assertTrue(coordinator.calendarLoading.value)

    awaitCalendarEntries(coordinator)
    awaitCalendarLoadingFalse(coordinator)
    assertFalse(coordinator.calendarLoading.value)
  }

  @Test
  fun navigateToWeek_emptyWeek_showsLoadingUntilFetchCompletes() {
    val coordinator = createCoordinator(fakeClient = FakeAniListClient(airingSchedules = emptyList()))

    coordinator.navigateToWeek()

    assertTrue(coordinator.calendarLoading.value)
    assertTrue(coordinator.calendarState.value?.entries.isNullOrEmpty())

    Thread.sleep(100)
    assertFalse(coordinator.calendarLoading.value)
    assertTrue(rootState is UiState.Showing)
  }

  @Test
  fun loadWeek_networkFailureWithEmptyState_setsError() {
    val fakeClient = FakeAniListClient(airingSchedules = emptyList())
    fakeClient.airingSchedulesError = IOException("network down")
    val coordinator = createCoordinator(fakeClient = fakeClient)

    coordinator.navigateToWeek()
    Thread.sleep(100)

    assertTrue(rootState is UiState.Error)
    assertFalse(coordinator.calendarLoading.value)
  }

  @Test
  fun navigateToWeek_withCachedEntries_showsCalendarWhileRefreshing() {
    val coordinator = createCoordinator()

    coordinator.navigateToWeek()
    awaitCalendarEntries(coordinator)
    awaitCalendarLoadingFalse(coordinator)

    coordinator.shiftWeek(1)
    awaitCalendarEntries(coordinator)
    awaitCalendarLoadingFalse(coordinator)

    coordinator.shiftWeek(-1)

    assertTrue(coordinator.calendarLoading.value)
    assertTrue(rootState is UiState.Showing)
    assertTrue(coordinator.calendarState.value?.entries?.isNotEmpty() == true)
  }

  @Test
  fun shiftWeekBack_restoresWeekStart() {
    val fakeClient =
        FakeAniListClient(
            airingSchedules = listOf(sampleCalendarEntry(mediaId = 42)),
        )
    val coordinator = createCoordinator(fakeClient = fakeClient)

    coordinator.navigateToWeek()
    awaitCalendarEntries(coordinator)
    val initialWeek = coordinator.calendarState.value?.weekStart

    coordinator.shiftWeek(1)
    awaitCalendarEntries(coordinator)
    assertTrue(initialWeek != null && coordinator.calendarState.value?.weekStart != initialWeek)

    coordinator.shiftWeek(-1)
    awaitCalendarEntries(coordinator)
    assertEquals(initialWeek, coordinator.calendarState.value?.weekStart)
  }
}
