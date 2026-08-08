@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.data.RemoteClientManager
import org.churchpresenter.app.churchpresenter.data.SettingsManager
import org.churchpresenter.app.churchpresenter.dialogs.OptionsDialogContent
import org.churchpresenter.app.churchpresenter.server.CompanionServer
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import java.io.File
import kotlin.test.Test

class AppPreviewSettingsScreenshotTest {

    private fun settingsTab(name: String, tab: Int) {
        TestSingletons.latchSkikoHostOs()
        TestSingletons.latchToTestHome()
        val appSettings = library()
        THEMES.forEach { (suffix, mode) ->
            runSkikoComposeUiTest(size = Size(1400f, 900f), density = Density(1f)) {
                setContent {
                    OptionsDialogContent(
                        theme = mode,
                        settingsManager = SettingsManager(),
                        companionServer = CompanionServer(),
                        remoteClientManager = RemoteClientManager(),
                        presenterManager = PresenterManager(),
                        onDismiss = {},
                        initialTab = tab,
                        initialSettings = appSettings,
                        detectScreens = { emptyList() },
                    )
                }
                waitForIdle()
                // The System tab scans the song folder in the background: until it finishes it shows
                // "Scanning folder…", and then replaces it with the songbook tally — which is both
                // different text and a taller block, so everything under it moves. A capture taken
                // mid-scan is a different picture every run. The condition starts out true and flips
                // when the scan ends, so this waits on the scan finishing, not on a clock; on the
                // tabs that never scan there is nothing to wait for and it returns at once.
                waitUntil(timeoutMillis = RENDER_TIMEOUT_MS) {
                    onAllNodesWithText(SCANNING, substring = true)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isEmpty()
                }
                captureTo(File("$SCREENSHOT_ROOT/previewApp/settings_${name}_$suffix.png"))
            }
        }
    }

    @Test
    fun appearance() = settingsTab("appearance", 0)

    @Test
    fun bible() = settingsTab("bible", 1)

    @Test
    fun song() = settingsTab("song", 2)

    @Test
    fun background() = settingsTab("background", 3)

    @Test
    fun projection() = settingsTab("projection", 4)

    @Test
    fun `lower third`() = settingsTab("lower_third", 5)

    @Test
    fun server() = settingsTab("server", 6)

    @Test
    fun `stage monitor`() = settingsTab("stage_monitor", 7)

    @Test
    fun atem() = settingsTab("atem", 8)

    @Test
    fun dictionary() = settingsTab("dictionary", 9)

    @Test
    fun `companion satellite`() = settingsTab("companion_satellite", 10)

    private companion object {
        /** Shown while the song folder is being read; gone once the tally replaces it. */
        const val SCANNING = "Scanning folder"
    }
}
