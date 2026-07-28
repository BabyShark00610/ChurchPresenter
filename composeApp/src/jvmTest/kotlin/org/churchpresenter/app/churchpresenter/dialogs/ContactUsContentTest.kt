@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContactUsContentTest {

    private val types = listOf(
        "Feature Request" to "featureRequest",
        "Feedback" to "feedback",
        "Testimonial" to "testimonial",
        "Bug Report" to "bugReport",
    )

    private class Result {
        var dismissed = 0
        var sentWith: Triple<String, String, String>? = null
    }

    private fun dialog(status: SendStatus = SendStatus.Idle, block: ComposeUiTest.(Result) -> Unit) {
        val result = Result()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var selectedType by remember { mutableStateOf(types.first()) }
                    var name by remember { mutableStateOf("") }
                    var email by remember { mutableStateOf("") }
                    var message by remember { mutableStateOf("") }
                    ContactUsDialogContent(
                        onDismiss = { result.dismissed++ },
                        types = types,
                        selectedType = selectedType,
                        onSelectedTypeChange = { selectedType = it },
                        name = name,
                        onNameChange = { name = it },
                        email = email,
                        onEmailChange = { email = it },
                        message = message,
                        onMessageChange = { message = it },
                        status = status,
                        onSend = { result.sentWith = Triple(selectedType.second, name, message) },
                        sentText = "All done!",
                    )
                }
            }
            block(result)
        }
    }

    private fun ComposeUiTest.nameField(): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[0]
    private fun ComposeUiTest.messageField(): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[2]

    @Test
    fun `Send is disabled with no name or message`() = dialog {
        onNodeWithText("Send").assertIsNotEnabled()
    }

    @Test
    fun `Send is disabled with a message but no name`() = dialog {
        messageField().performTextInput("Loving the app!")
        onNodeWithText("Send").assertIsNotEnabled()
    }

    @Test
    fun `Send becomes enabled once both name and message are filled in`() = dialog {
        nameField().performTextInput("A Church")
        messageField().performTextInput("Loving the app!")
        onNodeWithText("Send").assertIsEnabled()
    }

    @Test
    fun `Send is disabled while a send is already in flight`() = dialog(status = SendStatus.Sending) {
        nameField().performTextInput("A Church")
        messageField().performTextInput("Loving the app!")
        onNodeWithText("Send").assertIsNotEnabled()
    }

    @Test
    fun `clicking Send hands back the typed name, message and selected type`() = dialog { result ->
        nameField().performTextInput("A Church")
        messageField().performTextInput("Loving the app!")
        onNodeWithText("Send").performClick()

        assertEquals(Triple("featureRequest", "A Church", "Loving the app!"), result.sentWith)
    }

    @Test
    fun `picking a different type changes what Send hands back`() = dialog { result ->
        onNodeWithText("Bug Report").performClick()
        nameField().performTextInput("A Church")
        messageField().performTextInput("Something's broken")
        onNodeWithText("Send").performClick()

        assertEquals("bugReport", result.sentWith?.first)
    }

    @Test
    fun `Cancel dismisses without sending`() = dialog { result ->
        nameField().performTextInput("A Church")
        onNodeWithText("Cancel").performClick()

        assertEquals(1, result.dismissed)
        assertEquals(null, result.sentWith)
    }

    @Test
    fun `a Sending status shows a sending message`() = dialog(status = SendStatus.Sending) {
        onNodeWithText("Sending…").assertExists()
    }

    @Test
    fun `a Sent status shows the caller-supplied confirmation`() = dialog(status = SendStatus.Sent) {
        onNodeWithText("All done!").assertExists()
    }

    @Test
    fun `an Error status shows its own message`() = dialog(status = SendStatus.Error("Something went wrong")) {
        onNodeWithText("Something went wrong").assertExists()
    }

    @Test
    fun `an Idle status shows no status line`() = dialog {
        onNodeWithText("Sending…").assertDoesNotExist()
        onNodeWithText("All done!").assertDoesNotExist()
    }

    @Test
    fun `the browser fallback button is present and enabled`() = dialog {
        onNodeWithText("Open in Browser").assertIsEnabled()
    }
}
