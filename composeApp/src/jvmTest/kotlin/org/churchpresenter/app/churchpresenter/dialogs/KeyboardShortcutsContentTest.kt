@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardShortcutsContentTest {

    private fun dialog(block: ComposeUiTest.(dismissed: () -> Int) -> Unit) {
        var dismissed = 0
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    KeyboardShortcutsDialogContent(onDismiss = { dismissed++ })
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
            "Global", "Bible Tab", "Songs Tab", "Pictures Tab", "Presentation Tab", "Media Tab",
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
    fun `the new schedule shortcut is listed under Global`() = dialog {
        onNodeWithText("Ctrl+Shift+N").assertExists()
        onNodeWithText("New Schedule").assertExists()
    }

    @Test
    fun `the media mute shortcut is listed`() = dialog {
        onNodeWithText("M").assertExists()
        onNodeWithText("Mute / Unmute (M key)").assertExists()
    }
}
