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
          slides.getJSONObject(i).toAnimeSlideOrNull()?.let { add(it) }
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
          slides.getJSONObject(i).toAnimeSlideOrNull()?.let { add(it) }
        }
      }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
  }

  fun save(key: String, slides: List<AnimeSlide>) {
    if (slides.isEmpty()) return
    val payload =
        JSONObject()
            .put("savedAt", System.currentTimeMillis())
            .put("slides", JSONArray().apply { slides.forEach { put(it.toCacheJson()) } })
    fileFor(key).writeText(payload.toString())
  }

  private fun fileFor(key: String): File = File(dir, "${key.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.json")

  companion object {
    const val DEFAULT_MAX_AGE_MS = 6 * 60 * 60 * 1000L
  }
}
