package com.frqtools.dealtrackcrm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// --- Static brand colors used to construct the Color Schemes in Theme.kt ---
val BrandPrimaryBlue = Color(0xFF1565C0)
val BrandPrimaryLight = Color(0xFF1E88E5)
val BrandPrimaryContainer = Color(0xFFDBEAFE)

val BrandDarkPrimaryBlue = Color(0xFF90CAF9)       // Vibrant light blue for dark mode primary
val BrandDarkPrimaryLight = Color(0xFF64B5F6)      // Lighter accent blue for dark mode secondary
val BrandDarkPrimaryContainer = Color(0xFF0D47A1)  // Rich deep blue container for dark mode

val BrandLightSurfaceBg = Color(0xFFFFFFFF)
val BrandLightScreenBg = Color(0xFFF1F5F9) // Light blue-gray background
val BrandLightOnSurfaceText = Color(0xFF1A1A2E)
val BrandLightOnSurfaceVariantText = Color(0xFF64748B)
val BrandOutlineColor = Color(0xFFCBD5E1)

// --- Light Mode Status Colors ---
val BrandWonGreen = Color(0xFF16A34A)
val BrandWonGreenContainer = Color(0xFFDCFCE7)

val BrandLostRed = Color(0xFFDC2626)
val BrandLostRedContainer = Color(0xFFFEE2E2)

val BrandWarningAmber = Color(0xFFD97706)
val BrandWarningContainer = Color(0xFFFEF3C7)

val BrandOnHoldGray = Color(0xFF6B7280)
val BrandOnHoldContainer = Color(0xFFF3F4F6)

// --- Dark Mode Status Colors ---
val BrandDarkWonGreen = Color(0xFF4ADE80)
val BrandDarkWonGreenContainer = Color(0xFF14532D)

val BrandDarkLostRed = Color(0xFFF87171)
val BrandDarkLostRedContainer = Color(0xFF7F1D1D)

val BrandDarkWarningAmber = Color(0xFFFBBF24)
val BrandDarkWarningContainer = Color(0xFF78350F)

val BrandDarkOnHoldGray = Color(0xFF9CA3AF)
val BrandDarkOnHoldContainer = Color(0xFF374151)

// --- Theme-Aware Composable Getters (Used throughout the App) ---
val PrimaryBlue: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val PrimaryLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val PrimaryContainer: Color
    @Composable
    get() = MaterialTheme.colorScheme.primaryContainer

val SurfaceBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val ScreenBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val OnSurfaceText: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val OnSurfaceVariantText: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val OutlineColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

val WonGreen: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkWonGreen else BrandWonGreen

val WonGreenContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkWonGreenContainer else BrandWonGreenContainer

val LostRed: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkLostRed else BrandLostRed

val LostRedContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkLostRedContainer else BrandLostRedContainer

val WarningAmber: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkWarningAmber else BrandWarningAmber

val WarningContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkWarningContainer else BrandWarningContainer

val OnHoldGray: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkOnHoldGray else BrandOnHoldGray

val OnHoldContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) BrandDarkOnHoldContainer else BrandOnHoldContainer

val ProposalPurple: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFC084FC) else Color(0xFF6A1B9A)

val ProposalPurpleContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF581C87) else Color(0xFFF3E8FF)

// Selected Avatar Colors
val AvatarColors = listOf(
    Color(0xFF1565C0), // Blue
    Color(0xFF6A1B9A), // Purple
    Color(0xFF00695C), // Teal
    Color(0xFFC62828), // Red
    Color(0xFF4527A0), // Deep Purple
    Color(0xFF1B5E20), // Green
    Color(0xFF0277BD), // Light Blue
    Color(0xFF37474F)  // Blue Gray
)
