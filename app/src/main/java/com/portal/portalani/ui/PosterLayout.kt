package com.portal.portalani.ui

import androidx.compose.ui.unit.Dp

/**
 * Canonical portrait poster proportions used across Poster, Informative, and Calendar modes.
 * Reference size: 460 × 610 (width × height).
 */
object PosterLayout {
  const val ReferenceWidth = 460f
  const val ReferenceHeight = 610f

  /** Compose [androidx.compose.foundation.layout.aspectRatio]: width / height. */
  const val AspectRatio = ReferenceWidth / ReferenceHeight

  fun heightForWidth(width: Dp): Dp = width / AspectRatio

  fun widthForHeight(height: Dp): Dp = height * AspectRatio

  /** Largest poster size that fits inside [maxWidth] × [maxHeight] while keeping [AspectRatio]. */
  fun fitIn(maxWidth: Dp, maxHeight: Dp): PosterSize {
    val heightIfWidthFits = heightForWidth(maxWidth)
    return if (heightIfWidthFits <= maxHeight) {
      PosterSize(width = maxWidth, height = heightIfWidthFits)
    } else {
      PosterSize(width = widthForHeight(maxHeight), height = maxHeight)
    }
  }
}

data class PosterSize(
    val width: Dp,
    val height: Dp,
)
