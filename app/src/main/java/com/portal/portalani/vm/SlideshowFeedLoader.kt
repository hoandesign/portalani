package com.portal.portalani.vm

import com.portal.portalani.UiState
import com.portal.portalani.data.AniListClient
import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.AnimeSlideCache
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.TokenStore
import com.portal.portalani.isAniListAuthFailure
import com.portal.portalani.userVisibleError
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

private const val MIN_LOADING_DISPLAY_MS = 400L

internal class SlideshowFeedLoader(
    private val scope: CoroutineScope,
    private val client: AniListClientPort,
    private val slideCache: AnimeSlideCache,
    private val tokens: TokenStore,
    private val getSettings: () -> AppSettings,
    private val isAuthConfigured: () -> Boolean,
    private val getCurrentShowing: () -> UiState.Showing?,
    private val onRootState: (UiState) -> Unit,
    private val onSessionExpired: (String) -> Unit,
) {
  private var feedNextPage = 1
  private var feedHasMore = false
  private var feedLoadingMore = false
  private var feedListPages: Map<ListStatus, Pair<Int, Boolean>> = emptyMap()
  private var orderResetToken = 0
  private var activeFeedKey: String? = null

  fun onSlideIndexChanged(index: Int) {
    val showing = getCurrentShowing() ?: return
    if (!feedHasMore || feedLoadingMore) return
    if (showing.slides.isEmpty()) return
    if (index < showing.slides.lastIndex - LOAD_MORE_THRESHOLD) return
    loadMoreSlides()
  }

  suspend fun loadSlides(showLoading: Boolean) {
    val settings = getSettings()
    val token = tokens.accessToken()
    val cacheKey = settings.cacheKey()
    val previousFeedKey = activeFeedKey
    val feedKeyChanged = previousFeedKey != null && previousFeedKey != cacheKey

    resetFeedPagination(cacheKey)

    val cachedFresh =
        withContext(Dispatchers.IO) { slideCache.load(cacheKey) }
            ?.let { filterSlidesForSettings(it, settings) }
            ?.takeIf { it.isNotEmpty() }
    val cachedStale =
        cachedFresh
            ?: withContext(Dispatchers.IO) { slideCache.loadStale(cacheKey) }
                ?.let { filterSlidesForSettings(it, settings) }
                ?.takeIf { it.isNotEmpty() }

    val currentlyShowing = getCurrentShowing()
    val hasDisplayedSlides = currentlyShowing?.slides?.isNotEmpty() == true
    val loadStartedAt = System.currentTimeMillis()

    when {
      showLoading && !hasDisplayedSlides -> onRootState(UiState.Loading)
      showLoading && hasDisplayedSlides && cachedFresh != null ->
          onRootState(showingState(cachedFresh, fromCache = true, isRefreshing = true))
      showLoading && hasDisplayedSlides && cachedStale != null ->
          onRootState(showingState(cachedStale, fromCache = true, isRefreshing = true))
      feedKeyChanged && cachedFresh == null -> onRootState(UiState.Loading)
      cachedFresh != null -> onRootState(showingState(cachedFresh, fromCache = true))
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
                fetchPersonalSlides(
                    accessToken = token,
                    userId = userId,
                    statuses = settings.listStatuses,
                    initial = true,
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
      val rawSlides = batch?.slides.orEmpty()
      val slides = filterSlidesForSettings(rawSlides, settings)
      if (slides.isEmpty()) {
        if (cachedStale != null) {
          onRootState(showingState(cachedStale, fromCache = true))
          return
        }
        if (settings.sourceMode == SourceMode.PERSONAL && token == null) {
          onRootState(
              UiState.NeedsSetup(
                  message = "Sign in to show anime from your personal AniList.",
                  canSignIn = isAuthConfigured(),
                  canUseLibrary = true,
              ),
          )
        } else {
          val message =
              if (settings.sourceMode == SourceMode.PERSONAL) {
                personalListEmptyMessage(settings.listStatuses, filtersExcludedAll = rawSlides.isNotEmpty())
              } else {
                "No anime found for these filters. Try broader filters in Settings."
              }
          onRootState(UiState.Error(message = message, canOpenSettings = true))
        }
        return
      }
      if (batch != null) {
        feedNextPage = batch.nextPage
        feedHasMore = batch.hasMore
      }
      withContext(Dispatchers.IO) { slideCache.save(cacheKey, slides) }
      if (showLoading && !hasDisplayedSlides) {
        val elapsed = System.currentTimeMillis() - loadStartedAt
        val remaining = MIN_LOADING_DISPLAY_MS - elapsed
        if (remaining > 0) {
          withContext(Dispatchers.IO) { Thread.sleep(remaining) }
        }
      }
      onRootState(showingState(slides, fromCache = false))
    } catch (e: Throwable) {
      when (e) {
        is IOException, is JSONException -> {
          if (cachedStale != null) {
            onRootState(showingState(cachedStale, fromCache = true))
            return
          }
          if (isAniListAuthFailure(e)) {
            onSessionExpired(userVisibleError(e, "Your AniList sign-in expired. Sign in again."))
            return
          }
          if (settings.sourceMode == SourceMode.PERSONAL && token != null) {
            onRootState(
                UiState.Error(
                    userVisibleError(
                        e,
                        "Could not load your list. Check your connection and try again.",
                    ),
                    canOpenSettings = true,
                ),
            )
          } else {
            onRootState(UiState.Error(userVisibleError(e, "Could not load anime")))
          }
        }
        else -> throw e
      }
    }
  }

  fun loadMoreSlides() {
    if (feedLoadingMore || !feedHasMore) return
    val settings = getSettings()
    val token = tokens.accessToken()
    if (settings.sourceMode == SourceMode.PERSONAL && token == null) return

    feedLoadingMore = true
    scope.launch {
      try {
        val batch =
            withContext(Dispatchers.IO) {
              when (settings.sourceMode) {
                SourceMode.PERSONAL -> {
                  val accessToken = token ?: return@withContext FetchBatchResult(emptyList(), feedNextPage, false)
                  val userId =
                      tokens.viewerId()
                          ?: client.fetchViewer(accessToken).also { tokens.saveViewer(it) }.id
                  fetchPersonalSlides(
                      accessToken = accessToken,
                      userId = userId,
                      statuses = settings.listStatuses,
                      initial = false,
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
        val filtered = filterSlidesForSettings(batch.slides, settings)
        if (filtered.isNotEmpty()) {
          appendSlides(filtered)
          persistCurrentSlides()
        }
      } catch (e: Throwable) {
        when (e) {
          is IOException, is JSONException -> Unit
          else -> throw e
        }
      } finally {
        feedLoadingMore = false
      }
    }
  }

  suspend fun persistCurrentSlides() {
    val current = getCurrentShowing() ?: return
    withContext(Dispatchers.IO) {
      slideCache.save(getSettings().cacheKey(), current.slides)
    }
  }

  private fun resetFeedPagination(cacheKey: String) {
    if (activeFeedKey != cacheKey) {
      activeFeedKey = cacheKey
      orderResetToken++
      feedListPages = emptyMap()
    }
    feedNextPage = 1
    feedHasMore = false
    feedLoadingMore = false
  }

  private suspend fun fetchPersonalSlides(
      accessToken: String,
      userId: Int,
      statuses: Set<ListStatus>,
      initial: Boolean,
  ): FetchBatchResult {
    if (statuses.isEmpty()) return FetchBatchResult(emptyList(), 1, false)

    val merged = mutableListOf<AnimeSlide>()
    val updatedPages = feedListPages.toMutableMap()
    var anyHasMore = false

    for (status in statuses.sortedBy { it.ordinal }) {
      if (!initial) {
        val hasMore = updatedPages[status]?.second == true
        if (!hasMore) continue
      }

      val startPage = if (initial) 1 else updatedPages[status]?.first ?: 1
      val pageCount =
          if (initial) {
            AniListClient.DEFAULT_INITIAL_PAGES
          } else {
            AniListClient.DEFAULT_LOAD_MORE_PAGES
          }
      val batch =
          client.fetchViewerListPages(
              accessToken = accessToken,
              userId = userId,
              status = status,
              startPage = startPage,
              pageCount = pageCount,
          )
      merged += batch.slides
      updatedPages[status] = batch.nextPage to batch.hasMore
      if (batch.hasMore) anyHasMore = true
    }

    feedListPages = updatedPages
    feedHasMore = anyHasMore
    return FetchBatchResult(merged, 1, anyHasMore)
  }

  private fun filterSlidesForSettings(slides: List<AnimeSlide>, settings: AppSettings): List<AnimeSlide> {
    val filters = settings.libraryFilters()
    val applySeasonFilter = settings.sourceMode == SourceMode.LIBRARY
    return slides.filter { filters.matchesSlide(it, applySeasonFilter = applySeasonFilter) }
  }

  private fun showingState(
      slides: List<AnimeSlide>,
      fromCache: Boolean,
      isRefreshing: Boolean = false,
  ): UiState.Showing =
      UiState.Showing(
          slides = slides,
          fromCache = fromCache,
          isRefreshing = isRefreshing,
          orderResetToken = orderResetToken,
      )

  private fun appendSlides(batch: List<AnimeSlide>) {
    val current = getCurrentShowing() ?: return
    val existingIds = current.slides.asSequence().map { it.id }.toSet()
    val merged = current.slides + batch.filter { it.id !in existingIds }
    if (merged.size == current.slides.size) return
    onRootState(current.copy(slides = merged))
  }

  private fun personalListEmptyMessage(statuses: Set<ListStatus>, filtersExcludedAll: Boolean = false): String {
    if (filtersExcludedAll) {
      return "Nothing on your lists matches the current filters. Try broader filters in Settings, or switch to Full library."
    }
    if (statuses.isEmpty()) {
      return "Choose at least one list in Settings."
    }
    if (statuses.size > 1) {
      return "Your selected lists are empty on AniList. Try other lists in Settings, or add anime on anilist.co."
    }
    val status = statuses.first()
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

  private companion object {
    const val LOAD_MORE_THRESHOLD = 8
  }
}
