package com.localbites.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class ExtendedColors(
    val charcoalText: Color = CharcoalText,
    val secondaryText: Color = SecondaryText,
    val ratingGreen: Color = RatingGreen,
    val warmCream: Color = WarmCream,
    val offerBlue: Color = OfferBlue,
    val promoGold: Color = PromoGold,
    val gradientScrimStart: Color = GradientScrimStart,
    val gradientScrimMid: Color = GradientScrimMid,
    val gradientScrimEnd: Color = GradientScrimEnd,
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

private val LightColorScheme = lightColorScheme(
    primary = Orange60,
    onPrimary = Color.White,
    primaryContainer = Orange95,
    onPrimaryContainer = Orange10,
    secondary = Green60,
    onSecondary = Green99,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Brown60,
    onTertiary = Brown95,
    tertiaryContainer = Brown90,
    onTertiaryContainer = Brown10,
    background = WarmCream,
    onBackground = CharcoalText,
    surface = Color.White,
    onSurface = CharcoalText,
    surfaceVariant = Color(0xFFE8E8ED),
    onSurfaceVariant = SecondaryText,
    outline = Neutral40,
    outlineVariant = Neutral80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Orange70,
    onPrimary = Orange10,
    primaryContainer = Orange30,
    onPrimaryContainer = Orange90,
    secondary = Green70,
    onSecondary = Green10,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = Brown70,
    onTertiary = Brown10,
    tertiaryContainer = Brown30,
    onTertiaryContainer = Brown90,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Neutral60,
    outlineVariant = Neutral30,
)

@Composable
fun LocalBitesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled to preserve brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides ExtendedColors()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LocalBitesTypography,
            content = content
        )
    }
}
