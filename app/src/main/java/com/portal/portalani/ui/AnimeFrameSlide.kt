package com.portal.portalani.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.portal.portalani.R
import com.portal.portalani.data.AnimeSlide
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.MediaRanking
import com.portal.portalani.data.RankScope
import com.portal.portalani.data.RankType

@Composable
fun AnimeFrameSlide(
    slide: AnimeSlide,
    modifier: Modifier = Modifier,
    frameMode: FrameMode = FrameMode.POSTER_ONLY,
    isSignedIn: Boolean = false,
    onPlayTrailer: (() -> Unit)? = null,
    onOpenAniList: (() -> Unit)? = null,
    onTapScore: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onEditList: (() -> Unit)? = null,
    posterExpanded: Boolean = false,
    onPosterToggle: () -> Unit = {},
    onPosterLongPress: (() -> Unit)? = null,
    onShowRelated: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val (bgDriftX, bgDriftY) = rememberPosterParallaxOffsets(enabled = !posterExpanded, slideId = slide.id)
  val posterDriftX = -bgDriftX * 0.22f
  val posterDriftY = -bgDriftY * 0.22f

  val entrance = remember(slide.id) { Animatable(0.5f) }
  LaunchedEffect(slide.id) {
    entrance.snapTo(0.5f)
    entrance.animateTo(1f, animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing))
  }
  val enter = entrance.value
  val infoEnter = ((enter - 0.12f) / 0.88f).coerceIn(0f, 1f)
  val bgScale = 1.34f - (0.12f * enter)
  val bgAlpha = 0.62f * enter

  val expandProgress by animateFloatAsState(
      targetValue = if (posterExpanded) 1f else 0f,
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      label = "posterExpand",
  )

  Box(modifier = modifier.fillMaxSize()) {
    SlideParallaxBackground(
        slide = slide,
        context = context,
        enter = enter,
        bgScale = bgScale,
        bgAlpha = bgAlpha,
        bgDriftX = bgDriftX,
        bgDriftY = bgDriftY,
        posterOnly = frameMode == FrameMode.POSTER_ONLY && expandProgress < 0.92f,
    )

    when (frameMode) {
      FrameMode.INFORMATIVE ->
          InformativeFrameContent(
              slide = slide,
              context = context,
              enter = enter,
              infoEnter = infoEnter,
              posterDriftX = posterDriftX,
              posterDriftY = posterDriftY,
              onPlayTrailer = onPlayTrailer,
              onOpenAniList = onOpenAniList,
              onTapScore = onTapScore,
              onToggleFavourite = onToggleFavourite,
              onEditList = onEditList,
              onShowRelated = onShowRelated,
          )
      FrameMode.POSTER_ONLY ->
          PosterModeFrameContent(
              slide = slide,
              context = context,
              enter = enter,
              expandProgress = expandProgress,
              posterDriftX = posterDriftX,
              posterDriftY = posterDriftY,
              onPosterToggle = onPosterToggle,
              onPosterLongPress = onPosterLongPress,
              onPlayTrailer = onPlayTrailer,
              onOpenAniList = onOpenAniList,
              onTapScore = onTapScore,
              onToggleFavourite = onToggleFavourite,
              onEditList = onEditList,
              onShowRelated = onShowRelated,
          )
      FrameMode.CALENDAR -> Unit
    }
  }
}

@Composable
internal fun SlideParallaxBackground(
    slide: AnimeSlide,
    context: android.content.Context,
    enter: Float,
    bgScale: Float,
    bgAlpha: Float,
    bgDriftX: Float,
    bgDriftY: Float,
    posterOnly: Boolean,
) {
  AsyncImage(
      model = slide.bannerImageRequest(context),
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
                  if (posterOnly) {
                    Brush.verticalGradient(
                        0f to Color(0xD805070C),
                        0.45f to Color(0x8C05070C),
                        1f to Color(0xD805070C),
                    )
                  } else {
                    Brush.horizontalGradient(
                        0f to Color(0xE605070C),
                        0.22f to Color(0x9905070C),
                        0.55f to Color(0x7305070C),
                        1f to Color(0xCC05070C),
                    )
                  },
              ),
  )
}

@Composable
private fun InformativeFrameContent(
    slide: AnimeSlide,
    context: android.content.Context,
    enter: Float,
    infoEnter: Float,
    posterDriftX: Float,
    posterDriftY: Float,
    onPlayTrailer: (() -> Unit)?,
    onOpenAniList: (() -> Unit)?,
    onTapScore: (() -> Unit)?,
    onToggleFavourite: (() -> Unit)?,
    onEditList: (() -> Unit)?,
    onShowRelated: (() -> Unit)?,
) {
  Row(
      modifier =
          Modifier.fillMaxSize()
              .padding(FrameViewerInsets.detailContentPadding),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(FrameViewerInsets.posterInfoGap),
  ) {
    val posterShape = PortalAniShapes.Poster
    Box(
        modifier =
            Modifier.fillMaxHeight(0.98f)
                .aspectRatio(PosterLayout.AspectRatio)
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
          model = slide.coverImageRequest(context),
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
        onShowRelated = onShowRelated,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterModeFrameContent(
    slide: AnimeSlide,
    context: android.content.Context,
    enter: Float,
    expandProgress: Float,
    posterDriftX: Float,
    posterDriftY: Float,
    onPosterToggle: () -> Unit,
    onPosterLongPress: (() -> Unit)?,
    onPlayTrailer: (() -> Unit)?,
    onOpenAniList: (() -> Unit)?,
    onTapScore: (() -> Unit)?,
    onToggleFavourite: (() -> Unit)?,
    onEditList: (() -> Unit)?,
    onShowRelated: (() -> Unit)?,
) {
  val posterShape = PortalAniShapes.Poster
  val score = slide.averageScore?.let { it / 10.0 }
  val density = LocalDensity.current
  val overlayAlpha = (1f - expandProgress * 1.35f).coerceIn(0f, 1f)
  val flipRotationY = expandProgress * 180f
  val flipMirror = flipRotationY > 90f

  BoxWithConstraints(
      modifier =
          Modifier.fillMaxSize()
              .padding(FrameViewerInsets.detailContentPadding),
  ) {
    val collapsedPoster = PosterLayout.fitIn(maxWidth = maxWidth, maxHeight = maxHeight)
    val posterHeight =
        collapsedPoster.height *
            (1f + (FrameViewerInsets.expandedPosterHeightFraction - 1f) * expandProgress)
    val posterWidth = PosterLayout.widthForHeight(posterHeight)
    val centerToStartShift = (maxWidth - posterWidth) / 2f
    val posterOffsetX = lerp(0.dp, -centerToStartShift, expandProgress)
    val driftScale = 1f - expandProgress * 0.65f

    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .offset(x = posterOffsetX)
                .width(posterWidth)
                .height(posterHeight)
                .graphicsLayer {
                  val posterScale = 0.94f + (0.06f * enter)
                  val mirrorScale = if (flipMirror) -1f else 1f
                  scaleX = posterScale * mirrorScale
                  scaleY = posterScale
                  alpha = enter
                  translationX = posterDriftX * driftScale
                  translationY = posterDriftY * driftScale + (1f - enter) * 20f * (1f - expandProgress)
                  rotationY = flipRotationY
                  cameraDistance = 14f * density.density
                }
                .shadow(
                    elevation = lerp(32.dp, 24.dp, expandProgress),
                    shape = posterShape,
                    clip = true,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .clip(posterShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.2f * enter), posterShape)
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onPosterToggle,
                    onLongClick = { onPosterLongPress?.invoke() },
                ),
    ) {
      AsyncImage(
          model = slide.coverImageRequest(context),
          contentDescription = slide.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
      )

      if (overlayAlpha > 0.02f) {
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(168.dp)
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color(0x88000000),
                            1f to Color(0xE6000000),
                        ),
                    ),
        )

        Column(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = overlayAlpha }
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = slide.title,
              color = PortalAniColors.TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 26.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.9f), blurRadius = 16f)),
          )

          if (!slide.nativeTitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = slide.nativeTitle,
                color = PortalAniColors.TextMuted,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 10f)),
            )
          }

          if (score != null) {
            Spacer(Modifier.height(10.dp))
            CommunityScoreInline(score = score, compact = true)
          }

          slide.nextAiringLabel()?.let { airing ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = airing,
                color = PortalAniColors.Cyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 10f)),
            )
          }
        }
      }
    }

    if (expandProgress > 0.04f) {
      val infoAlpha = ((expandProgress - 0.22f) / 0.78f).coerceIn(0f, 1f)
      val infoEnter = infoAlpha * enter
      AnimeInfoPanel(
          slide = slide,
          modifier =
              Modifier.align(Alignment.CenterEnd)
                  .width((maxWidth - posterWidth - FrameViewerInsets.posterInfoGap).coerceAtLeast(280.dp))
                  .fillMaxHeight()
                  .graphicsLayer {
                    alpha = infoEnter
                    translationX = (1f - expandProgress) * 56f
                    translationY = (1f - expandProgress) * 18f
                  },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimeInfoPanel(
    slide: AnimeSlide,
    modifier: Modifier = Modifier,
    stableLayout: Boolean = false,
    onPlayTrailer: (() -> Unit)? = null,
    onOpenAniList: (() -> Unit)? = null,
    onTapScore: (() -> Unit)? = null,
    onToggleFavourite: (() -> Unit)? = null,
    onEditList: (() -> Unit)? = null,
    onShowRelated: (() -> Unit)? = null,
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
    if (slide.isOnList && slide.listStatus != null) {
      ListStatusEyebrow(status = slide.listStatus)
      Spacer(Modifier.height(10.dp))
    } else if (stableLayout) {
      Spacer(Modifier.height(34.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
          modifier = Modifier.weight(1f, fill = false),
      )
      if (onShowRelated != null) {
        PortalCircleIconButton(
            icon = PortalIcons.Related,
            contentDescription = stringResource(R.string.show_related_anime),
            onClick = onShowRelated,
            size = 42.dp,
            iconSize = 21.dp,
            tint = PortalAniColors.Cyan,
        )
      }
    }

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
    } else if (stableLayout) {
      Spacer(Modifier.height(52.dp))
    }

    if (slide.averageScore != null || meta.isNotBlank()) {
      Spacer(Modifier.height(16.dp))
      ScoreMetaRow(
          score = slide.averageScore?.let { it / 10.0 },
          meta = meta,
      )
    } else if (stableLayout) {
      Spacer(Modifier.height(60.dp))
    }

    if (slide.rankings.isNotEmpty()) {
      Spacer(Modifier.height(14.dp))
      FlowRow(
          modifier = Modifier.widthIn(max = 760.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        slide.rankings.forEach { ranking ->
          val isRated = ranking.type == RankType.RATED
          RankStatChip(
              rank = ranking.rank,
              periodLabel = mediaRankingPeriodLabel(ranking),
              icon = if (isRated) PortalIcons.RankRated else PortalIcons.RankPopular,
              label =
                  stringResource(
                      if (isRated) R.string.rank_rated_label else R.string.rank_popular_label,
                  ),
              accent = if (isRated) PortalAniColors.Gold else PortalAniColors.Cyan,
          )
        }
      }
    } else if (stableLayout) {
      Spacer(Modifier.height(46.dp))
    }

    if (!synopsis.isNullOrBlank()) {
      Spacer(Modifier.height(14.dp))
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
    } else if (stableLayout) {
      Spacer(Modifier.height(110.dp))
    }

    if (slide.genres.isNotEmpty()) {
      Spacer(Modifier.height(14.dp))
      FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.widthIn(max = 760.dp),
      ) {
        slide.genres.take(6).forEach { genre ->
          GenreTagChip(label = genre)
        }
      }
    } else if (stableLayout) {
      Spacer(Modifier.height(38.dp))
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
          } else if (stableLayout && onPlayTrailer != null) {
            Spacer(Modifier.height(44.dp).width(156.dp))
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
private fun ListStatusEyebrow(status: ListStatus) {
  val accent = status.accentColor()
  Surface(
      color = accent.copy(alpha = 0.14f),
      shape = PortalAniShapes.Pill,
      border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
          imageVector = status.icon(),
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(14.dp),
      )
      Text(
          text = listStatusLabel(status),
          color = accent,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.4.sp,
      )
    }
  }
}

@Composable
private fun ScoreMetaRow(score: Double?, meta: String) {
  Surface(
      color = Color(0x10FFFFFF),
      shape = PortalAniShapes.Chip,
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      score?.let { CommunityScoreInline(score = it) }
      if (score != null && meta.isNotBlank()) {
        Box(
            modifier =
                Modifier.size(width = 1.dp, height = 18.dp)
                    .background(PortalAniColors.BorderStrong),
        )
      }
      if (meta.isNotBlank()) {
        Text(
            text = meta,
            color = PortalAniColors.TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.45f), blurRadius = 6f)),
        )
      }
    }
  }
}

@Composable
private fun CommunityScoreInline(score: Double, compact: Boolean = false) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
  ) {
    Icon(
        imageVector = PortalIcons.ScoreStar,
        contentDescription = null,
        tint = PortalAniColors.Gold,
        modifier = Modifier.size(if (compact) 14.dp else 16.dp),
    )
    Text(
        text = stringResource(R.string.community_score, score),
        color = PortalAniColors.Gold,
        fontSize = if (compact) 14.sp else 16.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 8f)),
    )
  }
}

@Composable
private fun GenreTagChip(label: String) {
  Surface(
      color = Color(0x12FFFFFF),
      shape = PortalAniShapes.Pill,
      border = androidx.compose.foundation.BorderStroke(1.dp, PortalAniColors.Border),
  ) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
        color = PortalAniColors.TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
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
private fun mediaRankingPeriodLabel(ranking: MediaRanking): String =
    when (ranking.scope) {
      RankScope.ALL_TIME -> stringResource(R.string.rank_all_time)
      RankScope.YEAR -> ranking.year?.toString() ?: stringResource(R.string.rank_all_time)
      RankScope.SEASON -> {
        val season = rankingSeasonLabel(ranking.season)
        when {
          season != null && ranking.year != null ->
              stringResource(R.string.rank_season_year, season, ranking.year)
          ranking.year != null -> ranking.year.toString()
          season != null -> season
          else -> stringResource(R.string.rank_all_time)
        }
      }
    }

private fun rankingSeasonLabel(season: String?): String? =
    when (season?.uppercase()) {
      "WINTER" -> "Winter"
      "SPRING" -> "Spring"
      "SUMMER" -> "Summer"
      "FALL" -> "Fall"
      else -> null
    }

@Composable
private fun RankStatChip(
    rank: Int,
    periodLabel: String,
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier,
      color = accent.copy(alpha = 0.12f),
      shape = PortalAniShapes.Chip,
      border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
          modifier =
              Modifier.size(44.dp)
                  .clip(CircleShape)
                  .background(accent.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
      }
      Column {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
              text = "#$rank",
              color = accent,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 26.sp,
          )
          Text(
              text = periodLabel,
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

private fun buildMetaLine(slide: AnimeSlide): String {
  val statusLabel =
      slide.nextAiringLabel()
          ?: slide.status?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
  return listOfNotNull(
          slide.format?.replace('_', ' ')?.uppercase(),
          slide.seasonYear?.toString() ?: slide.startDateYear?.toString(),
          slide.episodes?.let { "$it eps" },
          slide.studio,
          statusLabel,
      )
      .joinToString(" · ")
}
