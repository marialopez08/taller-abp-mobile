package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = MintPrimary,
    onPrimary = OnMintPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = OnMintPrimaryContainer,
    secondary = SkyBlueSecondary,
    onSecondary = OnSkyBlueSecondary,
    secondaryContainer = SkyBlueContainer,
    onSecondaryContainer = OnSkyBlueContainer,
    tertiary = WarmBeigeTertiary,
    onTertiary = OnWarmBeigeTertiary,
    tertiaryContainer = WarmBeigeContainer,
    onTertiaryContainer = OnWarmBeigeContainer,
    background = SpaBackground,
    onBackground = SpaOnBackground,
    surface = SpaSurface,
    onSurface = SpaOnSurface,
    surfaceVariant = SpaSurfaceVariant,
    onSurfaceVariant = SpaOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F6F0),
    surfaceContainer = Color(0xFFF3EDE2),
    surfaceContainerHigh = Color(0xFFEDE5D7),
    outline = SpaOutline,
    outlineVariant = SpaOutlineVariant,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkMintPrimary,
    onPrimary = DarkOnMintPrimary,
    primaryContainer = DarkMintPrimaryContainer,
    onPrimaryContainer = DarkOnMintPrimaryContainer,
    secondary = DarkSkyBlueSecondary,
    onSecondary = Color(0xFF0A3249),
    secondaryContainer = DarkSkyBlueContainer,
    onSecondaryContainer = SkyBlueContainer,
    tertiary = Color(0xFFD5BE9D),
    onTertiary = Color(0xFF3A2B17),
    tertiaryContainer = Color(0xFF534027),
    onTertiaryContainer = WarmBeigeContainer,
    background = DarkBackground,
    onBackground = Color(0xFFE5ECE8),
    surface = DarkSurface,
    onSurface = Color(0xFFE5ECE8),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C2BC),
    surfaceContainerLow = Color(0xFF1D2622),
    surfaceContainer = Color(0xFF222C28),
    surfaceContainerHigh = Color(0xFF2A3530),
    outline = Color(0xFF4F5D57),
    outlineVariant = Color(0xFF36423D),
  )

val PetSpaShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun PetSpaTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = PetSpaShapes,
    content = content,
  )
}
