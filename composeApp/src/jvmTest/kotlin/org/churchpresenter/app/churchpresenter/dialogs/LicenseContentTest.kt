@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LicenseContentTest {

    private class Result {
        var accepted = 0
        var declined = 0
    }

    private fun dialog(licenseText: String = "Terms and conditions.", block: ComposeUiTest.(Result) -> Unit) {
        val result = Result()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    LicenseDialogContent(
                        licenseText = licenseText,
                        onAccept = { result.accepted++ },
                        onDecline = { result.declined++ },
                    )
                }
            }
            block(result)
        }
    }

    @Test
    fun `the license text is shown`() = dialog(licenseText = "You must agree to these terms.") {
        onNodeWithText("You must agree to these terms.").assertExists()
    }

    @Test
    fun `clicking I Accept accepts and does not decline`() = dialog { result ->
        onNodeWithText("I Accept").performClick()
        assertEquals(1, result.accepted)
        assertEquals(0, result.declined)
    }

    @Test
    fun `clicking Decline and Exit declines and does not accept`() = dialog { result ->
        onNodeWithText("Decline & Exit").performClick()
        assertEquals(0, result.accepted)
        assertEquals(1, result.declined)
    }

    @Test
    fun `the title and prompt are shown`() = dialog {
        onNodeWithText("End User License Agreement (EULA)").assertExists()
        onNodeWithText("Please read and accept the End User License Agreement to use Church Presenter.").assertExists()
    }

    @Test
    fun `LicenseDialog renders nothing when not visible`() = runComposeUiTest {
        setContent {
            LicenseDialog(isVisible = false, onAccept = {}, onDecline = {})
        }
        onNodeWithText("I Accept").assertDoesNotExist()
    }

    @Test
    fun `loadEulaText reads the real bundled EULA`() = runBlocking {
        val text = loadEulaText()

        assertTrue(text.isNotBlank())
        assertTrue(text.contains("License", ignoreCase = true))
    }
}
