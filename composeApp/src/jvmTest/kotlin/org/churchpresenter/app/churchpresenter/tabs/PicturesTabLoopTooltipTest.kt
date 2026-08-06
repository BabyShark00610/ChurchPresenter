@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performMouseInput
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The slideshow loop toggle's tooltip, which is the only thing on screen that says whether looping
 * is on.
 *
 * The button is an icon with no label, and its two states differ only by container colour — so the
 * tooltip is not a convenience here, it is the readout. One stuck on `Loop On` would tell the
 * operator the opposite of the truth, and the failure is silent until a slideshow dead-ends at the
 * last picture mid-service.
 *
 * **`PicturesTabExtraTest` records this button as unreachable** — "whose icon carries neither text
 * nor a content description to address it by". That is true of the usual selectors and not of the
 * tree: it is the *only* clickable node with neither, which is a selector in itself. [loopButton]
 * asserts that uniqueness rather than assuming it, so if a second unlabelled button ever appears
 * this fails loudly instead of silently addressing the wrong one — the approach `CanvasTab`'s
 * rename tick already uses.
 */
class PicturesTabLoopTooltipTest {

    /** The one clickable node carrying neither text nor a content description. */
    private fun ComposeUiTest.loopButton(): SemanticsNodeInteraction {
        val all = onAllNodes(hasClickAction())
        val nodes = all.fetchSemanticsNodes(atLeastOneRootRequired = false)
        val bare = nodes.indices.filter { i ->
            nodes[i].config.getOrNull(SemanticsProperties.ContentDescription) == null &&
                nodes[i].config.getOrNull(SemanticsProperties.Text) == null
        }
        assertEquals(
            1, bare.size,
            "the loop toggle is addressed as the only unlabelled button; found ${bare.size}",
        )
        return all[bare.single()]
    }

    private fun ComposeUiTest.countOf(text: String) =
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes(false).size

    private fun ComposeUiTest.hoverLoop() {
        loopButton().performMouseInput { moveTo(center) }
        waitForIdle()
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
    }

    @Test
    fun `with looping on the tooltip says so`() = picturesTab { vm, _ ->
        // Looping is the default, so this is what an operator sees without touching anything.
        assertEquals(true, vm.isLooping)
        val before = countOf("Loop On")

        hoverLoop()

        assertEquals(before + 1, countOf("Loop On"))
    }

    @Test
    fun `with looping off it says the other thing`() {
        // The readout has to follow the state. The two buttons differ only by container colour, so
        // a tooltip stuck on one string is invisible until the slideshow dead-ends at the last
        // picture — and by then the operator is mid-service wondering why it stopped.
        picturesTab(
            settings = { it.copy(pictureSettings = it.pictureSettings.copy(isLooping = false)) },
        ) { vm, _ ->
            assertEquals(false, vm.isLooping)
            val beforeOff = countOf("Loop Off")
            val beforeOn = countOf("Loop On")

            hoverLoop()

            assertEquals(beforeOff + 1, countOf("Loop Off"))
            assertEquals(beforeOn, countOf("Loop On"), "and must not still claim looping is on")
        }
    }
}
