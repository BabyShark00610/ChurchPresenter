@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.server.TunnelStatus
import org.churchpresenter.app.churchpresenter.viewmodel.PresentationViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager

/**
 * Harness and fixtures for the `PresentationTab` test classes.
 *
 * The tab had **no test file at all** before this one, on the grounds that it needs a real deck through
 * `DeckRasterizer`. Its *slide grid* does. The rest — the empty state, the toolbar, the freeze and clear
 * controls, the remote-serving panel — is ordinary Compose over a [PresentationViewModel] that
 * constructs with no deck at all and already has five test suites of its own.
 *
 * `WebTab` had established the same thing for a JCEF-bound tab; this is the deck-bound equivalent.
 *
 * **What is deliberately out of reach:** anything behind `viewModel.slideFiles` — the grid,
 * `SlideThumbnail`, the build counter, slide navigation. Those need rasterized output, and a fixture
 * that faked it would be asserting the fake. The tab is composed with an empty view model throughout,
 * so every test here is about the no-deck state and the controls that surround it.
 */

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/** What the tab reported back, so a test asserts on the choice rather than on a stub. */
internal class PresentationReports {
    /** filePath, fileName, slideCount, fileType — exactly what the schedule would be given. */
    val scheduled = mutableListOf<String>()
    var settingsChanges = 0
    var settingsAfterChange: AppSettings? = null
    var freezeToggles = 0
    var clears = 0
    val displayUrlChanges = mutableListOf<String>()
    var tunnelStarts = 0
    var tunnelStops = 0
}

@OptIn(ExperimentalTestApi::class)
internal fun presentationTab(
    settings: (AppSettings) -> AppSettings = { it },
    presenterManager: PresenterManager? = null,
    presentationFrozen: Boolean = false,
    tunnelStatus: TunnelStatus = TunnelStatus.Idle,
    tunnelUrl: String = "",
    serverUrl: String = "",
    presentationDisplayUrl: String = "",
    /** Whether VLC is usable — only decides the missing-VLC banner for a deck containing video. */
    vlcAvailable: Boolean = true,
    block: ComposeUiTest.(vm: PresentationViewModel, reports: PresentationReports) -> Unit,
) {
    // PresentationViewModel's slide disk cache resolves under user.home.
    TestSingletons.latchToTestHome()
    val appSettings = settings(AppSettings())
    val reports = PresentationReports()
    val vm = PresentationViewModel(appSettings)
    runComposeUiTest {
        setContent {
            MaterialTheme {
                PresentationTab(
                    appSettings = appSettings,
                    viewModel = vm,
                    presenterManager = presenterManager,
                    onAddToSchedule = { path, name, count, type -> reports.scheduled += "$path:$name:$count:$type" },
                    onSettingsChange = { transform ->
                        reports.settingsChanges++
                        reports.settingsAfterChange = transform(reports.settingsAfterChange ?: appSettings)
                    },
                    tunnelStatus = tunnelStatus,
                    tunnelUrl = tunnelUrl,
                    serverUrl = serverUrl,
                    presentationDisplayUrl = presentationDisplayUrl,
                    onPresentationDisplayUrlChanged = { reports.displayUrlChanges += it },
                    onStartTunnel = { reports.tunnelStarts++ },
                    onStopTunnel = { reports.tunnelStops++ },
                    presentationFrozen = presentationFrozen,
                    onFreezeToggle = { reports.freezeToggles++ },
                    onClearPresentation = { reports.clears++ },
                    vlcAvailable = vlcAvailable,
                )
            }
        }
        block(vm, reports)
    }
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object PresentationLabel {
    const val NO_FILE = "No file selected"
    const val SELECT_FILE = "Select File"
    const val BLANK_OUTPUT = "Blank Output"
    const val UNBLANK_OUTPUT = "Unblank Output"
    const val CLEAR = "Clear Presentation"
    const val REMOTE = "Open Remote Control"
    const val GO_LIVE = "Go Live"
    const val ADD_TO_SCHEDULE = "Add to Schedule"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────

// renderedText/showsExactly/showsContainingText live in TabRenderedText.kt — shared with the other
// tab suites in this package. Do not redeclare them.

/** A button, addressed by the content description its tooltip gives it. */
internal fun ComposeUiTest.presentationButton(label: String) = onNodeWithContentDescription(label)

internal fun ComposeUiTest.hasPresentationButton(label: String): Boolean =
    onAllNodesWithContentDescription(label)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
