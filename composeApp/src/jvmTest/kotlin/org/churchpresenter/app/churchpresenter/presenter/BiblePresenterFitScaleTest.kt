package org.churchpresenter.app.churchpresenter.presenter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The fit search behind every Bible layout: it must always come back with a scale that fits.
 *
 * Verse text shrinks to stay whole. It is never cut off to hold a size — not with eight translations
 * stacked, not with the longest verse in the Bible, not in a lower-third band. That is the whole
 * contract, and it is what the search did not previously offer: it started at a fixed 15% floor and
 * returned that floor **without ever testing it**, so "the largest scale that fits" and "nothing in
 * range fits" came back indistinguishable and the second silently overflowed (issue #97).
 *
 * The first fix attempt made it worse by raising the floor to a readable 8sp, which turned
 * unreadably-small into cut-off. Both are wrong; fitting wins.
 *
 * The `fits` predicates below stand in for the real measurement. That is the whole of the decision
 * being tested — the real ones differ only in calling `TextMeasurer`, which needs a composition and
 * would test Compose's text metrics rather than this search.
 */
class BiblePresenterFitScaleTest {

    /** A predicate shaped like the real ones: content of [contentAtFullScale] into [available]. */
    private fun fitsWithin(available: Float, contentAtFullScale: Float): (Float) -> Boolean =
        { scale -> contentAtFullScale * scale <= available }

    @Test
    fun `content far too big for its frame still gets a scale that fits`() {
        // 40x over: no scale at or above the old 15% floor fits, which is exactly the case that used
        // to be returned as the floor itself and drawn overflowing.
        val fits = fitsWithin(available = 100f, contentAtFullScale = 4000f)
        val scale = binarySearchFitScale(fits = fits)

        assertTrue(fits(scale), "the returned scale must fit; was $scale")
    }

    @Test
    fun `a scale that fits is returned at every realistic degree of overflow`() {
        // Eight translations of a long passage crammed into a lower-third band is the worst real case
        // and overflows by tens of times. The range here runs to 1000x, well past it.
        listOf(2f, 10f, 100f, 1_000f).forEach { overflowFactor ->
            val fits = fitsWithin(available = 100f, contentAtFullScale = 100f * overflowFactor)
            val scale = binarySearchFitScale(fits = fits)

            assertTrue(fits(scale), "overflowing ${overflowFactor}x returned $scale, which does not fit")
            assertTrue(scale > 0f, "the scale must stay positive; was $scale")
        }
    }

    @Test
    fun `the largest fitting scale is found, not merely a fitting one`() {
        // Half-size fits exactly. Shrinking further than needed would waste the frame.
        val scale = binarySearchFitScale(iterations = 14, fits = fitsWithin(100f, 200f))

        assertTrue(scale <= 0.5f, "must not exceed what fits; was $scale")
        assertTrue(scale > 0.49f, "must converge on the largest fitting scale; was $scale")
    }

    @Test
    fun `content that already fits is left at full size`() {
        assertEquals(1f, binarySearchFitScale(fits = { true }))
    }

    @Test
    fun `a starting scale that already fits is not lowered first`() {
        // The halving is only for when the start does not fit; from a start that does, the search
        // bisects upward as before.
        val scale = binarySearchFitScale(startScale = 0.15f, iterations = 14, fits = fitsWithin(100f, 125f))

        assertTrue(scale > 0.79f && scale <= 0.8f, "expected to climb to ~0.8; was $scale")
    }

    @Test
    fun `a predicate nothing can satisfy terminates instead of hanging`() {
        // The shape of a layout holding part of its height fixed — a reference line measured once at
        // full size — which no scale can rescue. It must bottom out rather than loop.
        val scale = binarySearchFitScale(fits = { false })

        assertTrue(scale > 0f, "must return a usable scale rather than zero; was $scale")
        assertTrue(scale < 0.01f, "must have gone all the way down; was $scale")
    }

    @Test
    fun `a start above one is not allowed to enlarge the text`() {
        assertTrue(binarySearchFitScale(startScale = 5f, fits = { true }) <= 1f)
    }
}
