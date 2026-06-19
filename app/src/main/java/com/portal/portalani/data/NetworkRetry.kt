package com.portal.portalani.data

import java.io.IOException

/** Retries idempotent blocking network calls on transient I/O failures. */
object NetworkRetry {
  private const val MAX_RETRIES = 2
  private val BACKOFF_MS = longArrayOf(300, 900)

  @Throws(IOException::class)
  fun <T> withRetry(block: () -> T): T {
    var lastIo: IOException? = null
    repeat(MAX_RETRIES + 1) { attempt ->
      try {
        return block()
      } catch (e: AniListHttpException) {
        throw e
      } catch (e: IOException) {
        lastIo = e
        if (attempt < MAX_RETRIES) {
          Thread.sleep(BACKOFF_MS[attempt])
        }
      }
    }
    throw checkNotNull(lastIo)
  }
}
