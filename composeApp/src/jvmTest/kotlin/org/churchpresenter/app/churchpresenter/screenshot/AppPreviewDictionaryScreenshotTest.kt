@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.churchpresenter.app.churchpresenter.tabs.Tabs
import kotlin.test.Test

class AppPreviewDictionaryScreenshotTest {

    @Test
    fun `the dictionary tab`() = appPreview("dictionary", Tabs.DICTIONARY) {
        onAllNodes(hasSetTextAction())[0].performTextReplacement("H2617")
        waitForIdle()
        // the search field also reads "H2617", so the row is addressed by its transliteration
        onAllNodes(hasText("chêçêd", substring = true))[0].performClick()
        waitForIdle()
        // Clearing the search puts all 14,197 entries back in the list; the selection survives it.
        onAllNodes(hasSetTextAction())[0].performTextReplacement("")
        waitForIdle()
        goLive()
    }
}
