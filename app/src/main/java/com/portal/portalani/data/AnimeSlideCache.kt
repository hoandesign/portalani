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
        .put("seasonYear", seasonYear ?: JSONObject.NULL)
        .put("format", format)
        .put("studio", studio)
        .put("genres", JSONArray(genres))
        .put("description", description)
        .put("ratedRankAllTime", ratedRankAllTime ?: JSONObject.NULL)
        .put("popularRankAllTime", popularRankAllTime ?: JSONObject.NULL)
        .put("siteUrl", siteUrl)
        .put("trailerYoutubeId", trailerYoutubeId)
        .put("listEntryId", listEntryId ?: JSONObject.NULL)
        .put("listStatus", listStatus?.name)
        .put("userScore", userScore?.toDouble() ?: JSONObject.NULL)
        .put("isFavourite", isFavourite)

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
      seasonYear = optInt("seasonYear").takeIf { it > 0 },
      format = optString("format").takeIf { it.isNotBlank() && it != "null" },
      studio = optString("studio").takeIf { it.isNotBlank() && it != "null" },
      genres = genres,
      description = optString("description").takeIf { it.isNotBlank() && it != "null" },
      ratedRankAllTime = optInt("ratedRankAllTime").takeIf { it > 0 },
      popularRankAllTime = optInt("popularRankAllTime").takeIf { it > 0 },
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
  )
}
