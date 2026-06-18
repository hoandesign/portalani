package com.portal.portalani.vm

import android.net.Uri
import com.portal.portalani.data.AniListAuth
import com.portal.portalani.data.AniListAuthPort

class FakeAniListAuth(
    var configured: Boolean = true,
    var exchangeToken: String = "fake-token",
    private var callbackData: AniListAuth.CallbackData? = null,
) : AniListAuthPort {
  override fun isConfigured(): Boolean = configured

  override fun buildAuthorizeUrl(state: String): Uri =
      Uri.parse("https://anilist.co/api/v2/oauth/authorize?state=$state")

  override fun parseCallback(uri: Uri): AniListAuth.CallbackData? = callbackData

  override fun exchangeCode(code: String): String = exchangeToken

  fun setCallback(
      code: String? = null,
      state: String? = null,
      error: String? = null,
  ) {
    callbackData = AniListAuth.CallbackData(code = code, state = state, error = error)
  }
}
