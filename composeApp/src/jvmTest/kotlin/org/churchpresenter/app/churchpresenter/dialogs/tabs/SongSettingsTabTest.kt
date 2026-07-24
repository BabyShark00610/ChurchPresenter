package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/**
 * The song settings tab is the largest pure-View surface in the app (~1,400 lines of control
 * wiring). This composes the whole tab from default [AppSettings] with no presenter attached and
 * asserts its first section renders — guarding against a regression that throws or blanks the tab
 * when opened with defaults.
 */
@OptIn(ExperimentalTestApi::class)
class SongSettingsTabTest {

    @Test
    fun `the tab composes and shows the title-slide section`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                SongSettingsTab(settings = AppSettings(), onSettingsChange = {}, presenterManager = null)
            }
        }
        onAllNodesWithText("Song Title Slide", substring = true).onFirst()
            .assertExists("the title-slide section must render when the tab opens")
    }
}
