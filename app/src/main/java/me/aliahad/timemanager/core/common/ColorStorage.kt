package me.aliahad.timemanager.core.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Safely encodes Compose [Color] into a 32-bit unsigned ARGB value stored in [Long].
 * Avoids the Compose `Color.value.toLong()` pitfall where upper-32-bit packing causes
 * `Color(Long)` to double-shift to 0x00000000 (Transparent).
 */
fun Color.toStorageLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

/**
 * Normalizes a persisted [Long] color to canonical 32-bit ARGB (`0x00000000_AARRGGBB`).
 * Recovers corrupted 64-bit values previously saved via `Color.value.toLong()`.
 */
fun normalizeStoredColor(raw: Long, fallback: Long = 0xFF4DABF7L): Long {
    val lower = raw and 0xFFFFFFFFL
    if (lower != 0L) return lower
    val upper = (raw ushr 32) and 0xFFFFFFFFL
    return if (upper != 0L) upper else fallback
}

fun Long.toComposeColor(fallback: Long = 0xFF4DABF7L): Color {
    return Color(normalizeStoredColor(this, fallback))
}
