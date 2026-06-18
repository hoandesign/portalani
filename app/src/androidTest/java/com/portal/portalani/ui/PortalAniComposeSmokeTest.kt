package com.portal.portalani.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.portal.portalani.UiState
import com.portal.portalani.data.AppSettings
import com.portal.portalani.data.FormatFilter
import com.portal.portalani.data.ListStatus
import com.portal.portalani.data.SourceMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSheetSmokeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun errorScreen_openSettings_showsSettingsSheet() {
    composeRule.setContent {
      PortalAniTheme {
        PortalAniApp(
            state = UiState.Error(message = "Test error", canOpenSettings = true),
            settings = AppSettings(sourceMode = SourceMode.LIBRARY),
            viewerName = null,
            isSignedIn = false,
            userMessage = null,
            onSignIn = {},
            onSignOut = {},
            onRetry = {},
            onUseLibrary = {},
            onClearUserMessage = {},
            onSetUserScore = { _, _ -> },
            onToggleFavourite = {},
            onSetAnimeListStatus = { _, _ -> },
            onRemoveFromList = {},
            onSetShuffle = {},
            onSetFrameMode = {},
            onSetShowPosterClock = {},
            onSetIntervalSeconds = {},
            onSetSourceMode = {},
            onSetListStatuses = {},
            onSetFormatFilters = {},
            onSetCountryFilters = {},
            onSetSourceFilters = {},
            onSetDemographicFilters = {},
            onSetHideHentai = {},
            onSetLibrarySort = {},
            onSetSeasonKey = {},
            onSetPowerMode = {},
        )
      }
    }

    composeRule.onNodeWithTag(PortalTestTags.OPEN_SETTINGS).performClick()
    composeRule.onNodeWithTag(PortalTestTags.SETTINGS_SHEET).assertIsDisplayed()
  }
}

@RunWith(AndroidJUnit4::class)
class FilterDialogSmokeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun formatFiltersDialog_showsApplyAndClose() {
    composeRule.setContent {
      PortalAniTheme {
        FormatFiltersDialog(
            selected = FormatFilter.defaultSelection(),
            onDismiss = {},
            onApply = {},
        )
      }
    }

    composeRule.onNodeWithTag(PortalTestTags.FILTER_DIALOG).assertIsDisplayed()
    composeRule.onNodeWithTag(PortalTestTags.FILTER_DIALOG_APPLY).assertIsDisplayed()
    composeRule.onNodeWithTag(PortalTestTags.FILTER_DIALOG_CLOSE).performClick()
  }
}

@RunWith(AndroidJUnit4::class)
class ListStatusDialogSmokeTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun listStatusDialog_scrollsThroughStatuses() {
    composeRule.setContent {
      PortalAniTheme {
        ListStatusDialog(
            animeTitle = "Cowboy Bebop",
            currentStatus = ListStatus.CURRENT,
            onDismiss = {},
            onSelect = {},
            onRemove = {},
        )
      }
    }

    composeRule.onNodeWithTag(PortalTestTags.LIST_STATUS_DIALOG).assertIsDisplayed()
    val lastIndex = ListStatus.entries.lastIndex
    composeRule.onNodeWithTag(PortalTestTags.LIST_STATUS_DIALOG_LIST).performScrollToIndex(lastIndex)
  }
}
