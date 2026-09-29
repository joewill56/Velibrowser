package com.example.ui.theme

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

private val VeilDarkColorScheme = darkColorScheme(
  primary = CyanPrimary,
  onPrimary = Color.Black,
  primaryContainer = Color(0xFF0F3A4A),
  onPrimaryContainer = CyanPrimary,
  secondary = NeonPurple,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF2E1B4E),
  onSecondaryContainer = Color(0xFFE9D5FF),
  tertiary = ElectricBlue,
  onTertiary = Color.Black,
  background = DarkBackground,
  onBackground = Color(0xFFF1F5F9),
  surface = DarkSurface,
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = DarkBorder,
  error = DangerRed,
  onError = Color.White
)

private val VeilLightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = LightSecondary,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFEEF2FF),
  onSecondaryContainer = Color(0xFF4338CA),
  tertiary = CyanPrimaryVariant,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = Color(0xFF0F172A),
  surface = LightSurface,
  onSurface = Color(0xFF0F172A),
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Color(0xFF64748B),
  outline = LightBorder,
  error = DangerRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to privacy-dark styling
  dynamicColor: Boolean = false, // Keep tailored brand security palette by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> VeilDarkColorScheme
      else -> VeilLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
