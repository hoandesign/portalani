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
            .put("entries", JSONArray().apply { entries.forEach { put(it.toCacheJson()) } })
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
                entries.getJSONObject(i).toCalendarAiringEntryOrNull()?.let { add(it) }
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
