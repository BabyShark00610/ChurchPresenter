@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
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
 * the reason this needs its own suite: it makes "none selected" unrepresentable as a selection, so
 * showing none of them is stored as `bibleMode = SONG_LANG_OFF` instead, which is the same statement
 * about the output. Before that, unticking the last box wrote an empty list that read straight back
 * as *all* and every box silently re-ticked.
 *
 * Because clearing switches scripture off, the menu is mounted whenever the stack has more than one
 * translation rather than only while scripture is on — otherwise Clear would shut the menu under the
 * hand that just pressed it. With it off, every box reads empty and the count reads zero.
 *
 * Translations are named by file stem — this tab deliberately does not open the bible folder — and
 * the code badge is the part of that stem after the underscore, since the downloader names what it
 * installs `LANGUAGE_CODE`. The fixture uses that shape so badge and name are distinct.
 *
 * Two locator traps worth knowing: the menu's header renders **uppercased**, so it is
 * `"BIBLE TRANSLATIONS"` on screen; and the trigger's cleared caption, "None", is also what an
 * unassigned target-display dropdown reads, so the cleared state is asserted through the menu's
 * summary line instead.
 */
class ProjectionSettingsTabTranslationPickerTest {

    private fun ComposeUiTest.openContentOutputs(row: Int = 0) {
        gridButton(Grid.contentOutputs(row)).performScrollTo().performClick()
        waitForIdle()
    }

    /** Opens the translation menu by its trigger. */
    private fun ComposeUiTest.openPicker(caption: String) {
        translationTrigger(caption).performScrollTo().performClick()
        waitForIdle()
    }

    /** Clicks a translation row inside the open menu, by the name it shows. */
    private fun ComposeUiTest.toggleTranslation(name: String) {
        translationRow(name).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.assertMenuOpen() =
        translationRow("ENG_KJV").assertExists("the menu must still be open")

    private fun row0(get: () -> AppSettings): ScreenAssignment =
        get().projectionSettings.screenAssignments[0]

    // ── When the picker exists at all ───────────────────────────────────────────────────────────

    @Test
    fun `the picker is hidden with a single translation`() = projectionTab(initial = oneTranslation()) { _ ->
        openContentOutputs()

        // Nothing to choose between: the Bible checkbox alone says whether the output shows it.
        onAllNodesWithText("BIBLE TRANSLATIONS").assertCountEquals(0)
        onAllNodesWithText("ENG_KJV").assertCountEquals(0)
    }

    @Test
    fun `the picker is hidden with no translations configured`() = projectionTab { _ ->
        openContentOutputs()

        onAllNodesWithText("BIBLE TRANSLATIONS").assertCountEquals(0)
    }

    // ── Reading the current selection ───────────────────────────────────────────────────────────

    @Test
    fun `the trigger names the first translation and how many follow it`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()

        assertEquals(emptyList(), row0(get).bibleTranslations, "an untouched output shows all of them")
        translationTrigger("KJV +2").assertExists()
        onNodeWithText("Bible · 3").assertExists("the preview counts the whole stack")
    }

    @Test
    fun `the menu says how many of the stack are enabled`() = projectionTab(
        initial = threeTranslations(),
    ) { _ ->
        openContentOutputs()
        openPicker("KJV +2")

        onNodeWithText("3 of 3 translations enabled").assertExists()
    }

    @Test
    fun `an index left behind by a removed translation is not counted`() = projectionTab(
        // Index 7 refers to a translation that is no longer in the stack.
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0, 7)))),
        ),
    ) { _ ->
        openContentOutputs()

        translationTrigger("KJV").assertExists("only the one surviving index counts, so there is no \"+N\"")
    }

    // ── Row content ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `each row carries the code from its file stem`() = projectionTab(initial = threeTranslations()) { _ ->
        openContentOutputs()
        openPicker("KJV +2")

        // The badge is the stem after the underscore; the name beside it is the whole stem.
        translationRow("SYN").assertExists()
        translationRow("RUS_SYN").assertExists()
        translationRow("LUT").assertExists()
    }

    @Test
    fun `a stem with no underscore is its own code`() = projectionTab(initial = unprefixedTranslations()) { _ ->
        openContentOutputs()
        openPicker("KJV +1")

        translationRow("NIV").assertExists()
    }

    @Test
    fun `only the first translation in the stack is tagged as primary`() = projectionTab(
        initial = threeTranslations(),
    ) { _ ->
        openContentOutputs()
        openPicker("KJV +2")

        onAllNodesWithText("PRIMARY").assertCountEquals(1)
        // ...and it is the first row's, not a tag floating elsewhere in the menu.
        onNode(isToggleable() and hasText("ENG_KJV") and hasText("PRIMARY")).assertExists()
    }

    // ── Changing the selection ──────────────────────────────────────────────────────────────────

    @Test
    fun `unticking a translation stores the rest and leaves the menu open`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +2")
        toggleTranslation("RUS_SYN")

        assertEquals(listOf(0, 2), row0(get).bibleTranslations)
        translationTrigger("KJV +1").assertExists()
        onNodeWithText("Bible · 2").assertExists()
        assertMenuOpen()
    }

    @Test
    fun `several translations can be unticked without reopening the menu`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +2")
        toggleTranslation("RUS_SYN")
        toggleTranslation("DEU_LUT")

        assertEquals(listOf(0), row0(get).bibleTranslations)
        translationTrigger("KJV").assertExists()
    }

    @Test
    fun `ticking the last missing translation stores all of them`() = projectionTab(
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0, 1)))),
        ),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +1")
        toggleTranslation("DEU_LUT")

        assertEquals(
            emptyList(), row0(get).bibleTranslations,
            "everything ticked is stored as \"all\", so a translation added later is included too",
        )
        translationTrigger("KJV +2").assertExists()
    }

    @Test
    fun `unticking the last translation switches the output off`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +2")
        toggleTranslation("ENG_KJV")
        toggleTranslation("RUS_SYN")
        toggleTranslation("DEU_LUT")

        // Showing none of them is the same statement as an unticked cell. Storing it as an empty
        // selection instead is what used to read back as "all" and re-tick every box.
        assertEquals(Constants.SONG_LANG_OFF, row0(get).bibleMode)
        onNodeWithText("0 of 3 translations enabled").assertExists()
        onAllNodesWithText("Bible · 3").assertCountEquals(0)
    }

    @Test
    fun `ticking a translation while the output is off switches it back on alone`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +2")
        // Reached through Clear rather than a fixture, both because it is the flow an operator
        // actually takes and because the cleared trigger reads "None" — which is also what an
        // unassigned target dropdown reads, so it cannot be used to find the trigger.
        onNodeWithText("Clear").performClick()
        waitForIdle()

        toggleTranslation("RUS_SYN")

        // Both halves in one event — the old pair of callbacks would have dropped one of them.
        assertEquals(Constants.SONG_LANG_BOTH, row0(get).bibleMode)
        assertEquals(listOf(1), row0(get).bibleTranslations, "only the one just ticked")
        translationTrigger("SYN").assertExists()
    }

    // ── The All and Clear chips ─────────────────────────────────────────────────────────────────

    @Test
    fun `the All chip restores every translation and leaves the menu open`() = projectionTab(
        initial = threeTranslations(
            ProjectionSettings(screenAssignments = listOf(ScreenAssignment(bibleTranslations = listOf(0)))),
        ),
    ) { get ->
        openContentOutputs()
        openPicker("KJV")
        onNodeWithText("All").performClick()
        waitForIdle()

        assertEquals(emptyList(), row0(get).bibleTranslations)
        translationTrigger("KJV +2").assertExists()
        assertMenuOpen()
    }

    @Test
    fun `the Clear chip empties every tick and switches the output off`() = projectionTab(
        initial = threeTranslations(),
    ) { get ->
        openContentOutputs()
        openPicker("KJV +2")
        onNodeWithText("Clear").performClick()
        waitForIdle()

        assertEquals(Constants.SONG_LANG_OFF, row0(get).bibleMode)
        assertEquals(emptyList(), row0(get).bibleTranslations)
        onNodeWithText("0 of 3 translations enabled").assertExists()
    }

    @Test
    fun `the Clear chip does not close the menu`() = projectionTab(initial = threeTranslations()) { _ ->
        openContentOutputs()
        openPicker("KJV +2")
        onNodeWithText("Clear").performClick()
        waitForIdle()

        // Clearing is usually a step towards picking a couple, so the menu has to survive it — which
        // is only possible because it is mounted independently of whether scripture is on.
        assertMenuOpen()
        onNodeWithText("Clear").assertExists("and its own chips are still there to use")
    }

    @Test
    fun `the menu chips do not collide with Quick Select`() = projectionTab(
        initial = threeTranslations(),
    ) { _ ->
        openContentOutputs()
        openPicker("KJV +2")

        // Deliberately shorter captions than the dialog's own Quick Select buttons, so every one of
        // the four stays addressable by the text it shows.
        onAllNodesWithText("All").assertCountEquals(1)
        onAllNodesWithText("Clear").assertCountEquals(1)
        onAllNodesWithText("Select All").assertCountEquals(1)
        onAllNodesWithText("Clear All").assertCountEquals(1)
    }
}
