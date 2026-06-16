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

class AniListClient(private val http: OkHttpClient) {
  @Throws(IOException::class)
  fun fetchViewer(accessToken: String): ViewerProfile {
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
    filters.format.apiValue?.let { variables.put("format", JSONArray().put(it)) }
    season.season?.let { variables.put("season", it) }
    if (season.season != null) {
      season.seasonYear?.let { variables.put("seasonYear", it) }
    } else {
      season.startDateGreater()?.let { variables.put("startDate_greater", it) }
      season.startDateLesser()?.let { variables.put("startDate_lesser", it) }
    }

    val query = if (accessToken != null) LIBRARY_AUTH_QUERY else LIBRARY_QUERY
    val data = postGraphQl(query, variables, accessToken)
    return parseMediaPage(data.getJSONObject("Page"))
  }

  @Throws(IOException::class)
  fun fetchLibraryPages(
      filters: LibraryFilters,
      startPage: Int = 1,
      pageCount: Int = DEFAULT_INITIAL_PAGES,
      perPage: Int = DEFAULT_PER_PAGE,
      accessToken: String? = null,
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
  fun fetchViewerListPages(
      accessToken: String,
      userId: Int,
      status: ListStatus,
      startPage: Int = 1,
      pageCount: Int = DEFAULT_INITIAL_PAGES,
      perPage: Int = DEFAULT_PER_PAGE,
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
        parseMediaListPage(pageNode)
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
  fun saveMediaListEntry(
      accessToken: String,
      mediaId: Int,
      listEntryId: Int? = null,
      status: ListStatus? = null,
      score: Float? = null,
  ): MediaListUpdate {
    val variables = JSONObject().put("mediaId", mediaId)
    listEntryId?.let { variables.put("id", it) }
    status?.let { variables.put("status", it.apiValue) }
    when {
      score == null -> Unit
      score <= 0f -> variables.put("score", 0)
      else -> variables.put("score", score.toDouble())
    }

    val data = postGraphQl(SAVE_MEDIA_LIST_ENTRY_MUTATION, variables, accessToken)
    val entry = data.getJSONObject("SaveMediaListEntry")
    return MediaListUpdate(
        listEntryId = entry.getInt("id"),
        listStatus = entry.optString("status").toListStatusOrNull(),
        userScore = parseUserScore(entry.optDouble("score", 0.0)),
    )
  }

  @Throws(IOException::class)
  fun deleteMediaListEntry(accessToken: String, listEntryId: Int) {
    val variables = JSONObject().put("id", listEntryId)
    postGraphQl(DELETE_MEDIA_LIST_ENTRY_MUTATION, variables, accessToken)
  }

  @Throws(IOException::class)
  fun toggleFavourite(accessToken: String, animeId: Int): Boolean {
    val variables = JSONObject().put("animeId", animeId)
    val data = postGraphQl(TOGGLE_FAVOURITE_MUTATION, variables, accessToken)
    val favourites = data.getJSONObject("ToggleFavourite")
    val anime = favourites.optJSONObject("anime")?.optJSONArray("nodes") ?: JSONArray()
    for (i in 0 until anime.length()) {
      if (anime.getJSONObject(i).optInt("id") == animeId) return true
    }
    return false
  }

  private fun parseMediaListPage(page: JSONObject): List<AnimeSlide> {
    val entries = page.optJSONArray("mediaList") ?: JSONArray()
    val slides = mutableListOf<AnimeSlide>()
    for (i in 0 until entries.length()) {
      val entry = entries.getJSONObject(i)
      val media = entry.optJSONObject("media") ?: continue
      parseMedia(media, entry)?.let { slides += it }
    }
    return slides
  }

  private fun parseMediaPage(page: JSONObject): List<AnimeSlide> {
    val media = page.optJSONArray("media") ?: JSONArray()
    val slides = mutableListOf<AnimeSlide>()
    for (i in 0 until media.length()) {
      parseMedia(media.getJSONObject(i))?.let { slides += it }
    }
    return slides
  }

  private fun parseMedia(media: JSONObject, listEntry: JSONObject? = null): AnimeSlide? {
    val coverImage = media.optJSONObject("coverImage")
    val cover =
        sequenceOf("extraLarge", "large", "medium")
            .mapNotNull { key ->
              coverImage?.optString(key).orEmpty().takeIf { it.isNotBlank() && it != "null" }
            }
            .firstOrNull()
    val banner = media.optString("bannerImage").takeIf { it.isNotBlank() && it != "null" }
    val coverUrl = cover ?: banner ?: return null
    val bannerUrl = banner ?: cover ?: coverUrl

    val titleObj = media.optJSONObject("title")
    val english = titleObj?.optString("english").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val romaji = titleObj?.optString("romaji").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val native = titleObj?.optString("native").orEmpty().takeIf { it.isNotBlank() && it != "null" }

    val genres = mutableListOf<String>()
    media.optJSONArray("genres")?.let { arr ->
      for (i in 0 until arr.length()) {
        arr.optString(i).takeIf { it.isNotBlank() }?.let { genres += it }
      }
    }

    val (ratedRank, popularRank) = parseAllTimeRankings(media)
    val id = media.getInt("id")
    val entry = listEntry ?: media.optJSONObject("mediaListEntry")
    val listEntryId = entry?.optInt("id")?.takeIf { it > 0 }
    val listStatus = entry?.optString("status").toListStatusOrNull()
    val userScore = entry?.let { parseUserScore(it.optDouble("score", 0.0)) }
    val isFavourite = media.optBoolean("isFavourite", false)
    val studio = parseMainStudio(media)

    return AnimeSlide(
        id = id,
        title = english ?: romaji ?: native ?: "Anime",
        nativeTitle = native?.takeIf { it != (english ?: romaji) },
        coverUrl = coverUrl,
        bannerUrl = bannerUrl,
        averageScore = media.optInt("averageScore").takeIf { it > 0 },
        episodes = media.optInt("episodes").takeIf { it > 0 },
        status = media.optString("status").takeIf { it.isNotBlank() && it != "null" },
        season = media.optString("season").takeIf { it.isNotBlank() && it != "null" },
        seasonYear = media.optInt("seasonYear").takeIf { it > 0 },
        startDateYear = media.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 },
        format = media.optString("format").takeIf { it.isNotBlank() && it != "null" },
        studio = studio,
        genres = genres,
        description = cleanDescription(media.optString("description")),
        ratedRankAllTime = ratedRank,
        popularRankAllTime = popularRank,
        siteUrl = media.optString("siteUrl").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://anilist.co/anime/$id",
        trailerYoutubeId = parseYoutubeTrailerId(media),
        listEntryId = listEntryId,
        listStatus = listStatus,
        userScore = userScore,
        isFavourite = isFavourite,
    )
  }

  private fun parseMainStudio(media: JSONObject): String? {
    val nodes = media.optJSONObject("studios")?.optJSONArray("nodes") ?: return null
    for (i in 0 until nodes.length()) {
      val name = nodes.optJSONObject(i)?.optString("name").orEmpty()
      if (name.isNotBlank() && name != "null") return name
    }
    return null
  }

  private fun parseAllTimeRankings(media: JSONObject): Pair<Int?, Int?> {
    var rated: Int? = null
    var popular: Int? = null
    val rankings = media.optJSONArray("rankings") ?: return null to null
    for (i in 0 until rankings.length()) {
      val entry = rankings.getJSONObject(i)
      if (!entry.optBoolean("allTime", false)) continue
      when (entry.optString("type")) {
        "RATED" -> rated = entry.optInt("rank").takeIf { it > 0 }
        "POPULAR" -> popular = entry.optInt("rank").takeIf { it > 0 }
      }
    }
    return rated to popular
  }

  private fun parseYoutubeTrailerId(media: JSONObject): String? {
    val trailer = media.optJSONObject("trailer") ?: return null
    if (!trailer.optString("site").equals("youtube", ignoreCase = true)) return null
    return trailer.optString("id").takeIf { it.isNotBlank() && it != "null" }
  }

  private fun cleanDescription(raw: String?): String? {
    if (raw.isNullOrBlank() || raw == "null") return null
    return raw
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotBlank() }
  }

  private fun parseUserScore(raw: Double): Float? {
    if (raw <= 0.0) return null
    return if (raw <= 10.0) raw.toFloat() else (raw / 10.0).toFloat()
  }

  private fun String?.toListStatusOrNull(): ListStatus? {
    if (isNullOrBlank() || this == "null") return null
    return runCatching { ListStatus.valueOf(this) }.getOrNull()
  }

  @Throws(IOException::class)
  private fun postGraphQl(
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
        throw IOException("GraphQL failed (${response.code}): $raw")
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
        season
        seasonYear
        startDate { year month day }
        format
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
          ${'$'}season: MediaSeason,
          ${'$'}seasonYear: Int,
          ${'$'}startDate_greater: FuzzyDateInt,
          ${'$'}startDate_lesser: FuzzyDateInt,
          ${'$'}sort: [MediaSort]
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(
              type: ANIME
              isAdult: false
              format_in: ${'$'}format
              season: ${'$'}season
              seasonYear: ${'$'}seasonYear
              startDate_greater: ${'$'}startDate_greater
              startDate_lesser: ${'$'}startDate_lesser
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
          ${'$'}season: MediaSeason,
          ${'$'}seasonYear: Int,
          ${'$'}startDate_greater: FuzzyDateInt,
          ${'$'}startDate_lesser: FuzzyDateInt,
          ${'$'}sort: [MediaSort]
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(
              type: ANIME
              isAdult: false
              format_in: ${'$'}format
              season: ${'$'}season
              seasonYear: ${'$'}seasonYear
              startDate_greater: ${'$'}startDate_greater
              startDate_lesser: ${'$'}startDate_lesser
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
