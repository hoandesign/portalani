package com.portal.portalani.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal.portalani.R

@Composable
internal fun SigningInScreen(
    onCancel: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text("Portal Ani", color = PortalAniColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    Text(
        stringResource(R.string.sign_in_hint),
        color = PortalAniColors.TextMuted,
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    PortalSecondaryButton(
        text = stringResource(R.string.cancel_sign_in),
        onClick = onCancel,
        modifier = Modifier.width(360.dp),
    )
  }
}

@Composable
internal fun SetupScreen(
    message: String,
    canSignIn: Boolean,
    canUseLibrary: Boolean,
    onSignIn: () -> Unit,
    onUseLibrary: () -> Unit,
    onRetry: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text("Portal Ani", color = PortalAniColors.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    Text(message, color = PortalAniColors.TextMuted, fontSize = 20.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    if (canSignIn) {
      PortalPrimaryButton(
          text = stringResource(R.string.sign_in),
          onClick = onSignIn,
          modifier = Modifier.width(360.dp),
      )
      Spacer(Modifier.height(16.dp))
    }
    if (canUseLibrary) {
      PortalSecondaryButton(
          text = stringResource(R.string.use_full_library),
          onClick = onUseLibrary,
          modifier = Modifier.width(360.dp),
      )
      Spacer(Modifier.height(16.dp))
    }
    TextButton(onClick = onRetry) {
      Text(stringResource(R.string.retry), fontSize = 20.sp, color = PortalAniColors.TextSecondary)
    }
  }
}

@Composable
internal fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    canOpenSettings: Boolean = false,
    onOpenSettings: () -> Unit = {},
) {
  Column(
      modifier = Modifier.fillMaxSize().padding(top = 64.dp, start = 48.dp, end = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(message, color = PortalAniColors.TextPrimary, fontSize = 24.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    if (canOpenSettings) {
      PortalPrimaryButton(
          text = stringResource(R.string.open_settings),
          onClick = onOpenSettings,
          modifier = Modifier.width(320.dp).testTag(PortalTestTags.OPEN_SETTINGS),
      )
      Spacer(Modifier.height(16.dp))
      PortalSecondaryButton(
          text = stringResource(R.string.retry),
          onClick = onRetry,
          modifier = Modifier.width(320.dp),
      )
    } else {
      PortalPrimaryButton(
          text = stringResource(R.string.retry),
          onClick = onRetry,
          modifier = Modifier.width(280.dp),
      )
    }
  }
}
