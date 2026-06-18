package com.portal.portalani.data

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class MediaListUpdate(
    val listEntryId: Int,
    val listStatus: ListStatus?,
    val userScore: Float?,
)

data class FetchBatchResult(
    val slides: List<AnimeSlide>,
    val nextPage: Int,
    val hasMore: Boolean,
)

class AniListClient(private val http: OkHttpClient) : AniListClientPort {
  @Throws(IOException::class)
  override fun fetchViewer(accessToken: String): ViewerProfile {
    val data = postGraphQl(VIEWER_QUERY, null, accessToken)
    val viewer = data.getJSONObject("Viewer")
    return ViewerProfile(
        id = viewer.getInt("id"),
        name = viewer.getString("name"),
    )
  }

  @Throws(IOException::class)
  fun fetchLibrary(
      filters: LibraryFilters,
      page: Int = 1,
      perPage: Int = DEFAULT_PER_PAGE,
      accessToken: String? = null,
  ): List<AnimeSlide> {
    val season = filters.resolvedSeason()
    val variables =
        JSONObject()
            .put("page", page)
            .put("perPage", perPage)
            .put("sort", JSONArray().put(filters.sort.apiSort))
    filters.formatApiValues()?.let { values ->
      variables.put("format", JSONArray(values))
    }
    filters.sourceApiValues()?.let { values ->
      variables.put("source", JSONArray(values))
    }
    if (filters.hideHentai) {
      variables.put("genre_not_in", JSONArray().put(HENTAI_GENRE))
    }
    season.season?.let { variables.put("season", it) }
    if (season.season != null) {
      season.seasonYear?.let { variables.put("seasonYear", it) }
    } else {
      season.startDateGreater()?.let { variables.put("startDate_greater", it) }
      season.startDateLesser()?.let { variables.put("startDate_lesser", it) }
    }

    val query = if (accessToken != null) LIBRARY_AUTH_QUERY else LIBRARY_QUERY
    val data = postGraphQl(query, variables, accessToken)
    return AniListJsonParser.parseMediaPage(data.getJSONObject("Page"))
  }

  @Throws(IOException::class)
  override fun fetchLibraryPages(
      filters: LibraryFilters,
      startPage: Int,
      pageCount: Int,
      perPage: Int,
      accessToken: String?,
  ): FetchBatchResult =
      fetchPages(
          startPage = startPage,
          pageCount = pageCount,
          perPage = perPage,
          slideFilter = { slide -> filters.matchesSlide(slide) },
      ) { page -> fetchLibrary(filters, page, perPage, accessToken) }

  @Throws(IOException::class)
  fun fetchTrending(page: Int = 1, perPage: Int = DEFAULT_PER_PAGE): List<AnimeSlide> =
      fetchLibrary(
          LibraryFilters(sort = LibrarySort.TRENDING),
          page = page,
          perPage = perPage,
      )

  @Throws(IOException::class)
  override fun fetchMediaById(id: Int, accessToken: String?): AnimeSlide? {
    val variables = JSONObject().put("id", id)
    val query = if (accessToken != null) MEDIA_BY_ID_AUTH_QUERY else MEDIA_BY_ID_QUERY
    val data = postGraphQl(query, variables, accessToken)
    val media = data.optJSONObject("Media") ?: return null
    return AniListJsonParser.parseMedia(media)
  }

  @Throws(IOException::class)
  override fun fetchAiringSchedules(
      airingAtGreater: Int,
      airingAtLesser: Int,
      accessToken: String?,
      perPage: Int,
  ): List<CalendarAiringEntry> {
    val all = mutableListOf<CalendarAiringEntry>()
    var page = 1
    while (page <= 20) {
      val variables =
          JSONObject()
              .put("page", page)
              .put("perPage", perPage)
              .put("airingAt_greater", airingAtGreater)
              .put("airingAt_lesser", airingAtLesser)
      val query = if (accessToken != null) AIRING_SCHEDULE_AUTH_QUERY else AIRING_SCHEDULE_QUERY
      val data = postGraphQl(query, variables, accessToken)
      val schedules = data.getJSONObject("Page").optJSONArray("airingSchedules") ?: JSONArray()
      if (schedules.length() == 0) break
      for (i in 0 until schedules.length()) {
        AniListJsonParser.parseAiringSchedule(schedules.getJSONObject(i))?.let { all += it }
      }
      if (schedules.length() < perPage) break
      page++
    }
    return all
  }

  @Throws(IOException::class)
  fun fetchViewerList(
      accessToken: String,
      userId: Int,
      status: ListStatus,
      perPage: Int = DEFAULT_PER_PAGE,
      maxPages: Int = DEFAULT_INITIAL_PAGES,
  ): List<AnimeSlide> =
      fetchViewerListPages(accessToken, userId, status, startPage = 1, pageCount = maxPages, perPage = perPage)
          .slides

  @Throws(IOException::class)
  override fun fetchViewerListPages(
      accessToken: String,
      userId: Int,
      status: ListStatus,
      startPage: Int,
      pageCount: Int,
      perPage: Int,
  ): FetchBatchResult =
      fetchPages(startPage = startPage, pageCount = pageCount, perPage = perPage) { page ->
        val variables =
            JSONObject()
                .put("userId", userId)
                .put("status", status.apiValue)
                .put("page", page)
                .put("perPage", perPage)
        val data = postGraphQl(VIEWER_LIST_QUERY, variables, accessToken)
        val pageNode = data.optJSONObject("Page") ?: JSONObject()
        AniListJsonParser.parseMediaListPage(pageNode)
      }

  private fun fetchPages(
      startPage: Int,
      pageCount: Int,
      perPage: Int,
      slideFilter: (AnimeSlide) -> Boolean = { true },
      fetchPage: (Int) -> List<AnimeSlide>,
  ): FetchBatchResult {
    val slides = mutableListOf<AnimeSlide>()
    var page = startPage
    val maxPages = pageCount * 4
    var pagesFetched = 0

    while (pagesFetched < maxPages) {
      val raw = fetchPage(page)
      if (raw.isEmpty()) {
        return FetchBatchResult(slides, page, hasMore = false)
      }
      slides += raw.filter(slideFilter)
      pagesFetched++
      if (raw.size < perPage) {
        return FetchBatchResult(slides, page + 1, hasMore = false)
      }
      page++
      if (pagesFetched >= pageCount && slides.size >= perPage) {
        return FetchBatchResult(slides, page, hasMore = true)
      }
    }
    return FetchBatchResult(slides, page, hasMore = slides.isNotEmpty())
  }

  @Throws(IOException::class)
  override fun saveMediaListEntry(
      accessToken: String,
      mediaId: Int,
      listEntryId: Int?,
      status: ListStatus?,
      score: Float?,
  ): MediaListUpdate {
    val variables = JSONObject().put("mediaId", mediaId)
    listEntryId?.let { variables.put("id", it) }
    status?.let { variables.put("status", it.apiValue) }
    when {
      score == null -> Unit
      score <= 0f -> variables.put("score", 0)
      else -> variables.put("score", score.toDouble())
    }

    val data = postGraphQl(SAVE_MEDIA_LIST_ENTRY_MUTATION, variables, accessToken, retryOnIo = false)
    val entry = data.getJSONObject("SaveMediaListEntry")
    return MediaListUpdate(
        listEntryId = entry.getInt("id"),
        listStatus = AniListJsonParser.parseListStatus(entry.optString("status")),
        userScore = AniListJsonParser.parseUserScore(entry.optDouble("score", 0.0)),
    )
  }

  @Throws(IOException::class)
  override fun deleteMediaListEntry(accessToken: String, listEntryId: Int) {
    val variables = JSONObject().put("id", listEntryId)
    postGraphQl(DELETE_MEDIA_LIST_ENTRY_MUTATION, variables, accessToken, retryOnIo = false)
  }

  @Throws(IOException::class)
  override fun toggleFavourite(accessToken: String, animeId: Int): Boolean {
    val variables = JSONObject().put("animeId", animeId)
    val data = postGraphQl(TOGGLE_FAVOURITE_MUTATION, variables, accessToken, retryOnIo = false)
    val favourites = data.getJSONObject("ToggleFavourite")
    val anime = favourites.optJSONObject("anime")?.optJSONArray("nodes") ?: JSONArray()
    for (i in 0 until anime.length()) {
      if (anime.getJSONObject(i).optInt("id") == animeId) return true
    }
    return false
  }

  @Throws(IOException::class)
  private fun postGraphQl(
      query: String,
      variables: JSONObject?,
      accessToken: String?,
      retryOnIo: Boolean = true,
  ): JSONObject {
    val execute = { executeGraphQl(query, variables, accessToken) }
    return if (retryOnIo) NetworkRetry.withRetry(execute) else execute()
  }

  @Throws(IOException::class)
  private fun executeGraphQl(
      query: String,
      variables: JSONObject?,
      accessToken: String?,
  ): JSONObject {
    val payload = JSONObject().put("query", query)
    if (variables != null) payload.put("variables", variables)

    val builder =
        Request.Builder()
            .url(GRAPHQL_URL)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
    if (!accessToken.isNullOrBlank()) {
      builder.header("Authorization", "Bearer $accessToken")
    }

    http.newCall(builder.build()).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw AniListHttpException(response.code, raw)
      }
      val root = JSONObject(raw)
      if (root.has("errors")) {
        throw IOException(root.getJSONArray("errors").toString())
      }
      return root.getJSONObject("data")
    }
  }

  companion object {
    private const val GRAPHQL_URL = "https://graphql.anilist.co"
    const val DEFAULT_PER_PAGE = 50
    const val DEFAULT_INITIAL_PAGES = 5
    const val DEFAULT_LOAD_MORE_PAGES = 2

    private val MEDIA_FIELDS =
        """
        id
        title { romaji english native }
        bannerImage
        coverImage { extraLarge large medium }
        averageScore
        episodes
        status
        nextAiringEpisode {
          episode
          airingAt
        }
        season
        seasonYear
        startDate { year month day }
        format
        countryOfOrigin
        source
        tags {
          name
        }
        genres
        studios(isMain: true) {
          nodes {
            name
          }
        }
        description(asHtml: false)
        siteUrl
        trailer { id site }
        rankings {
          rank
          type
          allTime
          year
          season
        }
        """

    private val MEDIA_USER_FIELDS =
        """
        isFavourite
        mediaListEntry {
          id
          status
          score
        }
        """

    private val MEDIA_BY_ID_QUERY =
        """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            $MEDIA_FIELDS
          }
        }
        """
            .trimIndent()

    private val MEDIA_BY_ID_AUTH_QUERY =
        """
        query (${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            $MEDIA_FIELDS
            $MEDIA_USER_FIELDS
          }
        }
        """
            .trimIndent()

    private val VIEWER_QUERY =
        """
        query {
          Viewer {
            id
            name
          }
        }
        """
            .trimIndent()

    private val LIBRARY_QUERY =
        """
        query (
          ${'$'}page: Int,
          ${'$'}perPage: Int,
          ${'$'}format: [MediaFormat],
          ${'$'}source: [MediaSource],
          ${'$'}season: MediaSeason,
          ${'$'}seasonYear: Int,
          ${'$'}startDate_greater: FuzzyDateInt,
          ${'$'}startDate_lesser: FuzzyDateInt,
          ${'$'}genre_not_in: [String],
          ${'$'}sort: [MediaSort]
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(
              type: ANIME
              isAdult: false
              format_in: ${'$'}format
              source_in: ${'$'}source
              season: ${'$'}season
              seasonYear: ${'$'}seasonYear
              startDate_greater: ${'$'}startDate_greater
              startDate_lesser: ${'$'}startDate_lesser
              genre_not_in: ${'$'}genre_not_in
              sort: ${'$'}sort
            ) {
              $MEDIA_FIELDS
            }
          }
        }
        """
            .trimIndent()

    private val LIBRARY_AUTH_QUERY =
        """
        query (
          ${'$'}page: Int,
          ${'$'}perPage: Int,
          ${'$'}format: [MediaFormat],
          ${'$'}source: [MediaSource],
          ${'$'}season: MediaSeason,
          ${'$'}seasonYear: Int,
          ${'$'}startDate_greater: FuzzyDateInt,
          ${'$'}startDate_lesser: FuzzyDateInt,
          ${'$'}genre_not_in: [String],
          ${'$'}sort: [MediaSort]
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(
              type: ANIME
              isAdult: false
              format_in: ${'$'}format
              source_in: ${'$'}source
              season: ${'$'}season
              seasonYear: ${'$'}seasonYear
              startDate_greater: ${'$'}startDate_greater
              startDate_lesser: ${'$'}startDate_lesser
              genre_not_in: ${'$'}genre_not_in
              sort: ${'$'}sort
            ) {
              $MEDIA_FIELDS
              $MEDIA_USER_FIELDS
            }
          }
        }
        """
            .trimIndent()

    private val VIEWER_LIST_QUERY =
        """
        query (
          ${'$'}userId: Int,
          ${'$'}status: MediaListStatus,
          ${'$'}page: Int,
          ${'$'}perPage: Int
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            mediaList(
              userId: ${'$'}userId,
              status: ${'$'}status,
              sort: UPDATED_TIME_DESC,
              type: ANIME
            ) {
              id
              status
              score
              media {
                $MEDIA_FIELDS
                isFavourite
              }
            }
          }
        }
        """
            .trimIndent()

    private val AIRING_SCHEDULE_MEDIA_FIELDS =
        """
        id
        title { romaji english }
        coverImage { extraLarge large medium }
        format
        countryOfOrigin
        source
        tags {
          name
        }
        season
        seasonYear
        startDate { year month day }
        episodes
        status
        averageScore
        popularity
        genres
        """

    private val AIRING_SCHEDULE_QUERY =
        """
        query (
          ${'$'}page: Int,
          ${'$'}perPage: Int,
          ${'$'}airingAt_greater: Int,
          ${'$'}airingAt_lesser: Int
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            airingSchedules(
              airingAt_greater: ${'$'}airingAt_greater
              airingAt_lesser: ${'$'}airingAt_lesser
              sort: TIME
            ) {
              id
              airingAt
              episode
              mediaId
              media {
                $AIRING_SCHEDULE_MEDIA_FIELDS
              }
            }
          }
        }
        """
            .trimIndent()

    private val AIRING_SCHEDULE_AUTH_QUERY =
        """
        query (
          ${'$'}page: Int,
          ${'$'}perPage: Int,
          ${'$'}airingAt_greater: Int,
          ${'$'}airingAt_lesser: Int
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            airingSchedules(
              airingAt_greater: ${'$'}airingAt_greater
              airingAt_lesser: ${'$'}airingAt_lesser
              sort: TIME
            ) {
              id
              airingAt
              episode
              mediaId
              media {
                $AIRING_SCHEDULE_MEDIA_FIELDS
                mediaListEntry {
                  id
                  status
                }
              }
            }
          }
        }
        """
            .trimIndent()

    private val SAVE_MEDIA_LIST_ENTRY_MUTATION =
        """
        mutation (
          ${'$'}id: Int,
          ${'$'}mediaId: Int,
          ${'$'}status: MediaListStatus,
          ${'$'}score: Float
        ) {
          SaveMediaListEntry(id: ${'$'}id, mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score) {
            id
            status
            score
          }
        }
        """
            .trimIndent()

    private val DELETE_MEDIA_LIST_ENTRY_MUTATION =
        """
        mutation (${'$'}id: Int) {
          DeleteMediaListEntry(id: ${'$'}id) {
            deleted
          }
        }
        """
            .trimIndent()

    private val TOGGLE_FAVOURITE_MUTATION =
        """
        mutation (${'$'}animeId: Int) {
          ToggleFavourite(animeId: ${'$'}animeId) {
            anime {
              nodes {
                id
              }
            }
          }
        }
        """
            .trimIndent()
  }
}
