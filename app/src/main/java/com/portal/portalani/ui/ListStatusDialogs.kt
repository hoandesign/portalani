package com.portal.portalani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R
import com.portal.portalani.data.ListStatus

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
      maxHeight = PortalDialogWidths.PickerDialog,
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
    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Text(
            stringResource(R.string.list_statuses_picker_hint),
            color = PortalAniColors.TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
      }
      items(
          items = ListStatus.entries,
          key = { it.name },
      ) { status ->
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
      modifier = Modifier.testTag(PortalTestTags.LIST_STATUS_DIALOG),
      width = PortalDialogWidths.Picker,
      maxHeight = PortalDialogWidths.PickerDialog,
      lazyListBody = true,
  ) {
    val bodyMax = LocalPortalDialogBodyMax.current
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = bodyMax).testTag(PortalTestTags.LIST_STATUS_DIALOG_LIST),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        Text(
            stringResource(R.string.list_status_dialog_hint),
            color = PortalAniColors.TextMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(6.dp))
      }
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
      if (onRemove != null && currentStatus != null) {
        item {
          Spacer(Modifier.height(4.dp))
          PortalSettingsDivider()
          PortalSettingsActionRow(
              label = stringResource(R.string.remove_from_list),
              onClick = onRemove,
              labelColor = PortalAniColors.TextMuted,
          )
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
  PortalCenteredDialog(onDismiss = onDismiss) {
    Column(
        modifier =
            Modifier.width(PortalDialogWidths.Form)
                .wrapContentHeight()
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
