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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.data.WeatherNow
import kotlinx.coroutines.delay

/** Screen-fixed clock + optional weather. Lives above slide transitions so it does not move when swiping. */
@Composable
fun AnimatedPosterClock(
    visible: Boolean,
    showWeather: Boolean,
    weather: WeatherNow?,
    modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
      visible = visible,
      enter =
          fadeIn(tween(PortalAnimation.CLOCK_ENTER_MS, easing = FastOutSlowInEasing)) +
              slideInVertically(
                  animationSpec = tween(PortalAnimation.CLOCK_ENTER_MS, easing = FastOutSlowInEasing),
                  initialOffsetY = { fullHeight -> fullHeight / 5 },
              ),
      exit =
          fadeOut(tween(PortalAnimation.CLOCK_EXIT_MS, easing = FastOutSlowInEasing)) +
              slideOutVertically(
                  animationSpec = tween(PortalAnimation.CLOCK_EXIT_MS, easing = FastOutSlowInEasing),
                  targetOffsetY = { fullHeight -> fullHeight / 4 },
              ),
      modifier = modifier,
  ) {
    PosterClockOverlay(showWeather = showWeather, weather = weather)
  }
}

@Composable
fun PosterClockOverlay(
    showWeather: Boolean,
    weather: WeatherNow?,
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

  val shadow = Shadow(color = Color.Black.copy(alpha = 0.88f), blurRadius = 18f)

  Column(
      modifier =
          modifier.padding(
              start = FrameViewerInsets.clockStart,
              bottom = FrameViewerInsets.clockBottom,
          ),
  ) {
    Text(
        text = timeText,
        color = PortalAniColors.TextPrimary,
        fontSize = 48.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 50.sp,
        style = TextStyle(shadow = shadow),
    )
    Spacer(Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
          text = dateText,
          color = PortalAniColors.TextMuted,
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 22.sp,
          style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 12f)),
      )
      if (showWeather && weather != null) {
        WeatherChip(weather, large = true)
      }
    }
  }
}
