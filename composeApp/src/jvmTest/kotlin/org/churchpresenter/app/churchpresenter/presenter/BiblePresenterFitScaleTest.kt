package org.churchpresenter.app.churchpresenter.presenter

import org.churchpresenter.app.churchpresenter.utils.MIN_AUTO_FIT_FONT_SIZE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How far the parallel translation stack is allowed to shrink to fit (issue #97).
 *
 * The stack shares one fit scale across every translation on it, so the more translations an output
 * shows the smaller that scale has to go. It used to be allowed down to 3% — which fits eight
 * translations on a screen at around 2sp, a size nobody in the room can read. The floor now says what
 * it actually means: no line under [MIN_AUTO_FIT_FONT_SIZE].
 *
 * Not covered here: the `clipToBounds` that keeps a stack too tall even at the floor inside the
 * configured margins. It is a draw-time modifier on the layout with no observable state, so there is
 * nothing to assert against short of rasterising an output window.
 */
class BiblePresenterFitScaleTest {

    /** What a line configured at [fontSize] actually renders at once both scalings are applied. */
    private fun renderedSize(fontSize: Int, scaleFactor: Float): Float =
        fontSize * scaleFactor * multiTranslationMinFitScale(fontSize, scaleFactor)

    @Test
    fun `the floor keeps the smallest line at the minimum readable size`() {
        listOf(70 to 1f, 70 to 3f, 70 to 0.5f, 24 to 1f, 300 to 2f).forEach { (fontSize, scaleFactor) ->
            assertEquals(
                MIN_AUTO_FIT_FONT_SIZE.toFloat(),
                renderedSize(fontSize, scaleFactor),
                absoluteTolerance = 0.01f,
                "$fontSize at scaleFactor $scaleFactor",
            )
        }
    }

    @Test
    fun `the smallest configured size in the stack is what sets the floor`() {
        // A stack of eight at the default 70 is the case from the issue: the old 3% floor let it down
        // to ~2sp. The floor is now over five times that, whatever the stack length.
        val floor = multiTranslationMinFitScale(smallestFontSize = 70, scaleFactor = 1f)
        assertTrue(floor > 0.1f, "expected a floor well above the old 0.03, was $floor")
        // A reference line configured smaller than the verse text hits the limit first, so it is the
        // one the floor has to be computed from.
        assertTrue(
            multiTranslationMinFitScale(24, 1f) > floor,
            "a smaller configured size must yield a higher floor",
        )
    }

    @Test
    fun `a stack already configured at or below the minimum is never scaled up`() {
        assertEquals(1f, multiTranslationMinFitScale(MIN_AUTO_FIT_FONT_SIZE, 1f))
        assertEquals(1f, multiTranslationMinFitScale(4, 1f))
        // Nor is a nonsense size allowed to divide by zero or invert the scale.
        assertEquals(1f, multiTranslationMinFitScale(0, 1f))
        assertEquals(1f, multiTranslationMinFitScale(-5, 1f))
    }

    @Test
    fun `the fit search never returns a scale under its floor`() {
        val floor = multiTranslationMinFitScale(smallestFontSize = 70, scaleFactor = 1f)
        // Nothing fits, however far it shrinks — the case that used to bottom out at 2sp.
        val scale = binarySearchFitScale(minScale = floor, iterations = 10) { false }
        assertEquals(floor, scale, "a stack that cannot fit must stop at the floor, not below it")
    }

    @Test
    fun `the fit search still finds the largest scale that fits`() {
        val floor = multiTranslationMinFitScale(smallestFontSize = 70, scaleFactor = 1f)
        val scale = binarySearchFitScale(minScale = floor, iterations = 12) { it <= 0.5f }
        assertTrue(scale <= 0.5f, "must not return a scale that does not fit, was $scale")
        assertTrue(scale > 0.49f, "must get close to the largest fitting scale, was $scale")
    }
}
