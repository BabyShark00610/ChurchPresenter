package org.churchpresenter.app.churchpresenter.utils

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import org.churchpresenter.app.churchpresenter.models.LyricSection

/**
 * The smallest font size any auto-fit in the app will settle on, in settings units at the 1920×1080
 * reference resolution.
 *
 * Text below this is not readable from a pew, so a fit that cannot be reached without going under it
 * stops here and lets the content overflow (clipped) instead — an obviously-too-long verse reads
 * better cut off than rendered at a size nobody can make out.
 */
const val MIN_AUTO_FIT_FONT_SIZE = 8

/**
 * Binary-searches for the largest font size (in settings units, before scaleFactor)
 * whose rendered text fits within [availableWidth] × [availableHeight] pixels
 * at the 1920×1080 reference resolution (scaleFactor = 1).
 *
 * Measures each line separately and sums their heights to match the
 * presenter layout, which renders each line as a separate Text composable.
 *
 * Uses Density(1f) so that sp values map 1:1 to pixels, matching the
 * reference coordinate system used by the presenter.
 */
fun calculateAutoFitFontSize(
    textMeasurer: TextMeasurer,
    text: String,
    baseStyle: TextStyle,
    availableWidth: Int,
    availableHeight: Int,
): Int {
    if (text.isBlank() || availableWidth <= 0 || availableHeight <= 0) return MIN_AUTO_FIT_FONT_SIZE
    val referenceDensity = Density(1f)
    val lines = text.split("\n")
    val widthConstraints = Constraints(maxWidth = availableWidth)
    var low = MIN_AUTO_FIT_FONT_SIZE
    var high = 300
    while (high - low > 1) {
        val mid = (low + high) / 2
        val style = baseStyle.copy(fontSize = mid.sp)
        val totalHeight = lines.sumOf { line ->
            textMeasurer.measure(
                text = line,
                style = style,
                constraints = widthConstraints,
                density = referenceDensity
            ).size.height
        }
        if (totalHeight <= availableHeight) low = mid else high = mid
    }
    return (low - 1).coerceAtLeast(MIN_AUTO_FIT_FONT_SIZE)
}

/**
 * Finds the largest font size that fits ALL sections of a song without line wrapping.
 * Checks every line in every section against both width (no wrap) and height (fits vertically).
 */
fun calculateAutoFitForAllSections(
    textMeasurer: TextMeasurer,
    sections: List<LyricSection>,
    baseStyle: TextStyle,
    availableWidth: Int,
    availableHeight: Int,
    reservedHeight: Int = 0,
    includeEndIndicator: Boolean = false,
): Int {
    if (sections.isEmpty() || availableWidth <= 0 || availableHeight <= 0) return MIN_AUTO_FIT_FONT_SIZE
    val allLines = sections.flatMap { it.lines }
    if (allLines.all { it.isBlank() }) return MIN_AUTO_FIT_FONT_SIZE

    val effectiveHeight = (availableHeight - reservedHeight).coerceAtLeast(1)
    val referenceDensity = Density(1f)
    // Use unconstrained width to measure natural line width (no wrapping)
    val unconstrainedConstraints = Constraints()

    var low = MIN_AUTO_FIT_FONT_SIZE
    var high = 300
    while (high - low > 1) {
        val mid = (low + high) / 2
        val style = baseStyle.copy(fontSize = mid.sp)
        var fits = true

        for ((sectionIdx, section) in sections.withIndex()) {
            // Check both primary and secondary lines so bilingual text also fits
            val lineSets = if (section.secondaryLines.isNotEmpty())
                listOf(section.lines, section.secondaryLines) else listOf(section.lines)
            for (lines in lineSets) {
                if (lines.isEmpty()) continue
                var sectionHeight = 0
                for (line in lines) {
                    val result = textMeasurer.measure(
                        text = line,
                        style = style,
                        constraints = unconstrainedConstraints,
                        density = referenceDensity
                    )
                    // Check width: line must fit without wrapping
                    if (result.size.width > availableWidth) {
                        fits = false
                        break
                    }
                    sectionHeight += result.size.height
                }
                if (!fits) break
                // Reserve space for end-of-song indicator on the last section
                if (includeEndIndicator && sectionIdx == sections.lastIndex) {
                    val lineHeight = textMeasurer.measure(
                        text = "* * *",
                        style = style,
                        constraints = unconstrainedConstraints,
                        density = referenceDensity
                    ).size.height
                    // Spacer (4px reference) + indicator line height
                    sectionHeight += 4 + lineHeight
                }
                // Check height: all lines of this section must fit
                if (sectionHeight > effectiveHeight) {
                    fits = false
                    break
                }
            }
            if (!fits) break
        }
        if (fits) low = mid else high = mid
    }
    return (low - 1).coerceAtLeast(MIN_AUTO_FIT_FONT_SIZE)
}
