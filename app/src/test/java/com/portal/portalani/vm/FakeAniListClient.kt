package com.portal.portalani.vm

import com.portal.portalani.data.AniListClientPort
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.CalendarAiringEntry
import com.portal.portalani.data.FetchBatchResult
import com.portal.portalani.data.LibraryFilters
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.MediaListUpdate
import com.portal.portalani.data.ViewerProfile

class FakeAniListClient(
    var libraryPagesResult: FetchBatchResult = FetchBatchResult(emptyList(), 1, false),
    var viewerListPagesResult: FetchBatchResult = FetchBatchResult(emptyList(), 1, false),
    var viewerProfile: ViewerProfile = ViewerProfile(id = 1, name = "Test User"),
    var airingSchedules: List<CalendarAiringEntry> = emptyList(),
    var mediaById: AnimeSlide? = null,
) : AniListClientPort {
  var fetchLibraryPagesCalls = 0
  var fetchViewerListPagesCalls = 0
  var fetchAiringSchedulesCalls = 0
  var fetchViewerCalls = 0

  override fun fetchViewer(accessToken: String): ViewerProfile {
    fetchViewerCalls++
    return viewerProfile
  }

  override fun fetchLibraryPages(
      filters: LibraryFilters,
      startPage: Int,
      pageCount: Int,
      perPage: Int,
      accessToken: String?,
  ): FetchBatchResult {
    fetchLibraryPagesCalls++
    return libraryPagesResult
  }

  override fun fetchViewerListPages(
      accessToken: String,
      userId: Int,
      status: ListStatus,
      startPage: Int,
      pageCount: Int,
      perPage: Int,
  ): FetchBatchResult {
    fetchViewerListPagesCalls++
    return viewerListPagesResult
  }

  override fun fetchMediaById(id: Int, accessToken: String?): AnimeSlide? = mediaById

  override fun fetchAiringSchedules(
      airingAtGreater: Int,
      airingAtLesser: Int,
      accessToken: String?,
      perPage: Int,
  ): List<CalendarAiringEntry> {
    fetchAiringSchedulesCalls++
    return airingSchedules
  }

  override fun saveMediaListEntry(
      accessToken: String,
      mediaId: Int,
      listEntryId: Int?,
      status: ListStatus?,
      score: Float?,
  ): MediaListUpdate = MediaListUpdate(listEntryId ?: 1, status, score)

  override fun deleteMediaListEntry(accessToken: String, listEntryId: Int) = Unit

  override fun toggleFavourite(accessToken: String, animeId: Int): Boolean = true
}
