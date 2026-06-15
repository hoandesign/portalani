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
import kotlin.math.roundToInt

@Composable
fun PortalPickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
        modifier =
            Modifier.padding(horizontal = 48.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .heightIn(max = 420.dp)
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
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
      Spacer(Modifier.height(12.dp))
      LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(options, key = { it.first }) { (key, label) ->
          val selected = key == selectedKey
          Surface(
              onClick = {
                onSelect(key)
                onDismiss()
              },
              modifier = Modifier.fillMaxWidth(),
              shape = PortalAniShapes.Field,
              color = if (selected) PortalAniColors.AccentSoft else Color(0x10FFFFFF),
              border =
                  androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (selected) PortalAniColors.Accent else PortalAniColors.Border,
                  ),
          ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                color = if (selected) PortalAniColors.Accent else PortalAniColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
          }
        }
      }
    }
  }
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
                .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
                .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
                .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, color = PortalAniColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = onDismiss) {
          Text(stringResource(R.string.close), color = PortalAniColors.TextSecondary)
        }
      }
      Spacer(Modifier.height(8.dp))
      Text(stringResource(R.string.list_status_dialog_hint), color = PortalAniColors.TextMuted, fontSize = 15.sp)
      Spacer(Modifier.height(16.dp))
      ListStatus.entries.forEach { status ->
        val selected = currentStatus == status
        Surface(
            onClick = { onSelect(status) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            shape = PortalAniShapes.Field,
            color = if (selected) PortalAniColors.AccentSoft else Color(0x10FFFFFF),
            border =
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) PortalAniColors.Accent else PortalAniColors.Border,
                ),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                listStatusLabel(status),
                color = if (selected) PortalAniColors.Accent else PortalAniColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (selected) {
              Icon(
                  imageVector = PortalIcons.Check,
                  contentDescription = null,
                  tint = PortalAniColors.Accent,
                  modifier = Modifier.size(22.dp),
              )
            }
          }
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
