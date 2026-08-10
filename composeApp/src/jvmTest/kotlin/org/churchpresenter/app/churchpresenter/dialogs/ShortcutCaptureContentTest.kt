@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.utils.ShortcutMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Recording a binding, driven through the real capture UI.
 *
 * The dialog's body was split out of its `DialogWindow` precisely so this could exist — a
 * `DialogWindow` needs a real window and cannot be composed by `runComposeUiTest`, which is why the
 * capture flow previously had no end-to-end coverage and why two bugs shipped in it.
 */
class ShortcutCaptureContentTest {

    private fun capture(
        action: ShortcutAction = ShortcutAction.MEDIA_MUTE,
        shortcuts: ShortcutMap = ShortcutMap.DEFAULT,
        block: ComposeUiTest.(confirmed: () -> List<KeyChord>, dismissed: () -> Int) -> Unit,
    ) {
        val confirmed = mutableListOf<KeyChord>()
        var dismissed = 0
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    ShortcutCaptureDialogContent(
                        action = action,
                        shortcuts = shortcuts,
                        onConfirm = { confirmed += it },
                        onDismiss = { dismissed++ },
                    )
                }
            }
            block({ confirmed }, { dismissed })
        }
    }

    /** The rendered text of a node, for asserting on wording rather than on a node's existence. */
    private fun ComposeUiTest.textOf(tag: String): String =
        onNodeWithTag(tag).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }

    @Test
    fun `a pressed key is shown in the preview`() = capture { _, _ ->
        onRoot().performKeyInput { pressKey(Key.J) }
        waitForIdle()

        assertEquals("J", textOf(ShortcutCaptureTags.PREVIEW))
    }

    @Test
    fun `modifiers held with the key are part of the recorded chord`() = capture { confirmed, _ ->
        onRoot().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.J) } }
        waitForIdle()
        onNodeWithTag(ShortcutCaptureTags.CONFIRM).performClick()

        assertEquals(listOf(KeyChord.of(Key.J, ctrl = true)), confirmed())
    }

    @Test
    fun `a bare modifier press leaves the preview waiting`() = capture { _, _ ->
        val before = textOf(ShortcutCaptureTags.PREVIEW)

        onRoot().performKeyInput { pressKey(Key.CtrlLeft) }
        waitForIdle()

        assertEquals(before, textOf(ShortcutCaptureTags.PREVIEW), "holding Ctrl must not become the binding")
    }

    @Test
    fun `Escape is recorded rather than closing the dialog`() = capture { _, dismissed ->
        // Escape is the shipped Clear Output binding; a dialog that cancelled on it could never
        // rebind it. It is captured here rather than confirmed, because against the default map it
        // is a genuine conflict — `a chord freed by an unsaved edit is accepted` confirms one.
        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()

        assertEquals("Esc", textOf(ShortcutCaptureTags.PREVIEW))
        assertEquals(0, dismissed(), "Escape must not dismiss the dialog")
    }

    // ── Conflicts ───────────────────────────────────────────────────────────────

    @Test
    fun `a clashing chord names the action it clashes with, with no raw placeholder left in`() =
        capture(action = ShortcutAction.MEDIA_MUTE) { _, _ ->
            // Escape is Clear Output, which is global and so competes with a Media binding.
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            val warning = textOf(ShortcutCaptureTags.CONFLICT)
            assertTrue("Clear / Stop Presenting" in warning, "the clashing action must be named: '$warning'")
            // The bug this pins: the string used a bare %s, so it rendered "Already used by: %s".
            assertFalse("%" in warning, "an unsubstituted placeholder leaked into the warning: '$warning'")
        }

    @Test
    fun `a clashing chord cannot be confirmed`() = capture(action = ShortcutAction.MEDIA_MUTE) { confirmed, _ ->
        onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForIdle()

        onNodeWithTag(ShortcutCaptureTags.CONFIRM).assertIsNotEnabled()
        assertEquals(emptyList(), confirmed())
    }

    @Test
    fun `a free chord shows no warning and can be confirmed`() = capture { confirmed, _ ->
        onRoot().performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.J) } }
        waitForIdle()

        onNodeWithTag(ShortcutCaptureTags.CONFLICT).assertDoesNotExist()
        onNodeWithTag(ShortcutCaptureTags.CONFIRM).assertIsEnabled()
        onNodeWithTag(ShortcutCaptureTags.CONFIRM).performClick()
        assertEquals(listOf(KeyChord.of(Key.J, alt = true)), confirmed())
    }

    @Test
    fun `nothing can be confirmed before a key is pressed`() = capture { _, _ ->
        onNodeWithTag(ShortcutCaptureTags.CONFIRM).assertIsNotEnabled()
    }

    /**
     * The regression test for validating against saved rather than pending state.
     *
     * With the binding cleared in the map handed to the dialog, its chord is free — previously the
     * dialog read `LocalShortcuts`, saw the still-saved binding, and refused the key.
     */
    @Test
    fun `a chord freed by an unsaved edit is accepted`() {
        val clearedClearOutput = ShortcutMap.from(
            KeyboardShortcutSettings(overrides = mapOf(ShortcutAction.CLEAR_OUTPUT.name to emptyList()))
        )
        capture(action = ShortcutAction.MEDIA_MUTE, shortcuts = clearedClearOutput) { confirmed, _ ->
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()

            onNodeWithTag(ShortcutCaptureTags.CONFLICT).assertDoesNotExist()
            onNodeWithTag(ShortcutCaptureTags.CONFIRM).performClick()
            assertEquals(listOf(KeyChord.of(Key.Escape)), confirmed())
        }
    }

    /** The symmetric case: a chord taken by an unsaved edit is reported, not silently allowed. */
    @Test
    fun `a chord taken by an unsaved edit is reported as a conflict`() {
        val undoMovedToJ = ShortcutMap.from(
            KeyboardShortcutSettings(overrides = mapOf(ShortcutAction.UNDO.name to listOf(KeyChord.of(Key.J))))
        )
        capture(action = ShortcutAction.MEDIA_MUTE, shortcuts = undoMovedToJ) { _, _ ->
            onRoot().performKeyInput { pressKey(Key.J) }
            waitForIdle()

            assertTrue("Undo" in textOf(ShortcutCaptureTags.CONFLICT))
            onNodeWithTag(ShortcutCaptureTags.CONFIRM).assertIsNotEnabled()
        }
    }
}
