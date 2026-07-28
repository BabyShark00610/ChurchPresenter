@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.BibleSyncMode
import org.churchpresenter.app.churchpresenter.data.settings.InstanceLinkRole
import org.churchpresenter.app.churchpresenter.data.settings.InstanceLinkSettings
import org.churchpresenter.app.churchpresenter.server.InstanceLinkStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstanceLinkContentTest {

    private data class Connected(
        val host: String,
        val port: Int,
        val apiKey: String,
        val autoConnect: Boolean,
        val allowPushToSchedule: Boolean,
        val bibleSyncMode: BibleSyncMode,
        val mirrorBackgrounds: Boolean,
        val role: InstanceLinkRole,
    )

    private class Result {
        var connected: Connected? = null
        var disconnectCalls = 0
        var dismissed = 0
    }

    private fun dialog(
        settings: InstanceLinkSettings = InstanceLinkSettings(),
        connectionStatus: InstanceLinkStatus = InstanceLinkStatus.DISCONNECTED,
        remoteScheduleCount: Int = 0,
        block: ComposeUiTest.(Result) -> Unit,
    ) {
        val result = Result()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    InstanceLinkDialogContent(
                        isVisible = true,
                        settings = settings,
                        connectionStatus = connectionStatus,
                        remoteLiveState = null,
                        remoteScheduleCount = remoteScheduleCount,
                        onConnect = { host, port, apiKey, autoConnect, allowPushToSchedule, bibleSyncMode, mirrorBackgrounds, role ->
                            result.connected = Connected(host, port, apiKey, autoConnect, allowPushToSchedule, bibleSyncMode, mirrorBackgrounds, role)
                        },
                        onDisconnect = { result.disconnectCalls++ },
                        onDismiss = { result.dismissed++ },
                    )
                }
            }
            block(result)
        }
    }

    private fun ComposeUiTest.hostField(): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[0]
    private fun ComposeUiTest.portField(): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[1]
    private fun ComposeUiTest.apiKeyField(): SemanticsNodeInteraction = onAllNodes(hasSetTextAction())[2]

    // ── Connect's enabled state ─────────────────────────────────────────────────

    @Test
    fun `Connect is disabled with no host`() = dialog {
        portField().performTextInput("8080")
        onNodeWithText("Connect").assertIsNotEnabled()
    }

    @Test
    fun `Connect is disabled with no port`() = dialog {
        hostField().performTextInput("192.168.1.10")
        onNodeWithText("Connect").assertIsNotEnabled()
    }

    @Test
    fun `Connect is disabled while the port is not a number`() = dialog {
        hostField().performTextInput("192.168.1.10")
        portField().performTextInput("abc")
        onNodeWithText("Connect").assertIsNotEnabled()
    }

    @Test
    fun `Connect becomes enabled with a host and a numeric port`() = dialog {
        hostField().performTextInput("192.168.1.10")
        portField().performTextInput("8080")
        onNodeWithText("Connect").assertIsEnabled()
    }

    @Test
    fun `an edit that introduces a non-digit character is rejected outright`() = dialog {
        portField().performTextInput("8080")
        hostField().performTextInput("192.168.1.10")
        onNodeWithText("Connect").assertIsEnabled()

        portField().performTextInput("x")

        // The whole edit is rejected rather than just the bad character, so the field keeps its
        // last valid value and Connect stays enabled on it.
        onNodeWithText("Connect").assertIsEnabled()
    }

    @Test
    fun `a port typed with a non-digit character never reaches a valid state`() = dialog {
        hostField().performTextInput("192.168.1.10")
        portField().performTextInput("8x")

        onNodeWithText("Connect").assertIsNotEnabled()
    }

    // ── Connecting ───────────────────────────────────────────────────────────────

    @Test
    fun `Connect hands back the trimmed host, api key and parsed port`() = dialog { result ->
        hostField().performTextInput("  192.168.1.10  ")
        portField().performTextInput("8080")
        apiKeyField().performTextInput("  secret  ")
        onNodeWithText("Connect").performClick()

        assertEquals("192.168.1.10", result.connected?.host)
        assertEquals(8080, result.connected?.port)
        assertEquals("secret", result.connected?.apiKey)
    }

    @Test
    fun `Connect closes the dialog after connecting`() = dialog { result ->
        hostField().performTextInput("192.168.1.10")
        portField().performTextInput("8080")
        onNodeWithText("Connect").performClick()

        assertEquals(1, result.dismissed)
    }

    @Test
    fun `Cancel dismisses without connecting`() = dialog { result ->
        hostField().performTextInput("192.168.1.10")
        onNodeWithText("Cancel").performClick()

        assertNull(result.connected)
        assertEquals(1, result.dismissed)
    }

    @Test
    fun `existing settings pre-fill the fields`() = dialog(
        settings = InstanceLinkSettings(primaryHost = "10.0.0.5", primaryPort = 9090, apiKey = "existing-key"),
    ) { result ->
        onNodeWithText("Connect").performClick()

        assertEquals("10.0.0.5", result.connected?.host)
        assertEquals(9090, result.connected?.port)
        assertEquals("existing-key", result.connected?.apiKey)
    }

    // ── Disconnect ───────────────────────────────────────────────────────────────

    @Test
    fun `Disconnect is not offered while disconnected`() = dialog(connectionStatus = InstanceLinkStatus.DISCONNECTED) {
        onNodeWithText("Disconnect").assertDoesNotExist()
    }

    @Test
    fun `Disconnect is offered while connected and calls onDisconnect`() = dialog(connectionStatus = InstanceLinkStatus.CONNECTED) { result ->
        onNodeWithText("Disconnect").performClick()
        assertEquals(1, result.disconnectCalls)
    }

    @Test
    fun `the primary's schedule count is shown while connected`() = dialog(
        connectionStatus = InstanceLinkStatus.CONNECTED,
        remoteScheduleCount = 7,
    ) {
        onNodeWithText("Primary schedule: 7 item(s)").assertExists()
    }

    // ── Switches ─────────────────────────────────────────────────────────────────

    @Test
    fun `autoConnect starts off by default and can be turned on`() = dialog { result ->
        hostField().performTextInput("h")
        portField().performTextInput("1")
        onAllNodes(isToggleable())[0].assertIsOff().performClick()

        onNodeWithText("Connect").performClick()
        assertEquals(true, result.connected?.autoConnect)
    }

    @Test
    fun `autoConnect reflects an already-enabled setting`() = dialog(settings = InstanceLinkSettings(autoConnect = true)) {
        onAllNodes(isToggleable())[0].assertIsOn()
    }

    @Test
    fun `allowPushToSchedule can be turned on`() = dialog { result ->
        hostField().performTextInput("h")
        portField().performTextInput("1")
        onAllNodes(isToggleable())[1].assertIsOff().performClick()

        onNodeWithText("Connect").performClick()
        assertEquals(true, result.connected?.allowPushToSchedule)
    }

    // ── Role ─────────────────────────────────────────────────────────────────────

    @Test
    fun `the role defaults to Controlled`() = dialog {
        onAllNodes(isSelectable())[0].assertIsSelected()
    }

    @Test
    fun `picking Controller switches the role and hides the Controlled-only settings`() = dialog { result ->
        onAllNodes(isSelectable())[1].performClick()
        onNodeWithText("Bible sync").assertDoesNotExist()

        hostField().performTextInput("h")
        portField().performTextInput("1")
        onNodeWithText("Connect").performClick()
        assertEquals(InstanceLinkRole.CONTROLLER, result.connected?.role)
    }

    @Test
    fun `Controlled-only settings are shown for the default role`() = dialog {
        onNodeWithText("Bible sync").assertExists()
    }

    // ── Bible sync (Controlled only) ─────────────────────────────────────────────

    @Test
    fun `bible sync defaults to full replica`() = dialog { result ->
        hostField().performTextInput("h")
        portField().performTextInput("1")
        onNodeWithText("Connect").performClick()
        assertEquals(BibleSyncMode.FULL_REPLICA, result.connected?.bibleSyncMode)
    }

    @Test
    fun `picking reference-only bible sync changes what Connect sends`() = dialog { result ->
        onAllNodes(isSelectable())[3].performClick()

        hostField().performTextInput("h")
        portField().performTextInput("1")
        onNodeWithText("Connect").performClick()
        assertEquals(BibleSyncMode.REFERENCE_ONLY, result.connected?.bibleSyncMode)
    }

    @Test
    fun `mirror backgrounds can be turned on`() = dialog { result ->
        hostField().performTextInput("h")
        portField().performTextInput("1")
        onAllNodes(isToggleable())[2].assertIsOff().performClick()

        onNodeWithText("Connect").performClick()
        assertEquals(true, result.connected?.mirrorBackgrounds)
    }
}
