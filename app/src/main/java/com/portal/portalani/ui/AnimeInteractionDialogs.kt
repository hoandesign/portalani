package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portal.portalani.R
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.SourceFilter
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.PowerPolicy
import com.portal.portalani.data.SeasonPickerSeason
import com.portal.portalani.data.SeasonPickerState
import com.portal.portalani.data.SeasonPickerYear
import com.portal.portalani.data.SeasonSelection
import kotlin.math.roundToInt

@Composable
fun PortalPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    iconForKey: ((String) -> ImageVector)? = null,
    tintForKey: ((String) -> Color)? = null,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            Modifier.width(PortalDialogWidths.Picker)
                .heightIn(max = 460.dp)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
      PortalPickerDialogHeader(title = title, onDismiss = onDismiss)
      Spacer(Modifier.height(12.dp))
      LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(options, key = { it.first }) { (key, label) ->
          PortalPickerOptionRow(
              label = label,
              selected = key == selectedKey,
              onClick = {
                onSelect(key)
                onDismiss()
              },
              leadingIcon = iconForKey?.invoke(key),
              iconTint = tintForKey?.invoke(key),
          )
        }
      }
    }
  }
}

@Composable
fun PortalSeasonPickerDialog(
    title: String,
    initialState: SeasonPickerState,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
  var draft by remember(initialState) { mutableStateOf(SeasonSelection.normalizePickerState(initialState)) }
  val nowYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
  val seasonOptions = remember { SeasonSelection.seasonColumnOptions() }
  val yearOptions = remember(nowYear) { SeasonSelection.yearColumnOptions(nowYear) }
  val yearColumnEnabled = !draft.season.ignoresYear

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            Modifier.width(PortalDialogWidths.SeasonPicker)
                .heightIn(max = 520.dp)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
      PortalPickerDialogHeader(title = title, onDismiss = onDismiss)
      Spacer(Modifier.height(10.dp))
      Text(
          text = SeasonSelection.labelFor(draft),
          color = PortalAniColors.Accent,
          fontSize = 17.sp,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(14.dp))
      Row(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = stringResource(R.string.season_picker_season),
              color = PortalAniColors.TextMuted,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
          )
          Spacer(Modifier.height(8.dp))
          LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 290.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(seasonOptions, key = { it.name }) { season ->
              PortalPickerOptionRow(
                  label = season.label,
                  selected = draft.season == season,
                  onClick = {
                    draft =
                        if (season.ignoresYear) {
                          SeasonPickerState(season, SeasonPickerYear.Any)
                        } else {
                          draft.copy(season = season)
                        }
                  },
                  compact = true,
              )
            }
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = stringResource(R.string.season_picker_year),
              color = if (yearColumnEnabled) PortalAniColors.TextMuted else PortalAniColors.TextMuted.copy(alpha = 0.45f),
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
          )
          Spacer(Modifier.height(8.dp))
          LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 290.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(yearOptions, key = { yearOptionKey(it) }) { year ->
              PortalPickerOptionRow(
                  label = year.label(nowYear),
                  selected = draft.year::class == year::class &&
                      when (year) {
                        is SeasonPickerYear.Specific ->
                            draft.year is SeasonPickerYear.Specific &&
                                (draft.year as SeasonPickerYear.Specific).year == year.year
                        else -> draft.year == year
                      },
                  enabled = yearColumnEnabled,
                  onClick = { draft = draft.copy(year = year) },
                  compact = true,
              )
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
      ) {
        PortalPrimaryButton(
            text = stringResource(R.string.apply),
            onClick = {
              onApply(SeasonSelection.encode(draft))
              onDismiss()
            },
        )
      }
    }
  }
}

@Composable
private fun PortalPickerDialogHeader(title: String, onDismiss: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, color = PortalAniColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    PortalCircleIconButton(
        icon = PortalIcons.Close,
        contentDescription = stringResource(R.string.close),
        onClick = onDismiss,
    )
  }
}

@Composable
private fun PortalPickerOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    leadingIcon: ImageVector? = null,
    iconTint: Color? = null,
) {
  val colors =
      when {
        !enabled -> PortalAniColors.TextMuted.copy(alpha = 0.45f) to Color(0x08FFFFFF)
        selected -> PortalAniColors.Accent to PortalAniColors.AccentSoft
        else -> PortalAniColors.TextPrimary to Color(0x10FFFFFF)
      }
  val resolvedIconTint =
      when {
        !enabled -> colors.first
        iconTint != null -> if (selected) iconTint else iconTint.copy(alpha = 0.82f)
        selected -> colors.first
        else -> PortalAniColors.TextSecondary
      }
  Surface(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
      shape = PortalAniShapes.Field,
      color = colors.second,
      border =
          androidx.compose.foundation.BorderStroke(
              1.dp,
              when {
                !enabled -> PortalAniColors.Border.copy(alpha = 0.35f)
                selected -> iconTint ?: PortalAniColors.Accent
                else -> PortalAniColors.Border
              },
          ),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = if (compact) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (leadingIcon != null) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = resolvedIconTint,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
        )
      }
      Text(
          text = label,
          color = colors.first,
          fontSize = if (compact) 16.sp else 17.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
          modifier = Modifier.weight(1f, fill = false),
      )
    }
  }
}

private fun yearOptionKey(year: SeasonPickerYear): String =
    when (year) {
      SeasonPickerYear.Any -> "any"
      SeasonPickerYear.ThisYear -> "this"
      SeasonPickerYear.LastYear -> "last"
      is SeasonPickerYear.Specific -> "y${year.year}"
    }

@Composable
fun PortalTimePickerDialog(
    title: String,
    selectedMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
  val options = remember { PowerPolicy.timePickerOptions() }
  PortalPickerDialog(
      title = title,
      options = options.map { minutes -> minutes.toString() to PowerPolicy.formatMinutesOfDay(minutes) },
      selectedKey = selectedMinutes.toString(),
      onDismiss = onDismiss,
      onSelect = { key -> onSelect(key.toIntOrNull() ?: selectedMinutes) },
  )
}

@Composable
fun PortalCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = PortalAniColors.TextSecondary,
    background: Color = Color(0x1AFFFFFF),
    borderColor: Color = PortalAniColors.BorderStrong,
) {
  Surface(
      onClick = onClick,
      modifier = modifier.size(size),
      shape = CircleShape,
      color = background,
      border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          tint = tint,
          modifier = Modifier.size(iconSize),
      )
    }
  }
}

@Composable
fun PortalScoreActionButton(
    score: Float?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val hasScore = score != null && score > 0f
  val tint = if (hasScore) PortalAniColors.Gold else PortalAniColors.TextPrimary
  val bg = if (hasScore) PortalAniColors.Gold.copy(alpha = 0.22f) else Color(0x1FFFFFFF)
  val borderColor = if (hasScore) PortalAniColors.Gold.copy(alpha = 0.62f) else PortalAniColors.Border

  Surface(
      onClick = onClick,
      modifier = modifier.size(52.dp),
      shape = CircleShape,
      color = bg,
      border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (hasScore) {
        Text(
            text = score.roundToInt().toString(),
            color = tint,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
      } else {
        Icon(
            imageVector = PortalIcons.ScoreStar,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = 0.72f),
            modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

@Composable
fun PortalIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color = PortalAniColors.Accent,
    idleTint: Color = PortalAniColors.TextPrimary,
) {
  val tint = if (selected) selectedTint else idleTint
  val bg = if (selected) selectedTint.copy(alpha = 0.18f) else Color(0x1FFFFFFF)
  val borderColor = if (selected) selectedTint.copy(alpha = 0.55f) else PortalAniColors.Border

  Surface(
      onClick = onClick,
      modifier = modifier.size(52.dp),
      shape = CircleShape,
      color = bg,
      border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          tint = tint,
          modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
fun PortalFormDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = PortalDialogWidths.Form,
    maxHeight: Dp? = 560.dp,
    subtitle: String? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            modifier
                .width(width)
                .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
      PortalPickerDialogHeader(title = title, onDismiss = onDismiss)
      if (!subtitle.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = PortalAniColors.TextMuted,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(12.dp))
      content()
      if (footer != null) {
        Spacer(Modifier.height(16.dp))
        footer()
      }
    }
  }
}

@Composable
private fun PortalDialogFooter(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (secondaryText != null && onSecondary != null) {
      PortalSecondaryButton(
          text = secondaryText,
          onClick = onSecondary,
          modifier = Modifier.widthIn(min = 120.dp),
      )
    }
    PortalPrimaryButton(
        text = primaryText,
        onClick = onPrimary,
        modifier = Modifier.widthIn(min = 120.dp),
    )
  }
}

@Composable
fun ScoreSliderDialog(
    animeTitle: String,
    initialScore: Float?,
    onDismiss: () -> Unit,
    onSave: (Float?) -> Unit,
) {
  var score by remember {
    mutableStateOf(initialScore?.roundToInt()?.takeIf { it in 1..10 })
  }

  PortalFormDialog(
      title = stringResource(R.string.score_dialog_title),
      subtitle = animeTitle,
      onDismiss = onDismiss,
      width = PortalDialogWidths.Rating,
      footer = {
        PortalDialogFooter(
            primaryText = stringResource(R.string.save),
            onPrimary = { onSave(score?.toFloat()) },
            secondaryText = stringResource(R.string.score_clear),
            onSecondary = { score = null },
        )
      },
  ) {
    Text(
        stringResource(R.string.score_slider_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(20.dp))
    PortalStarRatingRow(
        score = score,
        onScoreSelected = { score = it },
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text =
            if (score != null) {
              stringResource(R.string.score_out_of_ten, score!!)
            } else {
              stringResource(R.string.score_none)
            },
        color = if (score != null) PortalAniColors.Gold else PortalAniColors.TextSecondary,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
  }
}

private const val STAR_RATING_COUNT = 10

private fun scoreForPointerX(x: Float, widthPx: Float): Int? {
  if (widthPx <= 0f) return null
  val segment = widthPx / STAR_RATING_COUNT
  if (segment <= 0f) return null
  return ((x / segment).toInt().coerceIn(0, STAR_RATING_COUNT - 1)) + 1
}

@Composable
private fun PortalStarRatingRow(
    score: Int?,
    onScoreSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 44.dp,
) {
  val density = LocalDensity.current
  val starSpacing = 6.dp
  val touchHeight = starSize + 8.dp

  BoxWithConstraints(
      modifier = modifier.fillMaxWidth(),
  ) {
    val rowWidthPx = with(density) { maxWidth.toPx() }

    fun applyPointerX(x: Float) {
      onScoreSelected(scoreForPointerX(x, rowWidthPx))
    }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(touchHeight)
                .pointerInput(rowWidthPx) {
                  detectDragGestures(
                      onDragStart = { offset -> applyPointerX(offset.x) },
                      onDrag = { change, _ -> applyPointerX(change.position.x) },
                  )
                },
        horizontalArrangement = Arrangement.spacedBy(starSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      for (starIndex in 1..STAR_RATING_COUNT) {
        val filled = score != null && starIndex <= score
        PortalStarRatingStar(filled = filled, size = starSize)
      }
    }
  }
}

@Composable
private fun PortalStarRatingStar(
    filled: Boolean,
    size: Dp,
) {
  val emptyTint = PortalAniColors.TextMuted.copy(alpha = 0.45f)
  val gold = PortalAniColors.Gold
  val iconSize = size * 0.68f

  Box(
      modifier = Modifier.size(size),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = PortalIcons.ScoreStar,
        contentDescription = null,
        tint = emptyTint,
        modifier = Modifier.size(iconSize),
    )
    if (filled) {
      Icon(
          imageVector = PortalIcons.ScoreStar,
          contentDescription = null,
          tint = gold,
          modifier = Modifier.size(iconSize),
      )
    }
  }
}

@Composable
fun FormatFiltersDialog(
    selected: Set<FormatFilter>,
    onDismiss: () -> Unit,
    onApply: (Set<FormatFilter>) -> Unit,
) {
  var draft by remember(selected) { mutableStateOf(FormatFilter.normalizeSelection(selected)) }

  PortalFormDialog(
      title = stringResource(R.string.format_filter),
      subtitle = stringResource(R.string.library_filters),
      onDismiss = onDismiss,
      width = PortalDialogWidths.Picker,
      maxHeight = null,
  ) {
    Text(
        stringResource(R.string.format_filters_picker_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FormatFilter.selectable.forEach { format ->
        val isSelected = format in draft
        PortalPickerOptionRow(
            label = format.label,
            selected = isSelected,
            onClick = {
              draft =
                  if (isSelected) {
                    if (draft.size == 1) draft else draft - format
                  } else {
                    draft + format
                  }
            },
        )
      }
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
      PortalSecondaryButton(text = stringResource(R.string.close), onClick = onDismiss)
      Spacer(Modifier.width(10.dp))
      PortalPrimaryButton(
          text = stringResource(R.string.apply),
          onClick = {
            if (draft.isNotEmpty()) {
              onApply(draft)
            }
            onDismiss()
          },
      )
    }
  }
}

@Composable
fun formatFiltersSettingLabel(formats: Set<FormatFilter>): String {
  val normalized = FormatFilter.normalizeSelection(formats)
  if (FormatFilter.isAllSelected(normalized)) return stringResource(R.string.format_filters_all)
  val ordered = FormatFilter.selectable.filter { it in normalized }
  return when (ordered.size) {
    0 -> stringResource(R.string.format_filters_all)
    1 -> ordered.first().label
    2 -> "${ordered[0].label}, ${ordered[1].label}"
    else -> stringResource(R.string.format_filters_count, ordered.size)
  }
}

@Composable
fun countryFiltersSettingLabel(countries: Set<CountryFilter>): String {
  val normalized = CountryFilter.normalizeSelection(countries)
  if (CountryFilter.isAllSelected(normalized)) return stringResource(R.string.country_filters_all)
  val ordered = CountryFilter.selectable.filter { it in normalized }
  return when (ordered.size) {
    0 -> stringResource(R.string.country_filters_all)
    1 -> ordered.first().label
    2 -> "${ordered[0].label}, ${ordered[1].label}"
    else -> stringResource(R.string.country_filters_count, ordered.size)
  }
}

@Composable
fun sourceFiltersSettingLabel(sources: Set<SourceFilter>): String {
  val normalized = SourceFilter.normalizeSelection(sources)
  if (SourceFilter.isAllSelected(normalized)) return stringResource(R.string.source_filters_all)
  val ordered = SourceFilter.selectable.filter { it in normalized }
  return when (ordered.size) {
    0 -> stringResource(R.string.source_filters_all)
    1 -> ordered.first().label
    2 -> "${ordered[0].label}, ${ordered[1].label}"
    else -> stringResource(R.string.source_filters_count, ordered.size)
  }
}

@Composable
fun demographicFiltersSettingLabel(demographics: Set<DemographicFilter>): String {
  val normalized = DemographicFilter.normalizeSelection(demographics)
  if (DemographicFilter.isAllSelected(normalized)) return stringResource(R.string.demographic_filters_all)
  val ordered = DemographicFilter.selectable.filter { it in normalized }
  return when (ordered.size) {
    0 -> stringResource(R.string.demographic_filters_all)
    1 -> ordered.first().label
    2 -> "${ordered[0].label}, ${ordered[1].label}"
    else -> stringResource(R.string.demographic_filters_count, ordered.size)
  }
}

@Composable
fun CountryFiltersDialog(
    selected: Set<CountryFilter>,
    onDismiss: () -> Unit,
    onApply: (Set<CountryFilter>) -> Unit,
) {
  MultiSelectFilterDialog(
      title = stringResource(R.string.country_filter),
      hint = stringResource(R.string.country_filters_picker_hint),
      options = CountryFilter.selectable,
      labelOf = { it.label },
      selected = selected,
      normalize = CountryFilter::normalizeSelection,
      onDismiss = onDismiss,
      onApply = onApply,
  )
}

@Composable
fun SourceFiltersDialog(
    selected: Set<SourceFilter>,
    onDismiss: () -> Unit,
    onApply: (Set<SourceFilter>) -> Unit,
) {
  MultiSelectFilterDialog(
      title = stringResource(R.string.source_material_filter),
      hint = stringResource(R.string.source_filters_picker_hint),
      options = SourceFilter.selectable,
      labelOf = { it.label },
      selected = selected,
      normalize = SourceFilter::normalizeSelection,
      onDismiss = onDismiss,
      onApply = onApply,
  )
}

@Composable
fun DemographicFiltersDialog(
    selected: Set<DemographicFilter>,
    onDismiss: () -> Unit,
    onApply: (Set<DemographicFilter>) -> Unit,
) {
  MultiSelectFilterDialog(
      title = stringResource(R.string.demographic_filter),
      hint = stringResource(R.string.demographic_filters_picker_hint),
      options = DemographicFilter.selectable,
      labelOf = { it.label },
      selected = selected,
      normalize = DemographicFilter::normalizeSelection,
      onDismiss = onDismiss,
      onApply = onApply,
  )
}

@Composable
private fun <T> MultiSelectFilterDialog(
    title: String,
    hint: String,
    options: List<T>,
    labelOf: (T) -> String,
    selected: Set<T>,
    normalize: (Set<T>) -> Set<T>,
    onDismiss: () -> Unit,
    onApply: (Set<T>) -> Unit,
) {
  var draft by remember(selected) { mutableStateOf(normalize(selected)) }

  PortalFormDialog(
      title = title,
      subtitle = stringResource(R.string.library_filters),
      onDismiss = onDismiss,
      width = PortalDialogWidths.Picker,
      maxHeight = null,
  ) {
    Text(
        hint,
        color = PortalAniColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      options.forEach { option ->
        val isSelected = option in draft
        PortalPickerOptionRow(
            label = labelOf(option),
            selected = isSelected,
            onClick = {
              draft =
                  if (isSelected) {
                    if (draft.size == 1) draft else draft - option
                  } else {
                    draft + option
                  }
            },
        )
      }
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
      PortalSecondaryButton(text = stringResource(R.string.close), onClick = onDismiss)
      Spacer(Modifier.width(10.dp))
      PortalPrimaryButton(
          text = stringResource(R.string.apply),
          onClick = {
            if (draft.isNotEmpty()) {
              onApply(draft)
            }
            onDismiss()
          },
      )
    }
  }
}

@Composable
fun PersonalListStatusesDialog(
    selected: Set<ListStatus>,
    onDismiss: () -> Unit,
    onApply: (Set<ListStatus>) -> Unit,
) {
  var draft by remember(selected) { mutableStateOf(selected) }

  PortalFormDialog(
      title = stringResource(R.string.list_status),
      subtitle = stringResource(R.string.personal_lists_hint),
      onDismiss = onDismiss,
      width = PortalDialogWidths.Picker,
      maxHeight = null,
  ) {
    Text(
        stringResource(R.string.list_statuses_picker_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ListStatus.entries.forEach { status ->
        val isSelected = status in draft
        PortalPickerOptionRow(
            label = listStatusLabel(status),
            selected = isSelected,
            onClick = {
              draft =
                  if (isSelected) {
                    if (draft.size == 1) draft else draft - status
                  } else {
                    draft + status
                  }
            },
            leadingIcon = status.icon(),
            iconTint = status.accentColor(),
        )
      }
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
      PortalSecondaryButton(text = stringResource(R.string.close), onClick = onDismiss)
      Spacer(Modifier.width(10.dp))
      PortalPrimaryButton(
          text = stringResource(R.string.apply),
          onClick = {
            if (draft.isNotEmpty()) {
              onApply(draft)
            }
            onDismiss()
          },
      )
    }
  }
}

@Composable
fun listStatusesSettingLabel(statuses: Set<ListStatus>): String {
  if (statuses.size >= ListStatus.entries.size) {
    return stringResource(R.string.list_statuses_all)
  }
  val ordered = ListStatus.entries.filter { it in statuses }
  return when (ordered.size) {
    0 -> stringResource(R.string.list_statuses_count, 0)
    1 -> listStatusLabel(ordered.first())
    2 -> "${listStatusLabel(ordered[0])}, ${listStatusLabel(ordered[1])}"
    else -> stringResource(R.string.list_statuses_count, ordered.size)
  }
}

@Composable
fun ListStatusDialog(
    animeTitle: String,
    currentStatus: ListStatus?,
    onDismiss: () -> Unit,
    onSelect: (ListStatus) -> Unit,
    onRemove: (() -> Unit)?,
) {
  val title =
      if (currentStatus != null) {
        stringResource(R.string.list_dialog_change_title)
      } else {
        stringResource(R.string.list_dialog_add_title)
      }

  PortalFormDialog(
      title = title,
      subtitle = animeTitle,
      onDismiss = onDismiss,
      width = PortalDialogWidths.Picker,
      maxHeight = null,
  ) {
    Text(
        stringResource(R.string.list_status_dialog_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    Spacer(Modifier.height(14.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ListStatus.entries.forEach { status ->
        val selected = currentStatus == status
        PortalPickerOptionRow(
            label = listStatusLabel(status),
            selected = selected,
            onClick = { onSelect(status) },
            leadingIcon = status.icon(),
            iconTint = status.accentColor(),
        )
      }
    }
    if (onRemove != null && currentStatus != null) {
      Spacer(Modifier.height(12.dp))
      PortalSettingsDivider()
      PortalSettingsActionRow(
          label = stringResource(R.string.remove_from_list),
          onClick = onRemove,
          labelColor = PortalAniColors.TextMuted,
      )
    }
  }
}

@Composable
fun SignInPromptDialog(
    message: String,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(message, color = PortalAniColors.TextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
      Spacer(Modifier.height(20.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PortalSecondaryButton(text = stringResource(R.string.close), onClick = onDismiss)
        PortalPrimaryButton(text = stringResource(R.string.sign_in), onClick = onSignIn)
      }
    }
  }
}

@Composable
fun listStatusLabel(status: ListStatus): String =
    when (status) {
      ListStatus.CURRENT -> stringResource(R.string.status_current)
      ListStatus.PLANNING -> stringResource(R.string.status_planning)
      ListStatus.COMPLETED -> stringResource(R.string.status_completed)
      ListStatus.PAUSED -> stringResource(R.string.status_paused)
      ListStatus.DROPPED -> stringResource(R.string.status_dropped)
      ListStatus.REPEATING -> stringResource(R.string.status_repeating)
    }
