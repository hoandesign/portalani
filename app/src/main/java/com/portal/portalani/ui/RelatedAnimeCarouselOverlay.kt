package com.portal.portalani.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.portal.portalani.R
import com.portal.portalani.data.RelatedAnime
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun RelatedAnimeCarouselOverlay(
    sourceTitle: String,
    items: List<RelatedAnime>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSelectRelated: (RelatedAnime) -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  val focusedIndex by remember {
    derivedStateOf {
      val layout = listState.layoutInfo
      if (layout.visibleItemsInfo.isEmpty()) {
        0
      } else {
        val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
        layout.visibleItemsInfo.minByOrNull { item ->
          abs((item.offset + item.size / 2) - viewportCenter)
        }?.index ?: listState.firstVisibleItemIndex
      }
    }
  }

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
                .fillMaxWidth(0.94f)
                .clip(PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass)
                .border(1.dp, PortalAniColors.BorderStrong, PortalAniShapes.Card)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = stringResource(R.string.related_anime_title),
              color = PortalAniColors.TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text = sourceTitle,
              color = PortalAniColors.TextMuted,
              fontSize = 14.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
        PortalCircleIconButton(
            icon = PortalIcons.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
        )
      }

      Spacer(Modifier.height(16.dp))

      when {
        loading ->
            Box(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                contentAlignment = Alignment.Center,
            ) {
              Text(
                  text = stringResource(R.string.related_anime_loading),
                  color = PortalAniColors.TextMuted,
                  fontSize = 16.sp,
              )
            }
        items.isEmpty() ->
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
              Text(
                  text = stringResource(R.string.related_anime_empty),
                  color = PortalAniColors.TextMuted,
                  fontSize = 16.sp,
                  textAlign = TextAlign.Center,
              )
            }
        else -> {
          LazyRow(
              state = listState,
              modifier = Modifier.fillMaxWidth().height(380.dp),
              contentPadding = PaddingValues(horizontal = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(18.dp),
              flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
          ) {
            itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
              RelatedAnimeCard(
                  item = item,
                  onOpen = { onSelectRelated(item) },
              )
            }
          }

          Spacer(Modifier.height(12.dp))

          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            PortalCircleIconButton(
                icon = PortalIcons.SwipeBack,
                contentDescription = stringResource(R.string.related_anime_previous),
                onClick = {
                  if (focusedIndex > 0) {
                    scope.launch { listState.animateScrollToItem(focusedIndex - 1) }
                  }
                },
                tint =
                    if (focusedIndex > 0) {
                      PortalAniColors.TextSecondary
                    } else {
                      PortalAniColors.TextMuted.copy(alpha = 0.35f)
                    },
            )
            Text(
                text = stringResource(R.string.related_anime_position, focusedIndex + 1, items.size),
                color = PortalAniColors.TextMuted,
                fontSize = 14.sp,
            )
            PortalCircleIconButton(
                icon = PortalIcons.SwipeForward,
                contentDescription = stringResource(R.string.related_anime_next),
                onClick = {
                  if (focusedIndex < items.lastIndex) {
                    scope.launch { listState.animateScrollToItem(focusedIndex + 1) }
                  }
                },
                tint =
                    if (focusedIndex < items.lastIndex) {
                      PortalAniColors.TextSecondary
                    } else {
                      PortalAniColors.TextMuted.copy(alpha = 0.35f)
                    },
            )
          }

          Text(
              text = stringResource(R.string.related_anime_hint),
              color = PortalAniColors.TextMuted,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun RelatedAnimeCard(
    item: RelatedAnime,
    onOpen: () -> Unit,
) {
  val context = LocalContext.current
  val shape = PortalAniShapes.Poster

  Column(
      modifier = Modifier.width(220.dp).clickable(onClick = onOpen),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .aspectRatio(PosterLayout.AspectRatio)
                .clip(shape)
                .border(1.dp, PortalAniColors.BorderStrong, shape),
    ) {
      AsyncImage(
          model = ImageRequest.Builder(context).data(item.coverUrl).crossfade(true).build(),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
      )
      item.kindLabel?.let { label ->
        Surface(
            color = Color(0xCC0B0D12),
            shape = PortalAniShapes.Pill,
            border = BorderStroke(1.dp, PortalAniColors.Border),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
          Text(
              text = label,
              color = PortalAniColors.Cyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

    Spacer(Modifier.height(10.dp))

    Text(
        text = item.title,
        color = PortalAniColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 220.dp),
    )

    item.averageScore?.let { score ->
      Spacer(Modifier.height(4.dp))
      Text(
          text = stringResource(R.string.community_score, score / 10.0),
          color = PortalAniColors.Gold,
          fontSize = 13.sp,
      )
    }
  }
}
