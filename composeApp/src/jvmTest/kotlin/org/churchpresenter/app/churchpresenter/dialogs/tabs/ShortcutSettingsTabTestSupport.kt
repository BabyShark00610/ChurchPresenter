@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.models.ShortcutAction

/**
 * Harness for `ShortcutSettingsTab`.
 *
 * Mirrors the other tab suites: the tab is composed over real state, `block` reads back the
 * `AppSettings` the tab produced, and nothing is stubbed.
 *
 * **The capture dialog is not reachable from here.** It is a `DialogWindow`, which needs a real
 * window and so cannot be composed by `runComposeUiTest`. Clicking *Change* is therefore only
 * assertable up to "the tab asked for a capture"; what the dialog does with a key press is covered
 * by `ShortcutCaptureLogicTest` against the same functions the dialog calls. Recording an actual
 * key through the real dialog is the one gap in this suite.
 */
internal fun shortcutTab(
    initial: AppSettings = AppSettings(),
    block: ComposeUiTest.(get: () -> AppSettings) -> Unit,
) = runComposeUiTest {
    var current = initial
    setContent {
        MaterialTheme {
            var state by remember { mutableStateOf(current) }
            ShortcutSettingsTab(
                settings = state,
                onSettingsChange = { transform -> state = transform(state); current = state },
            )
        }
    }
    block { current }
}

/**
 * The key chip for one action's row.
 *
 * Located by tag rather than by its text because the text *is* what most of these tests assert on,
 * and because a modifier renders as `⌃` on macOS and `Ctrl` elsewhere.
 */
internal fun ComposeUiTest.chipFor(action: ShortcutAction) = onNodeWithTag(shortcutChipTag(action))

/** The rebind button for one action's row. */
internal fun ComposeUiTest.changeButtonFor(action: ShortcutAction) =
    onNodeWithTag(shortcutChangeTag(action))

/**
 * The Reset (or Clear) button for one action's row.
 *
 * Every row has one, so "the Clear button" matches ~40 nodes — it has to be addressed per action.
 */
internal fun ComposeUiTest.revertButtonFor(action: ShortcutAction) =
    onNodeWithTag(shortcutRevertTag(action))
