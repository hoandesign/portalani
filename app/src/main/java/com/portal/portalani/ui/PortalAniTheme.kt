package com.portal.portalani.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PortalAniColors {
  val Background = Color(0xFF0B0D12)
  val Surface = Color(0xFF161A22)
  val SurfaceElevated = Color(0xFF1F2430)
  val SurfaceGlass = Color(0xE8161A22)
  val Accent = Color(0xFF4F8CFF)
  val AccentSoft = Color(0x334F8CFF)
  val AccentStrong = Color(0xFF2F6FE8)
  val TextPrimary = Color(0xFFF5F7FB)
  val TextSecondary = Color(0xFFC4CAD6)
  val TextMuted = Color(0xFF8B93A3)
  val Border = Color(0x22FFFFFF)
  val BorderStrong = Color(0x44FFFFFF)
  val Gold = Color(0xFFFFD166)
  val Cyan = Color(0xFF7DD3FC)
  val Score = Color(0xFF3B82F6)
  val Overlay = Color(0xCC05070C)
}

object FrameViewerInsets {
  /** Slideshow is immersive (system bars hidden) — no extra top safe-zone inset. */
  val horizontal = 32.dp
  val vertical = 12.dp
}

object PortalDialogWidths {
  val Picker = 400.dp
  val SeasonPicker = 520.dp
  val Form = 440.dp
  val Rating = 560.dp
}

object PortalAniShapes {
  val Poster = RoundedCornerShape(12.dp)
  val Card = RoundedCornerShape(28.dp)
  val Panel = RoundedCornerShape(20.dp)
  val Pill = RoundedCornerShape(50)
  val Chip = RoundedCornerShape(12.dp)
  val Button = RoundedCornerShape(14.dp)
  val Field = RoundedCornerShape(16.dp)
}

private val PortalColorScheme =
    darkColorScheme(
        primary = PortalAniColors.Accent,
        onPrimary = Color.White,
        surface = PortalAniColors.Surface,
        onSurface = PortalAniColors.TextPrimary,
        outline = PortalAniColors.BorderStrong,
    )

@Composable
fun PortalAniTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = PortalColorScheme, content = content)
}

@Composable
fun PortalPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.height(52.dp),
      shape = PortalAniShapes.Button,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = PortalAniColors.Accent,
              contentColor = Color.White,
              disabledContainerColor = PortalAniColors.Accent.copy(alpha = 0.35f),
          ),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
  ) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun PortalSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  OutlinedButton(
      onClick = onClick,
      modifier = modifier.height(52.dp),
      shape = PortalAniShapes.Button,
      border = BorderStroke(1.dp, PortalAniColors.BorderStrong),
      colors =
          ButtonDefaults.outlinedButtonColors(
              contentColor = PortalAniColors.TextPrimary,
              containerColor = Color(0x14FFFFFF),
          ),
  ) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun PortalSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier =
          modifier
              .clip(PortalAniShapes.Field)
              .background(PortalAniColors.SurfaceElevated)
              .border(1.dp, PortalAniColors.Border, PortalAniShapes.Field)
              .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    options.forEachIndexed { index, label ->
      val selected = index == selectedIndex
      Box(
          modifier =
              Modifier.weight(1f)
                  .height(48.dp)
                  .clip(PortalAniShapes.Chip)
                  .background(
                      if (selected) {
                        Brush.horizontalGradient(
                            listOf(PortalAniColors.AccentStrong, PortalAniColors.Accent),
                        )
                      } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                      },
                  )
                  .clickable { onSelect(index) },
          contentAlignment = Alignment.Center,
      ) {
        Text(
            text = label,
            color = if (selected) Color.White else PortalAniColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
      }
    }
  }
}

@Composable
fun PortalChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val bg = if (selected) PortalAniColors.AccentSoft else Color(0x12FFFFFF)
  val border = if (selected) PortalAniColors.Accent else PortalAniColors.Border
  Surface(
      onClick = onClick,
      modifier = modifier.height(48.dp),
      shape = PortalAniShapes.Chip,
      color = bg,
      border = BorderStroke(1.dp, border),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
      Text(
          text = label,
          color = if (selected) PortalAniColors.Accent else PortalAniColors.TextPrimary,
          fontSize = 16.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      )
    }
  }
}

@Composable
fun PortalSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = title,
        color = PortalAniColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
    )
    if (!subtitle.isNullOrBlank()) {
      Spacer(Modifier.height(4.dp))
      Text(
          text = subtitle,
          color = PortalAniColors.TextMuted,
          fontSize = 14.sp,
          lineHeight = 19.sp,
      )
    }
  }
}

@Composable
fun PortalSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      shape = PortalAniShapes.Panel,
      color = PortalAniColors.SurfaceElevated,
      border = BorderStroke(1.dp, PortalAniColors.Border),
  ) {
    Column(modifier = Modifier.padding(vertical = 4.dp), content = content)
  }
}

@Composable
fun PortalSettingsDivider() {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp)
              .height(1.dp)
              .background(PortalAniColors.Border),
  )
}

@Composable
fun PortalSettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showChevron: Boolean = true,
) {
  Surface(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
      color = Color.Transparent,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = label,
          color = if (enabled) PortalAniColors.TextPrimary else PortalAniColors.TextMuted,
          fontSize = 18.sp,
          modifier = Modifier.weight(1f).padding(end = 16.dp),
      )
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (value.isNotBlank()) {
          Text(
              text = value,
              color = if (enabled) PortalAniColors.TextMuted else PortalAniColors.TextMuted.copy(alpha = 0.55f),
              fontSize = 17.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
        if (showChevron && enabled) {
          Icon(
              imageVector = PortalIcons.Dropdown,
              contentDescription = null,
              tint = PortalAniColors.TextMuted,
              modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
fun PortalSettingsActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = PortalAniColors.Accent,
) {
  Surface(
      onClick = onClick,
      modifier = modifier.fillMaxWidth(),
      color = Color.Transparent,
  ) {
    Text(
        text = label,
        color = labelColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    )
  }
}

@Composable
fun PortalSettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = label,
        color = PortalAniColors.TextPrimary,
        fontSize = 18.sp,
        modifier = Modifier.weight(1f).padding(end = 16.dp),
    )
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors =
            androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PortalAniColors.Accent,
                uncheckedThumbColor = PortalAniColors.TextSecondary,
                uncheckedTrackColor = PortalAniColors.Surface,
            ),
    )
  }
}

@Composable
fun PortalFilterField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      onClick = onClick,
      modifier = modifier.fillMaxWidth(),
      shape = PortalAniShapes.Field,
      color = PortalAniColors.SurfaceElevated,
      border = BorderStroke(1.dp, PortalAniColors.Border),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(label, color = PortalAniColors.TextMuted, fontSize = 13.sp)
        Text(value, color = PortalAniColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
      }
      Icon(
          imageVector = PortalIcons.Dropdown,
          contentDescription = null,
          tint = PortalAniColors.TextMuted,
          modifier = Modifier.size(24.dp),
      )
    }
  }
}
