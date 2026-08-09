@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The F1 reference, which is now generated from the binding registry rather than hand-written.
 *
 * Assertions here name **descriptions**, not key text, wherever they can: a key label renders as
 * `Ctrl+Z` on Windows and Linux but `⌃Z` on macOS, so pinning the rendered key would pass on one
 * platform and fail on the others. Where a binding genuinely has to be checked, the test compares
 * against the label the registry produces rather than a literal.
 */
class KeyboardShortcutsContentTest {

    private fun dialog(
        settings: AppSettings = AppSettings(),
        block: ComposeUiTest.(dismissed: () -> Int) -> Unit,
    ) {
        var dismissed = 0
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    KeyboardShortcutsDialogContent(
                        initialSettings = settings,
                        onSave = {},
                        onDismiss = { dismissed++ },
                    )
                }
            }
            block { dismissed }
        }
    }

    private fun settingsWith(action: ShortcutAction, chords: List<KeyChord>) = AppSettings(
        keyboardShortcutSettings = KeyboardShortcutSettings(overrides = mapOf(action.name to chords))
    )

    @Test
    fun `clicking OK dismisses the dialog`() = dialog { dismissed ->
        onNodeWithText("OK", substring = true).performClick()
        assertEquals(1, dismissed())
    }

    @Test
    fun `every category heading is shown`() {
        val categories = listOf(
            "Menus", "Global", "Bible Tab", "Songs Tab", "Pictures Tab", "Presentation Tab",
            "Media Tab", "Canvas Tab", "Mouse",
        )
        dialog {
            categories.forEach { onNodeWithText(it).assertExists() }
        }
    }

    @Test
    fun `the global shortcut for opening this dialog is listed`() = dialog {
        onNodeWithText("F1").assertExists()
        onNodeWithText("Open Keyboard Shortcuts").assertExists()
    }

    @Test
    fun `the new schedule shortcut is listed`() = dialog {
        onNodeWithText("New Schedule").assertExists()
    }

    @Test
    fun `the media mute shortcut is listed`() = dialog {
        onNodeWithText("M").assertExists()
        onNodeWithText("Mute / Unmute").assertExists()
    }

    @Test
    fun `bindings the dialog never used to mention are now listed`() = dialog {
        // Page Up/Down, B and '.' were all handled by the app but appeared nowhere in this dialog
        // while its rows were hand-written. Generating them from the registry closed that gap.
        onNodeWithText("Next (presentation clicker)").assertExists()
        onNodeWithText("Previous (presentation clicker)").assertExists()
        onNodeWithText("Blank Screen").assertExists()
        onNodeWithText("PgDn").assertExists()
    }

    @Test
    fun `a rebound action shows its new key, not the shipped one`() {
        dialog(settingsWith(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) {
            onNodeWithText("J").assertExists()
            onNodeWithText("Mute / Unmute").assertExists()
        }
    }

    @Test
    fun `an unbound action is shown as not set rather than blank`() {
        // Save As ships unbound, so the default map already shows one "Not set". Clearing Mute
        // must add a second rather than leave an empty key box.
        fun ComposeUiTest.unboundRows() =
            onAllNodesWithText("Not set").fetchSemanticsNodes(atLeastOneRootRequired = false).size

        dialog { assertEquals(1, unboundRows()) }

        dialog(settingsWith(ShortcutAction.MEDIA_MUTE, emptyList())) { assertEquals(2, unboundRows()) }
    }

    @Test
    fun `mouse gestures are still listed, since they are not rebindable`() = dialog {
        onNodeWithText("Double-click").assertExists()
        onNodeWithText("Right-click").assertExists()
        onNodeWithText("Shift+Drag").assertExists()
    }
}
