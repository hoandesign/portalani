package com.portal.portalani.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.portal.portalani.data.AnimeSlide
import kotlin.math.ceil
import kotlin.math.max

internal fun Modifier.calendarThumbPlaceholderBlur(): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      graphicsLayer { scaleX = 1.08f; scaleY = 1.08f }
          .blur(24.dp)
    } else {
      graphicsLayer {
        scaleX = 1.18f
        scaleY = 1.18f
      }
    }

@Composable
internal fun CalendarDetailPosterImage(
    slide: AnimeSlide,
    thumbWidthPx: Int,
    thumbHeightPx: Int,
    detailWidthPx: Int,
    detailHeightPx: Int,
    modifier: Modifier = Modifier,
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val thumbRequest =
      remember(slide.id, thumbWidthPx, thumbHeightPx) {
        calendarCoverImageRequest(
            context = context,
            coverUrl = slide.coverUrl,
            mediaId = slide.id,
            widthPx = thumbWidthPx,
            heightPx = thumbHeightPx,
            crossfade = false,
        )
      }
  val detailRequest =
      remember(slide.id, detailWidthPx, detailHeightPx) {
        slide.coverImageRequest(
            context = context,
            widthPx = detailWidthPx,
            heightPx = detailHeightPx,
            cacheKey = "cover-detail-${slide.id}",
            crossfade = false,
            placeholderMemoryCacheKey = "cal-thumb-${slide.id}",
        )
      }

  val thumbPainter = rememberAsyncImagePainter(model = thumbRequest)
  val detailPainter = rememberAsyncImagePainter(model = detailRequest)

  val detailSuccess = detailPainter.state is AsyncImagePainter.State.Success
  val detailLoading =
      detailPainter.state is AsyncImagePainter.State.Loading ||
          detailPainter.state is AsyncImagePainter.State.Empty
  val thumbHasContent =
      thumbPainter.state is AsyncImagePainter.State.Success ||
          thumbPainter.state is AsyncImagePainter.State.Loading

  Box(modifier = modifier) {
    if (!detailSuccess) {
      if (thumbHasContent) {
        Image(
            painter = thumbPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().calendarThumbPlaceholderBlur(),
        )
      } else {
        Box(Modifier.fillMaxSize().background(Color(0xFF141820)))
      }
      Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
    }

    if (detailSuccess) {
      Image(
          painter = detailPainter,
          contentDescription = slide.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
      )
    }

    if (!detailSuccess && detailLoading) {
      CircularProgressIndicator(
          modifier = Modifier.align(Alignment.Center).size(36.dp),
          color = PortalAniColors.Cyan,
          strokeWidth = 2.5.dp,
      )
    }
  }
}

internal fun AnimeSlide.coverImageRequest(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    cacheKey: String,
    crossfade: Boolean = true,
    placeholderMemoryCacheKey: String? = null,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(coverUrl)
        .size(max(1, widthPx), max(1, heightPx))
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .apply {
          if (placeholderMemoryCacheKey != null) {
            placeholderMemoryCacheKey(placeholderMemoryCacheKey)
          }
        }
        .crossfade(crossfade)
        .build()

/** Full-resolution cover for poster / informative slideshow. */
internal fun AnimeSlide.coverImageRequest(context: Context): ImageRequest =
    ImageRequest.Builder(context)
        .data(coverUrl)
        .memoryCacheKey("cover-$id")
        .diskCacheKey("cover-$id")
        .crossfade(true)
        .build()

internal fun AnimeSlide.bannerImageRequest(context: Context): ImageRequest =
    ImageRequest.Builder(context)
        .data(bannerUrl)
        .crossfade(true)
        .memoryCacheKey("banner-$id")
        .diskCacheKey("banner-$id")
        .build()

/** Calendar grid thumbnail — separate cache from detail so expansion loads a sharp image. */
internal fun calendarCoverImageRequest(
    context: Context,
    coverUrl: String,
    mediaId: Int,
    widthPx: Int,
    heightPx: Int,
    crossfade: Boolean = true,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(coverUrl)
        .size(max(1, widthPx), max(1, heightPx))
        .memoryCacheKey("cal-thumb-$mediaId")
        .diskCacheKey("cal-thumb-$mediaId")
        .crossfade(crossfade)
        .build()

internal fun detailCoverSizePx(displayWidthPx: Float, displayHeightPx: Float): Pair<Int, Int> {
  val scale = 2f
  return ceil(displayWidthPx * scale).toInt() to ceil(displayHeightPx * scale).toInt()
}
