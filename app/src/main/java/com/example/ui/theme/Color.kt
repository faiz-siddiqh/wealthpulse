package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global theme state observer
object AppThemeState {
    var themeName by mutableStateOf("Cosmic Slate")
}

// Dynamic Natural Tones & Themed Design Palette
val EmeraldPrimary: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFFF5A5F)  // Airbnb Rose/Coral
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFFD4AF37)   // Sunset bronze / warm gold
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF00D2FF) // Neon Cyan / Cosmic Ocean
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF2BC272) // Forest Mint / Crisp Dark Mint
        else -> Color(0xFF386B1F)                                          // Forest Leaf Green (Cosmic Slate)
    }

val GoldSecondary: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF00A699)  // Airbnb Teal Accent
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFFF5A623)   // Vibrant Sunset Amber
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFFFFB703) // Electric Yellow-Gold
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF9E7E38) // Earthy Accent Gold
        else -> Color(0xFF9E7E38)                                          // Earthy Muted Gold (Cosmic Slate)
    }

val ForestTertiary: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF767676)  // Muted Mid Gray
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF8C7B6E)   // Warm taupe
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF415A77) // Ocean Steel Grey
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF55624C) // Sage Accent
        else -> Color(0xFF55624C)                                          // Soft Green-Gray Accent (Cosmic Slate)
    }

val MidnightBack: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFFDFDFD)  // Airbnb Soft Warm Off-White
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF1A1510)   // Dark chocolate/espresso
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF0D1B2A) // Intense Oceanic Dark Blue
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF121214) // Rich Carbon Slate Black
        else -> Color(0xFFFBFDF8)                                          // Soft natural canvas background (Cosmic Slate)
    }

val CharcoalSurface: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFFFFFFF)  // Pure elegant white
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF2A221A)   // Bronze surface
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF1B263B) // Medium Oceanic Navy
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF1E1E22) // Surface dark carbon gray
        else -> Color(0xFFFFFFFF)                                          // Clean white elevated surfaces (Cosmic Slate)
    }

val CardGreenBorder: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFEBEBEB)  // Soft light border line
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF3D3227)   // Deep bronze border
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF2E3E56) // Navy steel divider
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF2D2D34) // Carbon divider
        else -> Color(0xFFDDE5D8)                                          // Natural slate-green boundary line (Cosmic Slate)
    }

val GainGreen: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF00A699)  // Airbnb Teal for gains
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF81C784)
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF00E676) // Bright Electric Mint
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF2BC272) // Mint green
        else -> Color(0xFF386B1F)                                          // Positive profit indicator matches active primary green
    }

val LossRed: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFFF5A5F)  // Airbnb Rose/Coral for alerts
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFFE57373)
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFFFF5252) // Vibrant Crimson Red
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFFE57373)
        else -> Color(0xFFBC3D2E)                                          // Muted organic terracotta red for losses
    }

val PureWhite: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF222222)  // Deepest charcoal readable text
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFFF5EBE0)   // Soft warm cream text
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFFE0E1DD) // Ice Silver text
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFFF8F9FA) // Crisp active white text
        else -> Color(0xFF191D17)                                          // Main high-contrast green-black text
    }

val MutedText: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF717171)  // Airbnb grey text
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF9E8E80)   // Warm muted gray-brown
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF778DA9) // Midnight slate secondary text
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF8E8E9F) // Soft carbon silver secondary text
        else -> Color(0xFF55624C)                                          // Subtle secondary text (Cosmic Slate)
    }

val BorderSlate: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFEBEBEB)  // Regular divider gray
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF3D3227)
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF2C3E50)
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF2A2A30)
        else -> Color(0xFFE1E4DC)                                          // Clean natural slate-green borders (Cosmic Slate)
    }

// Highlight elements
val NaturalHeroLight: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFFFFEBEE)  // Warm rose blush block
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFF4E3629)   // Dark copper banner
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF112233) // Soft glowing dark navy glass
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF1B2D26) // Soft mint carbon highlights
        else -> Color(0xFFD7E8CD)                                          // Hero soft green background color (Cosmic Slate)
    }

val DarkPineText: Color
    get() = when {
        AppThemeState.themeName.startsWith("Airbnb") -> Color(0xFF7A0F1A)  // Elegant dark red raspberry accent
        AppThemeState.themeName.startsWith("Amber") -> Color(0xFFFFE0B2)
        AppThemeState.themeName.startsWith("Midnight") -> Color(0xFF80FAFF) // Soft cyan high impact
        AppThemeState.themeName.startsWith("Charcoal") -> Color(0xFF81FFB7) // Mint highlight text
        else -> Color(0xFF111F0E)                                          // Deep pine green for high-emphasis hero highlights
    }

val TruePaperWhite = Color(0xFFFFFFFF)      // True white for buttons/labels drawn on top of dark/colored surfaces
