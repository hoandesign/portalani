package com.portal.portalani

import java.io.IOException
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ViewModelErrorsTest {
  @Test
  fun userVisibleError_ioException_usesMessage() {
    assertEquals("timeout", userVisibleError(IOException("timeout"), "fallback"))
  }

  @Test
  fun userVisibleError_jsonException_usesFallback() {
    assertEquals("fallback", userVisibleError(JSONException("bad json"), "fallback"))
  }

  @Test
  fun userVisibleError_blankIoMessage_usesFallback() {
    assertEquals("fallback", userVisibleError(IOException(""), "fallback"))
  }
}
