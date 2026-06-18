package com.portal.portalani.data

import android.content.Context
import java.io.File
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Persists weekly airing schedules so calendar swipes stay fast offline. */
class CalendarWeekCache(context: Context) {
  private val dir = File(context.filesDir, "calendar_weeks").apply { mkdirs() }

  fun load(key: String, maxAgeMs: Long = DEFAULT_MAX_AGE_MS): CachedCalendarWeek? {
    val file = fileFor(key)
    if (!file.exists()) return null
    if (System.currentTimeMillis() - file.lastModified() > maxAgeMs) return null
    return parse(file)
  }

  fun loadStale(key: String): CachedCalendarWeek? {
    val file = fileFor(key)
    if (!file.exists()) return null
    return parse(file)
  }

  fun save(key: String, weekStart: LocalDate, entries: List<CalendarAiringEntry>) {
    if (entries.isEmpty()) return
    val payload =
        JSONObject()
            .put("savedAt", System.currentTimeMillis())
            .put("weekStart", weekStart.toEpochDay())
            .put("entries", JSONArray().apply { entries.forEach { put(it.toJson()) } })
    fileFor(key).writeText(payload.toString())
  }

  private fun parse(file: File): CachedCalendarWeek? =
      runCatching {
        val root = JSONObject(file.readText())
        val weekStart = LocalDate.ofEpochDay(root.getLong("weekStart"))
        val entries = root.getJSONArray("entries")
        val parsed =
            buildList {
              for (i in 0 until entries.length()) {
                entries.getJSONObject(i).toCalendarAiringEntry()?.let { add(it) }
              }
            }
        CachedCalendarWeek(weekStart = weekStart, entries = parsed)
      }.getOrNull()?.takeIf { it.entries.isNotEmpty() }

  private fun fileFor(key: String): File = File(dir, "${key.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.json")

  companion object {
    const val DEFAULT_MAX_AGE_MS = 30 * 60 * 1000L
  }
}

data class CachedCalendarWeek(
    val weekStart: LocalDate,
    val entries: List<CalendarAiringEntry>,
)

private fun CalendarAiringEntry.toJson(): JSONObject =
    JSONObject()
        .put("scheduleId", scheduleId)
        .put("mediaId", mediaId)
        .put("englishTitle", englishTitle)
        .put("coverUrl", coverUrl)
        .put("episode", episode)
        .put("airingAt", airingAt)
        .put("format", format)
        .put("countryOfOrigin", countryOfOrigin)
        .put("source", source)
        .put("tags", JSONArray(tags))
        .put("season", season)
        .put("seasonYear", seasonYear ?: JSONObject.NULL)
        .put("startDateYear", startDateYear ?: JSONObject.NULL)
        .put("startDateMonth", startDateMonth ?: JSONObject.NULL)
        .put("startDateDay", startDateDay ?: JSONObject.NULL)
        .put("episodes", episodes ?: JSONObject.NULL)
        .put("status", status)
        .put("averageScore", averageScore ?: JSONObject.NULL)
        .put("popularity", popularity ?: JSONObject.NULL)
        .put("listStatus", listStatus?.name)
        .put("genres", JSONArray(genres))

private fun JSONObject.toCalendarAiringEntry(): CalendarAiringEntry? {
  val cover = optString("coverUrl").takeIf { it.isNotBlank() && it != "null" } ?: return null
  val episode = optInt("episode").takeIf { it > 0 } ?: return null
  val airingAt = optInt("airingAt").takeIf { it > 0 } ?: return null
  val genres = mutableListOf<String>()
  optJSONArray("genres")?.let { arr ->
    for (i in 0 until arr.length()) {
      arr.optString(i).takeIf { it.isNotBlank() }?.let { genres += it }
    }
  }
  val tags = mutableListOf<String>()
  optJSONArray("tags")?.let { arr ->
    for (i in 0 until arr.length()) {
      arr.optString(i).takeIf { it.isNotBlank() }?.let { tags += it }
    }
  }
  return CalendarAiringEntry(
      scheduleId = optInt("scheduleId"),
      mediaId = optInt("mediaId"),
      englishTitle = optString("englishTitle").ifBlank { "Anime" },
      coverUrl = cover,
      episode = episode,
      airingAt = airingAt,
      format = optString("format").takeIf { it.isNotBlank() && it != "null" },
      countryOfOrigin = optString("countryOfOrigin").takeIf { it.isNotBlank() && it != "null" },
      source = optString("source").takeIf { it.isNotBlank() && it != "null" },
      tags = tags,
      season = optString("season").takeIf { it.isNotBlank() && it != "null" },
      seasonYear = optInt("seasonYear").takeIf { it > 0 },
      startDateYear = optInt("startDateYear").takeIf { it > 0 },
      startDateMonth = optInt("startDateMonth").takeIf { it > 0 },
      startDateDay = optInt("startDateDay").takeIf { it > 0 },
      episodes = optInt("episodes").takeIf { it > 0 },
      status = optString("status").takeIf { it.isNotBlank() && it != "null" },
      averageScore = optInt("averageScore").takeIf { it > 0 },
      popularity = optInt("popularity").takeIf { it > 0 },
      listStatus =
          optString("listStatus").takeIf { it.isNotBlank() && it != "null" }?.let {
            runCatching { ListStatus.valueOf(it) }.getOrNull()
          },
      genres = genres,
  )
}
