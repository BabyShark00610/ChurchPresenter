package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/**
 * The background settings tab is one of the largest pure-View surfaces in the app. This composes the
 * whole tab from default [AppSettings] — exercising every per-content-type background section and
 * its controls — and asserts the first section renders, guarding against a regression that makes the
 * tab throw or come up empty when opened.
 */
@OptIn(ExperimentalTestApi::class)
class BackgroundSettingsTabTest {

    @Test
    fun `the tab composes and shows its first section`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                BackgroundSettingsTab(settings = AppSettings(), onSettingsChange = {})
            }
        }
        onNodeWithText("Default Background", substring = true)
            .assertExists("the default-background section must render when the tab opens")
    }
}
