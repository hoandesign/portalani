package com.portal.portalani.data

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Persists the last fetched slideshow feed to disk so idle mode works offline. */
class AnimeSlideCache(context: Context) {
  private val dir = File(context.filesDir, "feeds").apply { mkdirs() }

  fun load(key: String, maxAgeMs: Long = DEFAULT_MAX_AGE_MS): List<AnimeSlide>? {
    val file = fileFor(key)
    if (!file.exists()) return null
    if (System.currentTimeMillis() - file.lastModified() > maxAgeMs) return null
    return runCatching {
      val root = JSONObject(file.readText())
      val slides = root.getJSONArray("slides")
      buildList {
        for (i in 0 until slides.length()) {
          slides.getJSONObject(i).toAnimeSlide()?.let { add(it) }
        }
      }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
  }

  fun loadStale(key: String): List<AnimeSlide>? {
    val file = fileFor(key)
    if (!file.exists()) return null
    return runCatching {
      val root = JSONObject(file.readText())
      val slides = root.getJSONArray("slides")
      buildList {
        for (i in 0 until slides.length()) {
          slides.getJSONObject(i).toAnimeSlide()?.let { add(it) }
        }
      }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
  }

  fun save(key: String, slides: List<AnimeSlide>) {
    if (slides.isEmpty()) return
    val payload =
        JSONObject()
            .put("savedAt", System.currentTimeMillis())
            .put("slides", JSONArray().apply { slides.forEach { put(it.toJson()) } })
    fileFor(key).writeText(payload.toString())
  }

  private fun fileFor(key: String): File = File(dir, "${key.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.json")

  companion object {
    const val DEFAULT_MAX_AGE_MS = 6 * 60 * 60 * 1000L
  }
}

private fun AnimeSlide.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("title", title)
        .put("nativeTitle", nativeTitle)
        .put("coverUrl", coverUrl)
        .put("bannerUrl", bannerUrl)
        .put("averageScore", averageScore ?: JSONObject.NULL)
        .put("episodes", episodes ?: JSONObject.NULL)
        .put("status", status)
        .put("season", season)
        .put("seasonYear", seasonYear ?: JSONObject.NULL)
        .put("startDateYear", startDateYear ?: JSONObject.NULL)
        .put("startDateMonth", startDateMonth ?: JSONObject.NULL)
        .put("startDateDay", startDateDay ?: JSONObject.NULL)
        .put("format", format)
        .put("studio", studio)
        .put("genres", JSONArray(genres))
        .put("description", description)
        .put("rankings", JSONArray().apply { rankings.forEach { put(it.toJson()) } })
        .put("siteUrl", siteUrl)
        .put("trailerYoutubeId", trailerYoutubeId)
        .put("listEntryId", listEntryId ?: JSONObject.NULL)
        .put("listStatus", listStatus?.name)
        .put("userScore", userScore?.toDouble() ?: JSONObject.NULL)
        .put("isFavourite", isFavourite)
        .put("nextAiringEpisode", nextAiringEpisode ?: JSONObject.NULL)
        .put("nextAiringAt", nextAiringAt ?: JSONObject.NULL)

private fun JSONObject.toAnimeSlide(): AnimeSlide? {
  val cover = optString("coverUrl").takeIf { it.isNotBlank() && it != "null" } ?: return null
  val banner = optString("bannerUrl").takeIf { it.isNotBlank() && it != "null" } ?: cover
  val genres = mutableListOf<String>()
  optJSONArray("genres")?.let { arr ->
    for (i in 0 until arr.length()) {
      arr.optString(i).takeIf { it.isNotBlank() }?.let { genres += it }
    }
  }
  return AnimeSlide(
      id = getInt("id"),
      title = optString("title").ifBlank { "Anime" },
      nativeTitle = optString("nativeTitle").takeIf { it.isNotBlank() && it != "null" },
      coverUrl = cover,
      bannerUrl = banner,
      averageScore = optInt("averageScore").takeIf { it > 0 },
      episodes = optInt("episodes").takeIf { it > 0 },
      status = optString("status").takeIf { it.isNotBlank() && it != "null" },
      season = optString("season").takeIf { it.isNotBlank() && it != "null" },
      seasonYear = optInt("seasonYear").takeIf { it > 0 },
      startDateYear = optInt("startDateYear").takeIf { it > 0 },
      startDateMonth = optInt("startDateMonth").takeIf { it > 0 },
      startDateDay = optInt("startDateDay").takeIf { it > 0 },
      format = optString("format").takeIf { it.isNotBlank() && it != "null" },
      studio = optString("studio").takeIf { it.isNotBlank() && it != "null" },
      genres = genres,
      description = optString("description").takeIf { it.isNotBlank() && it != "null" },
      rankings = parseStoredRankings(),
      siteUrl = optString("siteUrl").ifBlank { "https://anilist.co/anime/${getInt("id")}" },
      trailerYoutubeId = optString("trailerYoutubeId").takeIf { it.isNotBlank() && it != "null" },
      listEntryId = optInt("listEntryId").takeIf { it > 0 },
      listStatus =
          optString("listStatus").takeIf { it.isNotBlank() && it != "null" }?.let {
            runCatching { ListStatus.valueOf(it) }.getOrNull()
          },
      userScore =
          optDouble("userScore", 0.0).takeIf { !it.isNaN() && it > 0.0 }?.toFloat()?.let {
            if (it <= 10f) it else it / 10f
          },
      isFavourite = optBoolean("isFavourite", false),
      nextAiringEpisode = optInt("nextAiringEpisode").takeIf { it > 0 },
      nextAiringAt = optInt("nextAiringAt").takeIf { it > 0 },
  )
}

private fun JSONObject.parseStoredRankings(): List<MediaRanking> {
  optJSONArray("rankings")?.let { array ->
    val parsed =
        buildList {
          for (i in 0 until array.length()) {
            array.optJSONObject(i)?.toMediaRanking()?.let { add(it) }
          }
        }
    if (parsed.isNotEmpty()) return parsed
  }
  val legacy =
      parseRankingList("ratedRankings", "ratedRanking", "ratedRankAllTime", RankType.RATED) +
          parseRankingList("popularRankings", "popularRanking", "popularRankAllTime", RankType.POPULAR)
  return MediaRanking.pickTop(legacy)
}

private fun JSONObject.parseRankingList(
    arrayKey: String,
    objectKey: String,
    legacyIntKey: String,
    defaultType: RankType,
): List<MediaRanking> {
  optJSONArray(arrayKey)?.let { array ->
    return buildList {
      for (i in 0 until array.length()) {
        array.optJSONObject(i)?.toMediaRanking(defaultType)?.let { add(it) }
      }
    }
  }
  optJSONObject(objectKey)?.toMediaRanking(defaultType)?.let { return listOf(it) }
  optInt(legacyIntKey).takeIf { it > 0 }?.let {
    return listOf(MediaRanking(it, defaultType, RankScope.ALL_TIME))
  }
  return emptyList()
}

private fun MediaRanking.toJson(): JSONObject =
    JSONObject()
        .put("rank", rank)
        .put("type", type.name)
        .put("scope", scope.name)
        .put("year", year ?: JSONObject.NULL)
        .put("season", season)

private fun JSONObject.toMediaRanking(defaultType: RankType = RankType.RATED): MediaRanking? {
  val rank = optInt("rank").takeIf { it > 0 } ?: return null
  val type =
      optString("type").takeIf { it.isNotBlank() && it != "null" }?.let {
        runCatching { RankType.valueOf(it) }.getOrNull()
      } ?: defaultType
  val scope =
      runCatching { RankScope.valueOf(optString("scope")) }.getOrNull() ?: RankScope.ALL_TIME
  return MediaRanking(
      rank = rank,
      type = type,
      scope = scope,
      year = optInt("year").takeIf { it > 0 },
      season = optString("season").takeIf { it.isNotBlank() && it != "null" },
  )
}
