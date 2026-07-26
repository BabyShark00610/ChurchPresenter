@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.QASettings
import org.churchpresenter.app.churchpresenter.server.TunnelStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Q&A remote window, and the two addresses it works out for the room.
 *
 * One of these goes on a screen for a congregation to scan and the other is the moderator's own way
 * in, so getting them mixed up either hides the queue or hands the room the controls. They are
 * *derived* — from the server address, an optional public tunnel, and whether an API key is set —
 * which is what this covers.
 *
 * `QARemoteDialog` opens a `DialogWindow` and sizes it with a grow-to-fit effect, neither of which
 * runs headless, so the body was lifted into `QARemoteContent`. Three things that content now takes
 * as parameters were previously reached for inside it: the font list (`GraphicsEnvironment`), the
 * clipboard writer, and the scroll state shared with the sizing effect. Injecting the clipboard is
 * what lets these tests press Copy and assert *which* address was handed over, rather than only
 * that a button exists.
 *
 * Left uncovered: the `DialogWindow` call and the grow-to-fit effect.
 */
class QARemoteContentTest {

    private companion object {
        const val SERVER = "http://192.168.1.50:8080"
        const val TUNNEL = "https://abc-def.trycloudflare.com"
    }

    private object Label {
        const val COPY = "Copy URL"
    }


    private class Clipboard {
        val written = mutableListOf<String>()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun qaRemote(
        serverUrl: String = SERVER,
        qaDisplayUrl: String = "",
        apiKeyEnabled: Boolean = false,
        apiKey: String = "",
        tunnelStatus: TunnelStatus = TunnelStatus.Idle,
        tunnelUrl: String = "",
        block: ComposeUiTest.(Clipboard) -> Unit,
    ) {
        val clipboard = Clipboard()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    QARemoteContent(
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        onQaDisplayUrlChanged = {},
                        apiKeyEnabled = apiKeyEnabled,
                        apiKey = apiKey,
                        tunnelStatus = tunnelStatus,
                        tunnelUrl = tunnelUrl,
                        onStartTunnel = {},
                        onStopTunnel = {},
                        qaSettings = QASettings(),
                        onSettingsChange = {},
                        availableFonts = listOf("Arial", "Helvetica"),
                        copyText = { clipboard.written += it },
                        onDismiss = {},
                    )
                }
            }
            block(clipboard)
        }
    }

    /** Exact match: the URL panels each render their address as one whole node. */
    private fun ComposeUiTest.shows(text: String): Boolean =
        onAllNodes(hasText(text)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    /** Substring match, for asserting no address was built at all. */
    private fun ComposeUiTest.showsAnyContaining(fragment: String): Boolean =
        onAllNodes(hasText(fragment, substring = true))
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    /** Presses the nth Copy button: the submission panel's is first, the moderator's second. */
    private fun ComposeUiTest.copy(index: Int) {
        onAllNodes(hasText(Label.COPY))[index].performClick()
        waitForIdle()
    }

    // ── The address the congregation scans ──────────────────────────────────────

    @Test
    fun `the submission address is the server with the Q and A path on it`() = qaRemote { _ ->
        assertTrue(shows("$SERVER/qa"), "the room's address is the server plus /qa")
    }

    @Test
    fun `a configured display address is preferred over the server's own`() =
        qaRemote(qaDisplayUrl = TUNNEL, tunnelUrl = TUNNEL) { _ ->
            assertTrue(shows("$TUNNEL/qa"), "the address chosen for display is the one to publish")
            assertTrue(!shows("$SERVER/qa"), "the local address must not be the one shown to the room")
        }

    @Test
    fun `with no server address at all nothing is offered to scan`() =
        qaRemote(serverUrl = "") { _ ->
            assertTrue(
                !showsAnyContaining("/qa"),
                "an address cannot be built from nothing, so neither panel may show a path",
            )
        }

    // ── The moderator's own way in ──────────────────────────────────────────────

    @Test
    fun `the moderator address stays on the local server while the tunnel is unused`() = qaRemote { _ ->
        assertTrue(shows("$SERVER/qa/admin"), "moderation stays on the LAN unless deliberately published")
    }

    @Test
    fun `the moderator address follows the tunnel only when the tunnel is what is being displayed`() =
        qaRemote(qaDisplayUrl = TUNNEL, tunnelUrl = TUNNEL) { _ ->
            assertTrue(shows("$TUNNEL/qa/admin"), "publishing over the tunnel moves moderation there too")
        }

    @Test
    fun `a running tunnel that is not being displayed leaves moderation local`() =
        qaRemote(qaDisplayUrl = "", tunnelUrl = TUNNEL, tunnelStatus = TunnelStatus.Connected(TUNNEL)) { _ ->
            // The tunnel exists but the room is being given the LAN address, so the moderator keeps it too.
            assertTrue(shows("$SERVER/qa/admin"), "a tunnel merely running must not move the moderator link")
        }

    // ── The API key in the moderator link ───────────────────────────────────────

    @Test
    fun `an enabled API key is carried in the moderator link so the QR opens straight in`() =
        qaRemote(apiKeyEnabled = true, apiKey = "secret123") { _ ->
            assertTrue(
                shows("$SERVER/qa/admin?password=secret123"),
                "the moderator's QR has to get past the key without it being typed on a phone",
            )
        }

    @Test
    fun `a key with characters that would break a URL is encoded`() =
        qaRemote(apiKeyEnabled = true, apiKey = "p@ss word&more") { _ ->
            assertTrue(
                shows("$SERVER/qa/admin?password=p%40ss+word%26more"),
                "an unencoded & would cut the key short and an unencoded space would break the link",
            )
        }

    @Test
    fun `a key that is set but not enabled is left out`() =
        qaRemote(apiKeyEnabled = false, apiKey = "secret123") { _ ->
            assertTrue(shows("$SERVER/qa/admin"), "the plain link is the one to show")
            assertTrue(!shows("$SERVER/qa/admin?password=secret123"), "a disabled key must not be published")
        }

    @Test
    fun `an enabled but empty key adds nothing`() =
        qaRemote(apiKeyEnabled = true, apiKey = "") { _ ->
            assertTrue(shows("$SERVER/qa/admin"), "there is no key to carry, so the link stays plain")
        }

    // ── Copying ─────────────────────────────────────────────────────────────────

    @Test
    fun `copying the first panel copies the address the congregation scans`() = qaRemote { clipboard ->
        copy(0)
        assertEquals(listOf("$SERVER/qa"), clipboard.written)
    }

    @Test
    fun `copying the moderator panel copies the link including its key`() =
        qaRemote(apiKeyEnabled = true, apiKey = "secret123") { clipboard ->
            copy(1)
            assertEquals(
                listOf("$SERVER/qa/admin?password=secret123"),
                clipboard.written,
                "the copied moderator link must be the one that actually gets in",
            )
        }

    // ── Tunnel state ────────────────────────────────────────────────────────────

    @Test
    fun `a tunnel error is reported rather than swallowed`() =
        qaRemote(tunnelStatus = TunnelStatus.Error("cloudflared exited")) { _ ->
            onNodeWithText("cloudflared exited").assertIsDisplayed()
        }
}
