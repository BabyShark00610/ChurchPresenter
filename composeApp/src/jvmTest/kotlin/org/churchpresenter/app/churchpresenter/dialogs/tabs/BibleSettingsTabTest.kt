package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test

/** First cut: does this tab compose at all headless? It enumerates system fonts and builds a ViewModel. */
@OptIn(ExperimentalTestApi::class)
class BibleSettingsTabTest {

    @Test
    fun `the tab composes and shows its sections`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                BibleSettingsTab(settings = AppSettings(), onSettingsChange = {})
            }
        }

        listOf("Bible Selection", "Primary Bible Text").forEach { title ->
            onAllNodesWithText(title, substring = true).onFirst().assertExists("$title must render")
        }
    }
}
