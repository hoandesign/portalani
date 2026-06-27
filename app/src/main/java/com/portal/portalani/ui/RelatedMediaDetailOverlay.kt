package com.portal.portalani.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.portal.portalani.R
import com.portal.portalani.data.AnimeSlide

@Composable
fun RelatedMediaDetailOverlay(
    slide: AnimeSlide,
    detailLoading: Boolean,
    onDismiss: () -> Unit,
    onPlayTrailer: (() -> Unit)? = null,
    onOpenAniList: (() -> Unit)? = null,
    onTapScore: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onEditList: (() -> Unit)? = null,
    onShowRelated: (() -> Unit)? = null,
) {
  BackHandler(onBack = onDismiss)
  val context = LocalContext.current
  val shape = PortalAniShapes.Poster

  Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xF005070C))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
    )

    Column(
        modifier =
            Modifier.align(Alignment.Center)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass)
                .border(1.dp, PortalAniColors.BorderStrong, PortalAniShapes.Card)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = stringResource(R.string.related_media_detail_title),
            color = PortalAniColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        PortalCircleIconButton(
            icon = PortalIcons.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
        )
      }

      Spacer(Modifier.height(16.dp))

      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val posterHeight = maxHeight.coerceAtMost(520.dp)
        val posterWidth = PosterLayout.widthForHeight(posterHeight)
        val infoWidth = (maxWidth - posterWidth - FrameViewerInsets.posterInfoGap).coerceAtLeast(280.dp)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(FrameViewerInsets.posterInfoGap),
        ) {
          Box(
              modifier =
                  Modifier.width(posterWidth)
                      .height(posterHeight)
                      .clip(shape)
                      .border(1.dp, PortalAniColors.BorderStrong, shape),
          ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(slide.coverUrl).crossfade(true).build(),
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
          }

          Box(modifier = Modifier.width(infoWidth).fillMaxHeight()) {
            if (detailLoading) {
              InformativePanelLoadingSkeleton(modifier = Modifier.fillMaxSize())
              Text(
                  text = stringResource(R.string.calendar_loading_detail),
                  color = PortalAniColors.TextMuted,
                  modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp),
              )
            } else {
              AnimeInfoPanel(
                  slide = slide,
                  stableLayout = true,
                  modifier = Modifier.fillMaxSize(),
                  onPlayTrailer = onPlayTrailer,
                  onOpenAniList = onOpenAniList,
                  onTapScore = onTapScore,
                  onToggleFavourite = onToggleFavourite,
                  onEditList = onEditList,
                  onShowRelated = onShowRelated,
              )
            }
          }
        }
      }
    }
  }
}
