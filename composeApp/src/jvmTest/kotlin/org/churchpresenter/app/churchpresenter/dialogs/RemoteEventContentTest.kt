@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteEventContentTest {

    private class Result {
        var allowCalls = 0
        var allowForSessionCalls = 0
        var allowPermanentlyCalls = 0
        var blockForSessionCalls = 0
        var blockPermanentlyCalls = 0
        var denyCalls = 0
    }

    private fun dialog(
        event: RemoteEvent = RemoteEvent(type = RemoteEventType.ADD_TO_SCHEDULE, title = "Amazing Grace"),
        remaining: Int = 0,
        showAllowPermanently: Boolean = true,
        isClientKnownAllowed: Boolean = false,
        isClientKnownBlocked: Boolean = false,
        isInstanceLinkFollower: Boolean = false,
        block: ComposeUiTest.(Result) -> Unit,
    ) {
        val result = Result()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    RemoteEventDialogContent(
                        event = event,
                        actionLabel = "Add to Schedule",
                        typeIcon = Icons.Filled.CalendarMonth,
                        typeAccent = MaterialTheme.colorScheme.primary,
                        bodyTitle = event.title,
                        remaining = remaining,
                        showAllowPermanently = showAllowPermanently,
                        isClientKnownAllowed = isClientKnownAllowed,
                        isClientKnownBlocked = isClientKnownBlocked,
                        isInstanceLinkFollower = isInstanceLinkFollower,
                        onAllow = { result.allowCalls++ },
                        onAllowForSession = { result.allowForSessionCalls++ },
                        onAllowPermanently = { result.allowPermanentlyCalls++ },
                        onBlockForSession = { result.blockForSessionCalls++ },
                        onBlockPermanently = { result.blockPermanentlyCalls++ },
                        onDeny = { result.denyCalls++ },
                    )
                }
            }
            block(result)
        }
    }

    @Test
    fun `the action label and item title are shown`() = dialog {
        onNodeWithText("Add to Schedule").assertExists()
        onNodeWithText("Amazing Grace").assertExists()
    }

    @Test
    fun `the prominent Allow button calls onAllow`() = dialog { result ->
        onNodeWithText("Allow").performClick()
        assertEquals(1, result.allowCalls)
    }

    @Test
    fun `the Allow icon button also calls onAllow`() = dialog { result ->
        onNodeWithContentDescription("Allow").performClick()
        assertEquals(1, result.allowCalls)
    }

    @Test
    fun `Deny calls onDeny`() = dialog { result ->
        onNodeWithContentDescription("Deny").performClick()
        assertEquals(1, result.denyCalls)
    }

    @Test
    fun `Allow for Session calls onAllowForSession`() = dialog { result ->
        onNodeWithContentDescription("Allow for Session").performClick()
        assertEquals(1, result.allowForSessionCalls)
    }

    @Test
    fun `Allow Permanently calls onAllowPermanently when it is offered`() = dialog { result ->
        onNodeWithContentDescription("Allow Permanently").performClick()
        assertEquals(1, result.allowPermanentlyCalls)
    }

    @Test
    fun `Allow Permanently is hidden once the client is already known`() = dialog(showAllowPermanently = false) {
        onNodeWithContentDescription("Allow Permanently").assertDoesNotExist()
    }

    @Test
    fun `Block for Session calls onBlockForSession`() = dialog { result ->
        onNodeWithContentDescription("Block for Session").performClick()
        assertEquals(1, result.blockForSessionCalls)
    }

    @Test
    fun `Block Permanently calls onBlockPermanently`() = dialog { result ->
        onNodeWithContentDescription("Block Permanently").performClick()
        assertEquals(1, result.blockPermanentlyCalls)
    }

    @Test
    fun `no queue badge is shown when nothing else is waiting`() = dialog(remaining = 0) {
        onNodeWithText("1 more request waiting in queue").assertDoesNotExist()
    }

    @Test
    fun `a single queued item uses the singular wording`() = dialog(remaining = 1) {
        onNodeWithText("+1").assertExists()
        onNodeWithText("1 more request waiting in queue").assertExists()
    }

    @Test
    fun `several queued items use the plural wording with a count`() = dialog(remaining = 3) {
        onNodeWithText("+3").assertExists()
        onNodeWithText("3 more requests waiting in queue").assertExists()
    }

    @Test
    fun `an allowed client shows its badge`() = dialog(
        event = RemoteEvent(type = RemoteEventType.ADD_TO_SCHEDULE, title = "x", clientId = "device-1"),
        isClientKnownAllowed = true,
    ) {
        onNodeWithText("✓ allowed").assertExists()
    }

    @Test
    fun `a blocked client shows its badge`() = dialog(
        event = RemoteEvent(type = RemoteEventType.ADD_TO_SCHEDULE, title = "x", clientId = "device-1"),
        isClientKnownBlocked = true,
    ) {
        onNodeWithText("⛔ blocked").assertExists()
    }

    @Test
    fun `an instance link follower shows its own badge`() = dialog(
        event = RemoteEvent(type = RemoteEventType.ADD_TO_SCHEDULE, title = "x", clientId = "device-1"),
        isInstanceLinkFollower = true,
    ) {
        onNodeWithText("Instance Link — currently connected").assertExists()
    }

    @Test
    fun `a client label is shown alongside the raw device id`() = dialog(
        event = RemoteEvent(
            type = RemoteEventType.ADD_TO_SCHEDULE,
            title = "x",
            clientId = "device-1",
            clientLabel = "Front Row iPad",
        ),
    ) {
        onNodeWithText("Front Row iPad").assertExists()
        onNodeWithText("device-1").assertExists()
    }

    @Test
    fun `event detail text is shown when present`() = dialog(
        event = RemoteEvent(type = RemoteEventType.ADD_TO_SCHEDULE, title = "Amazing Grace", detail = "Hymnal #42"),
    ) {
        onNodeWithText("Hymnal #42").assertExists()
    }
}
