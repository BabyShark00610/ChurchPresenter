package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.viewmodel.OBSWebSocketManager
import kotlin.test.Test

/**
 * The OBS Studio settings tab composes from default [AppSettings] with an unconnected
 * [OBSWebSocketManager] (constructing one opens no socket — it connects only on connect()).
 * Asserting the connection section renders guards against a regression that throws or blanks the
 * tab when opened with defaults.
 */
@OptIn(ExperimentalTestApi::class)
class OBSSettingsTabTest {

    @Test
    fun `the tab composes and shows the OBS connection section`() = runComposeUiTest {
        val obsManager = OBSWebSocketManager()
        setContent {
            MaterialTheme {
                OBSSettingsTab(
                    settings = AppSettings(),
                    onSettingsChange = {},
                    obsManager = obsManager,
                )
            }
        }
        onAllNodesWithText("OBS WebSocket", substring = true).onFirst()
            .assertExists("the OBS connection section must render when the tab opens")
    }
}
