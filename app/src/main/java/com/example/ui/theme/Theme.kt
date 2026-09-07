package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavyBlue,
    onPrimary = WhiteSurface,
    primaryContainer = PrimaryNavyBlue.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryNavyBlue,
    secondary = ActionEmeraldGreen,
    onSecondary = WhiteSurface,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = WhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = WhiteSurface,
    onSurfaceVariant = TextSecondaryLight,
    error = ActionCoralRed,
    errorContainer = ActionCoralRed.copy(alpha = 0.1f),
    onError = WhiteSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNavyBlue,
    onPrimary = WhiteSurface,
    primaryContainer = PrimaryNavyBlue.copy(alpha = 0.2f),
    onPrimaryContainer = WhiteSurface,
    secondary = ActionEmeraldGreen,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondaryDark,
    error = ActionCoralRed,
    errorContainer = ActionCoralRed.copy(alpha = 0.2f),
    onError = DarkSurface
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
  val animatedColor by infiniteTransition.animateColor(
    initialValue = Color(0xFFF8FAFC),
    targetValue = Color(0xFFE2E8F0),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 5000, easing = androidx.compose.animation.core.LinearEasing),
      repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
    ),
    label = "bgColor"
  )

  val customLightColorScheme = LightColorScheme.copy(
    background = animatedColor,
    surface = WhiteSurface
  )

  MaterialTheme(
    colorScheme = customLightColorScheme,
    typography = Typography,
    content = content
  )
}
