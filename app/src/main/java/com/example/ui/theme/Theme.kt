package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StreamVaultDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = EmeraldAudio,
    onSecondary = Color(0xFF002116),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldLight,
    tertiary = CyanThinking,
    onTertiary = Color(0xFF002630),
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanThinking,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondary,
    outline = CardStroke
)

private val StreamVaultLightColorScheme = lightColorScheme(
    primary = Color(0xFFD90429),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E8),
    onPrimaryContainer = Color(0xFF5C000E),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StreamVaultDarkColorScheme else StreamVaultLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
