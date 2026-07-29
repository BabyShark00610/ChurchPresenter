@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.ProjectionSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The per-output Bible translation picker in the Content Outputs dialog.
 *
 * Which translations an output shows is stored as [ScreenAssignment.bibleTranslations], a list of
 * positions in the configured stack, where **empty means all of them** — so that a translation added
 * later appears on every output rather than having to be ticked on each one. That normalisation is
 * the reason this needs its own suite: it makes "none selected" unrepresentable, so unticking the
 * last translation is routed to `bibleMode = SONG_LANG_OFF` instead, which is the same statement
 * about the output and one the settings can actually hold. Before that, unticking the last one wrote
 * an empty list and every box silently re-ticked.
 *
 * Every case asserts both halves: the stored assignment, and a signal on screen — the trigger's own
 * caption, or the dialog's "THIS OUTPUT SHOWS" preview, which reads `Bible · N` for the count.
 *
 * The rest of the dialog is covered by [ProjectionSettingsTabContentOutputsTest]; its fixture has no
 * translations configured, so the picker never composes there.
 */
class ProjectionSettingsTabTranslationPickerTest {

    private fun ComposeUiTest.openContentOutputs(row: Int = 0) {
        gridButton(Grid.contentOutputs(row)).performScrollTo().performClick()
        waitForIdle()
    }

    /** Opens the translation menu by its count trigger. */
    private fun ComposeUiTest.openPicker(caption: String) {
        onNodeWithText(caption).performScrollTo().performClick()
        waitForIdle()
    }

    /** Clicks a translation row inside the open menu. */
    private fun ComposeUiTest.toggleTranslation(name: String) {
        onNode(isToggleable() and hasTextExactly(name)).performClick()
        waitForIdle()
    }

    private fun row0(get: () -> AppSettings): ScreenAssignment =
        get().projectionSettings.screenAssignments[0]

    // ── When the picker exists at all ───────────────────────────────────────────────────────────

    @Test
    fun `the picker is hidden with a single translation`() = projectionTab(initial = oneTranslation()) { _ ->
        openContentOutputs()

        // Nothing to choose between: the Bible checkbox alone says whether the output shows it.
        onAllNodesWithText("All (1)").assertCountEquals(0)
    }

    @Test
    fun `the picker is hidden with no translations configured`() = projectionTab { _ ->
        openContentOutputs()

        onAllNodesWithText("All (0)").assertCountEquals(0)
    }

    // ── Reading the current selection ───────────────────────────────────────────────────────────

    @Test
    fun `the trigger reads All when every translation is shown`() = projectionTab(initial = threeTranslations()) { get ->
        openContentOutputs()

        assertEquals(emptyList(), row0(get).bibleTranslations, "an untouched output shows all of them")
        onNodeWithText("All (3)").assertExists()
        onNodeWithText("Bible · 3").assertExists("the preview counts the whole stack")
    }

    @Test
    fun `an index left behind by a removed translation is not counted`() = projectionTab(
        // Index 7 refers to a translation that is no longer in the stack.
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0, 7)))),
        ),
    ) { _ ->
        openContentOutputs()

        onNodeWithText("1 of 3").assertExists()
    }

    // ── Changing the selection ──────────────────────────────────────────────────────────────────

    @Test
    fun `unticking a translation stores the rest and leaves the menu open`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("All (3)")
        toggleTranslation("NIV")

        assertEquals(listOf(0, 2), row0(get).bibleTranslations)
        onNodeWithText("2 of 3").assertExists()
        onNodeWithText("Bible · 2").assertExists()
        // Still open, so a second pick costs no extra trip.
        onNode(isToggleable() and hasTextExactly("KJV")).assertExists()
    }

    @Test
    fun `several translations can be unticked without reopening the menu`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("All (3)")
        toggleTranslation("NIV")
        toggleTranslation("ESV")

        assertEquals(listOf(0), row0(get).bibleTranslations)
        onNodeWithText("1 of 3").assertExists()
    }

    @Test
    fun `ticking the last missing translation stores all of them`() = projectionTab(
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0, 1)))),
        ),
    ) { get ->
        openContentOutputs()
        openPicker("2 of 3")
        toggleTranslation("ESV")

        assertEquals(
            emptyList(), row0(get).bibleTranslations,
            "everything ticked is stored as \"all\", so a translation added later is included too",
        )
        onNodeWithText("All (3)").assertExists()
    }

    @Test
    fun `unticking the last translation switches the output off`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("All (3)")
        toggleTranslation("KJV")
        toggleTranslation("NIV")
        toggleTranslation("ESV")

        // Showing none of them is the same statement as an unticked cell. Storing it as an empty
        // list instead is what used to read back as "all" and re-tick every box.
        assertEquals(Constants.SONG_LANG_OFF, row0(get).bibleMode)
        onAllNodesWithText("All (3)").assertCountEquals(0)
        onAllNodesWithText("Bible · 3").assertCountEquals(0)
    }

    // ── Select All and Clear All ────────────────────────────────────────────────────────────────

    @Test
    fun `Select All restores every translation and leaves the menu open`() = projectionTab(
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0)))),
        ),
    ) { get ->
        openContentOutputs()
        openPicker("1 of 3")
        openMenuItem("Select All").performClick()
        waitForIdle()

        assertEquals(emptyList(), row0(get).bibleTranslations)
        onNodeWithText("All (3)").assertExists()
        onNode(isToggleable() and hasTextExactly("ESV")).assertExists("the menu stays open")
    }

    @Test
    fun `Clear All switches the output off`() = projectionTab(initial = threeTranslations()) { get ->
        openContentOutputs()
        openPicker("All (3)")
        openMenuItem("Clear All").performClick()
        waitForIdle()

        assertEquals(Constants.SONG_LANG_OFF, row0(get).bibleMode)
        onAllNodesWithText("All (3)").assertCountEquals(0)
    }

    @Test
    fun `the menu does not reopen by itself after Clear All`() = projectionTab(
        initial = threeTranslations(),
    ) { _ ->
        openContentOutputs()
        openPicker("All (3)")
        openMenuItem("Clear All").performClick()
        waitForIdle()

        // Re-tick Bible — the cell's own checkbox, which is the dialog's first toggleable node; its
        // label is a sibling, so the two cannot be matched as one. The open flag outlives the guard
        // that hides the trigger, so without being cleared it would pop the menu straight back open.
        onAllNodes(isToggleable())[0].performScrollTo().performClick()
        waitForIdle()

        onAllNodesWithText("KJV").assertCountEquals(0)
        onNodeWithText("All (3)").assertExists("the trigger is back, but closed")
    }

    @Test
    fun `Quick Select and the menu stay tellable apart while both are on screen`() = projectionTab(
        initial = threeTranslations(),
    ) { _ ->
        openContentOutputs()
        openPicker("All (3)")

        // "Select All" and "Clear All" are now on screen twice over, so a plain text lookup matches
        // two nodes and throws. Only the Quick Select pair are real buttons; menu items publish no
        // role, which is what keeps each addressable.
        onAllNodesWithText("Clear All").assertCountEquals(2)
        quickSelectButton("Clear All").assertExists()
        openMenuItem("Clear All").assertExists()
        quickSelectButton("Select All").assertExists()
        openMenuItem("Select All").assertExists()
    }
}
