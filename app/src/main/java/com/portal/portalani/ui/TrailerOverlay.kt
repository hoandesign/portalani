package com.portal.portalani.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.portal.portalani.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerOverlay(
    youtubeId: String,
    onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xE805070C))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
    )

    Column(
        modifier =
            Modifier.align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .clip(PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass)
                .border(1.dp, PortalAniColors.BorderStrong, PortalAniShapes.Card)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = stringResource(R.string.trailer_title),
            color = PortalAniColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        PortalCircleIconButton(
            icon = PortalIcons.Close,
            contentDescription = stringResource(R.string.close_trailer),
            onClick = onDismiss,
        )
      }

      AndroidView(
          modifier =
              Modifier.fillMaxWidth()
                  .aspectRatio(16f / 9f)
                  .clip(PortalAniShapes.Panel)
                  .border(1.dp, PortalAniColors.Border, PortalAniShapes.Panel),
          factory = { context ->
            WebView(context).apply {
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.mediaPlaybackRequiresUserGesture = false
              webChromeClient = WebChromeClient()
              webViewClient = WebViewClient()
              setBackgroundColor(android.graphics.Color.BLACK)
              loadUrl("https://www.youtube.com/embed/$youtubeId?autoplay=1&rel=0&playsinline=1")
            }
          },
      )

      Text(
          text = stringResource(R.string.trailer_dismiss_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 14.sp,
          modifier = Modifier.padding(top = 14.dp),
      )
    }
  }
}
