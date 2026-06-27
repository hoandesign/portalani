package com.portal.portalani.data

import org.json.JSONArray
import org.json.JSONObject

/** Parses AniList GraphQL JSON nodes into domain models (testable without HTTP). */
internal object AniListJsonParser {
  fun parseMediaListPage(page: JSONObject): List<AnimeSlide> {
    val entries = page.optJSONArray("mediaList") ?: JSONArray()
    val slides = mutableListOf<AnimeSlide>()
    for (i in 0 until entries.length()) {
      val entry = entries.getJSONObject(i)
      val media = entry.optJSONObject("media") ?: continue
      parseMedia(media, entry)?.let { slides += it }
    }
    return slides
  }

  fun parseMediaPage(page: JSONObject): List<AnimeSlide> {
    val media = page.optJSONArray("media") ?: JSONArray()
    val slides = mutableListOf<AnimeSlide>()
    for (i in 0 until media.length()) {
      parseMedia(media.getJSONObject(i))?.let { slides += it }
    }
    return slides
  }

  fun parseMedia(media: JSONObject, listEntry: JSONObject? = null): AnimeSlide? {
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
    val tags = parseTagNames(media)

    val rankings = parseRankings(media)
    val id = media.getInt("id")
    val entry = listEntry ?: media.optJSONObject("mediaListEntry")
    val listEntryId = entry?.optInt("id")?.takeIf { it > 0 }
    val listStatus = parseListStatus(entry?.optString("status"))
    val userScore = entry?.let { parseUserScore(it.optDouble("score", 0.0)) }
    val isFavourite = media.optBoolean("isFavourite", false)
    val studio = parseMainStudio(media)
    val nextAiring = media.optJSONObject("nextAiringEpisode")
    val startDate = media.optJSONObject("startDate")

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
        startDateYear = startDate?.optInt("year")?.takeIf { it > 0 },
        startDateMonth = startDate?.optInt("month")?.takeIf { it > 0 },
        startDateDay = startDate?.optInt("day")?.takeIf { it > 0 },
        format = media.optString("format").takeIf { it.isNotBlank() && it != "null" },
        countryOfOrigin = media.optString("countryOfOrigin").takeIf { it.isNotBlank() && it != "null" },
        source = media.optString("source").takeIf { it.isNotBlank() && it != "null" },
        tags = tags,
        studio = studio,
        genres = genres,
        description = cleanDescription(media.optString("description")),
        rankings = rankings,
        siteUrl = media.optString("siteUrl").takeIf { it.isNotBlank() && it != "null" }
            ?: "https://anilist.co/anime/$id",
        trailerYoutubeId = parseYoutubeTrailerId(media),
        listEntryId = listEntryId,
        listStatus = listStatus,
        userScore = userScore,
        isFavourite = isFavourite,
        nextAiringEpisode = nextAiring?.optInt("episode")?.takeIf { it > 0 },
        nextAiringAt = nextAiring?.optInt("airingAt")?.takeIf { it > 0 },
    )
  }

  fun parseAiringSchedule(node: JSONObject): CalendarAiringEntry? {
    val media = node.optJSONObject("media") ?: return null
    val coverImage = media.optJSONObject("coverImage")
    val cover =
        sequenceOf("extraLarge", "large", "medium")
            .mapNotNull { key ->
              coverImage?.optString(key).orEmpty().takeIf { it.isNotBlank() && it != "null" }
            }
            .firstOrNull()
            ?: return null
    val titleObj = media.optJSONObject("title")
    val english = titleObj?.optString("english").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val romaji = titleObj?.optString("romaji").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val entry = media.optJSONObject("mediaListEntry")
    val startDate = media.optJSONObject("startDate")
    val genres = mutableListOf<String>()
    media.optJSONArray("genres")?.let { arr ->
      for (i in 0 until arr.length()) {
        arr.optString(i).takeIf { it.isNotBlank() }?.let { genres += it }
      }
    }
    val tags = parseTagNames(media)
    return CalendarAiringEntry(
        scheduleId = node.getInt("id"),
        mediaId = media.getInt("id"),
        englishTitle = english ?: romaji ?: "Anime",
        coverUrl = cover,
        episode = node.optInt("episode").takeIf { it > 0 } ?: return null,
        airingAt = node.optInt("airingAt").takeIf { it > 0 } ?: return null,
        format = media.optString("format").takeIf { it.isNotBlank() && it != "null" },
        countryOfOrigin = media.optString("countryOfOrigin").takeIf { it.isNotBlank() && it != "null" },
        source = media.optString("source").takeIf { it.isNotBlank() && it != "null" },
        tags = tags,
        season = media.optString("season").takeIf { it.isNotBlank() && it != "null" },
        seasonYear = media.optInt("seasonYear").takeIf { it > 0 },
        startDateYear = startDate?.optInt("year")?.takeIf { it > 0 },
        startDateMonth = startDate?.optInt("month")?.takeIf { it > 0 },
        startDateDay = startDate?.optInt("day")?.takeIf { it > 0 },
        episodes = media.optInt("episodes").takeIf { it > 0 },
        status = media.optString("status").takeIf { it.isNotBlank() && it != "null" },
        averageScore = media.optInt("averageScore").takeIf { it > 0 },
        popularity = media.optInt("popularity").takeIf { it > 0 },
        listStatus = parseListStatus(entry?.optString("status")),
        genres = genres,
    )
  }

  private fun parseTagNames(media: JSONObject): List<String> {
    val tags = media.optJSONArray("tags") ?: return emptyList()
    val names = mutableListOf<String>()
    for (i in 0 until tags.length()) {
      tags.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { names += it }
    }
    return names
  }

  private fun parseMainStudio(media: JSONObject): String? {
    val nodes = media.optJSONObject("studios")?.optJSONArray("nodes") ?: return null
    for (i in 0 until nodes.length()) {
      val name = nodes.optJSONObject(i)?.optString("name").orEmpty()
      if (name.isNotBlank() && name != "null") return name
    }
    return null
  }

  private fun parseRankings(media: JSONObject): List<MediaRanking> {
    val rankings = media.optJSONArray("rankings") ?: return emptyList()
    val all = mutableListOf<MediaRanking>()
    for (i in 0 until rankings.length()) {
      parseRankingEntry(rankings.getJSONObject(i))?.let { all += it }
    }
    return MediaRanking.pickTop(all)
  }

  private fun parseRankingEntry(entry: JSONObject): MediaRanking? {
    val rank = entry.optInt("rank").takeIf { it > 0 } ?: return null
    val scope = rankingScope(entry) ?: return null
    val type =
        when (entry.optString("type")) {
          "RATED" -> RankType.RATED
          "POPULAR" -> RankType.POPULAR
          else -> return null
        }
    return MediaRanking(
        rank = rank,
        type = type,
        scope = scope,
        year = entry.optInt("year").takeIf { it > 0 },
        season = entry.optString("season").takeIf { it.isNotBlank() && it != "null" },
    )
  }

  private fun rankingScope(entry: JSONObject): RankScope? =
      when {
        entry.optBoolean("allTime", false) -> RankScope.ALL_TIME
        entry.optString("season").takeIf { it.isNotBlank() && it != "null" } != null -> RankScope.SEASON
        entry.optInt("year").takeIf { it > 0 } != null -> RankScope.YEAR
        else -> null
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

  fun parseListStatus(raw: String?): ListStatus? = enumValueOrNull<ListStatus>(raw)

  fun parseUserScore(raw: Double): Float? {
    if (raw <= 0.0) return null
    return if (raw <= 10.0) raw.toFloat() else (raw / 10.0).toFloat()
  }

  fun parseRelatedMedia(media: JSONObject, sourceId: Int): List<RelatedAnime> {
    val edges = parseRelationEdges(media)
    val nodes = media.optJSONObject("recommendations")?.optJSONArray("nodes") ?: JSONArray()
    val recommendations =
        (0 until nodes.length()).mapNotNull { index ->
          nodes.optJSONObject(index)?.optJSONObject("mediaRecommendation")
        }
    return mergeRelatedMedia(
        sourceId = sourceId,
        relationEdges = edges,
        recommendationNodes = recommendations,
    )
  }

  fun mergeRelatedMedia(
      sourceId: Int,
      franchiseCluster: List<RelatedAnime> = emptyList(),
      relationEdges: List<Pair<String, JSONObject>> = emptyList(),
      recommendationNodes: List<JSONObject> = emptyList(),
  ): List<RelatedAnime> {
    val seen = mutableSetOf(sourceId)
    val relations = mutableListOf<RelatedAnime>()

    fun addRelation(item: RelatedAnime) {
      if (item.id == sourceId || !seen.add(item.id)) return
      relations += item
    }

    franchiseCluster.forEach(::addRelation)

    val supplemental =
        relationEdges
            .asSequence()
            .mapNotNull { (relationType, node) -> relatedFromEdge(relationType, node) }
            .sortedWith(compareBy({ relationSortKey(it.kindLabel) }, { it.sortYear ?: Int.MAX_VALUE }, { it.title }))
            .toList()
    supplemental.forEach(::addRelation)

    val recommendations = mutableListOf<RelatedAnime>()
    for (node in recommendationNodes) {
      parseRelatedNode(node, "Recommendation")?.let { item ->
        if (item.id != sourceId && seen.add(item.id)) {
          recommendations += item.copy(isRecommendation = true)
        }
      }
    }

    return relations + recommendations
  }

  fun relatedFromEdge(relationType: String, node: JSONObject): RelatedAnime? {
    if (!node.optString("type").equals("ANIME", ignoreCase = true)) return null
    return parseRelatedNode(node, formatRelationLabel(relationType))
  }

  fun parseRelationEdges(media: JSONObject): List<Pair<String, JSONObject>> {
    val edges = media.optJSONObject("relations")?.optJSONArray("edges") ?: return emptyList()
    val parsed = mutableListOf<Pair<String, JSONObject>>()
    for (i in 0 until edges.length()) {
      val edge = edges.optJSONObject(i) ?: continue
      val node = edge.optJSONObject("node") ?: continue
      val relationType = edge.optString("relationType").takeIf { it.isNotBlank() && it != "null" } ?: continue
      parsed += relationType to node
    }
    return parsed
  }

  fun parseRecommendationPageInfo(media: JSONObject): Boolean? {
    val pageInfo = media.optJSONObject("recommendations")?.optJSONObject("pageInfo") ?: return null
    return if (pageInfo.has("hasNextPage")) pageInfo.optBoolean("hasNextPage") else null
  }

  private fun relationSortKey(label: String?): Int =
      when (label?.lowercase()) {
        "prequel" -> 0
        "sequel" -> 1
        "parent" -> 2
        "side story" -> 3
        "spin off" -> 4
        "adaptation" -> 5
        "source" -> 6
        "summary" -> 7
        "alternative" -> 8
        "other" -> 9
        "character" -> 10
        "contains" -> 11
        "compilation" -> 12
        else -> 13
      }

  private fun parseRelatedNode(media: JSONObject, kindLabel: String?): RelatedAnime? {
    val coverImage = media.optJSONObject("coverImage")
    val cover =
        sequenceOf("large", "medium")
            .mapNotNull { key ->
              coverImage?.optString(key).orEmpty().takeIf { it.isNotBlank() && it != "null" }
            }
            .firstOrNull()
            ?: return null

    val titleObj = media.optJSONObject("title")
    val english = titleObj?.optString("english").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val romaji = titleObj?.optString("romaji").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val native = titleObj?.optString("native").orEmpty().takeIf { it.isNotBlank() && it != "null" }
    val sortYear = media.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 }

    return RelatedAnime(
        id = media.getInt("id"),
        title = english ?: romaji ?: native ?: "Anime",
        coverUrl = cover,
        averageScore = media.optInt("averageScore").takeIf { it > 0 },
        kindLabel = kindLabel,
        sortYear = sortYear,
    )
  }

  private fun formatRelationLabel(raw: String): String =
      raw
          .lowercase()
          .split('_')
          .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

}
