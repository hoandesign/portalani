package com.portal.portalani.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.data.WeatherNow

/** Weather code (WMO) + day/night → one of our vector glyphs. */
fun weatherIconDrawable(code: Int, isDay: Boolean): Int =
    when (code) {
      0 -> if (isDay) R.drawable.ic_weather_sunny else R.drawable.ic_weather_clear_night
      1, 2 -> R.drawable.ic_weather_partly_cloudy
      3 -> R.drawable.ic_weather_cloudy
      45, 48 -> R.drawable.ic_weather_fog
      in 51..57, in 61..67, in 80..82 -> R.drawable.ic_weather_rain
      in 71..77, 85, 86 -> R.drawable.ic_weather_snow
      95, 96, 99 -> R.drawable.ic_weather_thunder
      else -> R.drawable.ic_weather_cloudy
    }

@Composable
fun WeatherChip(weather: WeatherNow, modifier: Modifier = Modifier, large: Boolean = false) {
  val shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = if (large) 12f else 10f)
  val iconSize = if (large) 22.dp else 16.dp
  val fontSize = if (large) 18.sp else 14.sp
  val lineHeight = if (large) 22.sp else 18.sp
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(if (large) 6.dp else 4.dp),
  ) {
    Image(
        painter = painterResource(weatherIconDrawable(weather.code, weather.isDay)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(PortalAniColors.TextMuted),
        modifier = Modifier.size(iconSize),
    )
    Text(
        text = "${weather.temp}°${weather.unit}",
        color = PortalAniColors.TextMuted,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        lineHeight = lineHeight,
        style = TextStyle(shadow = shadow),
    )
  }
}
