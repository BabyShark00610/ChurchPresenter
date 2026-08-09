@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Filtering the Keyboard Shortcuts dialog.
 *
 * Search matches the action's description **and** its keys. The key half is the part worth testing
 * hard: bindings render platform-specifically — `Ctrl+Shift+N` on Windows and Linux, `⌃⇧N` on
 * macOS — so a naive implementation that matched only what is on screen would work on one OS and
 * silently match nothing on the other. These run on whichever platform the suite is on and assert
 * that both spellings work either way.
 */
class KeyboardShortcutsSearchTest {

    private class Saves {
        val saved = mutableListOf<AppSettings>()
        var dismissed = 0
    }

    private fun dialog(
        initial: AppSettings = AppSettings(),
        block: ComposeUiTest.(result: Saves) -> Unit,
    ) {
        val result = Saves()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    KeyboardShortcutsDialogContent(
                        initialSettings = initial,
                        onSave = { result.saved += it },
                        onDismiss = { result.dismissed++ },
                    )
                }
            }
            block(result)
        }
    }

    private fun ComposeUiTest.search(text: String) {
        onNode(hasSetTextAction()).performTextInput(text)
        waitForIdle()
    }

    /** Whether this action's row is currently rendered. The collection form is the public one. */
    private fun ComposeUiTest.rowExists(action: ShortcutAction) =
        onAllNodesWithTag(shortcutChipTag(action))
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    // ── Matching on the description ─────────────────────────────────────────────

    @Test
    fun `a description query narrows the list`() = dialog {
        search("verse")

        assertTrue(rowExists(ShortcutAction.BIBLE_NEXT_VERSE))
        assertTrue(!rowExists(ShortcutAction.MEDIA_MUTE), "an unrelated row must be filtered out")
    }

    @Test
    fun `matching is case-insensitive`() = dialog {
        search("VERSE")

        assertTrue(rowExists(ShortcutAction.BIBLE_NEXT_VERSE))
    }

    @Test
    fun `a category with nothing matching disappears entirely`() = dialog {
        onNodeWithText("Media Tab").assertExists()

        search("verse")

        onNodeWithText("Media Tab").assertDoesNotExist()
        onNodeWithText("Bible Tab").assertExists()
    }

    // ── Matching on the keys ────────────────────────────────────────────────────

    @Test
    fun `a plain key name finds the action bound to it`() = dialog {
        search("F6")

        assertTrue(rowExists(ShortcutAction.SWITCH_TO_BIBLE), "F6 should find the tab it switches to")
        assertTrue(!rowExists(ShortcutAction.SWITCH_TO_SONGS))
    }

    @Test
    fun `a modifier word finds it whichever way the platform draws the binding`() = dialog {
        // On macOS these rows display "⌃Z", not "Ctrl+Z". Typing the word must still find them.
        search("ctrl")

        assertTrue(rowExists(ShortcutAction.UNDO))
        assertTrue(rowExists(ShortcutAction.SAVE_SCHEDULE))
        assertTrue(!rowExists(ShortcutAction.BIBLE_NEXT_VERSE), "an unmodified binding must not match")
    }

    @Test
    fun `the modifier symbol finds it too`() = dialog {
        search("⌃")

        assertTrue(rowExists(ShortcutAction.UNDO))
    }

    @Test
    fun `a modifier alias finds it`() = dialog {
        // "control" and "command" are what people type; neither is the rendered label on either OS.
        search("control")

        assertTrue(rowExists(ShortcutAction.UNDO))
    }

    @Test
    fun `searching by key finds mouse rows too`() = dialog {
        search("double-click")

        onNodeWithText("Go Live (Double-click item)").assertExists()
        assertTrue(!rowExists(ShortcutAction.UNDO))
    }

    // ── Empty and cleared ───────────────────────────────────────────────────────

    @Test
    fun `a query matching nothing says so and names the query`() = dialog {
        search("zzzznotathing")

        // Read off the tagged node rather than searching for the text: the query is also sitting in
        // the search box, so a plain text match finds two nodes.
        val message = onNodeWithTag(SHORTCUT_NO_RESULTS_TAG).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }

        assertTrue("zzzznotathing" in message, "the message should quote the query: '$message'")
        ShortcutAction.entries.forEach { assertTrue(!rowExists(it)) }
    }

    @Test
    fun `clearing the search brings every row back`() = dialog {
        search("verse")
        assertTrue(!rowExists(ShortcutAction.MEDIA_MUTE))

        onNodeWithContentDescription("Clear search").performClick()
        waitForIdle()

        ShortcutAction.entries.forEach {
            assertTrue(rowExists(it), "$it should be listed again once the search is cleared")
        }
        onNodeWithTag(SHORTCUT_NO_RESULTS_TAG).assertDoesNotExist()
    }

    @Test
    fun `every row is listed before anything is typed`() = dialog {
        ShortcutAction.entries.forEach { assertTrue(rowExists(it)) }
        onNodeWithTag(SHORTCUT_NO_RESULTS_TAG).assertDoesNotExist()
    }

    // ── Search is view state, not settings ──────────────────────────────────────

    @Test
    fun `searching does not change what Apply saves`() = dialog { result ->
        search("verse")
        onNodeWithText("Apply").performClick()

        // A filtered view must save exactly what an unfiltered one would — no overrides at all here,
        // since nothing was edited.
        assertEquals(1, result.saved.size)
        assertEquals(emptyMap(), result.saved.last().keyboardShortcutSettings.overrides)
    }

    @Test
    fun `an edit made while filtered is kept after the filter is cleared`() = dialog { result ->
        search("mute")
        onNodeWithTag(shortcutRevertTag(ShortcutAction.MEDIA_MUTE)).performClick()
        waitForIdle()

        onNodeWithContentDescription("Clear search").performClick()
        waitForIdle()
        onNodeWithText("Apply").performClick()

        assertEquals(
            emptyList(),
            result.saved.last().keyboardShortcutSettings.overrides[ShortcutAction.MEDIA_MUTE.name],
        )
    }
}
