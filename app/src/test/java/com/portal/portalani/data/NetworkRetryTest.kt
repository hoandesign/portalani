package com.portal.portalani.data

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkRetryTest {
  @Test
  fun withRetry_succeedsOnFirstAttempt() {
    val calls = AtomicInteger(0)
    val result = NetworkRetry.withRetry {
      calls.incrementAndGet()
      "ok"
    }
    assertEquals("ok", result)
    assertEquals(1, calls.get())
  }

  @Test
  fun withRetry_retriesIOExceptionThenSucceeds() {
    val calls = AtomicInteger(0)
    val result =
        NetworkRetry.withRetry {
          val n = calls.incrementAndGet()
          if (n <= 2) throw IOException("transient")
          "recovered"
        }
    assertEquals("recovered", result)
    assertEquals(3, calls.get())
  }

  @Test
  fun withRetry_doesNotRetryAniListHttpException() {
    val calls = AtomicInteger(0)
    val ex =
        assertThrows(AniListHttpException::class.java) {
          NetworkRetry.withRetry {
            calls.incrementAndGet()
            throw AniListHttpException(401, "Unauthorized")
          }
        }
    assertEquals(401, ex.code)
    assertEquals(1, calls.get())
  }

  @Test
  fun withRetry_exhaustsRetriesAndThrowsLastIOException() {
    val calls = AtomicInteger(0)
    val ex =
        assertThrows(IOException::class.java) {
          NetworkRetry.withRetry {
            calls.incrementAndGet()
            throw IOException("still down")
          }
        }
    assertEquals("still down", ex.message)
    assertEquals(3, calls.get())
  }
}
