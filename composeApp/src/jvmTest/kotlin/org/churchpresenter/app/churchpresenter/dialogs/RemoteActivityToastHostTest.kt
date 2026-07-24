package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * The overlay that tells the operator what an auto-approved remote client just did on their behalf.
 * It shows the most-recent notification, labels the action, attributes it to a client (preferring a
 * human label, otherwise a truncated id) and badges a client that is an Instance Link follower.
 * These are the cues the operator uses to notice an unexpected remote action, so the action label,
 * the attribution and the follower badge are each asserted — plus the empty case that shows nothing.
 */
@OptIn(ExperimentalTestApi::class)
class RemoteActivityToastHostTest {

    private fun host(
        notifications: List<RemoteActivityNotification>,
        followers: Set<String> = emptySet(),
    ): @androidx.compose.runtime.Composable () -> Unit = {
        MaterialTheme {
            RemoteActivityToastHost(
                notifications = notifications,
                onDismiss = {},
                onDismissAll = {},
                onBlockForSession = {},
                connectedInstanceLinkFollowers = followers,
            )
        }
    }

    @Test
    fun `the most recent notification's action and title are shown`() = runComposeUiTest {
        setContent(
            host(
                listOf(
                    RemoteActivityNotification(RemoteEventType.UPLOAD, title = "Old upload"),
                    RemoteActivityNotification(RemoteEventType.PROJECT, title = "Amazing Grace"),
                )
            )
        )
        // PROJECT is the last entry, so its label ("Projected (Go Live)") and title win.
        onNodeWithText("Projected", substring = true)
            .assertExists("the action label for the newest notification must show")
        onNodeWithText("Amazing Grace", substring = true)
            .assertExists("the newest notification's title must show")
    }

    @Test
    fun `a client label is used for attribution when present`() = runComposeUiTest {
        setContent(
            host(
                listOf(
                    RemoteActivityNotification(
                        RemoteEventType.PROJECT,
                        title = "Psalm 23",
                        clientId = "device-abcdef",
                        clientLabel = "Booth iPad",
                    )
                )
            )
        )
        onNodeWithText("Booth iPad", substring = true)
            .assertExists("a human client label must be preferred for attribution")
    }

    @Test
    fun `an instance-link follower is badged`() = runComposeUiTest {
        setContent(
            host(
                notifications = listOf(
                    RemoteActivityNotification(
                        RemoteEventType.PROJECT,
                        title = "Psalm 23",
                        clientId = "follower-1",
                    )
                ),
                followers = setOf("follower-1"),
            )
        )
        onNodeWithText("Instance Link", substring = true)
            .assertExists("a connected follower's action must be badged as such")
    }

    @Test
    fun `with no notifications nothing is shown`() = runComposeUiTest {
        setContent(host(emptyList()))
        onNodeWithText("Dismiss", substring = true)
            .assertDoesNotExist()
    }
}
