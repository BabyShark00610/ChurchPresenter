@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.StreamingSettings
import java.io.File
import java.nio.file.Files

/**
 * Harness and fixtures shared by the `LowerThirdTab` test classes.
 *
 * The tab is a preset picker over a folder of Lottie files: it lists what is in the folder, previews
 * one, and hands the chosen animation to the output or the schedule. So the fixtures are real files
 * on disk — the tab reads and parses them itself, and a stub that never parses would exercise the
 * error path instead of the one under test.
 *
 * Nothing here drives the ATEM upload panel: it reaches a switcher over the network, and what can be
 * decided before that is already covered by `CompanionServerLowerThirdTest`.
 */

// ── Fixtures ────────────────────────────────────────────────────────────────────────────────────

/** A Lottie the app can time: 60 frames at 30fps = 2000ms. */
internal const val LOWER_THIRD_LOTTIE =
    """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":1920,"h":1080,"layers":[]}"""

/** A folder holding [names] as real Lottie files, plus one file that is not a preset. */
internal fun lottieFolder(vararg names: String): File =
    Files.createTempDirectory("cp-lowerthird-tab").toFile().apply {
        names.forEach { File(this, "$it.json").writeText(LOWER_THIRD_LOTTIE) }
        File(this, "notes.txt").writeText("not a preset")
    }

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/** What the tab reported back, so a test asserts on the choice rather than on a stub. */
internal class LowerThirdReports {
    /** presetId, presetLabel, pauseAtFrame, pauseDurationMs — as the schedule would be given them. */
    val scheduled = mutableListOf<List<Any>>()
    /** presetName of each go-live, in order. */
    val live = mutableListOf<String>()
    /** The json handed to the output for the most recent go-live. */
    var liveJson: String? = null
    var settingsChanges = 0
}

/**
 * Composes `LowerThirdTab` over [folder] and runs [block].
 *
 * Settings are fed back into the tab on every change, as `MainDesktop` does, so a control's effect
 * is visible on the next frame.
 */
@OptIn(ExperimentalTestApi::class)
internal fun lowerThirdTab(
    folder: File? = lottieFolder("Welcome", "Speaker Name"),
    block: ComposeUiTest.(reports: LowerThirdReports) -> Unit,
) {
    val reports = LowerThirdReports()
    try {
        runComposeUiTest {
            setContent {
                var settings by remember {
                    mutableStateOf(
                        AppSettings(
                            streamingSettings = StreamingSettings(
                                lowerThirdFolder = folder?.absolutePath ?: ""
                            )
                        )
                    )
                }
                MaterialTheme {
                    LowerThirdTab(
                        appSettings = settings,
                        onSettingsChange = { transform ->
                            settings = transform(settings)
                            reports.settingsChanges++
                        },
                        onAddToSchedule = { id, label, pause, pauseMs ->
                            reports.scheduled += listOf(id, label, pause, pauseMs)
                        },
                        onGoLive = { json, _, _, _, presetName ->
                            reports.live += presetName
                            reports.liveJson = json
                        },
                    )
                }
            }
            block(reports)
        }
    } finally {
        folder?.deleteRecursively()
    }
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object LowerThirdLabel {
    const val NO_PRESETS = "No presets saved yet"
    const val SELECT_PRESET = "Select a preset to preview"
    const val GO_LIVE = "Go Live"
    const val ADD_TO_SCHEDULE = "Add to Schedule"
    const val PAUSE = "Pause"
    const val REMOVE = "Remove"
    const val GENERATE = "Generate"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────
// (renderedText/showsExactly/showsContainingText are shared — see TabRenderedText.kt)

/** A button, addressed by the content description its tooltip gives it. */
internal fun ComposeUiTest.ltButton(label: String): SemanticsNodeInteraction =
    onNodeWithContentDescription(label)

internal fun ComposeUiTest.hasLtButton(label: String): Boolean =
    onAllNodesWithContentDescription(label)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

/** Selects a preset from the list by its name. */
internal fun ComposeUiTest.selectPreset(name: String) {
    onAllNodesWithText(name)[0].performClick()
    waitForIdle()
}
