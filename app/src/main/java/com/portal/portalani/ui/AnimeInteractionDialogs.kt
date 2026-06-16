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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portal.portalani.R
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
  var draft by remember(initialState) { mutableStateOf(initialState) }
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
fun ScoreSliderDialog(
    title: String,
    initialScore: Float?,
    onDismiss: () -> Unit,
    onSave: (Float?) -> Unit,
) {
  var sliderValue by remember { mutableFloatStateOf(initialScore ?: 0f) }
  var hasScore by remember { mutableStateOf(initialScore != null && initialScore > 0f) }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            Modifier.padding(horizontal = 48.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
      Text(title, color = PortalAniColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(8.dp))
      Text(
          stringResource(R.string.score_slider_hint),
          color = PortalAniColors.TextMuted,
          fontSize = 15.sp,
          lineHeight = 21.sp,
      )
      Spacer(Modifier.height(22.dp))
      Text(
          text =
              if (hasScore) {
                stringResource(R.string.your_score_value, sliderValue.roundToInt())
              } else {
                stringResource(R.string.score_none)
              },
          color = if (hasScore) PortalAniColors.Score else PortalAniColors.TextSecondary,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(18.dp))
      Slider(
          value = sliderValue.coerceIn(0f, 10f),
          onValueChange = {
            sliderValue = it
            hasScore = it > 0f
          },
          valueRange = 0f..10f,
          steps = 9,
          colors =
              SliderDefaults.colors(
                  thumbColor = PortalAniColors.Accent,
                  activeTrackColor = PortalAniColors.Accent,
                  inactiveTrackColor = PortalAniColors.SurfaceElevated,
              ),
      )
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("0", color = PortalAniColors.TextMuted, fontSize = 14.sp)
        Text("10", color = PortalAniColors.TextMuted, fontSize = 14.sp)
      }
      Spacer(Modifier.height(20.dp))
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.close), color = PortalAniColors.TextSecondary)
        }
        TextButton(
            onClick = {
              hasScore = false
              sliderValue = 0f
            },
        ) {
          Text(stringResource(R.string.score_clear), color = PortalAniColors.TextSecondary)
        }
        PortalPrimaryButton(
            text = stringResource(R.string.save),
            onClick = { onSave(if (hasScore) sliderValue.roundToInt().toFloat() else null) },
        )
      }
    }
  }
}

@Composable
fun ListStatusDialog(
    title: String,
    currentStatus: ListStatus?,
    onDismiss: () -> Unit,
    onSelect: (ListStatus) -> Unit,
    onRemove: (() -> Unit)?,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            Modifier.padding(horizontal = 48.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 520.dp)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
      PortalPickerDialogHeader(title = title, onDismiss = onDismiss)
      Spacer(Modifier.height(8.dp))
      Text(stringResource(R.string.list_status_dialog_hint), color = PortalAniColors.TextMuted, fontSize = 15.sp)
      Spacer(Modifier.height(16.dp))
      LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(ListStatus.entries, key = { it.name }) { status ->
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
        TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.CenterHorizontally)) {
          Text(stringResource(R.string.remove_from_list), color = PortalAniColors.TextMuted)
        }
      }
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
