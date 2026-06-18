package com.portal.portalani.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.FrameMode
import com.portal.portalani.data.GeoPlace
import com.portal.portalani.data.LibrarySort
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerMode
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SeasonSelection
import com.portal.portalani.data.SourceFilter
import com.portal.portalani.data.SourceMode
import com.portal.portalani.data.WeekStart

@Composable
fun SettingsPanel(
    settings: AppSettings,
    geoStatus: String?,
    geoResults: List<GeoPlace>,
    viewerName: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    onSetFrameMode: (FrameMode) -> Unit,
    onSetWeekStart: (WeekStart) -> Unit,
    onSetShowPosterClock: (Boolean) -> Unit,
    onSetShowWeather: (Boolean) -> Unit,
    onSetWeatherFahrenheit: (Boolean) -> Unit,
    onSearchLocation: (String) -> Unit,
    onChooseLocation: (GeoPlace) -> Unit,
    onDetectLocation: () -> Unit,
    onClearGeoSearch: () -> Unit,
    onSetIntervalSeconds: (Int) -> Unit,
    onSetSourceMode: (SourceMode) -> Unit,
    onSetListStatuses: (Set<ListStatus>) -> Unit,
    onSetFormatFilters: (Set<FormatFilter>) -> Unit,
    onSetCountryFilters: (Set<CountryFilter>) -> Unit,
    onSetSourceFilters: (Set<SourceFilter>) -> Unit,
    onSetDemographicFilters: (Set<DemographicFilter>) -> Unit,
    onSetHideHentai: (Boolean) -> Unit,
    onSetLibrarySort: (LibrarySort) -> Unit,
    onSetSeasonKey: (String) -> Unit,
    onSetPowerMode: (PowerMode) -> Unit,
    onSetIdleSleepMinutes: (Int) -> Unit,
    onSetSleepStartMinutes: (Int) -> Unit,
    onSetSleepEndMinutes: (Int) -> Unit,
    onUserInteraction: () -> Unit,
    onResetOnboarding: () -> Unit,
    appVersion: String,
) {
  var statusMenuOpen by remember { mutableStateOf(false) }
  var formatMenuOpen by remember { mutableStateOf(false) }
  var countryMenuOpen by remember { mutableStateOf(false) }
  var sourceMaterialMenuOpen by remember { mutableStateOf(false) }
  var demographicMenuOpen by remember { mutableStateOf(false) }
  var sortMenuOpen by remember { mutableStateOf(false) }
  var seasonMenuOpen by remember { mutableStateOf(false) }
  var sourceMenuOpen by remember { mutableStateOf(false) }
  var frameMenuOpen by remember { mutableStateOf(false) }
  var weekStartMenuOpen by remember { mutableStateOf(false) }
  var intervalMenuOpen by remember { mutableStateOf(false) }
  var powerMenuOpen by remember { mutableStateOf(false) }
  var idleMenuOpen by remember { mutableStateOf(false) }
  var sleepStartMenuOpen by remember { mutableStateOf(false) }
  var sleepEndMenuOpen by remember { mutableStateOf(false) }
  var tempUnitMenuOpen by remember { mutableStateOf(false) }
  var locationDialogOpen by remember { mutableStateOf(false) }
  var placeWhenLocationDialogOpened by remember { mutableStateOf<String?>(null) }
  val intervalSeconds = (settings.intervalMs / 1000L).toInt()
  val tempUnitOptions =
      listOf(
          "celsius" to stringResource(R.string.weather_celsius),
          "fahrenheit" to stringResource(R.string.weather_fahrenheit),
      )
  val sortOptions = LibrarySort.entries.map { sort -> sort.name to sort.label }
  val sourceOptions =
      listOf(
          SourceMode.PERSONAL.name to stringResource(R.string.source_personal),
          SourceMode.LIBRARY.name to stringResource(R.string.source_library),
      )
  val frameOptions =
      listOf(
          FrameMode.INFORMATIVE.name to stringResource(R.string.frame_mode_informative),
          FrameMode.POSTER_ONLY.name to stringResource(R.string.frame_mode_poster),
          FrameMode.CALENDAR.name to stringResource(R.string.frame_mode_calendar),
      )
  val weekStartOptions =
      listOf(
          WeekStart.MONDAY.name to stringResource(R.string.week_start_monday),
          WeekStart.SUNDAY.name to stringResource(R.string.week_start_sunday),
      )
  val intervalOptions = listOf(8, 12, 20, 30).map { seconds -> seconds.toString() to "$seconds s" }
  val powerOptions =
      listOf(
          PowerMode.ALWAYS_ON.name to stringResource(R.string.power_always_on),
          PowerMode.IDLE_SLEEP.name to stringResource(R.string.power_idle_sleep),
          PowerMode.SCHEDULED_SLEEP.name to stringResource(R.string.power_scheduled_sleep),
      )
  val idleOptions =
      PowerPolicy.IDLE_SLEEP_OPTIONS_MINUTES.map { minutes ->
        minutes.toString() to PowerPolicy.formatIdleDuration(minutes)
      }
  val anyMenuOpen =
      statusMenuOpen ||
          formatMenuOpen ||
          countryMenuOpen ||
          sourceMaterialMenuOpen ||
          demographicMenuOpen ||
          sortMenuOpen ||
          seasonMenuOpen ||
          sourceMenuOpen ||
          frameMenuOpen ||
          weekStartMenuOpen ||
          intervalMenuOpen ||
          powerMenuOpen ||
          idleMenuOpen ||
          sleepStartMenuOpen ||
          sleepEndMenuOpen ||
          tempUnitMenuOpen ||
          locationDialogOpen

  LaunchedEffect(locationDialogOpen) {
    if (locationDialogOpen) {
      placeWhenLocationDialogOpened = settings.weatherPlace
    }
  }

  LaunchedEffect(settings.weatherPlace) {
    if (locationDialogOpen && settings.weatherPlace != placeWhenLocationDialogOpened) {
      locationDialogOpen = false
      onClearGeoSearch()
    }
  }

  BackHandler(enabled = anyMenuOpen) {
    statusMenuOpen = false
    formatMenuOpen = false
    countryMenuOpen = false
    sourceMaterialMenuOpen = false
    demographicMenuOpen = false
    sortMenuOpen = false
    seasonMenuOpen = false
    sourceMenuOpen = false
    frameMenuOpen = false
    weekStartMenuOpen = false
    intervalMenuOpen = false
    powerMenuOpen = false
    idleMenuOpen = false
    sleepStartMenuOpen = false
    sleepEndMenuOpen = false
    tempUnitMenuOpen = false
    locationDialogOpen = false
    onClearGeoSearch()
  }

  BackHandler(enabled = !anyMenuOpen) { onDismiss() }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(PortalAniColors.Overlay)
              .clickable(onClick = onDismiss),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        modifier =
            Modifier.padding(horizontal = 48.dp, vertical = 52.dp)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .testTag(PortalTestTags.SETTINGS_SHEET)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
    ) {
      Column(
          modifier =
              Modifier.verticalScroll(rememberScrollState())
                  .padding(horizontal = 36.dp, vertical = 34.dp)
                  .padding(bottom = 12.dp),
      ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            stringResource(R.string.settings),
            color = PortalAniColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        PortalCircleIconButton(
            icon = PortalIcons.Close,
            contentDescription = stringResource(R.string.close),
            onClick = onDismiss,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
          stringResource(R.string.screensaver_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 15.sp,
          lineHeight = 21.sp,
      )
      Spacer(Modifier.height(24.dp))

      PortalSettingsSectionHeader(stringResource(R.string.settings_section_slideshow))
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.frame_mode),
            value =
                when (settings.frameMode) {
                  FrameMode.INFORMATIVE -> stringResource(R.string.frame_mode_informative)
                  FrameMode.POSTER_ONLY -> stringResource(R.string.frame_mode_poster)
                  FrameMode.CALENDAR -> stringResource(R.string.frame_mode_calendar)
                },
            onClick = {
              onUserInteraction()
              frameMenuOpen = true
            },
        )
        if (settings.frameMode == FrameMode.CALENDAR) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.week_starts_on),
              value =
                  if (settings.weekStart == WeekStart.MONDAY) {
                    stringResource(R.string.week_start_monday)
                  } else {
                    stringResource(R.string.week_start_sunday)
                  },
              onClick = {
                onUserInteraction()
                weekStartMenuOpen = true
              },
          )
        }
        if (settings.frameMode != FrameMode.CALENDAR) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.interval_seconds),
              value = "$intervalSeconds s",
              onClick = {
                onUserInteraction()
                intervalMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsToggleRow(
              label = stringResource(R.string.shuffle),
              checked = settings.shuffle,
              onCheckedChange = {
                onUserInteraction()
                onSetShuffle(it)
              },
          )
        }
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.settings_section_clock_weather),
          subtitle =
              if (!settings.showPosterClock) {
                stringResource(R.string.weather_requires_clock)
              } else {
                stringResource(R.string.weather_hint)
              },
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsToggleRow(
            label = stringResource(R.string.show_poster_clock),
            checked = settings.showPosterClock,
            onCheckedChange = {
              onUserInteraction()
              onSetShowPosterClock(it)
            },
        )
        PortalSettingsDivider()
        PortalSettingsToggleRow(
            label = stringResource(R.string.show_weather),
            checked = settings.showWeather,
            enabled = settings.showPosterClock,
            onCheckedChange = { enabled ->
              onUserInteraction()
              onSetShowWeather(enabled)
              if (enabled && settings.weatherPlace == null) {
                locationDialogOpen = true
              }
            },
        )
        if (settings.showWeather) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.weather_temperature_unit),
              value =
                  if (settings.weatherFahrenheit) {
                    stringResource(R.string.weather_fahrenheit)
                  } else {
                    stringResource(R.string.weather_celsius)
                  },
              onClick = {
                onUserInteraction()
                tempUnitMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.weather_location),
              value = settings.weatherPlace ?: stringResource(R.string.weather_location_not_set),
              onClick = {
                onUserInteraction()
                locationDialogOpen = true
              },
          )
        }
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.settings_section_content),
          subtitle =
              if (settings.sourceMode == SourceMode.LIBRARY) {
                stringResource(R.string.library_filters)
              } else {
                stringResource(R.string.personal_lists_hint)
              },
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.source_mode),
            value =
                if (settings.sourceMode == SourceMode.PERSONAL) {
                  stringResource(R.string.source_personal)
                } else {
                  stringResource(R.string.source_library)
                },
            onClick = {
              onUserInteraction()
              sourceMenuOpen = true
            },
        )
        PortalSettingsDivider()
        if (settings.sourceMode == SourceMode.PERSONAL) {
          PortalSettingsRow(
              label = stringResource(R.string.list_status),
              value = listStatusesSettingLabel(settings.listStatuses),
              onClick = { statusMenuOpen = true },
          )
          CatalogFilterSettingsRows(
              settings = settings,
              onFormatClick = { formatMenuOpen = true },
              onCountryClick = { countryMenuOpen = true },
              onSourceMaterialClick = { sourceMaterialMenuOpen = true },
              onDemographicClick = { demographicMenuOpen = true },
          )
          if (settings.frameMode == FrameMode.CALENDAR) {
            PortalSettingsDivider()
            PortalSettingsRow(
                label = stringResource(R.string.sort_by),
                value = settings.librarySort.label,
                onClick = { sortMenuOpen = true },
            )
          }
        } else {
          CatalogFilterSettingsRows(
              settings = settings,
              onFormatClick = { formatMenuOpen = true },
              onCountryClick = { countryMenuOpen = true },
              onSourceMaterialClick = { sourceMaterialMenuOpen = true },
              onDemographicClick = { demographicMenuOpen = true },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.sort_by),
              value = settings.librarySort.label,
              onClick = { sortMenuOpen = true },
          )
          PortalSettingsDivider()
          if (settings.frameMode == FrameMode.CALENDAR) {
            PortalSettingsRow(
                label = stringResource(R.string.season_filter),
                value = stringResource(R.string.calendar_season_window),
                onClick = {},
                enabled = false,
                showChevron = false,
            )
          } else {
            PortalSettingsRow(
                label = stringResource(R.string.season_filter),
                value = SeasonSelection.labelFor(settings.seasonKey),
                onClick = { seasonMenuOpen = true },
            )
          }
        }
        PortalSettingsDivider()
        PortalSettingsToggleRow(
            label = stringResource(R.string.hide_hentai_genre),
            checked = settings.hideHentai,
            onCheckedChange = {
              onUserInteraction()
              onSetHideHentai(it)
            },
        )
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(stringResource(R.string.settings_section_account))
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        if (viewerName != null) {
          PortalSettingsRow(
              label = stringResource(R.string.settings_section_account),
              value = viewerName,
              onClick = {},
              enabled = false,
              showChevron = false,
          )
          PortalSettingsDivider()
          PortalSettingsActionRow(
              label = stringResource(R.string.sign_out),
              onClick = onSignOut,
          )
        } else {
          PortalSettingsRow(
              label = stringResource(R.string.settings_section_account),
              value = stringResource(R.string.account_not_signed_in),
              onClick = onSignIn,
          )
        }
        PortalSettingsDivider()
        PortalSettingsRow(
            label = stringResource(R.string.settings_show_guide),
            value = stringResource(R.string.settings_show_guide_action),
            onClick = {
              onUserInteraction()
              onResetOnboarding()
              onDismiss()
            },
        )
      }

      Spacer(Modifier.height(22.dp))
      PortalSettingsSectionHeader(
          title = stringResource(R.string.power_settings),
          subtitle = stringResource(R.string.power_hint),
      )
      Spacer(Modifier.height(10.dp))
      PortalSettingsGroup {
        PortalSettingsRow(
            label = stringResource(R.string.power_mode),
            value =
                when (settings.powerMode) {
                  PowerMode.ALWAYS_ON -> stringResource(R.string.power_always_on)
                  PowerMode.IDLE_SLEEP -> stringResource(R.string.power_idle_sleep)
                  PowerMode.SCHEDULED_SLEEP -> stringResource(R.string.power_scheduled_sleep)
                },
            onClick = {
              onUserInteraction()
              powerMenuOpen = true
            },
        )

        if (settings.powerMode == PowerMode.IDLE_SLEEP) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_idle_after),
              value = PowerPolicy.formatIdleDuration(settings.idleSleepMinutes),
              onClick = {
                onUserInteraction()
                idleMenuOpen = true
              },
          )
        }

        if (settings.powerMode == PowerMode.SCHEDULED_SLEEP) {
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_sleep_from),
              value = PowerPolicy.formatMinutesOfDay(settings.sleepStartMinutes),
              onClick = {
                onUserInteraction()
                sleepStartMenuOpen = true
              },
          )
          PortalSettingsDivider()
          PortalSettingsRow(
              label = stringResource(R.string.power_wake_at),
              value = PowerPolicy.formatMinutesOfDay(settings.sleepEndMinutes),
              onClick = {
                onUserInteraction()
                sleepEndMenuOpen = true
              },
          )
        }
      }

      if (settings.powerMode == PowerMode.SCHEDULED_SLEEP) {
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.power_scheduled_hint),
            color = PortalAniColors.TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
      }

      Spacer(Modifier.height(20.dp))
      if (appVersion.isNotBlank()) {
        Text(
            text = stringResource(R.string.app_version, appVersion),
            color = PortalAniColors.TextMuted.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
      }
      }
    }
  }

  if (sourceMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.source_mode),
        options = sourceOptions,
        selectedKey = settings.sourceMode.name,
        onDismiss = { sourceMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetSourceMode(SourceMode.valueOf(key))
        },
    )
  }

  if (frameMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.frame_mode),
        options = frameOptions,
        selectedKey = settings.frameMode.name,
        onDismiss = { frameMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetFrameMode(FrameMode.valueOf(key))
        },
    )
  }

  if (weekStartMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.week_starts_on),
        options = weekStartOptions,
        selectedKey = settings.weekStart.name,
        onDismiss = { weekStartMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetWeekStart(WeekStart.valueOf(key))
        },
    )
  }

  if (intervalMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.interval_seconds),
        options = intervalOptions,
        selectedKey = intervalSeconds.toString(),
        onDismiss = { intervalMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetIntervalSeconds(key.toIntOrNull() ?: intervalSeconds)
        },
    )
  }

  if (tempUnitMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.weather_temperature_unit),
        options = tempUnitOptions,
        selectedKey = if (settings.weatherFahrenheit) "fahrenheit" else "celsius",
        onDismiss = { tempUnitMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetWeatherFahrenheit(key == "fahrenheit")
        },
    )
  }

  if (powerMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.power_settings),
        options = powerOptions,
        selectedKey = settings.powerMode.name,
        onDismiss = { powerMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetPowerMode(PowerMode.valueOf(key))
        },
    )
  }

  if (idleMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.power_idle_after),
        options = idleOptions,
        selectedKey = settings.idleSleepMinutes.toString(),
        onDismiss = { idleMenuOpen = false },
        onSelect = { key ->
          onUserInteraction()
          onSetIdleSleepMinutes(key.toIntOrNull() ?: settings.idleSleepMinutes)
        },
    )
  }

  if (statusMenuOpen) {
    PersonalListStatusesDialog(
        selected = settings.listStatuses,
        onDismiss = { statusMenuOpen = false },
        onApply = { statuses ->
          onUserInteraction()
          onSetListStatuses(statuses)
        },
    )
  }

  if (formatMenuOpen) {
    FormatFiltersDialog(
        selected = settings.formatFilters,
        onDismiss = { formatMenuOpen = false },
        onApply = { formats ->
          onUserInteraction()
          onSetFormatFilters(formats)
        },
    )
  }

  if (countryMenuOpen) {
    CountryFiltersDialog(
        selected = settings.countryFilters,
        onDismiss = { countryMenuOpen = false },
        onApply = { countries ->
          onUserInteraction()
          onSetCountryFilters(countries)
        },
    )
  }

  if (sourceMaterialMenuOpen) {
    SourceFiltersDialog(
        selected = settings.sourceFilters,
        onDismiss = { sourceMaterialMenuOpen = false },
        onApply = { sources ->
          onUserInteraction()
          onSetSourceFilters(sources)
        },
    )
  }

  if (demographicMenuOpen) {
    DemographicFiltersDialog(
        selected = settings.demographicFilters,
        onDismiss = { demographicMenuOpen = false },
        onApply = { demographics ->
          onUserInteraction()
          onSetDemographicFilters(demographics)
        },
    )
  }

  if (sortMenuOpen) {
    PortalPickerDialog(
        title = stringResource(R.string.sort_by),
        options = sortOptions,
        selectedKey = settings.librarySort.name,
        onDismiss = { sortMenuOpen = false },
        onSelect = { key -> onSetLibrarySort(LibrarySort.valueOf(key)) },
    )
  }

  if (seasonMenuOpen) {
    PortalSeasonPickerDialog(
        title = stringResource(R.string.season_filter),
        initialState = SeasonSelection.decode(settings.seasonKey),
        onDismiss = { seasonMenuOpen = false },
        onApply = onSetSeasonKey,
    )
  }

  if (sleepStartMenuOpen) {
    PortalTimePickerDialog(
        title = stringResource(R.string.power_sleep_from),
        selectedMinutes = settings.sleepStartMinutes,
        onDismiss = { sleepStartMenuOpen = false },
        onSelect = onSetSleepStartMinutes,
    )
  }

  if (sleepEndMenuOpen) {
    PortalTimePickerDialog(
        title = stringResource(R.string.power_wake_at),
        selectedMinutes = settings.sleepEndMinutes,
        onDismiss = { sleepEndMenuOpen = false },
        onSelect = onSetSleepEndMinutes,
    )
  }

  if (locationDialogOpen) {
    WeatherLocationDialog(
        currentPlace = settings.weatherPlace,
        geoStatus = geoStatus,
        geoResults = geoResults,
        onDismiss = {
          locationDialogOpen = false
          onClearGeoSearch()
        },
        onDetectLocation = {
          onUserInteraction()
          onDetectLocation()
        },
        onSearchLocation = { query ->
          onUserInteraction()
          onSearchLocation(query)
        },
        onChooseLocation = { place ->
          onUserInteraction()
          onChooseLocation(place)
          locationDialogOpen = false
          onClearGeoSearch()
        },
    )
  }
}

@Composable
private fun CatalogFilterSettingsRows(
    settings: AppSettings,
    onFormatClick: () -> Unit,
    onCountryClick: () -> Unit,
    onSourceMaterialClick: () -> Unit,
    onDemographicClick: () -> Unit,
) {
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.format_filter),
      value = formatFiltersSettingLabel(settings.formatFilters),
      onClick = onFormatClick,
      modifier = Modifier.testTag(PortalTestTags.FORMAT_FILTER_ROW),
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.country_filter),
      value = countryFiltersSettingLabel(settings.countryFilters),
      onClick = onCountryClick,
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.source_material_filter),
      value = sourceFiltersSettingLabel(settings.sourceFilters),
      onClick = onSourceMaterialClick,
  )
  PortalSettingsDivider()
  PortalSettingsRow(
      label = stringResource(R.string.demographic_filter),
      value = demographicFiltersSettingLabel(settings.demographicFilters),
      onClick = onDemographicClick,
  )
}
