package com.frqtools.dealtrackcrm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandPrimaryBlue,
    secondary = BrandPrimaryLight,
    onSecondary = Color.White,
    background = BrandLightScreenBg,
    onBackground = BrandLightOnSurfaceText,
    surface = BrandLightSurfaceBg,
    onSurface = BrandLightOnSurfaceText,
    surfaceVariant = BrandLightScreenBg,
    onSurfaceVariant = BrandLightOnSurfaceVariantText,
    outline = BrandOutlineColor
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandDarkPrimaryBlue,
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = BrandDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = BrandDarkPrimaryLight,
    onSecondary = Color(0xFF0D47A1),
    background = Color(0xFF0F172A),          // Elegant deep Slate-900 background
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),             // High-contrast Slate-800 for card surfaces
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),      // Slate-700
    onSurfaceVariant = Color(0xFF94A3B8),    // Muted slate text
    outline = Color(0xFF475569)              // Subtle borders
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent with brand by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
