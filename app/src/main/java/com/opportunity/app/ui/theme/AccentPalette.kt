package com.opportunity.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * User-selectable dynamic accent themes tailored for the Warm Smoked Atelier canvas.
 * Preserves enum names for persisted DataStore compatibility.
 */
enum class AccentPalette(
    val label: String,
    val swatch: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color
) {
    CORAL(
        label = "Coral",
        swatch = Color(0xFFFF6E52),
        primary = Color(0xFFFF8F77),
        onPrimary = Color(0xFF470D00),
        primaryContainer = Color(0xFFFF6E52),
        onPrimaryContainer = Color(0xFFFFEDE8),
        inversePrimary = Color(0xFFB83E26)
    ),
    AMBER(
        label = "Amber",
        swatch = Color(0xFFE5A93C),
        primary = Color(0xFFFFC665),
        onPrimary = Color(0xFF432C00),
        primaryContainer = Color(0xFFE5A93C),
        onPrimaryContainer = Color(0xFF5E4000),
        inversePrimary = Color(0xFF7E5700)
    ),
    SAGE(
        label = "Sage",
        swatch = Color(0xFF8DBEA6),
        primary = Color(0xFFA8DAC0),
        onPrimary = Color(0xFF023826),
        primaryContainer = Color(0xFF8DBEA6),
        onPrimaryContainer = Color(0xFF1E4E3B),
        inversePrimary = Color(0xFF2D6A50)
    ),
    DUSTY_BLUE(
        label = "Blue",
        swatch = Color(0xFF79A6D2),
        primary = Color(0xFF9BC4EB),
        onPrimary = Color(0xFF082742),
        primaryContainer = Color(0xFF79A6D2),
        onPrimaryContainer = Color(0xFF0A243C),
        inversePrimary = Color(0xFF2B5A84)
    ),
    ROSE(
        label = "Rose",
        swatch = Color(0xFFD47A96),
        primary = Color(0xFFF0A2B9),
        onPrimary = Color(0xFF420A1D),
        primaryContainer = Color(0xFFD47A96),
        onPrimaryContainer = Color(0xFF3E091A),
        inversePrimary = Color(0xFF853750)
    )
}
