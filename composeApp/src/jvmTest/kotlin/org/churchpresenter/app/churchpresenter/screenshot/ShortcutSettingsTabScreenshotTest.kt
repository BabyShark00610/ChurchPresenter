@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.dialogs.tabs.ShortcutSettingsTab
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.ui.theme.ChurchPresenterTheme
import kotlin.test.Test

/**
 * The Shortcuts tab of the settings dialog, in both themes.
 *
 * One row per rebindable action, grouped by the scope that decides what can collide with what, laid
 * out over two columns split by action count so the two sides come out roughly level.
 *
 * What changes the shape of a row rather than a value in it:
 *
 *  - **Whether the action is customized.** An untouched row offers *Clear*; one the user has moved
 *    offers *Reset* instead. Both are shot, because which of the two is showing is the only
 *    indication in the row that a binding is no longer the shipped one.
 *  - **Whether the action is bound at all.** An unbound row reads "Not set" rather than an empty
 *    chip. Save As ships that way, so the default image already carries one; the customized image
 *    adds a deliberately cleared row.
 *
 * The capture dialog is not shot: it is a `DialogWindow`, which needs a real window and cannot be
 * composed by the test runner. Its logic is covered by `ShortcutCaptureLogicTest`.
 *
 * **These images are macOS renders**, so modifiers appear as `⌃⌥⇧⌘` rather than `Ctrl+Alt+…`. That
 * is the label the same code produces on this platform, not a defect — see the platform table in
 * AGENT.md before re-recording anywhere else.
 */
class ShortcutSettingsTabScreenshotTest {

    @Test
    fun `as it opens`() = shoot("defaults")

    /**
     * Several bindings moved off their defaults, plus one cleared.
     *
     * Covers all three row states at once: a rebound chip with *Reset* beside it, an unbound row
     * reading "Not set", and untouched rows still offering *Clear*.
     */
    @Test
    fun `with customized bindings`() = shoot(
        "customized",
        settings = AppSettings(
            keyboardShortcutSettings = KeyboardShortcutSettings(
                overrides = mapOf(
                    ShortcutAction.UNDO.name to listOf(KeyChord.of(Key.U, ctrl = true)),
                    ShortcutAction.BIBLE_NEXT_VERSE.name to listOf(KeyChord.of(Key.J)),
                    ShortcutAction.MEDIA_MUTE.name to emptyList(),
                    ShortcutAction.SWITCH_TO_BIBLE.name to listOf(KeyChord.of(Key.B, ctrl = true, alt = true)),
                )
            )
        ),
    )

    // ── Harness ─────────────────────────────────────────────────────────────────────────────────

    private fun shoot(
        name: String,
        settings: AppSettings = AppSettings(),
    ) = stackedThemes(SECTION, name) { mode, file ->
        runComposeUiTest {
            setContent {
                ChurchPresenterTheme(themeMode = mode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) {
                            var current by remember { mutableStateOf(settings) }
                            ShortcutSettingsTab(
                                settings = current,
                                onSettingsChange = { transform -> current = transform(current) },
                            )
                        }
                    }
                }
            }
            waitForIdle()
            captureTo(file)
        }
    }

    private companion object {
        const val SECTION = "shortcutSettingsTab"
    }
}
