package com.portal.portalani.data

import android.net.Uri
import com.portal.portalani.BuildConfig
import java.io.IOException
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AniListAuth(
    private val http: OkHttpClient,
    private val tokens: TokenStore,
) : AniListAuthPort {
  val redirectUri: String = BuildConfig.ANILIST_REDIRECT_URI

  override fun isConfigured(): Boolean =
      BuildConfig.ANILIST_CLIENT_ID.isNotBlank() && BuildConfig.ANILIST_CLIENT_SECRET.isNotBlank()

  override fun buildAuthorizeUrl(state: String): Uri =
      Uri.parse(AUTHORIZE_URL)
          .buildUpon()
          .appendQueryParameter("client_id", BuildConfig.ANILIST_CLIENT_ID)
          .appendQueryParameter("redirect_uri", redirectUri)
          .appendQueryParameter("response_type", "code")
          .appendQueryParameter("state", state)
          .build()

  @Throws(IOException::class)
  override fun exchangeCode(code: String): String {
    val body =
        JSONObject()
            .put("grant_type", "authorization_code")
            .put("client_id", BuildConfig.ANILIST_CLIENT_ID)
            .put("client_secret", BuildConfig.ANILIST_CLIENT_SECRET)
            .put("redirect_uri", redirectUri)
            .put("code", code)
            .toString()
            .toRequestBody("application/json".toMediaType())

    val request =
        Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .build()

    http.newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IOException("Token exchange failed (${response.code}): $raw")
      }
      val token = JSONObject(raw).optString("access_token")
      if (token.isBlank()) throw IOException("No access_token in response")
      tokens.saveAccessToken(token)
      return token
    }
  }

  override fun parseCallback(uri: Uri): CallbackData? {
    if (uri.scheme != "portalani" || uri.host != "callback") return null
    return CallbackData(
        code = uri.getQueryParameter("code"),
        state = uri.getQueryParameter("state"),
        error = uri.getQueryParameter("error"),
    )
  }

  data class CallbackData(
      val code: String?,
      val state: String?,
      val error: String?,
  )

  companion object {
    private const val AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize"
    private const val TOKEN_URL = "https://anilist.co/api/v2/oauth/token"
  }
}
