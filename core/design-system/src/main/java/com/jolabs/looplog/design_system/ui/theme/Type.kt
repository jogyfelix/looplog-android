package com.jolabs.looplog.design_system.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.jolabs.looplog.design_system.R

/**
 * Bundled variable fonts (weight axis) shipped in res/font. These replace the previous
 * Google downloadable fonts ("Work Sans" / "Outfit"), which were fetched asynchronously
 * at runtime: until the fetch resolved, text rendered in the system fallback font and then
 * re-rendered once the font arrived - a flash-of-unstyled-text (FOUT) that was most visible
 * on early cold-start UI like the reminder-permission dialog. Bundling them makes the correct
 * typeface available on the first frame, with no network fetch, and works offline.
 *
 * minSdk is 29, so a single variable TTF per family covers every weight natively (no faux-bold
 * synthesis). We register the weights Material 3's typography actually asks for (400/500), plus
 * 600/700 as headroom, each pinned to the corresponding point on the font's weight axis.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int): FontFamily {
    val weights = listOf(
        FontWeight.Normal,   // 400
        FontWeight.Medium,   // 500
        FontWeight.SemiBold, // 600
        FontWeight.Bold,     // 700
    )
    return FontFamily(
        weights.map { weight ->
            Font(
                resId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        }
    )
}

val bodyFontFamily = variableFontFamily(R.font.work_sans_variable)

val displayFontFamily = variableFontFamily(R.font.outfit_variable)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)
