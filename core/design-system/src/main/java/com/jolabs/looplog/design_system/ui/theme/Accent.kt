package com.jolabs.looplog.design_system.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.jolabs.looplog.model.AppAccent

/**
 * Preset accent palettes. Each overrides only the primary tonal roles on top of the resolved
 * base [ColorScheme] (see [applyAccent]); neutral surfaces and the accessibility contrast
 * variants are inherited unchanged. Values approximate Material 3 tonal palettes
 * (light: primary=T40, container=T90; dark: primary=T80, container=T30).
 */
private data class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color
)

private fun lightAccent(accent: AppAccent): AccentColors? = when (accent) {
    AppAccent.SYSTEM -> null
    AppAccent.BLUE -> AccentColors(
        Color(0xFF415F91), Color(0xFFFFFFFF), Color(0xFFD6E3FF), Color(0xFF001B3E), Color(0xFFAAC7FF)
    )
    AppAccent.VIOLET -> AccentColors(
        Color(0xFF65558F), Color(0xFFFFFFFF), Color(0xFFE9DDFF), Color(0xFF21005D), Color(0xFFCFBCFF)
    )
    AppAccent.TEAL -> AccentColors(
        Color(0xFF006A63), Color(0xFFFFFFFF), Color(0xFF9EF2E4), Color(0xFF00201C), Color(0xFF82D5C8)
    )
    AppAccent.AMBER -> AccentColors(
        Color(0xFF8B5000), Color(0xFFFFFFFF), Color(0xFFFFDCC1), Color(0xFF2D1600), Color(0xFFFFB77C)
    )
    AppAccent.ROSE -> AccentColors(
        Color(0xFF984061), Color(0xFFFFFFFF), Color(0xFFFFD9E2), Color(0xFF3E001D), Color(0xFFFFB1C8)
    )
}

private fun darkAccent(accent: AppAccent): AccentColors? = when (accent) {
    AppAccent.SYSTEM -> null
    AppAccent.BLUE -> AccentColors(
        Color(0xFFAAC7FF), Color(0xFF0A305F), Color(0xFF28497A), Color(0xFFD6E3FF), Color(0xFF415F91)
    )
    AppAccent.VIOLET -> AccentColors(
        Color(0xFFCFBCFF), Color(0xFF36275D), Color(0xFF4D3D75), Color(0xFFE9DDFF), Color(0xFF65558F)
    )
    AppAccent.TEAL -> AccentColors(
        Color(0xFF82D5C8), Color(0xFF003731), Color(0xFF00504A), Color(0xFF9EF2E4), Color(0xFF006A63)
    )
    AppAccent.AMBER -> AccentColors(
        Color(0xFFFFB77C), Color(0xFF4A2800), Color(0xFF6A3C00), Color(0xFFFFDCC1), Color(0xFF8B5000)
    )
    AppAccent.ROSE -> AccentColors(
        Color(0xFFFFB1C8), Color(0xFF5E1133), Color(0xFF7B2949), Color(0xFFFFD9E2), Color(0xFF984061)
    )
}

/**
 * Returns [base] with the accent's primary roles applied. [AppAccent.SYSTEM] (and any accent
 * without a preset) returns [base] unchanged.
 */
fun applyAccent(base: ColorScheme, accent: AppAccent, dark: Boolean): ColorScheme {
    val colors = (if (dark) darkAccent(accent) else lightAccent(accent)) ?: return base
    return base.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        inversePrimary = colors.inversePrimary
    )
}

/** A representative swatch color for the accent picker. Null for [AppAccent.SYSTEM]. */
fun accentSwatchColor(accent: AppAccent): Color? = lightAccent(accent)?.primary
