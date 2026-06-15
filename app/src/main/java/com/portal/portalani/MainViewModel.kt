package com.portal.portalani

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.portal.portalani.data.AniListAuth
import com.portal.portalani.data.AniListClient
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.SettingsStore
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

sealed interface UiState {
  data object Loading : UiState

  data class NeedsSetup(
      val message: String,
      val canSignIn: Boolean,
      val canUseLibrary: Boolean,
  ) : UiState

  data object SigningIn : UiState

  data class Showing(
      val slides: List<AnimeSlide>,
      val fromCache: Boolean = false,
      /** Bumps on full feed reload; slideshow order resets when this changes. */
      val orderResetToken: Int = 0,
  ) : UiState

  data class Error(
      val message: String,
      val canOpenSettings: Boolean = false,
  ) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val http =
      OkHttpClient.Builder()
          .callTimeout(60, TimeUnit.SECONDS)
          .readTimeout(45, TimeUnit.SECONDS)
          .build()

  private val tokens = TokenStore(app)
  private val settingsStore = SettingsStore(app)
  private val slideCache = AnimeSlideCache(app)
  private val auth = AniListAuth(http, tokens)
  private val client = AniListClient(http)

  private val _state = MutableStateFlow<UiState>(UiState.Loading)
  val state: StateFlow<UiState> = _state.asStateFlow()

  private val _settings = MutableStateFlow(settingsStore.load())
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  private val _viewerName = MutableStateFlow(tokens.viewerName())
  val viewerName: StateFlow<String?> = _viewerName.asStateFlow()

  private val _isSignedIn = MutableStateFlow(!tokens.accessToken().isNullOrBlank())
  val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  private var pendingOAuthState: String? = null

  private var feedNextPage = 1
  private var feedHasMore = false
  private var feedLoadingMore = false
  private var orderResetToken = 0
  private var activeFeedKey: String? = null

  init {
    ScreensaverGuardWorker.schedule(app)
    AnimeDreamService.setAsDefaultScreensaver(app)
    refresh()
  }

  fun refresh() {
    viewModelScope.launch {
      loadSlides(showLoading = _state.value !is UiState.Showing)
    }
  }

  fun signIn() {
    if (!auth.isConfigured()) {
      _state.value =
          UiState.NeedsSetup(
              message = missingCredentialsMessage(),
              canSignIn = false,
              canUseLibrary = true,
          )
      return
    }
    val state = UUID.randomUUID().toString()
    pendingOAuthState = state
    tokens.saveOAuthState(state)
    _state.value = UiState.SigningIn
    val authUrl = auth.buildAuthorizeUrl(state).toString()
    val ctx = getApplication<Application>()
    val intent =
        Intent(ctx, AniListOAuthActivity::class.java)
            .putExtra(AniListOAuthActivity.EXTRA_AUTH_URL, authUrl)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(intent)
  }

  fun handleOAuthCallback(uri: android.net.Uri) {
    val callback = auth.parseCallback(uri) ?: return
    if (!callback.error.isNullOrBlank()) {
      _state.value = UiState.Error("AniList sign-in failed: ${callback.error}")
      pendingOAuthState = null
      tokens.clearOAuthState()
      return
    }
    val expected = pendingOAuthState ?: tokens.peekOAuthState()
    pendingOAuthState = null
    tokens.clearOAuthState()
    if (expected == null || callback.state != expected) {
      _state.value = UiState.Error("Sign-in state mismatch. Please try again.")
      return
    }
    val code = callback.code
    if (code.isNullOrBlank()) {
      _state.value = UiState.Error("No authorization code received.")
      return
    }
    viewModelScope.launch {
      _state.value = UiState.Loading
      try {
        withContext(Dispatchers.IO) {
          val token = auth.exchangeCode(code)
          val viewer = client.fetchViewer(token)
          tokens.saveViewer(viewer)
        }
        _viewerName.value = tokens.viewerName()
        _isSignedIn.value = true
        updateSettings(_settings.value.copy(sourceMode = SourceMode.PERSONAL))
        bringAppToFront()
        loadSlides(showLoading = true)
      } catch (e: Exception) {
        _state.value = UiState.Error(e.message ?: "Sign-in failed")
      }
    }
  }

  fun signOut() {
    tokens.clear()
    _viewerName.value = null
    _isSignedIn.value = false
    if (_settings.value.sourceMode == SourceMode.PERSONAL) {
      updateSettings(_settings.value.copy(sourceMode = SourceMode.LIBRARY))
    }
    refresh()
  }

  fun useLibrary() {
    updateSettings(_settings.value.copy(sourceMode = SourceMode.LIBRARY))
    refresh()
  }

  fun setSourceMode(mode: SourceMode) {
    updateSettings(_settings.value.copy(sourceMode = mode))
    refresh()
  }

  fun setShuffle(enabled: Boolean) {
    updateSettings(_settings.value.copy(shuffle = enabled))
  }

  fun setIntervalSeconds(seconds: Int) {
    updateSettings(_settings.value.copy(intervalMs = seconds.coerceIn(5, 120) * 1000L))
  }

  fun setListStatus(status: ListStatus) {
    updateSettings(_settings.value.copy(listStatus = status))
    refresh()
  }

  fun setFormatFilter(filter: FormatFilter) {
    updateSettings(_settings.value.copy(formatFilter = filter))
    refresh()
  }

  fun setLibrarySort(sort: LibrarySort) {
    updateSettings(_settings.value.copy(librarySort = sort))
    refresh()
  }

  fun setSeasonKey(key: String) {
    updateSettings(_settings.value.copy(seasonKey = key))
    refresh()
  }

  fun onSlideIndexChanged(index: Int) {
    val showing = _state.value as? UiState.Showing ?: return
    if (!feedHasMore || feedLoadingMore) return
    if (showing.slides.isEmpty()) return
    if (index < showing.slides.lastIndex - LOAD_MORE_THRESHOLD) return
    loadMoreSlides()
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun setUserScore(mediaId: Int, score: Float?) {
    performUserAction(mediaId, "Could not save score") { token, slide ->
      val update =
          client.saveMediaListEntry(
              accessToken = token,
              mediaId = mediaId,
              listEntryId = slide.listEntryId,
              status = slide.listStatus ?: ListStatus.PLANNING,
              score = score,
          )
      slide.withUserState(
          listEntryId = update.listEntryId,
          listStatus = update.listStatus ?: slide.listStatus ?: ListStatus.PLANNING,
          userScore = update.userScore,
      )
    }
  }

  fun toggleFavourite(mediaId: Int) {
    performUserAction(mediaId, "Could not update favourite") { token, slide ->
      val isFavourite = client.toggleFavourite(token, mediaId)
      slide.withUserState(isFavourite = isFavourite)
    }
  }

  fun setAnimeListStatus(mediaId: Int, status: ListStatus) {
    performUserAction(mediaId, "Could not update list") { token, slide ->
      val update =
          client.saveMediaListEntry(
              accessToken = token,
              mediaId = mediaId,
              listEntryId = slide.listEntryId,
              status = status,
              score = slide.userScore,
          )
      slide.withUserState(
          listEntryId = update.listEntryId,
          listStatus = update.listStatus ?: status,
          userScore = update.userScore ?: slide.userScore,
      )
    }
  }

  fun removeFromList(mediaId: Int) {
    val slide = (_state.value as? UiState.Showing)?.slides?.firstOrNull { it.id == mediaId }
    if (slide?.listEntryId == null) {
      _userMessage.value = "This anime is not on your list."
      return
    }
    performUserAction(mediaId, "Could not remove from list") { token, current ->
      client.deleteMediaListEntry(token, current.listEntryId!!)
      current.withUserState(listEntryId = null, listStatus = null)
    }
  }

  private fun performUserAction(
      mediaId: Int,
      failureMessage: String,
      transform: suspend (String, AnimeSlide) -> AnimeSlide,
  ) {
    val token = tokens.accessToken()
    if (token.isNullOrBlank()) {
      _userMessage.value = "Sign in to manage your AniList."
      return
    }
    val current = _state.value as? UiState.Showing ?: return
    val slide = current.slides.firstOrNull { it.id == mediaId } ?: return

    viewModelScope.launch {
      try {
        val updated = withContext(Dispatchers.IO) { transform(token, slide) }
        updateSlideInState(mediaId, updated)
        persistCurrentSlides()
      } catch (e: Exception) {
        _userMessage.value = e.message ?: failureMessage
      }
    }
  }

  private fun updateSlideInState(mediaId: Int, updated: AnimeSlide) {
    val current = _state.value as? UiState.Showing ?: return
    _state.value = current.copy(slides = current.slides.map { if (it.id == mediaId) updated else it })
  }

  private suspend fun persistCurrentSlides() {
    val current = _state.value as? UiState.Showing ?: return
    withContext(Dispatchers.IO) {
      slideCache.save(_settings.value.cacheKey(), current.slides)
    }
  }

  private fun updateSettings(next: AppSettings) {
    settingsStore.save(next)
    _settings.value = next
  }

  private suspend fun loadSlides(showLoading: Boolean) {
    val settings = _settings.value
    val token = tokens.accessToken()
    val cacheKey = settings.cacheKey()

    resetFeedPagination(cacheKey)

    val cachedFresh =
        withContext(Dispatchers.IO) { slideCache.load(cacheKey) }
    val cachedStale =
        cachedFresh ?: withContext(Dispatchers.IO) { slideCache.loadStale(cacheKey) }

    if (showLoading && cachedFresh == null) {
      _state.value = UiState.Loading
    } else if (cachedFresh != null) {
      _state.value = showingState(cachedFresh, fromCache = true)
    }

    try {
      val batch =
          withContext(Dispatchers.IO) {
            when (settings.sourceMode) {
              SourceMode.PERSONAL -> {
                if (token == null) return@withContext null
                val userId =
                    tokens.viewerId()
                        ?: client.fetchViewer(token).also { tokens.saveViewer(it) }.id
                client.fetchViewerListPages(
                    accessToken = token,
                    userId = userId,
                    status = settings.listStatus,
                    startPage = 1,
                    pageCount = AniListClient.DEFAULT_INITIAL_PAGES,
                )
              }
              SourceMode.LIBRARY ->
                  client.fetchLibraryPages(
                      filters = settings.libraryFilters(),
                      startPage = 1,
                      pageCount = AniListClient.DEFAULT_INITIAL_PAGES,
                      accessToken = token,
                  )
            }
          }
      val slides = batch?.slides.orEmpty()
      if (slides.isEmpty()) {
        if (cachedStale != null) {
          _state.value = showingState(cachedStale, fromCache = true)
          return
        }
        if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
          _state.value =
              UiState.NeedsSetup(
                  message = "Sign in to show anime from your personal AniList.",
                  canSignIn = auth.isConfigured(),
                  canUseLibrary = true,
              )
        } else {
          val message =
              if (settings.sourceMode == SourceMode.PERSONAL) {
                personalListEmptyMessage(settings.listStatus)
              } else {
                "No anime found for these filters. Try broader filters in Settings."
              }
          _state.value = UiState.Error(message = message, canOpenSettings = true)
        }
        return
      }
      if (batch != null) {
        feedNextPage = batch.nextPage
        feedHasMore = batch.hasMore
      }
      withContext(Dispatchers.IO) { slideCache.save(cacheKey, slides) }
      _state.value = showingState(slides, fromCache = false)
    } catch (e: Exception) {
      if (cachedStale != null) {
        _state.value = showingState(cachedStale, fromCache = true)
        return
      }
      if (settings.sourceMode == SourceMode.PERSONAL && token != null) {
        _state.value =
            UiState.NeedsSetup(
                message = e.message ?: "Could not load your list. Sign in again or switch to Full library.",
                canSignIn = auth.isConfigured(),
                canUseLibrary = true,
            )
      } else {
        _state.value = UiState.Error(e.message ?: "Could not load anime")
      }
    }
  }

  private fun loadMoreSlides() {
    if (feedLoadingMore || !feedHasMore) return
    val settings = _settings.value
    val token = tokens.accessToken()
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) return

    feedLoadingMore = true
    viewModelScope.launch {
      try {
        val batch =
            withContext(Dispatchers.IO) {
              when (settings.sourceMode) {
                SourceMode.PERSONAL -> {
                  val accessToken = token ?: return@withContext FetchBatchResult(emptyList(), feedNextPage, false)
                  val userId =
                      tokens.viewerId()
                          ?: client.fetchViewer(accessToken).also { tokens.saveViewer(it) }.id
                  client.fetchViewerListPages(
                      accessToken = accessToken,
                      userId = userId,
                      status = settings.listStatus,
                      startPage = feedNextPage,
                      pageCount = AniListClient.DEFAULT_LOAD_MORE_PAGES,
                  )
                }
                SourceMode.LIBRARY ->
                    client.fetchLibraryPages(
                        filters = settings.libraryFilters(),
                        startPage = feedNextPage,
                        pageCount = AniListClient.DEFAULT_LOAD_MORE_PAGES,
                        accessToken = token,
                    )
              }
            }
        feedNextPage = batch.nextPage
        feedHasMore = batch.hasMore
        if (batch.slides.isNotEmpty()) {
          appendSlides(batch.slides)
          persistCurrentSlides()
        }
      } catch (_: Exception) {
        // Keep current feed; try again on a later slide.
      } finally {
        feedLoadingMore = false
      }
    }
  }

  private fun resetFeedPagination(cacheKey: String) {
    if (activeFeedKey != cacheKey) {
      activeFeedKey = cacheKey
      orderResetToken++
    }
    feedNextPage = 1
    feedHasMore = false
    feedLoadingMore = false
  }

  private fun showingState(slides: List<AnimeSlide>, fromCache: Boolean): UiState.Showing =
      UiState.Showing(
          slides = slides,
          fromCache = fromCache,
          orderResetToken = orderResetToken,
      )

  private fun appendSlides(batch: List<AnimeSlide>) {
    val current = _state.value as? UiState.Showing ?: return
    val existingIds = current.slides.asSequence().map { it.id }.toSet()
    val merged = current.slides + batch.filter { it.id !in existingIds }
    if (merged.size == current.slides.size) return
    _state.value = current.copy(slides = merged)
  }

  private companion object {
    const val LOAD_MORE_THRESHOLD = 8
  }

  private fun bringAppToFront() {
    val ctx = getApplication<Application>()
    val intent =
        Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    ctx.startActivity(intent)
  }

  private fun personalListEmptyMessage(status: ListStatus): String {
    val label =
        when (status) {
          ListStatus.CURRENT -> "Currently watching"
          ListStatus.PLANNING -> "Planning"
          ListStatus.COMPLETED -> "Completed"
          ListStatus.PAUSED -> "Paused"
          ListStatus.DROPPED -> "Dropped"
          ListStatus.REPEATING -> "Rewatching"
        }
    return "Your \"$label\" list is empty on AniList. Try another list status in Settings, or add anime on anilist.co."
  }

  private fun missingCredentialsMessage(): String =
      "Add your AniList OAuth client to local.properties, then rebuild:\n\n" +
          "ANILIST_CLIENT_ID=...\n" +
          "ANILIST_CLIENT_SECRET=...\n\n" +
          "Redirect URI must be portalani://callback"
}
