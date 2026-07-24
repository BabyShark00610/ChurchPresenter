package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/**
 * The lower-third settings tab composes from default [AppSettings]. With no Lottie directory chosen
 * yet (the default), the Lottie Files section still renders its header — asserting it guards against
 * a regression that throws or blanks the tab before a directory has been picked.
 */
@OptIn(ExperimentalTestApi::class)
class LowerThirdSettingsTabTest {

    @Test
    fun `the tab composes and shows the Lottie files section`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                LowerThirdSettingsTab(settings = AppSettings(), onSettingsChange = {})
            }
        }
        onNodeWithText("Lottie Files", substring = true)
            .assertExists("the Lottie files section must render when the tab opens")
    }
}
