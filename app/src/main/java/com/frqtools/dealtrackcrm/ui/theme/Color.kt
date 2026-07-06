package com.frqtools.dealtrackcrm.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

val PrimaryBlue = Color(0xFF1565C0)
val PrimaryLight = Color(0xFF1E88E5)
val PrimaryContainer = Color(0xFFDBEAFE)

val LightSurfaceBg = Color(0xFFFFFFFF)
val LightScreenBg = Color(0xFFF1F5F9) // Light blue-gray background

val LightOnSurfaceText = Color(0xFF1A1A2E)
val LightOnSurfaceVariantText = Color(0xFF64748B)
val OutlineColor = Color(0xFFCBD5E1)

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

// Deal status and priority colors
val WonGreen = Color(0xFF16A34A)
val WonGreenContainer = Color(0xFFDCFCE7)

val LostRed = Color(0xFFDC2626)
val LostRedContainer = Color(0xFFFEE2E2)

val WarningAmber = Color(0xFFD97706)
val WarningContainer = Color(0xFFFEF3C7)

val OnHoldGray = Color(0xFF6B7280)
val OnHoldContainer = Color(0xFFF3F4F6)

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
