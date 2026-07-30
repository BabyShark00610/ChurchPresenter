@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.viewmodel.LocalMediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.MediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager

/**
 * Harness and fixtures for the `MediaTab` test classes.
 *
 * The tab had **no test file at all** before this one. It was written off as VLC-bound, but only its
 * playback surface is: the source picker, the URL entry, the recents bar and the transport controls are
 * ordinary Compose and render without VLC anywhere in sight. `WebTab` established the same thing for
 * JCEF, and `MediaViewModel` (97%) and `LocalMediaViewModel` (100%) show the logic underneath was never
 * the problem — only the tab.
 *
 * **[vlcAvailable] is the reason this works.** `isVlcAvailable` is a top-level `val` in
 * `composables/VideoPlayer.kt` that caches its answer in a process-wide field, so without a seam the tab
 * renders one thing on a developer's Mac with VLC installed and another on CI — neither branch testable
 * on purpose. The tab now takes it as a parameter defaulting to the real check, so a test names which
 * world it wants. Same shape as `WebTab`'s `cefInitialized`.
 */

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/** What the tab reported back, so a test asserts on the choice rather than on a stub. */
internal class MediaReports {
    /** mediaUrl, mediaTitle, mediaType — exactly what the schedule would be given. */
    val scheduled = mutableListOf<Triple<String, String, String>>()
}

@OptIn(ExperimentalTestApi::class)
internal fun mediaTab(
    settings: (AppSettings) -> AppSettings = { it },
    selectedMediaItem: ScheduleItem.MediaItem? = null,
    presenterManager: PresenterManager? = null,
    /** Which VLC world to compose in — see the note above. Defaults to "installed and working". */
    vlcAvailable: Boolean = true,
    vlcArchMismatch: Boolean = false,
    vlcLoadFailed: Boolean = false,
    block: ComposeUiTest.(vm: MediaViewModel, reports: MediaReports) -> Unit,
) {
    // The tab's recents list persists under user.home; pin the JVM-wide loggers first.
    TestSingletons.latchToTestHome()
    val appSettings = settings(AppSettings())
    val reports = MediaReports()
    // The tab's very first line is `LocalMediaViewModel.current ?: return`, so without this the whole
    // composable is a no-op and every assertion below would be about an empty screen.
    val vm = MediaViewModel()
    runComposeUiTest {
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalMediaViewModel provides vm) {
                MediaTab(
                    appSettings = appSettings,
                    selectedMediaItem = selectedMediaItem,
                    presenterManager = presenterManager,
                    onAddToSchedule = { url, title, type -> reports.scheduled += Triple(url, title, type) },
                    vlcAvailable = vlcAvailable,
                    vlcArchMismatch = vlcArchMismatch,
                    vlcLoadFailed = vlcLoadFailed,
                )
                }
            }
        }
        block(vm, reports)
    }
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object MediaLabel {
    const val VLC_REQUIRED = "VLC media player is required for media playback"
    const val VLC_INSTALL = "Please install VLC from videolan.org and restart the application"
    const val LOCAL_FILE = "Local File"
    const val NETWORK_URL = "Network URL"
    const val SELECT_FILE = "Select File"
    const val URL_PLACEHOLDER = "Enter URL (http://, rtsp://, ...)"
    const val ADD_TO_SCHEDULE = "Add to Schedule"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────

// renderedText/showsExactly/showsContainingText live in TabRenderedText.kt — shared with the other
// tab suites in this package. Do not redeclare them.

/** A button, addressed by the content description its tooltip gives it. */
internal fun ComposeUiTest.mediaButton(label: String) = onNodeWithContentDescription(label)

internal fun ComposeUiTest.hasMediaButton(label: String): Boolean =
    onAllNodesWithContentDescription(label)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
