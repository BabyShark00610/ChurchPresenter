@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.dialogs.KeyboardShortcutsDialogContent
import org.churchpresenter.app.churchpresenter.dialogs.SHORTCUT_PRESS_MODE_TAG
import org.churchpresenter.app.churchpresenter.dialogs.SHORTCUT_PRESS_PANEL_TAG
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.ui.theme.ChurchPresenterTheme
import kotlin.test.Test

/**
 * The Keyboard Shortcuts dialog (Help → Keyboard Shortcuts, F1), in both themes.
 *
 * One row per rebindable action grouped by the scope that decides what can collide with what, a
 * read-only Mouse section at the bottom, and Cancel/Apply/OK. This is now the only place shortcuts
 * are both listed and changed — the editing UI was briefly a Settings tab and was merged in here.
 *
 * What changes the shape of a row rather than a value in it:
 *
 *  - **Whether the action is customized.** An untouched row offers *Clear*; one the user has moved
 *    offers *Reset*. Which of the two is showing is the only sign in the row that a binding is no
 *    longer the shipped one, so both are shot.
 *  - **Whether the action is bound at all.** An unbound row reads "Not set" rather than an empty
 *    chip. Save As ships that way, so the default image already carries one; the customized image
 *    adds a deliberately cleared row.
 *
 * The capture dialog is not shot: it is a `DialogWindow`, which needs a real window and cannot be
 * composed by the test runner. Its states are covered by `ShortcutCaptureContentTest`.
 *
 * **These are macOS renders**, so modifiers appear as `⌃⌥⇧⌘` rather than `Ctrl+Alt+…`. That is what
 * the same code produces on this platform, not a defect — see the platform table in AGENT.md before
 * re-recording anywhere else.
 */
class KeyboardShortcutsDialogScreenshotTest {

    @Test
    fun `as it opens`() = shoot("defaults")

    @Test
    fun `with customized bindings`() = shoot(
        "customized",
        settings = AppSettings(
            keyboardShortcutSettings = KeyboardShortcutSettings(
                overrides = mapOf(
                    ShortcutAction.UNDO.name to listOf(KeyChord.of(Key.U, ctrl = true)),
                    ShortcutAction.SAVE_SCHEDULE.name to listOf(KeyChord.of(Key.S, ctrl = true, shift = true)),
                    ShortcutAction.CLEAR_OUTPUT.name to emptyList(),
                    ShortcutAction.SWITCH_TO_BIBLE.name to listOf(KeyChord.of(Key.B, ctrl = true, alt = true)),
                )
            )
        ),
    )

    /**
     * Filtered down to one category.
     *
     * The search box matches descriptions *and* keys, and drops any category with nothing in it —
     * so a filtered dialog is a visibly different layout, not the same list with rows greyed out.
     */
    @Test
    fun `filtered by a search`() = shoot("filtered") {
        onNode(hasSetTextAction()).performTextInput("verse")
        waitForIdle()
    }

    /**
     * "Press key" mode, after pressing the left arrow.
     *
     * A distinct layout, not a variant of the text search: the box stops accepting text and shows
     * the chord instead, because the arrow keys have to reach the filter rather than move a cursor.
     * This is also the only shot where the header's listening state is visible.
     */
    @Test
    fun `filtered by a pressed key`() = shoot("press_key") {
        onNodeWithTag(SHORTCUT_PRESS_MODE_TAG).performClick()
        waitForIdle()
        onNodeWithTag(SHORTCUT_PRESS_PANEL_TAG).performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
    }

    // ── Harness ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Shot at the dialog's real size rather than the runner's default window.
     *
     * `KeyboardShortcutsDialog` opens at 760×720, and at the default 1024 the rows stretch and the
     * gap between description and key chip is far wider than anyone will ever see. The point of a
     * committed image is that a reviewer can approve what ships.
     */
    private fun shoot(
        name: String,
        settings: AppSettings = AppSettings(),
        drive: SkikoComposeUiTest.() -> Unit = {},
    ) = stackedThemes(SECTION, name) { mode, file ->
        runSkikoComposeUiTest(size = Size(DIALOG_WIDTH, DIALOG_HEIGHT), density = Density(1f)) {
            setContent {
                ChurchPresenterTheme(themeMode = mode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) {
                            KeyboardShortcutsDialogContent(
                                initialSettings = settings,
                                onSave = {},
                                onDismiss = {},
                            )
                        }
                    }
                }
            }
            waitForIdle()
            drive()
            waitForIdle()
            captureTo(file)
        }
    }

    private companion object {
        const val SECTION = "keyboardShortcutsDialog"

        /** Matches the DialogWindow size in `KeyboardShortcutsDialog`. */
        const val DIALOG_WIDTH = 760f
        const val DIALOG_HEIGHT = 720f
    }
}
