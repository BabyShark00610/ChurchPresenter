@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.ProjectionSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the parts of the tab the grid and browser-source suites leave alone: the audio card, the
 * custom VLC path, and the captions inside the Content Outputs dialog.
 *
 * The audio device list comes from VLC and so differs from machine to machine. Nothing here asserts
 * a particular device — only the entry every machine has ("System Default"), and the fallback the
 * tab uses when the stored device id matches nothing currently plugged in, which is the branch that
 * actually matters when a USB interface is unplugged between services.
 */
class ProjectionSettingsTabAudioAndLabelsTest {

    private fun settingsWith(change: ProjectionSettings.() -> ProjectionSettings): AppSettings =
        AppSettings().let { it.copy(projectionSettings = it.projectionSettings.change()) }

    // ── Audio output ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `the audio device dropdown starts on the system default`() = projectionTab { get ->
        assertEquals("", get().projectionSettings.audioOutputDeviceId, "no device chosen out of the box")
        onNodeWithText("System Default").assertExists("so the dropdown reads System Default")
    }

    @Test
    fun `the audio device dropdown offers the system default`() = projectionTab { _ ->
        onNodeWithText("System Default").performScrollTo().performClick()
        waitForIdle()
        // The closed button and the menu's own entry — the machine's real devices join them, and
        // which those are is not asserted here because it differs per machine.
        onAllNodesWithText("System Default").assertCountEquals(2)
    }

    @Test
    fun `picking the system default clears any stored device`() = projectionTab { get ->
        onNodeWithText("System Default").performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("System Default")[1].performClick()
        waitForIdle()
        assertEquals("", get().projectionSettings.audioOutputDeviceId, "the default stores an empty id")
        onNodeWithText("System Default").assertExists()
    }

    /**
     * A device that is no longer present — a USB interface unplugged since the last service — must
     * not leave the dropdown blank; it falls back to naming the system default.
     */
    @Test
    fun `a stored device that is no longer present falls back to the system default`() {
        projectionTab(initial = settingsWith { copy(audioOutputDeviceId = "usb-interface-that-is-gone") }) { get ->
            onNodeWithText("System Default").assertExists("an unknown device must not render blank")
            assertEquals(
                "usb-interface-that-is-gone",
                get().projectionSettings.audioOutputDeviceId,
                "but the stored id itself is left alone, so the device works again when replugged",
            )
        }
    }

    // ── Custom VLC path ─────────────────────────────────────────────────────────────────────────

    /**
     * The path box is **read-only** — its `onValueChange` is empty and it publishes no set-text
     * action — so it cannot be typed into. The only way to change it is the Browse button, which
     * opens a native directory chooser and so is never clicked here. What is testable is that the
     * box shows the right thing, which is the part an operator reads.
     */
    @Test
    fun `the VLC path box shows the stored path`() {
        projectionTab(initial = settingsWith { copy(vlcPath = "/opt/custom-vlc/lib") }) { _ ->
            onNodeWithText("Custom VLC path").assertExists("the row must be captioned")
            onNodeWithText("/opt/custom-vlc/lib").assertExists("and show the configured installation")
        }
    }

    @Test
    fun `a stored path replaces the auto-detected one`() {
        // With nothing stored the box shows whatever VLC this machine has, which differs per
        // machine; what is asserted is that a stored path takes precedence over it.
        projectionTab(initial = settingsWith { copy(vlcPath = "/opt/only-this-one/lib") }) { get ->
            onNodeWithText("/opt/only-this-one/lib").assertExists()
            assertEquals(
                "/opt/only-this-one/lib",
                get().projectionSettings.vlcPath,
                "and the stored value is what is shown",
            )
        }
    }

    @Test
    fun `the path box cannot be typed into`() {
        projectionTab(initial = settingsWith { copy(vlcPath = "/opt/custom-vlc/lib") }) { _ ->
            onAllNodes(hasSetTextAction() and hasText("/opt/custom-vlc/lib"))
                .assertCountEquals(0)
        }
    }

    // ── Content Outputs dialog captions ─────────────────────────────────────────────────────────

    @Test
    fun `the content outputs dialog groups its toggles under captions`() = projectionTab { _ ->
        gridButton(Grid.contentOutputs(row = 0)).performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("QUICK SELECT").assertExists("the Select All / Clear All pair is captioned")
        onNodeWithText("CONTENT").assertExists("as is the content group")
        onNodeWithText("BACKGROUNDS").assertExists("and the background group")
        onNodeWithText("THIS OUTPUT SHOWS").assertExists("and the preview")
    }

    @Test
    fun `the dialog's preview names both language modes`() = projectionTab { _ ->
        gridButton(Grid.contentOutputs(row = 0)).performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("Bible · Both").assertExists("the preview must name the Bible mode")
        onNodeWithText("Songs · Both").assertExists("and the Songs mode")
    }

    @Test
    fun `the Bible language dropdown offers every mode`() = projectionTab { get ->
        gridButton(Grid.contentOutputs(row = 0)).performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("Both")[0].performClick()
        waitForIdle()
        for (option in listOf("Off", "Bible 1", "Bible 2")) {
            onNodeWithText(option).assertExists("the Bible dropdown must offer $option")
        }
        onNodeWithText("Bible 1").performClick()
        waitForIdle()
        assertEquals(
            org.churchpresenter.app.churchpresenter.utils.Constants.SONG_LANG_PRIMARY,
            get().projectionSettings.screenAssignments[0].bibleMode,
            "picking Bible 1 must be stored",
        )
        onNodeWithText("Bible · Bible 1").assertExists("and be named in the preview")
    }

    @Test
    fun `the Songs language dropdown offers every mode`() = projectionTab { _ ->
        gridButton(Grid.contentOutputs(row = 0)).performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("Both")[1].performClick()
        waitForIdle()
        for (option in listOf("Off", "Language 1", "Language 2")) {
            onNodeWithText(option).assertExists("the Songs dropdown must offer $option")
        }
    }

    // ── Remove confirmation ─────────────────────────────────────────────────────────────────────

    @Test
    fun `the remove confirmation is titled`() {
        val withOutput = settingsWith { copy(browserSourceOutputs = listOf(ScreenAssignment())) }
        projectionTab(initial = withOutput) { _ ->
            onNodeWithText("Remove").performScrollTo().performClick()
            waitForIdle()
            onNodeWithText("Confirm Delete").assertExists("the confirmation must be titled")
            onNodeWithText("Are you sure you want to remove Browser Source 1?").assertExists()
            onNodeWithText("Cancel").assertExists()
        }
    }

    // ── Stepper arrows ──────────────────────────────────────────────────────────────────────────

    /**
     * Every stepper field publishes increment/decrement arrows, and every one of them lays out zero
     * pixels wide — so they draw nothing and cannot be clicked. The cause is in
     * `NumberSettingsTextField` itself (its `BasicTextField` takes `fillMaxWidth()`, leaving the
     * arrow column no room), so it affects every numeric field in the app rather than this tab.
     * Pinned as present-but-unusable rather than driven: a test that clicked them would be asserting
     * a defect works.
     */
    @Test
    fun `the stepper arrows are published but laid out unusably`() = projectionTab { _ ->
        for (description in listOf("Increment", "Decrement")) {
            val widths = onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .map { it.boundsInRoot.width }
            assertEquals(
                5,
                widths.size,
                "one $description arrow per stepper field: lower-third height plus four window offsets",
            )
            assertEquals(
                true,
                widths.all { it == 0f },
                "every $description arrow is zero pixels wide, so none of them can be clicked",
            )
        }
    }
}
