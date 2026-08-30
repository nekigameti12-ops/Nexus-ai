package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusDarkColorScheme = darkColorScheme(
  primary = NexusCyan,
  onPrimary = NexusBackground,
  primaryContainer = NexusSurfaceVariant,
  onPrimaryContainer = NexusCyan,
  secondary = NexusBlue,
  onSecondary = NexusTextPrimary,
  secondaryContainer = NexusSurfaceCard,
  onSecondaryContainer = NexusCyan,
  tertiary = NexusNeonViolet,
  onTertiary = NexusTextPrimary,
  background = NexusBackground,
  onBackground = NexusTextPrimary,
  surface = NexusSurface,
  onSurface = NexusTextPrimary,
  surfaceVariant = NexusSurfaceVariant,
  onSurfaceVariant = NexusTextSecondary,
  outline = NexusBorder,
  error = NexusRed,
  onError = NexusTextPrimary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to dark futuristic theme
  dynamicColor: Boolean = false, // Keep signature cyberpunk aesthetic
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = NexusDarkColorScheme,
    typography = Typography,
    content = content
  )
}

