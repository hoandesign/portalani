package com.portal.portalani.data

import android.net.Uri

/** Network boundary for slideshow + calendar feeds (production: [AniListClient]). */
internal interface AniListClientPort {
  fun fetchViewer(accessToken: String): ViewerProfile

  fun fetchLibraryPages(
      filters: LibraryFilters,
      startPage: Int = 1,
      pageCount: Int = AniListClient.DEFAULT_INITIAL_PAGES,
      perPage: Int = AniListClient.DEFAULT_PER_PAGE,
      accessToken: String? = null,
  ): FetchBatchResult

  fun fetchViewerListPages(
      accessToken: String,
      userId: Int,
      status: ListStatus,
      startPage: Int = 1,
      pageCount: Int = AniListClient.DEFAULT_INITIAL_PAGES,
      perPage: Int = AniListClient.DEFAULT_PER_PAGE,
  ): FetchBatchResult

  fun fetchMediaById(id: Int, accessToken: String? = null): AnimeSlide?

  fun fetchAiringSchedules(
      airingAtGreater: Int,
      airingAtLesser: Int,
      accessToken: String? = null,
      perPage: Int = AniListClient.DEFAULT_PER_PAGE,
  ): List<CalendarAiringEntry>

  fun saveMediaListEntry(
      accessToken: String,
      mediaId: Int,
      listEntryId: Int? = null,
      status: ListStatus? = null,
      score: Float? = null,
  ): MediaListUpdate

  fun deleteMediaListEntry(accessToken: String, listEntryId: Int)

  fun toggleFavourite(accessToken: String, animeId: Int): Boolean
}

/** OAuth boundary for AniList sign-in (production: [AniListAuth]). */
internal interface AniListAuthPort {
  fun isConfigured(): Boolean

  fun buildAuthorizeUrl(state: String): Uri

  fun parseCallback(uri: Uri): AniListAuth.CallbackData?

  fun exchangeCode(code: String): String
}
