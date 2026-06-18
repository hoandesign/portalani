package com.portal.portalani

import android.util.Log
import java.io.IOException
import org.json.JSONException

private const val TAG = "PortalAni"

/** Maps load/network failures to short user-facing copy; logs full stack in debug builds. */
internal fun userVisibleError(throwable: Throwable, fallback: String): String {
  if (BuildConfig.DEBUG) {
    Log.w(TAG, fallback, throwable)
  }
  return when (throwable) {
    is IOException -> throwable.message?.takeIf { it.isNotBlank() } ?: fallback
    is JSONException -> fallback
    else -> fallback
  }
}
