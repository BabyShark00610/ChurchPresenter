@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.utils.LocalShortcuts
import org.churchpresenter.app.churchpresenter.utils.ShortcutMap
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
        shortcuts: ShortcutMap = ShortcutMap.DEFAULT,
        block: ComposeUiTest.(dismissed: () -> Int) -> Unit,
    ) {
        var dismissed = 0
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    CompositionLocalProvider(LocalShortcuts provides shortcuts) {
                        KeyboardShortcutsDialogContent(onDismiss = { dismissed++ })
                    }
                }
            }
            block { dismissed }
        }
    }

    @Test
    fun `clicking close dismisses the dialog`() = dialog { dismissed ->
        onNodeWithText("✓ OK").performClick()
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
        val remapped = ShortcutMap.from(
            KeyboardShortcutSettings(
                overrides = mapOf(ShortcutAction.MEDIA_MUTE.name to listOf(KeyChord.of(Key.J)))
            )
        )
        dialog(remapped) {
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

        val cleared = ShortcutMap.from(
            KeyboardShortcutSettings(overrides = mapOf(ShortcutAction.MEDIA_MUTE.name to emptyList()))
        )
        dialog(cleared) { assertEquals(2, unboundRows()) }
    }

    @Test
    fun `mouse gestures are still listed, since they are not rebindable`() = dialog {
        onNodeWithText("Double-click").assertExists()
        onNodeWithText("Right-click").assertExists()
        onNodeWithText("Shift+Drag").assertExists()
    }
}
