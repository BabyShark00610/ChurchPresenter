package org.churchpresenter.app.churchpresenter.presenter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Shrinking a verse until it fits the screen.
 *
 * A long passage in a large font does not fit, and the presenter cannot ask "what scale would fit?"
 * — it can only lay text out at a given scale and ask whether the result overflowed. So it bisects,
 * keeping the largest scale that still fit.
 *
 * The property that matters, and is invisible from the call site: **the answer must always be one
 * that FITS**. A scale that overflows puts a verse on screen with its last line cut off, which is
 * exactly what auto-fit exists to prevent.
 *
 * That used to be only half true. The search took a floor, and when nothing at or above the floor
 * fitted it returned the floor anyway, unprobed — so the caller could not tell a fit from a failure
 * and drew the overflow. This suite asserted that behaviour as `text that fits nowhere still returns
 * the floor`, which contradicted its own opening paragraph. The floor is now a *starting point*: when
 * it does not fit, the search halves below it until something does.
 *
 * The cost of that is bounded but no longer identical for every passage — halving is what makes it
 * text-dependent — so `the number of layout probes stays bounded` replaces the old fixed-count
 * assertion. Each probe is a full layout of the whole passage and this runs on every verse change,
 * so the ceiling is the thing worth holding.
 *
 * The search is private to `BiblePresenter`, so it is reached by reflection on the file class.
 */
class BibleFitScaleTest {

    private val search =
        Class.forName("org.churchpresenter.app.churchpresenter.presenter.BiblePresenterKt")
            .getDeclaredMethod(
                "binarySearchFitScale",
                Float::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Function1::class.java,
            )
            .apply { isAccessible = true }

    /** Records every scale the search probed, in order. */
    private val probed = mutableListOf<Float>()

    /**
     * Runs the search against a text that fits at or below [fitsUpTo], the shape a real layout has:
     * every smaller scale fits and every larger one overflows.
     */
    private fun fitScale(fitsUpTo: Float, minScale: Float = 0.15f, iterations: Int = 8): Float {
        val fits = { scale: Float -> probed.add(scale); scale <= fitsUpTo }
        @Suppress("UNCHECKED_CAST")
        return search.invoke(null, minScale, iterations, fits as Function1<Float, Boolean>) as Float
    }

    // ── The answer is always usable ─────────────────────────────────────────────

    @Test
    fun `the scale it settles on fits`() {
        val fitsUpTo = 0.62f

        val scale = fitScale(fitsUpTo)

        assertTrue(
            scale <= fitsUpTo,
            "a scale that overflows cuts the last line of the verse off the bottom of the screen: $scale",
        )
    }

    @Test
    fun `text that fits at full size is not shrunk needlessly`() {
        val scale = fitScale(fitsUpTo = 1f)

        assertTrue(scale > 0.99f, "a short verse must fill the screen, not sit small in the middle of it: $scale")
    }

    @Test
    fun `text that fits nowhere is shrunk further rather than drawn overflowing`() {
        // fitsUpTo = 0.02 is far below the 0.15 the search starts from: nothing in its original range
        // fits, which used to be answered with the floor and an overflowing screen.
        val scale = fitScale(fitsUpTo = 0.02f)

        assertTrue(scale <= 0.02f, "the answer must fit, not sit at the floor overflowing: $scale")
        assertTrue(scale > 0f, "and it must still be a usable scale, not nothing")
    }

    @Test
    fun `the starting scale is a starting point, not a limit`() {
        listOf(0.15f, 0.3f, 0.5f).forEach { start ->
            val scale = fitScale(fitsUpTo = 0.05f, minScale = start)

            assertTrue(
                scale <= 0.05f,
                "starting at $start, the search must go below it to find a scale that fits: $scale",
            )
        }
    }

    @Test
    fun `the answer never exceeds full size`() {
        assertTrue(fitScale(fitsUpTo = 2f) <= 1f, "scaling past 1 would render the verse larger than its own style")
    }

    // ── It closes in on the right answer ────────────────────────────────────────

    @Test
    fun `the scale it settles on is close to the largest that fits`() {
        val fitsUpTo = 0.62f

        val scale = fitScale(fitsUpTo)

        assertTrue(
            fitsUpTo - scale < 0.01f,
            "eight halvings of the 0.15..1 range should land within a percent of the true limit, got $scale",
        )
    }

    @Test
    fun `more probes narrow the answer further`() {
        val coarse = fitScale(fitsUpTo = 0.62f, iterations = 3)
        probed.clear()
        val fine = fitScale(fitsUpTo = 0.62f, iterations = 12)

        assertTrue(fine >= coarse, "more work must not produce a worse fit")
        assertTrue(0.62f - fine <= 0.62f - coarse)
    }

    // ── It costs a fixed amount ─────────────────────────────────────────────────

    @Test
    fun `the number of layout probes stays bounded however badly the text overflows`() {
        fitScale(fitsUpTo = 0.62f)
        val forAMediumVerse = probed.size
        probed.clear()

        // The worst case this can be handed: nothing fits at any scale, so the halving runs its full
        // course before the bisection.
        fitScale(fitsUpTo = 0f)
        val forAHopelessOne = probed.size

        assertTrue(
            forAMediumVerse <= 12,
            "the ordinary case must stay close to the bisection budget: $forAMediumVerse probes",
        )
        assertTrue(
            forAHopelessOne <= 24,
            "each probe lays the whole passage out again, so even the hopeless case needs a ceiling: " +
                "$forAHopelessOne probes",
        )
    }

    @Test
    fun `no probe is wasted on a scale larger than full size`() {
        fitScale(fitsUpTo = 0.62f, minScale = 0.2f)

        assertTrue(
            probed.all { it <= 1f },
            "laying text out above full size wastes a probe on an answer that cannot be used: \$probed",
        )
    }

    @Test
    fun `with no bisection budget the starting scale is still made to fit`() {
        val scale = fitScale(fitsUpTo = 0.05f, iterations = 0)

        assertTrue(scale <= 0.05f, "iterations only fund the bisection; fitting is not optional: \$scale")
    }
}
