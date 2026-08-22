package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VibrantLavender,
    onPrimary = VibrantDeepPurple,
    primaryContainer = VibrantPurpleContainer,
    onPrimaryContainer = VibrantOnContainer,
    secondary = VibrantSecondary,
    onSecondary = VibrantDeepPurple,
    secondaryContainer = VibrantDarkSurfaceVariant,
    onSecondaryContainer = VibrantSecondary,
    tertiary = VibrantRosePink,
    onTertiary = VibrantDeepPurple,
    tertiaryContainer = VibrantRosePinkDark,
    onTertiaryContainer = VibrantRosePink,
    background = VibrantDarkBackground,
    onBackground = VibrantDarkTextPrimary,
    surface = VibrantDarkSurface,
    onSurface = VibrantDarkTextPrimary,
    surfaceVariant = VibrantDarkSurfaceVariant,
    onSurfaceVariant = VibrantDarkTextSecondary,
    outline = VibrantDarkTextMuted,
    outlineVariant = DarkCardBorder,
    error = VibrantCrimsonBright,
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = VibrantPurpleContainerLight,
    onPrimaryContainer = VibrantDeepPurple,
    secondary = VibrantSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = VibrantRosePink,
    onTertiaryContainer = Color(0xFF31111D),
    background = VibrantLightBackground,
    onBackground = VibrantLightTextPrimary,
    surface = VibrantLightSurface,
    onSurface = VibrantLightTextPrimary,
    surfaceVariant = VibrantLightSurfaceVariant,
    onSurfaceVariant = VibrantLightTextSecondary,
    outline = VibrantLightTextMuted,
    outlineVariant = Color(0x1F000000),
    error = VibrantCrimson,
    onError = Color.White
)

@Composable
fun VoiceChangerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
