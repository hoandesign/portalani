package com.portal.portalani.data

import org.json.JSONArray
import org.json.JSONObject

internal fun CalendarAiringEntry.toCacheJson(): JSONObject =
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

internal fun JSONObject.toCalendarAiringEntryOrNull(): CalendarAiringEntry? {
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
          optString("listStatus").takeIf { it.isNotBlank() && it != "null" }?.let { enumValueOrNull<ListStatus>(it) },
      genres = genres,
  )
}
