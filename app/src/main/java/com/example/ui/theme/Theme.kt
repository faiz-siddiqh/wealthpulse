package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldSecondary,
    tertiary = ForestTertiary,
    background = MidnightBack,
    surface = CharcoalSurface,
    onPrimary = TruePaperWhite,
    onSecondary = TruePaperWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    outline = CardGreenBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = GoldSecondary,
    tertiary = ForestTertiary,
    background = MidnightBack,
    surface = CharcoalSurface,
    onPrimary = TruePaperWhite,
    onSecondary = TruePaperWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    outline = CardGreenBorder
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Cosmic Slate",
    content: @Composable () -> Unit
) {
    AppThemeState.themeName = themeName
    
    val isDark = themeName.startsWith("Amber") || themeName.startsWith("Midnight") || themeName.startsWith("Charcoal")
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = EmeraldPrimary,
            secondary = GoldSecondary,
            tertiary = ForestTertiary,
            background = MidnightBack,
            surface = CharcoalSurface,
            onPrimary = TruePaperWhite,
            onSecondary = TruePaperWhite,
            onBackground = PureWhite,
            onSurface = PureWhite,
            outline = CardGreenBorder
        )
    } else {
        lightColorScheme(
            primary = EmeraldPrimary,
            secondary = GoldSecondary,
            tertiary = ForestTertiary,
            background = MidnightBack,
            surface = CharcoalSurface,
            onPrimary = TruePaperWhite,
            onSecondary = TruePaperWhite,
            onBackground = PureWhite,
            onSurface = PureWhite,
            outline = CardGreenBorder
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !isDark
                windowInsetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
