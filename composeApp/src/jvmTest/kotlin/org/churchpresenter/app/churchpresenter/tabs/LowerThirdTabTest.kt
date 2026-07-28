@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Lower Third tab: a preset picker over a folder of Lottie animations.
 *
 * An operator picks a preset before the service and fires it during, so what matters is that the
 * list reflects the folder, that nothing can be fired until something is chosen, and that what
 * reaches the output is the animation that was picked rather than a name that has to be resolved
 * again later.
 *
 * The ATEM upload panel is not driven here — it reaches a switcher over the network, and everything
 * decidable before that is covered by `CompanionServerLowerThirdTest`.
 *
 * See `LowerThirdTabTestSupport.kt` for the harness.
 */
class LowerThirdTabTest {

    // ── The preset list ─────────────────────────────────────────────────────────

    @Test
    fun `every Lottie in the folder is listed, and nothing else`() = lowerThirdTab { _ ->
        assertTrue(showsExactly("Welcome"), "got ${renderedText()}")
        assertTrue(showsExactly("Speaker Name"))
        assertFalse(showsExactly("notes"), "a non-Lottie file in the folder is not a preset")
    }

    @Test
    fun `with no folder configured the tab says there are no presets`() =
        lowerThirdTab(folder = null) { _ ->
            assertTrue(showsExactly(LowerThirdLabel.NO_PRESETS), "got ${renderedText()}")
        }

    @Test
    fun `an empty folder is the same as no presets`() = lowerThirdTab(folder = lottieFolder()) { _ ->
        // The folder exists but holds only the non-Lottie file.
        assertTrue(showsExactly(LowerThirdLabel.NO_PRESETS), "got ${renderedText()}")
    }

    // ── Choosing one ────────────────────────────────────────────────────────────

    @Test
    fun `nothing can be fired until a preset is chosen`() = lowerThirdTab { _ ->
        assertTrue(showsExactly(LowerThirdLabel.SELECT_PRESET), "the preview explains itself")
        ltButton(LowerThirdLabel.GO_LIVE).assertIsNotEnabled()
        ltButton(LowerThirdLabel.ADD_TO_SCHEDULE).assertIsNotEnabled()
    }

    @Test
    fun `choosing a preset opens it and enables the actions`() = lowerThirdTab { _ ->
        selectPreset("Welcome")

        assertFalse(showsExactly(LowerThirdLabel.SELECT_PRESET), "the placeholder is gone")
        assertTrue(showsExactly("Welcome"), "and the chosen preset is named")
        ltButton(LowerThirdLabel.GO_LIVE).assertIsEnabled()
        ltButton(LowerThirdLabel.ADD_TO_SCHEDULE).assertIsEnabled()
    }

    @Test
    fun `choosing a different preset replaces the first`() = lowerThirdTab { reports ->
        selectPreset("Welcome")
        selectPreset("Speaker Name")
        ltButton(LowerThirdLabel.GO_LIVE).performClick()
        waitForIdle()

        assertEquals(listOf("Speaker Name"), reports.live, "the last choice is what fires")
    }

    // ── Firing it ───────────────────────────────────────────────────────────────

    @Test
    fun `going live sends the animation itself, not just its name`() = lowerThirdTab { reports ->
        selectPreset("Welcome")
        ltButton(LowerThirdLabel.GO_LIVE).performClick()
        waitForIdle()

        assertEquals(listOf("Welcome"), reports.live)
        // The output is handed the Lottie JSON so it can play without going back to disk — a name
        // alone would leave the animation to be resolved again on the far side.
        assertEquals(LOWER_THIRD_LOTTIE, reports.liveJson)
    }

    @Test
    fun `adding to the schedule carries the preset and its pause settings`() =
        lowerThirdTab { reports ->
            selectPreset("Welcome")
            ltButton(LowerThirdLabel.ADD_TO_SCHEDULE).performClick()
            waitForIdle()

            val item = reports.scheduled.single()
            assertEquals("Welcome", item[1], "the label the schedule row shows")
            assertEquals(false, item[2], "pause-at-frame defaults off")
        }

    @Test
    fun `the same preset can be fired more than once`() = lowerThirdTab { reports ->
        // A lower third is often shown again later in the service.
        selectPreset("Welcome")
        ltButton(LowerThirdLabel.GO_LIVE).performClick()
        waitForIdle()
        ltButton(LowerThirdLabel.GO_LIVE).performClick()
        waitForIdle()

        assertEquals(listOf("Welcome", "Welcome"), reports.live)
    }

    // ── Managing the folder ─────────────────────────────────────────────────────

    @Test
    fun `each preset has its own remove button`() = lowerThirdTab { _ ->
        assertEquals(
            2,
            onAllNodesWithContentDescription(LowerThirdLabel.REMOVE)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size,
            "one per preset",
        )
    }

    @Test
    fun `the generator is offered even with no presets yet`() =
        lowerThirdTab(folder = lottieFolder()) { _ ->
            // Otherwise a new user with an empty folder has no way forward from this tab.
            assertTrue(showsExactly(LowerThirdLabel.GENERATE), "got ${renderedText()}")
        }
}
