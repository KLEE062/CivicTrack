package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class CivicStatusPalette(
  val verified: Color,
  val verifiedContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val error: Color,
  val errorContainer: Color,
)

val LocalCivicStatusPalette = staticCompositionLocalOf {
  CivicStatusPalette(
    verified = StatusVerifiedGreen,
    verifiedContainer = StatusVerifiedGreenBg,
    warning = StatusWarningAmber,
    warningContainer = StatusWarningAmberBg,
    error = StatusErrorRed,
    errorContainer = StatusErrorRedBg,
  )
}

private val LightColorScheme =
  lightColorScheme(
    primary = CivicTealPrimary,
    onPrimary = CivicOnPrimary,
    primaryContainer = CivicTealContainer,
    onPrimaryContainer = CivicOnTealContainer,
    secondary = CivicNavySecondary,
    onSecondary = CivicOnSecondary,
    secondaryContainer = CivicNavyContainer,
    onSecondaryContainer = CivicOnNavyContainer,
    tertiary = CivicCobaltTertiary,
    onTertiary = CivicOnTertiary,
    tertiaryContainer = CivicCobaltContainer,
    onTertiaryContainer = CivicOnCobaltContainer,
    background = CivicBackgroundLight,
    onBackground = CivicOnSurfaceLight,
    surface = CivicSurfaceLight,
    onSurface = CivicOnSurfaceLight,
    surfaceVariant = CivicSurfaceVariantLight,
    onSurfaceVariant = CivicOnSurfaceVariantLight,
    outline = CivicOutlineLight,
    error = StatusErrorRed,
    errorContainer = StatusErrorRedBg,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = CivicTealPrimaryDark,
    onPrimary = CivicOnPrimaryDark,
    primaryContainer = CivicTealContainerDark,
    onPrimaryContainer = CivicOnTealContainerDark,
    secondary = CivicNavySecondaryDark,
    onSecondary = CivicOnSecondaryDark,
    secondaryContainer = CivicNavyContainerDark,
    onSecondaryContainer = CivicOnNavyContainerDark,
    tertiary = CivicCobaltTertiaryDark,
    onTertiary = CivicOnTertiaryDark,
    tertiaryContainer = CivicCobaltContainerDark,
    onTertiaryContainer = CivicOnCobaltContainerDark,
    background = CivicBackgroundDark,
    onBackground = CivicOnSurfaceDark,
    surface = CivicSurfaceDark,
    onSurface = CivicOnSurfaceDark,
    surfaceVariant = CivicSurfaceVariantDark,
    onSurfaceVariant = CivicOnSurfaceVariantDark,
    outline = CivicOutlineDark,
    error = StatusErrorRedDark,
    errorContainer = StatusErrorRedBgDark,
  )

private val CivicShapes =
  Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun CivicTrackTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val statusPalette =
    if (darkTheme) {
      CivicStatusPalette(
        verified = StatusVerifiedGreenDark,
        verifiedContainer = StatusVerifiedGreenBgDark,
        warning = StatusWarningAmberDark,
        warningContainer = StatusWarningAmberBgDark,
        error = StatusErrorRedDark,
        errorContainer = StatusErrorRedBgDark,
      )
    } else {
      CivicStatusPalette(
        verified = StatusVerifiedGreen,
        verifiedContainer = StatusVerifiedGreenBg,
        warning = StatusWarningAmber,
        warningContainer = StatusWarningAmberBg,
        error = StatusErrorRed,
        errorContainer = StatusErrorRedBg,
      )
    }

  CompositionLocalProvider(LocalCivicStatusPalette provides statusPalette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = CivicShapes,
      content = content,
    )
  }
}
