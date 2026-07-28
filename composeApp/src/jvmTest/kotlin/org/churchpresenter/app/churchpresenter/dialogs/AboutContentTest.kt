@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AboutContentTest {

    private fun dialog(block: ComposeUiTest.(dismissed: () -> Int) -> Unit) {
        var dismissed = 0
        runComposeUiTest {
            setContent {
                AboutDialogContent(onDismiss = { dismissed++ }, appSettings = AppSettings(), theme = ThemeMode.LIGHT)
            }
            block { dismissed }
        }
    }

    @Test
    fun `the app name and copyright are shown`() = dialog {
        onNodeWithText("Church Presenter").assertExists()
        onNodeWithText("© 2026 Church Presenter").assertExists()
    }

    @Test
    fun `clicking OK dismisses the dialog`() = dialog { dismissed ->
        onNodeWithText("OK").performClick()
        assertEquals(1, dismissed())
    }

    @Test
    fun `the external-action buttons are present and enabled`() = dialog {
        onNodeWithText("Report a Bug").assertIsEnabled()
        onNodeWithText("Feature Request").assertIsEnabled()
        onNodeWithText("Open Crash Logs").assertIsEnabled()
        onNodeWithText("Save Diagnostic Info…").assertIsEnabled()
    }
}
