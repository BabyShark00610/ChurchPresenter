@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.STTSettings
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import org.churchpresenter.app.churchpresenter.viewmodel.STTManager
import org.json.JSONObject

/**
 * Harness and fixtures shared by the `STTTab` test classes.
 *
 * The tab is driven through a real [STTManager] and a real [PresenterManager]. Neither needs a
 * socket: captions arrive by handing the manager the same JSON payloads the STT server sends
 * (`handleTranscriptionUpdate` and friends), and the connection state is set through the
 * `apply*` transitions the socket callbacks themselves call. Nothing here opens a connection unless
 * a test clicks Connect, and [sttTab] disposes the manager afterwards either way.
 *
 * `appSettings` is a fixed value rather than hoisted state — as in the other tab suites — so a test
 * that needs a different display mode or segment cap passes it up front via [settings] rather than
 * expecting a click to change it. What a click *would* have changed is recorded in [STTReports].
 */

// ── What the tab reported back ──────────────────────────────────────────────────────────────────

internal class STTReports {
    val presenting = mutableListOf<Presenting>()
    var settingsChanges = 0
    var settingsAfterChange: AppSettings? = null
}

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/**
 * Composes `STTTab` over a real [STTManager]/[PresenterManager] and runs [block].
 *
 * [seed] runs before the first composition, for tests that want captions or a connection already in
 * place when the tab first draws.
 */
@OptIn(ExperimentalTestApi::class)
internal fun sttTab(
    settings: STTSettings = STTSettings(serverUrl = UNREACHABLE_URL),
    seed: STTManager.() -> Unit = {},
    block: ComposeUiTest.(stt: STTManager, presenter: PresenterManager, reports: STTReports) -> Unit,
) {
    val appSettings = AppSettings(sttSettings = settings)
    val stt = STTManager()
    val presenter = PresenterManager()
    val reports = STTReports()
    try {
        stt.seed()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    STTTab(
                        sttManager = stt,
                        presenterManager = presenter,
                        presenting = { reports.presenting += it },
                        appSettings = appSettings,
                        onSettingsChange = { transform ->
                            reports.settingsChanges++
                            reports.settingsAfterChange = transform(appSettings)
                        },
                    )
                }
            }
            block(stt, presenter, reports)
        }
    } finally {
        runCatching { stt.dispose() }
    }
}

/**
 * A documentation-reserved address (TEST-NET-1, RFC 5737) on a port nothing listens on.
 *
 * Used wherever a test clicks Connect: the click has to hand a real URL to socket.io, and this one
 * can never reach a host — on this machine or on CI — so the attempt stays a background no-op
 * instead of finding something. Tests assert on the state the click sets synchronously
 * (`connecting`), never on the outcome of the attempt.
 */
internal const val UNREACHABLE_URL = "http://192.0.2.1:1"

// ── Feeding the manager what the STT server would send ──────────────────────────────────────────

/** One completed transcription segment, as `transcription_update` delivers it. */
internal fun STTManager.transcribe(vararg texts: String) {
    val segments = texts.mapIndexed { index, text ->
        """{"id":$index,"timestamp":"00:0$index","text":"$text","start":$index.0,"end":${index + 1}.0,"completed":true}"""
    }
    handleTranscriptionUpdate(JSONObject("""{"segments":[${segments.joinToString(",")}]}"""))
}

/**
 * The partial phrase currently being spoken, which the tab draws dimmed under the segments.
 *
 * Sent as a bare string: the transcription parser stringifies anything that is not a String, so an
 * `{"text": …}` object would arrive as its own JSON rather than as the phrase. (Translation's parser
 * does read the object form — see [translateInProgress].)
 */
internal fun STTManager.transcribeInProgress(text: String) {
    // No "segments" key: the parser clears the segment list whenever one is present, so including an
    // empty array here would wipe whatever captions a test had already sent.
    handleTranscriptionUpdate(JSONObject("""{"in_progress":"$text"}"""))
}

internal fun STTManager.translate(vararg texts: String) {
    val segments = texts.mapIndexed { index, text ->
        """{"id":$index,"timestamp":"00:0$index","translated_text":"$text","start":$index.0,"end":${index + 1}.0,"completed":true}"""
    }
    handleTranslationUpdate(JSONObject("""{"segments":[${segments.joinToString(",")}]}"""))
}

internal fun STTManager.translateInProgress(text: String) {
    // As with [transcribeInProgress], no "segments" key — it would clear the translated captions.
    handleTranslationUpdate(JSONObject("""{"in_progress":{"translated_text":"$text"}}"""))
}

/** Words the STT server wants coloured, as `word_highlighting_update` delivers them. */
internal fun STTManager.highlight(vararg words: Pair<String, String>, enabled: Boolean = true) {
    val entries = words.map { (word, color) -> """{"word":"$word","color":"$color"}""" }
    handleWordHighlightingUpdate(
        JSONObject("""{"enabled":$enabled,"words":[${entries.joinToString(",")}]}""")
    )
}

/** Connected, with captions already on screen — the tab's main state. */
internal fun STTManager.live(vararg texts: String) {
    applyConnected()
    if (texts.isNotEmpty()) transcribe(*texts)
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object STTLabel {
    const val SERVER_URL = "STT Server URL"
    const val CONNECT = "Connect"
    const val DISCONNECT = "Disconnect"
    const val CLEAR = "Clear"
    const val SETTINGS = "STT Display Settings"
    const val GO_LIVE = "Go Live"
    const val LIVE_PREVIEW = "Live Preview"
    const val NOT_CONNECTED = "Not connected. Enter the STT server URL and click Connect."
    const val WAITING = "Waiting for transcription…"
    const val TRANSCRIPTION = "Transcription"
    const val TRANSLATION = "Translation"
    const val CONNECTING = "Connecting to STT…"
    const val UNREACHABLE = "Can't reach STT server — retrying…"
    const val RECONNECTING = "STT disconnected — reconnecting…"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────

// renderedText/showsExactly/showsContainingText live in TabRenderedText.kt — they are shared with
// the other tab suites in this package.

/** A button, addressed by the content description its tooltip gives it. */
internal fun ComposeUiTest.sttButton(label: String) = onNodeWithContentDescription(label)

internal fun ComposeUiTest.hasSttButton(label: String): Boolean =
    onAllNodesWithContentDescription(label)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

/** The server-URL field — the only control on the tab that takes typed text. */
internal fun ComposeUiTest.urlField() = onAllNodes(hasSetTextAction())[0]

/**
 * Whether the url field can still be typed into.
 *
 * The tab disables it while a connection is up or in flight, and a disabled text field drops its
 * set-text action entirely — so "locked" is the absence of the field rather than a disabled node.
 */
internal fun ComposeUiTest.urlFieldIsEditable(): Boolean =
    onAllNodes(hasSetTextAction()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

/**
 * What the url field currently holds.
 *
 * A field's contents are `EditableText`, not `Text`, so they never appear in [renderedText] — an
 * assertion phrased against that would pass whatever the field said.
 */
internal fun ComposeUiTest.urlFieldText(): String =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.EditableText)?.text }
        .orEmpty()
