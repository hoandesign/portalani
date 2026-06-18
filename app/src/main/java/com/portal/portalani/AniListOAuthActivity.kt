package com.portal.portalani

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.portal.portalani.ui.PortalAniColors
import com.portal.portalani.ui.PortalAniTheme

/** In-app OAuth browser — Portal Custom Tabs often fail to open custom-scheme redirects. */
class AniListOAuthActivity : ComponentActivity() {
  private var webView: WebView? = null

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enterImmersive()

    val authUrl = intent.getStringExtra(EXTRA_AUTH_URL)
    if (authUrl.isNullOrBlank()) {
      finish()
      return
    }

    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            val view = webView
            if (view != null && view.canGoBack()) {
              view.goBack()
            } else {
              notifySignInCancelled()
              finish()
            }
          }
        },
    )

    setContent {
      PortalAniTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(PortalAniColors.Background),
            contentAlignment = Alignment.TopCenter,
        ) {
          Text(
              text = "Sign in with AniList…",
              color = PortalAniColors.TextSecondary,
              fontSize = 18.sp,
              modifier = Modifier.padding(top = 72.dp),
          )
          AndroidView(
              modifier = Modifier.fillMaxSize().padding(top = 112.dp),
              factory = { context ->
                WebView(context).apply {
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  webViewClient =
                      object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                          val uri = request?.url ?: return false
                          return handleRedirect(uri)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                          val uri = url?.let(Uri::parse) ?: return false
                          return handleRedirect(uri)
                        }
                      }
                  loadUrl(authUrl)
                  webView = this
                }
              },
              update = { view -> webView = view },
          )
        }
      }
    }
  }

  private fun notifySignInCancelled() {
    startActivity(
        Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OAUTH_CANCELLED, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )
  }

  private fun handleRedirect(uri: Uri): Boolean {
    if (uri.scheme != "portalani" || uri.host != "callback") return false
    val returnIntent =
        Intent(this, MainActivity::class.java)
            .setData(uri)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    startActivity(returnIntent)
    finish()
    return true
  }

  private fun enterImmersive() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  companion object {
    const val EXTRA_AUTH_URL = "auth_url"
  }
}
