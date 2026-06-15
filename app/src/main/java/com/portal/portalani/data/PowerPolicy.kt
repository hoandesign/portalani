package com.portal.portalani.data

import java.util.Calendar
import java.util.Locale

enum class PowerMode {
  /** Screen stays on while the slideshow is running. */
  ALWAYS_ON,
  /** Allow sleep after a period without touch input. */
  IDLE_SLEEP,
  /** Allow sleep during a daily quiet-hours window (e.g. 10 PM–7 AM). */
  SCHEDULED_SLEEP,
  ;

  val label: String
    get() =
        when (this) {
          ALWAYS_ON -> "Always on"
          IDLE_SLEEP -> "Sleep when idle"
          SCHEDULED_SLEEP -> "Off when sleeping"
        }
}

object PowerPolicy {
  val IDLE_SLEEP_OPTIONS_MINUTES = listOf(30, 60, 120, 180, 240, 360, 480)

  const val DEFAULT_IDLE_SLEEP_MINUTES = 60
  const val DEFAULT_SLEEP_START_MINUTES = 22 * 60
  const val DEFAULT_SLEEP_END_MINUTES = 7 * 60

  fun shouldKeepScreenOn(settings: AppSettings, lastUserInteractionMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean =
      when (settings.powerMode) {
        PowerMode.ALWAYS_ON -> true
        PowerMode.IDLE_SLEEP -> nowMs - lastUserInteractionMs < settings.idleSleepMinutes * 60_000L
        PowerMode.SCHEDULED_SLEEP -> !isWithinSleepWindow(settings.sleepStartMinutes, settings.sleepEndMinutes, nowMs)
      }

  /** Whether the dream/screensaver should launch the slideshow. */
  fun shouldRunSlideshow(settings: AppSettings, nowMs: Long = System.currentTimeMillis()): Boolean =
      when (settings.powerMode) {
        PowerMode.ALWAYS_ON, PowerMode.IDLE_SLEEP -> true
        PowerMode.SCHEDULED_SLEEP -> !isWithinSleepWindow(settings.sleepStartMinutes, settings.sleepEndMinutes, nowMs)
      }

  fun isWithinSleepWindow(startMinutes: Int, endMinutes: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
    val nowMinutes = minutesOfDay(nowMs)
    return isWithinSleepWindow(startMinutes, endMinutes, nowMinutes)
  }

  fun isWithinSleepWindow(startMinutes: Int, endMinutes: Int, nowMinutes: Int): Boolean {
    val start = startMinutes.coerceIn(0, 24 * 60 - 1)
    val end = endMinutes.coerceIn(0, 24 * 60 - 1)
    if (start == end) return false
    return if (start < end) {
      nowMinutes in start until end
    } else {
      nowMinutes >= start || nowMinutes < end
    }
  }

  fun minutesOfDay(nowMs: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMs
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }

  fun formatMinutesOfDay(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60)
    val hour24 = normalized / 60
    val minute = normalized % 60
    val am = hour24 < 12
    val hour12 = when (val h = hour24 % 12) {
      0 -> 12
      else -> h
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, if (am) "AM" else "PM")
  }

  fun powerSummary(settings: AppSettings): String =
      when (settings.powerMode) {
        PowerMode.ALWAYS_ON -> PowerMode.ALWAYS_ON.label
        PowerMode.IDLE_SLEEP -> "After ${formatIdleDuration(settings.idleSleepMinutes)} idle"
        PowerMode.SCHEDULED_SLEEP ->
            "${formatMinutesOfDay(settings.sleepStartMinutes)} – ${formatMinutesOfDay(settings.sleepEndMinutes)}"
      }

  fun formatIdleDuration(minutes: Int): String =
      when {
        minutes % 60 == 0 && minutes >= 60 -> "${minutes / 60}h"
        minutes >= 60 && minutes % 60 != 0 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
      }

  fun timePickerOptions(stepMinutes: Int = 30): List<Int> =
      generateSequence(0) { previous ->
        val next = previous + stepMinutes
        if (next >= 24 * 60) null else next
      }.toList()
}
