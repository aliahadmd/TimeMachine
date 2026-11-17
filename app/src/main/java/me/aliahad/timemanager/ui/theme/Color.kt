package me.aliahad.timemanager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand & primary tokens
val BrandPrimaryLight = Color(0xFF0A66C2)
val BrandOnPrimaryLight = Color(0xFFFFFFFF)
val BrandPrimaryContainerLight = Color(0xFFD6E4FF)
val BrandOnPrimaryContainerLight = Color(0xFF001B3E)

val BrandSecondaryLight = Color(0xFF0D9488)
val BrandTertiaryLight = Color(0xFF6D28D9)

val BackgroundLight = Color(0xFFF7F8FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDF0F5)
val OutlineLight = Color(0xFFD5DCE6)

val BrandPrimaryDark = Color(0xFF7AB8FF)
val BrandOnPrimaryDark = Color(0xFF002F65)
val BrandPrimaryContainerDark = Color(0xFF00458F)
val BrandOnPrimaryContainerDark = Color(0xFFD6E4FF)

val BrandSecondaryDark = Color(0xFF4FD1C5)
val BrandTertiaryDark = Color(0xFFB794F4)

val BackgroundDark = Color(0xFF0F1115)
val SurfaceDark = Color(0xFF171A21)
val SurfaceVariantDark = Color(0xFF222732)
val OutlineDark = Color(0xFF333B4B)

@Immutable
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarning: Color,
    val danger: Color,
    val dangerContainer: Color,
    val focusAccent: Color,
    val plannerAccent: Color,
    val financeAccent: Color,
    val wellbeingAccent: Color,
    val cardBorder: Color
)

val LightStatusColors = StatusColors(
    success = Color(0xFF15803D),
    successContainer = Color(0xFFDCFCE7),
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    onWarning = Color(0xFFFFFFFF),
    danger = Color(0xFFDC2626),
    dangerContainer = Color(0xFFFEE2E2),
    focusAccent = Color(0xFF0A66C2),
    plannerAccent = Color(0xFFD97706),
    financeAccent = Color(0xFF059669),
    wellbeingAccent = Color(0xFF7C3AED),
    cardBorder = Color(0xFFE3E7EE)
)

val DarkStatusColors = StatusColors(
    success = Color(0xFF4ADE80),
    successContainer = Color(0xFF14532D),
    onSuccess = Color(0xFF052E16),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF78350F),
    onWarning = Color(0xFF451A03),
    danger = Color(0xFFF87171),
    dangerContainer = Color(0xFF7F1D1D),
    focusAccent = Color(0xFF7AB8FF),
    plannerAccent = Color(0xFFFBBF24),
    financeAccent = Color(0xFF34D399),
    wellbeingAccent = Color(0xFFC084FC),
    cardBorder = Color(0xFF2B313F)
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
