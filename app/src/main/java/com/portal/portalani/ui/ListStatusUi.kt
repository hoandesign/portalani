package com.portal.portalani.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.portal.portalani.data.ListStatus

fun ListStatus.icon(): ImageVector =
    when (this) {
      ListStatus.CURRENT -> Icons.Filled.PlayArrow
      ListStatus.PLANNING -> Icons.Filled.Add
      ListStatus.COMPLETED -> Icons.Filled.CheckCircle
      ListStatus.PAUSED -> Icons.Filled.Info
      ListStatus.DROPPED -> Icons.Filled.Close
      ListStatus.REPEATING -> Icons.Filled.Refresh
    }

fun ListStatus.accentColor(): Color =
    when (this) {
      ListStatus.CURRENT -> PortalAniColors.Accent
      ListStatus.PLANNING -> Color(0xFF7CB8FF)
      ListStatus.COMPLETED -> Color(0xFF5FD49A)
      ListStatus.PAUSED -> Color(0xFFFFB454)
      ListStatus.DROPPED -> Color(0xFFFF7A7A)
      ListStatus.REPEATING -> Color(0xFFC9A0FF)
    }
