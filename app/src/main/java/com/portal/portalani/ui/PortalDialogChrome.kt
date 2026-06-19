package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portal.portalani.R

internal val LocalPortalDialogBodyMax = compositionLocalOf { 320.dp }

internal fun BoxWithConstraintsScope.portalScreenDialogMaxHeight(requestedMax: Dp?): Dp {
  val screenCap = maxHeight * 0.86f
  return if (requestedMax != null) minOf(requestedMax, screenCap) else screenCap
}

internal fun portalDialogBodyMaxHeight(
    dialogMax: Dp,
    hasSubtitle: Boolean,
    hasFooter: Boolean,
): Dp {
  var reserved = 18.dp + 18.dp + 56.dp + 10.dp
  if (hasSubtitle) reserved += 34.dp
  if (hasFooter) reserved += 12.dp + 52.dp
  return (dialogMax - reserved).coerceAtLeast(180.dp)
}

@Composable
internal fun PortalCenteredDialog(
    onDismiss: () -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties =
          DialogProperties(
              usePlatformDefaultWidth = false,
              decorFitsSystemWindows = false,
          ),
  ) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
      BoxWithConstraints(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
          content = content,
      )
    }
  }
}

internal fun Modifier.portalDialogSurface(
    width: Dp,
    maxHeight: Dp? = null,
    fixedHeight: Dp? = null,
): Modifier =
    this
        .width(width)
        .then(
            if (fixedHeight != null) {
              Modifier.height(fixedHeight)
            } else {
              Modifier.wrapContentHeight()
                  .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
            },
        )
        .border(1.dp, PortalAniColors.Border, PortalAniShapes.Card)
        .background(PortalAniColors.SurfaceGlass, PortalAniShapes.Card)
        .padding(horizontal = 22.dp, vertical = 18.dp)

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
internal fun PortalPickerDialogHeader(title: String, onDismiss: () -> Unit) {
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
internal fun PortalPickerOptionRow(
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

@Composable
fun PortalFormDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = PortalDialogWidths.Form,
    maxHeight: Dp? = 560.dp,
    subtitle: String? = null,
    footer: @Composable (() -> Unit)? = null,
    lazyListBody: Boolean = false,
    content: @Composable () -> Unit,
) {
  PortalCenteredDialog(onDismiss = onDismiss) {
    val dialogMax = portalScreenDialogMaxHeight(maxHeight)
    val bodyMax = portalDialogBodyMaxHeight(dialogMax, !subtitle.isNullOrBlank(), footer != null)
    val pinFooter = lazyListBody && footer != null

    CompositionLocalProvider(LocalPortalDialogBodyMax provides bodyMax) {
      Column(
          modifier =
              modifier.portalDialogSurface(
                  width = width,
                  maxHeight = dialogMax,
                  fixedHeight = if (pinFooter) dialogMax else null,
              ),
      ) {
        PortalPickerDialogHeader(title = title, onDismiss = onDismiss)
        if (!subtitle.isNullOrBlank()) {
          Spacer(Modifier.height(6.dp))
          Text(
              text = subtitle,
              color = PortalAniColors.TextMuted,
              fontSize = 14.sp,
              lineHeight = 18.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(Modifier.height(10.dp))
        if (lazyListBody) {
          if (pinFooter) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              content()
            }
          } else {
            content()
          }
        } else {
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .heightIn(max = bodyMax)
                      .verticalScroll(rememberScrollState()),
          ) {
            content()
          }
        }
        if (footer != null) {
          Spacer(Modifier.height(12.dp))
          footer()
        }
      }
    }
  }
}

@Composable
internal fun PortalDialogFooter(
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

