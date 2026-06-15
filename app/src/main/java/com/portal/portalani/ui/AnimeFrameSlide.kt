package com.portal.portalani.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.portal.portalani.R
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.ListStatus

@Composable
fun AnimeFrameSlide(
    slide: AnimeSlide,
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    onPlayTrailer: (() -> Unit)? = null,
    onOpenAniList: (() -> Unit)? = null,
    onTapScore: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onEditList: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val transition = rememberInfiniteTransition(label = "parallax-${slide.id}")
  val bgDriftX by
      transition.animateFloat(
          initialValue = -24f,
          targetValue = 24f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(28_000, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse,
              ),
          label = "bgX",
      )
  val bgDriftY by
      transition.animateFloat(
          initialValue = -14f,
          targetValue = 14f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(34_000, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse,
              ),
          label = "bgY",
      )
  val posterDriftX = -bgDriftX * 0.22f
  val posterDriftY = -bgDriftY * 0.22f

  val entrance = remember(slide.id) { Animatable(0f) }
  LaunchedEffect(slide.id) {
    entrance.snapTo(0f)
    entrance.animateTo(1f, animationSpec = tween(durationMillis = 1_100, easing = FastOutSlowInEasing))
  }
  val enter = entrance.value
  val infoEnter = ((enter - 0.12f) / 0.88f).coerceIn(0f, 1f)
  val bgScale = 1.34f - (0.12f * enter)
  val bgAlpha = 0.62f * enter

  Box(modifier = modifier.fillMaxSize()) {
    AsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(slide.bannerUrl)
                .crossfade(true)
                .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.fillMaxSize()
                .graphicsLayer {
                  scaleX = bgScale
                  scaleY = bgScale
                  alpha = bgAlpha
                  translationX = bgDriftX
                  translationY = bgDriftY
                },
    )

    Box(
        modifier =
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = enter }
                .background(Color(0x88000000)),
    )

    Box(
        modifier =
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = enter }
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xE605070C),
                        0.22f to Color(0x9905070C),
                        0.55f to Color(0x7305070C),
                        1f to Color(0xCC05070C),
                    ),
                ),
    )

    Row(
        modifier =
            Modifier.fillMaxSize()
                .padding(top = 56.dp, bottom = 32.dp, start = 40.dp, end = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
      val posterShape = PortalAniShapes.Poster
      Box(
          modifier =
              Modifier.fillMaxHeight(0.94f)
                  .aspectRatio(2f / 3f)
                  .graphicsLayer {
                    val posterScale = 0.9f + (0.1f * enter)
                    scaleX = posterScale
                    scaleY = posterScale
                    alpha = enter
                    translationX = posterDriftX + (1f - enter) * -42f
                    translationY = posterDriftY + (1f - enter) * 18f
                  }
                  .shadow(
                      elevation = 24.dp,
                      shape = posterShape,
                      clip = true,
                      ambientColor = Color.Black,
                      spotColor = Color.Black,
                  )
                  .clip(posterShape)
                  .border(1.5.dp, Color.White.copy(alpha = 0.16f * enter), posterShape),
      ) {
        AsyncImage(
            model =
                ImageRequest.Builder(context)
                    .data(slide.coverUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = slide.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(posterShape),
        )
      }

      AnimeInfoPanel(
          slide = slide,
          modifier =
              Modifier.weight(1f)
                  .fillMaxHeight()
                  .graphicsLayer {
                    alpha = infoEnter
                    translationX = (1f - infoEnter) * 56f
                    translationY = (1f - infoEnter) * 22f
                  },
          onPlayTrailer = onPlayTrailer,
          onOpenAniList = onOpenAniList,
          onTapScore = onTapScore,
          onToggleFavourite = onToggleFavourite,
          onEditList = onEditList,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeInfoPanel(
    slide: AnimeSlide,
    modifier: Modifier = Modifier,
    onPlayTrailer: (() -> Unit)? = null,
    onOpenAniList: (() -> Unit)? = null,
    onTapScore: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onEditList: (() -> Unit)? = null,
) {
  val meta = remember(slide) { buildMetaLine(slide) }
  val synopsis = slide.description?.let { if (it.length > 320) it.take(317) + "…" else it }
  val hasActions =
      onPlayTrailer != null ||
          onOpenAniList != null ||
          onTapScore != null ||
          onToggleFavourite != null ||
          onEditList != null

  Column(
      modifier = modifier.padding(vertical = 4.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
  ) {
    Text(
        text = slide.title,
        color = PortalAniColors.TextPrimary,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.85f), blurRadius = 20f)),
    )

    if (!slide.nativeTitle.isNullOrBlank()) {
      Spacer(Modifier.height(6.dp))
      Text(
          text = slide.nativeTitle,
          color = PortalAniColors.TextMuted,
          fontSize = 19.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.65f), blurRadius = 10f)),
      )
    }

    Spacer(Modifier.height(20.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      slide.averageScore?.let { score ->
        CommunityScorePill(score = score / 10.0)
      }
      if (slide.isOnList && slide.listStatus != null) {
        ListStatusPill(status = slide.listStatus)
      }
    }

    if (meta.isNotBlank()) {
      Spacer(Modifier.height(12.dp))
      Text(
          text = meta,
          color = PortalAniColors.TextSecondary,
          fontSize = 16.sp,
          letterSpacing = 0.2.sp,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 8f)),
      )
    }

    if (slide.ratedRankAllTime != null || slide.popularRankAllTime != null) {
      val dualRanks = slide.ratedRankAllTime != null && slide.popularRankAllTime != null
      Spacer(Modifier.height(16.dp))
      Row(
          modifier =
              Modifier.widthIn(max = 760.dp).then(if (dualRanks) Modifier.fillMaxWidth() else Modifier),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        slide.ratedRankAllTime?.let { rank ->
          RankStatChip(
              rank = rank,
              icon = PortalIcons.RankRated,
              label = stringResource(R.string.rank_rated_label),
              accent = PortalAniColors.Gold,
              fillWidth = dualRanks,
              modifier = if (dualRanks) Modifier.weight(1f) else Modifier,
          )
        }
        slide.popularRankAllTime?.let { rank ->
          RankStatChip(
              rank = rank,
              icon = PortalIcons.RankPopular,
              label = stringResource(R.string.rank_popular_label),
              accent = PortalAniColors.Cyan,
              fillWidth = dualRanks,
              modifier = if (dualRanks) Modifier.weight(1f) else Modifier,
          )
        }
      }
    }

    if (slide.genres.isNotEmpty()) {
      Spacer(Modifier.height(14.dp))
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.widthIn(max = 760.dp),
      ) {
        slide.genres.take(6).forEach { genre ->
          Surface(
              color = Color(0x12FFFFFF),
              shape = PortalAniShapes.Pill,
              border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
          ) {
            Text(
                text = genre,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                color = PortalAniColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }

    if (!synopsis.isNullOrBlank()) {
      Spacer(Modifier.height(16.dp))
      Text(
          text = synopsis,
          color = PortalAniColors.TextMuted,
          fontSize = 16.sp,
          lineHeight = 24.sp,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Start,
          modifier = Modifier.widthIn(max = 720.dp),
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.55f), blurRadius = 8f)),
      )
    }

    if (hasActions) {
      Spacer(Modifier.height(24.dp))
      Row(
          modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          if (slide.trailerYoutubeId != null && onPlayTrailer != null) {
            PortalPrimaryButton(
                text = stringResource(R.string.play_trailer),
                onClick = onPlayTrailer,
            )
          }
          if (onOpenAniList != null) {
            PortalSecondaryButton(
                text = stringResource(R.string.open_anilist),
                onClick = onOpenAniList,
            )
          }
        }

        if (onTapScore != null || onToggleFavourite != null || onEditList != null) {
          QuickActionCluster(
              slide = slide,
              onTapScore = onTapScore,
              onToggleFavourite = onToggleFavourite,
              onEditList = onEditList,
          )
        }
      }
    }
  }
}

@Composable
private fun CommunityScorePill(score: Double) {
  Surface(
      color = PortalAniColors.Gold.copy(alpha = 0.16f),
      shape = PortalAniShapes.Chip,
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Gold.copy(alpha = 0.42f)),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
          imageVector = PortalIcons.ScoreStar,
          contentDescription = null,
          tint = PortalAniColors.Gold,
          modifier = Modifier.size(16.dp),
      )
      Text(
          text = stringResource(R.string.community_score, score),
          color = PortalAniColors.Gold,
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun ListStatusPill(status: ListStatus) {
  Surface(
      color = PortalAniColors.AccentSoft,
      shape = PortalAniShapes.Pill,
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Accent.copy(alpha = 0.35f)),
  ) {
    Text(
        text = listStatusLabel(status),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        color = PortalAniColors.Accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun QuickActionCluster(
    slide: AnimeSlide,
    onTapScore: (() -> Unit)?,
    onToggleFavourite: (() -> Unit)?,
    onEditList: (() -> Unit)?,
) {
  Surface(
      color = Color(0x18FFFFFF),
      shape = PortalAniShapes.Pill,
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.BorderStrong),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (onTapScore != null) {
        PortalScoreActionButton(
            score = slide.userScore,
            contentDescription = stringResource(R.string.rate_anime),
            onClick = onTapScore,
        )
      }
      if (onToggleFavourite != null) {
        PortalIconActionButton(
            icon = if (slide.isFavourite) PortalIcons.FavouriteOn else PortalIcons.FavouriteOff,
            contentDescription = stringResource(R.string.toggle_favourite),
            selected = slide.isFavourite,
            selectedTint = Color(0xFFFF5A7A),
            onClick = onToggleFavourite,
        )
      }
      if (onEditList != null) {
        PortalIconActionButton(
            icon = if (slide.isOnList) PortalIcons.OnList else PortalIcons.AddToList,
            contentDescription = stringResource(R.string.edit_list),
            selected = slide.isOnList,
            onClick = onEditList,
        )
      }
    }
  }
}

@Composable
private fun RankStatChip(
    rank: Int,
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
) {
  Surface(
      modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier),
      color = accent.copy(alpha = 0.12f),
      shape = PortalAniShapes.Chip,
      border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
  ) {
    Row(
        modifier =
            Modifier.padding(
                horizontal = if (fillWidth) 14.dp else 16.dp,
                vertical = if (fillWidth) 10.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
          modifier =
              Modifier.size(if (fillWidth) 40.dp else 44.dp)
                  .clip(CircleShape)
                  .background(accent.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(if (fillWidth) 20.dp else 22.dp),
        )
      }
      Column(modifier = if (fillWidth) Modifier.weight(1f) else Modifier) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
              text = "#$rank",
              color = accent,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 26.sp,
          )
          Text(
              text = stringResource(R.string.rank_all_time),
              color = PortalAniColors.TextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(bottom = 2.dp),
          )
        }
        Text(
            text = label,
            color = PortalAniColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

private fun buildMetaLine(slide: AnimeSlide): String =
    listOfNotNull(
            slide.format?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() },
            slide.seasonYear?.toString(),
            slide.episodes?.let { "$it eps" },
            slide.status?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() },
        )
        .joinToString(" · ")
