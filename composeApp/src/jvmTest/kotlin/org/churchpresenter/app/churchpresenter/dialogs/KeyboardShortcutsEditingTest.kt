@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.utils.ShortcutMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Editing bindings in the Keyboard Shortcuts dialog.
 *
 * The reference listing itself is covered by [KeyboardShortcutsContentTest]; this suite is about
 * the controls that changed it — which used to live in a Settings tab and were merged in here so
 * shortcuts are seen and set in one place.
 *
 * Edits are pending until Apply or OK, so most of these assert on what the dialog *reports*, not on
 * what a settings file contains.
 */
class KeyboardShortcutsEditingTest {

    /** What the dialog handed back, so a test asserts the outcome rather than that a stub ran. */
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

    private fun withOverride(action: ShortcutAction, chords: List<KeyChord>) = AppSettings(
        keyboardShortcutSettings = KeyboardShortcutSettings(overrides = mapOf(action.name to chords))
    )

    private fun ComposeUiTest.chipFor(action: ShortcutAction) = onNodeWithTag(shortcutChipTag(action))
    private fun ComposeUiTest.revertFor(action: ShortcutAction) = onNodeWithTag(shortcutRevertTag(action))

    /** The overrides in the most recent save, which is the only committed state a test can read. */
    private fun Saves.lastOverrides() = saved.last().keyboardShortcutSettings.overrides

    // ── Rows ────────────────────────────────────────────────────────────────────

    @Test
    fun `every action has a row`() = dialog {
        ShortcutAction.entries.forEach { chipFor(it).assertExists() }
    }

    @Test
    fun `a row shows the shipped binding when nothing has been changed`() = dialog {
        chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")
    }

    @Test
    fun `a row shows the override once one is set`() =
        dialog(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) {
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("J")
        }

    @Test
    fun `an unbound action reads Not set rather than empty`() =
        dialog(withOverride(ShortcutAction.MEDIA_MUTE, emptyList())) {
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("Not set")
        }

    // ── Clear, Reset, Reset All ─────────────────────────────────────────────────

    @Test
    fun `Clear unbinds the action`() = dialog { result ->
        revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
        chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("Not set")

        onNodeWithText("Apply").performClick()
        assertEquals(emptyList(), result.lastOverrides()[ShortcutAction.MEDIA_MUTE.name])
    }

    @Test
    fun `Reset removes the override entirely rather than writing the default back`() =
        dialog(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) { result ->
            revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")

            onNodeWithText("Apply").performClick()
            // Absent, not "present and equal to the default" — that distinction is what lets a
            // later release change a default for users who never touched the action.
            assertFalse(result.lastOverrides().containsKey(ShortcutAction.MEDIA_MUTE.name))
        }

    @Test
    fun `Reset All drops every override at once`() {
        val settings = AppSettings(
            keyboardShortcutSettings = KeyboardShortcutSettings(
                overrides = mapOf(
                    ShortcutAction.MEDIA_MUTE.name to listOf(KeyChord.of(Key.J)),
                    ShortcutAction.UNDO.name to emptyList(),
                )
            )
        )
        dialog(settings) { result ->
            onNodeWithTag(SHORTCUT_RESET_ALL_TAG).performClick()
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")

            onNodeWithText("Apply").performClick()
            assertEquals(emptyMap(), result.lastOverrides())
        }
    }

    @Test
    fun `editing one action leaves the others untouched`() =
        dialog(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) { result ->
            revertFor(ShortcutAction.MEDIA_PLAY_PAUSE).performScrollTo().performClick()

            onNodeWithText("Apply").performClick()
            assertEquals(
                mapOf(
                    ShortcutAction.MEDIA_MUTE.name to listOf(KeyChord.of(Key.J)),
                    ShortcutAction.MEDIA_PLAY_PAUSE.name to emptyList(),
                ),
                result.lastOverrides(),
            )
        }

    @Test
    fun `the dialog writes only to keyboardShortcutSettings`() {
        // Compared against the *seeded* instance, not a fresh AppSettings(): several of its
        // defaults are freshly generated UUIDs, so two constructions never compare equal.
        val initial = AppSettings()
        dialog(initial) { result ->
            revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
            onNodeWithText("Apply").performClick()

            val after = result.saved.last()
            assertEquals(initial.copy(keyboardShortcutSettings = after.keyboardShortcutSettings), after)
        }
    }

    // ── Cancel / Apply / OK ─────────────────────────────────────────────────────

    @Test
    fun `Cancel dismisses without saving`() = dialog { result ->
        revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
        onNodeWithText("Cancel", substring = true).performClick()

        assertEquals(1, result.dismissed)
        assertTrue(result.saved.isEmpty(), "a cancelled edit must not reach the settings")
    }

    @Test
    fun `Apply saves without dismissing`() = dialog { result ->
        revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
        onNodeWithText("Apply").performClick()

        assertEquals(0, result.dismissed)
        assertEquals(1, result.saved.size)
    }

    @Test
    fun `OK saves and dismisses`() = dialog { result ->
        revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()
        onNodeWithText("OK", substring = true).performClick()

        assertEquals(1, result.dismissed)
        assertEquals(emptyList(), result.lastOverrides()[ShortcutAction.MEDIA_MUTE.name])
    }

    @Test
    fun `an edit is visible in the row before it is applied`() = dialog { result ->
        revertFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()

        chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("Not set")
        assertTrue(result.saved.isEmpty(), "the row updates from pending state, not from a save")
    }

    // ── Pending edits and conflicts ─────────────────────────────────────────────

    /**
     * The regression test for validating against saved rather than pending state.
     *
     * The capture dialog is handed this dialog's own map, so a binding cleared a moment ago frees
     * its key immediately. Asserted on the map built from what the dialog reports, since the
     * capture window itself cannot be composed by the test runner.
     */
    @Test
    fun `clearing a binding frees its chord before Apply`() = dialog { result ->
        revertFor(ShortcutAction.CLEAR_OUTPUT).performScrollTo().performClick()
        onNodeWithText("Apply").performClick()

        val pending = ShortcutMap.from(result.saved.last().keyboardShortcutSettings)
        assertNull(
            pending.conflictFor(KeyChord.of(Key.Escape), ShortcutAction.MEDIA_MUTE),
            "Escape must be free once Clear Output has been cleared",
        )
    }

    @Test
    fun `a chord assigned to another action is seen as taken`() {
        val pending = ShortcutMap.from(
            KeyboardShortcutSettings(overrides = mapOf(ShortcutAction.UNDO.name to listOf(KeyChord.of(Key.J))))
        )

        assertEquals(
            ShortcutAction.UNDO,
            pending.conflictFor(KeyChord.of(Key.J), ShortcutAction.MEDIA_MUTE),
        )
    }
}
