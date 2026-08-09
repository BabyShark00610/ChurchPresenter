@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
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

    // ── Arrows, which cannot be typed ───────────────────────────────────────────

    /**
     * The arrows are the one binding you cannot search for by its own glyph.
     *
     * `←` is not on the keyboard, so before the aliases existed an arrow binding could only be
     * found by its description — which is exactly the gap that was reported.
     */
    @Test
    fun `an arrow binding is found by typing its name`() = dialog {
        search("left")

        assertTrue(rowExists(ShortcutAction.BIBLE_PREVIOUS_CHAPTER), "← should be findable as \"left\"")
        assertTrue(rowExists(ShortcutAction.PICTURES_PREVIOUS))
        assertTrue(!rowExists(ShortcutAction.BIBLE_NEXT_CHAPTER), "→ must not match \"left\"")
    }

    @Test
    fun `the word arrow finds every arrow binding`() = dialog {
        search("arrow")

        listOf(
            ShortcutAction.BIBLE_PREVIOUS_CHAPTER, ShortcutAction.BIBLE_NEXT_CHAPTER,
            ShortcutAction.BIBLE_PREVIOUS_VERSE, ShortcutAction.BIBLE_NEXT_VERSE,
        ).forEach { assertTrue(rowExists(it), "$it is bound to an arrow and should match") }
        assertTrue(!rowExists(ShortcutAction.MEDIA_MUTE))
    }

    @Test
    fun `the glyph still works for anyone who pastes it`() = dialog {
        search("←")

        assertTrue(rowExists(ShortcutAction.BIBLE_PREVIOUS_CHAPTER))
    }

    // ── Press-key mode ──────────────────────────────────────────────────────────

    private fun ComposeUiTest.enterPressMode() {
        onNodeWithTag(SHORTCUT_PRESS_MODE_TAG).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.pressToSearch(key: Key, ctrl: Boolean = false) {
        onNodeWithTag(SHORTCUT_PRESS_PANEL_TAG).performKeyInput {
            if (ctrl) withKeyDown(Key.CtrlLeft) { pressKey(key) } else pressKey(key)
        }
        waitForIdle()
    }

    @Test
    fun `pressing a key shows what is bound to it`() = dialog {
        enterPressMode()
        pressToSearch(Key.DirectionLeft)

        assertTrue(rowExists(ShortcutAction.BIBLE_PREVIOUS_CHAPTER))
        assertTrue(rowExists(ShortcutAction.PICTURES_PREVIOUS))
        assertTrue(!rowExists(ShortcutAction.BIBLE_NEXT_CHAPTER))
    }

    @Test
    fun `the match is on the exact combination, not just the key`() = dialog {
        enterPressMode()
        pressToSearch(Key.DirectionLeft, ctrl = true)

        // Nothing ships on Ctrl+←, so a filter that ignored modifiers would wrongly list every
        // plain-← binding here.
        assertTrue(!rowExists(ShortcutAction.BIBLE_PREVIOUS_CHAPTER))
        onNodeWithTag(SHORTCUT_NO_RESULTS_TAG).assertExists()
    }

    @Test
    fun `an unused combination reports itself rather than an empty query`() = dialog {
        enterPressMode()
        pressToSearch(Key.DirectionLeft, ctrl = true)

        val message = onNodeWithTag(SHORTCUT_NO_RESULTS_TAG).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }

        // The bug this guards: the message is built from the text query, which is empty in this
        // mode, so it would have read: No results found for "".
        assertTrue(message.trim().endsWith("\"\"").not(), "the message must name the chord: '$message'")
        assertTrue("Z" !in message)
    }

    @Test
    fun `Escape is a searchable key here rather than a way out`() = dialog {
        enterPressMode()
        pressToSearch(Key.Escape)

        assertTrue(rowExists(ShortcutAction.CLEAR_OUTPUT), "Escape is a real binding and must be findable")
    }

    @Test
    fun `leaving press mode drops its filter`() = dialog {
        enterPressMode()
        pressToSearch(Key.DirectionLeft)
        assertTrue(!rowExists(ShortcutAction.MEDIA_MUTE))

        onNodeWithTag(SHORTCUT_PRESS_MODE_TAG).performClick()
        waitForIdle()

        ShortcutAction.entries.forEach { assertTrue(rowExists(it), "$it should be back") }
    }

    @Test
    fun `entering press mode drops the text query`() = dialog {
        search("verse")
        enterPressMode()

        // Both filters narrowing the same list at once has no sensible reading, so each clears the
        // other. With nothing pressed yet, everything is listed.
        ShortcutAction.entries.forEach { assertTrue(rowExists(it)) }
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
