package com.portal.portalani.data

/** Safe [enumValueOf] for persisted strings; unknown or blank values become null. */
internal inline fun <reified E : Enum<E>> enumValueOrNull(raw: String?): E? {
  if (raw.isNullOrBlank() || raw == "null") return null
  return runCatching { enumValueOf<E>(raw) }.getOrNull()
}

/** Decode comma-separated enum names saved in SharedPreferences (multi-select filters). */
internal inline fun <reified E : Enum<E>> decodeCommaSeparatedEnumSelection(
    raw: String?,
    allValue: E,
    normalizeSelection: (Set<E>) -> Set<E>,
): Set<E> {
  if (raw.isNullOrBlank() || raw == allValue.name) return normalizeSelection(emptySet())
  val parsed =
      raw.split(',')
          .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.let { enumValueOrNull<E>(it) } }
          .filter { it != allValue }
          .toSet()
  return normalizeSelection(parsed)
}

internal fun <E : Enum<E>> encodeCommaSeparatedEnumSelection(
    selected: Set<E>,
    allValue: E,
    normalizeSelection: (Set<E>) -> Set<E>,
    isAllSelected: (Set<E>) -> Boolean,
): String {
  val normalized = normalizeSelection(selected)
  return if (isAllSelected(normalized)) allValue.name
  else normalized.sortedBy { it.ordinal }.joinToString(",") { it.name }
}
