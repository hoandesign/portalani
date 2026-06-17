package com.portal.portalani.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.data.WeatherNow
import kotlinx.coroutines.delay

enum class ClockOverlayVariant {
  /** Large clock + weather at the bottom-left (poster mode). */
  Poster,
  /** Compact clock + weather at the top-right (informative mode). */
  Informative,
}

/** Screen-fixed clock + optional weather. Lives above slide transitions so it does not move when swiping. */
@Composable
fun AnimatedPosterClock(
    visible: Boolean,
    showWeather: Boolean,
    weather: WeatherNow?,
    variant: ClockOverlayVariant = ClockOverlayVariant.Poster,
    modifier: Modifier = Modifier,
) {
  val enterOffsetY: (Int) -> Int
  val exitOffsetY: (Int) -> Int
  when (variant) {
    ClockOverlayVariant.Poster -> {
      enterOffsetY = { fullHeight -> fullHeight / 5 }
      exitOffsetY = { fullHeight -> fullHeight / 4 }
    }
    ClockOverlayVariant.Informative -> {
      enterOffsetY = { fullHeight -> -fullHeight / 5 }
      exitOffsetY = { fullHeight -> -fullHeight / 4 }
    }
  }

  AnimatedVisibility(
      visible = visible,
      enter =
          fadeIn(tween(PortalAnimation.CLOCK_ENTER_MS, easing = FastOutSlowInEasing)) +
              slideInVertically(
                  animationSpec = tween(PortalAnimation.CLOCK_ENTER_MS, easing = FastOutSlowInEasing),
                  initialOffsetY = enterOffsetY,
              ),
      exit =
          fadeOut(tween(PortalAnimation.CLOCK_EXIT_MS, easing = FastOutSlowInEasing)) +
              slideOutVertically(
                  animationSpec = tween(PortalAnimation.CLOCK_EXIT_MS, easing = FastOutSlowInEasing),
                  targetOffsetY = exitOffsetY,
              ),
      modifier = modifier,
  ) {
    PosterClockOverlay(showWeather = showWeather, weather = weather, variant = variant)
  }
}

@Composable
fun PosterClockOverlay(
    showWeather: Boolean,
    weather: WeatherNow?,
    variant: ClockOverlayVariant = ClockOverlayVariant.Poster,
    modifier: Modifier = Modifier,
) {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      delay(1_000)
    }
  }

  val locale = remember { java.util.Locale.getDefault() }
  val zone = remember { java.time.ZoneId.systemDefault() }
  val dateTime = remember(nowMillis) { java.time.Instant.ofEpochMilli(nowMillis).atZone(zone) }
  val timeText =
      remember(dateTime, locale) {
        java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
            .withLocale(locale)
            .format(dateTime)
      }
  val dateText =
      remember(dateTime, locale) {
        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(dateTime)
      }

  val timeFontSize =
      when (variant) {
        ClockOverlayVariant.Poster -> 48.sp
        ClockOverlayVariant.Informative -> 30.sp
      }
  val timeLineHeight =
      when (variant) {
        ClockOverlayVariant.Poster -> 50.sp
        ClockOverlayVariant.Informative -> 32.sp
      }
  val dateFontSize =
      when (variant) {
        ClockOverlayVariant.Poster -> 18.sp
        ClockOverlayVariant.Informative -> 13.sp
      }
  val dateLineHeight =
      when (variant) {
        ClockOverlayVariant.Poster -> 22.sp
        ClockOverlayVariant.Informative -> 16.sp
      }
  val shadowBlur =
      when (variant) {
        ClockOverlayVariant.Poster -> 18f
        ClockOverlayVariant.Informative -> 12f
      }
  val dateShadowBlur =
      when (variant) {
        ClockOverlayVariant.Poster -> 12f
        ClockOverlayVariant.Informative -> 8f
      }

  val shadow = Shadow(color = Color.Black.copy(alpha = 0.88f), blurRadius = shadowBlur)
  val textAlign =
      when (variant) {
        ClockOverlayVariant.Poster -> TextAlign.Start
        ClockOverlayVariant.Informative -> TextAlign.End
      }
  val columnAlignment =
      when (variant) {
        ClockOverlayVariant.Poster -> Alignment.Start
        ClockOverlayVariant.Informative -> Alignment.End
      }
  val contentPadding =
      when (variant) {
        ClockOverlayVariant.Poster ->
            Modifier.padding(
                start = FrameViewerInsets.clockStart,
                bottom = FrameViewerInsets.clockBottom,
            )
        ClockOverlayVariant.Informative ->
            Modifier.padding(
                end = FrameViewerInsets.horizontal,
                top = FrameViewerInsets.vertical,
            )
      }

  Column(
      modifier = modifier.then(contentPadding),
      horizontalAlignment = columnAlignment,
  ) {
    Text(
        text = timeText,
        color = PortalAniColors.TextPrimary,
        fontSize = timeFontSize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = timeLineHeight,
        textAlign = textAlign,
        style = TextStyle(shadow = shadow),
    )
    Spacer(Modifier.height(if (variant == ClockOverlayVariant.Poster) 4.dp else 2.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(
                if (variant == ClockOverlayVariant.Poster) 14.dp else 10.dp,
            ),
    ) {
      Text(
          text = dateText,
          color = PortalAniColors.TextMuted,
          fontSize = dateFontSize,
          fontWeight = FontWeight.Medium,
          lineHeight = dateLineHeight,
          textAlign = textAlign,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = dateShadowBlur)),
      )
      if (showWeather && weather != null) {
        WeatherChip(weather, large = variant == ClockOverlayVariant.Poster)
      }
    }
  }
}
