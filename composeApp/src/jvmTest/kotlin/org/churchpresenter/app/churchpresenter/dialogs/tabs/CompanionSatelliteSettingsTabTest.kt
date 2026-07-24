package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/**
 * The Companion Satellite settings tab composes from default [AppSettings] with no live view model.
 * Asserting its header renders guards against a regression that throws or blanks the tab before any
 * satellite connection has been configured (the default state).
 */
@OptIn(ExperimentalTestApi::class)
class CompanionSatelliteSettingsTabTest {

    @Test
    fun `the tab composes and offers to add a connection`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                CompanionSatelliteSettingsTab(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    viewModel = null,
                )
            }
        }
        // With no satellite configured (the default), the always-visible control is the add button.
        onAllNodesWithText("Add Connection", substring = true).onFirst()
            .assertExists("the add-connection control must render when the tab opens with no connections")
    }
}
