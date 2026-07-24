package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/**
 * The Blackmagic ATEM settings tab composes from default [AppSettings] with no live connection.
 * Asserting its connection section renders guards the tab against a regression that throws or blanks
 * when the ATEM integration is configured with defaults (the common case: no device attached yet).
 */
@OptIn(ExperimentalTestApi::class)
class AtemSettingsTabTest {

    @Test
    fun `the tab composes and shows the ATEM connection section`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AtemSettingsTab(settings = AppSettings(), onSettingsChange = {})
            }
        }
        onAllNodesWithText("Blackmagic ATEM", substring = true).onFirst()
            .assertExists("the ATEM connection section must render when the tab opens")
    }
}
