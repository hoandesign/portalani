package com.portal.portalani.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.data.CountryFilter
import com.portal.portalani.data.DemographicFilter
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.SourceFilter

@Composable
fun FormatFiltersDialog(
    selected: Set<FormatFilter>,
    onDismiss: () -> Unit,
    onApply: (Set<FormatFilter>) -> Unit,
) {
  MultiSelectFilterDialog(
      title = stringResource(R.string.format_filter),
      hint = stringResource(R.string.format_filters_picker_hint),
      options = FormatFilter.selectable,
      labelOf = { it.label },
      selected = selected,
      normalize = FormatFilter::normalizeSelection,
      onDismiss = onDismiss,
      onApply = onApply,
  )
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
    1 -> ordered.first().pickerLabel
    2 -> "${ordered[0].pickerLabel}, ${ordered[1].pickerLabel}"
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
      labelOf = { it.pickerLabel },
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
      onDismiss = onDismiss,
      modifier = Modifier.testTag(PortalTestTags.FILTER_DIALOG),
      width = PortalDialogWidths.Picker,
      maxHeight = PortalDialogWidths.FilterPickerDialog,
      lazyListBody = true,
      footer = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
          PortalSecondaryButton(
              text = stringResource(R.string.close),
              onClick = onDismiss,
              modifier = Modifier.testTag(PortalTestTags.FILTER_DIALOG_CLOSE),
          )
          Spacer(Modifier.width(10.dp))
          PortalPrimaryButton(
              text = stringResource(R.string.apply),
              onClick = {
                if (draft.isNotEmpty()) {
                  onApply(draft)
                }
                onDismiss()
              },
              modifier = Modifier.testTag(PortalTestTags.FILTER_DIALOG_APPLY),
          )
        }
      },
  ) {
    val bodyMax = LocalPortalDialogBodyMax.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = bodyMax),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Text(
            hint,
            color = PortalAniColors.TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
      }
      items(
          items = options,
          key = { option -> labelOf(option) },
      ) { option ->
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
  }
}
