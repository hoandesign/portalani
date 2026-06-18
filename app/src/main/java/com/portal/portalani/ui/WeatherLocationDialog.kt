package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portal.portalani.R
import com.portal.portalani.data.GeoPlace

@Composable
fun WeatherLocationDialog(
    currentPlace: String?,
    geoStatus: String?,
    geoResults: List<GeoPlace>,
    onDismiss: () -> Unit,
    onDetectLocation: () -> Unit,
    onSearchLocation: (String) -> Unit,
    onChooseLocation: (GeoPlace) -> Unit,
) {
  var cityQuery by remember { mutableStateOf("") }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          modifier =
              Modifier.width(PortalDialogWidths.Form)
                  .wrapContentHeight()
                  .heightIn(max = 460.dp)
                  .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                  .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                  .padding(horizontal = 22.dp, vertical = 18.dp),
      ) {
      WeatherLocationDialogHeader(onDismiss = onDismiss)
      Spacer(Modifier.height(12.dp))
      Text(
          text = stringResource(R.string.weather_location_dialog_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 14.sp,
          lineHeight = 20.sp,
      )
      if (!currentPlace.isNullOrBlank()) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.weather_location_current),
            color = PortalAniColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = currentPlace,
            color = PortalAniColors.Accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(Modifier.height(18.dp))
      PortalPrimaryButton(
          text = stringResource(R.string.weather_use_my_location),
          onClick = onDetectLocation,
          modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(20.dp))
      Text(
          text = stringResource(R.string.weather_search_city),
          color = PortalAniColors.TextMuted,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
      )
      Spacer(Modifier.height(8.dp))
      PortalSettingsTextField(
          value = cityQuery,
          onValueChange = { cityQuery = it },
          placeholder = stringResource(R.string.weather_city_placeholder),
      )
      Spacer(Modifier.height(10.dp))
      PortalSecondaryButton(
          text = stringResource(R.string.weather_search),
          onClick = {
            val query = cityQuery.trim()
            if (query.isNotEmpty()) onSearchLocation(query)
          },
          modifier = Modifier.fillMaxWidth(),
      )
      if (geoStatus != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = geoStatus,
            color = PortalAniColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
      }
      if (geoResults.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.weather_choose_match),
            color = PortalAniColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(geoResults, key = { "${it.lat}_${it.lon}_${it.label}" }) { place ->
            PortalSettingsPickRow(
                label = place.label,
                onClick = { onChooseLocation(place) },
            )
          }
        }
      }
    }
    }
  }
}

@Composable
private fun WeatherLocationDialogHeader(onDismiss: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = stringResource(R.string.weather_location_dialog_title),
        color = PortalAniColors.TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
    )
    PortalCircleIconButton(
        icon = PortalIcons.Close,
        contentDescription = stringResource(R.string.close),
        onClick = onDismiss,
    )
  }
}
