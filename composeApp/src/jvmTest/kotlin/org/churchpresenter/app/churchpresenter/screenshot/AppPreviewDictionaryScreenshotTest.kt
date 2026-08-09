@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
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
        selectUntilFullyScanned()
        // Clearing the search puts all 14,197 entries back in the list; the selection survives it.
        onAllNodes(hasSetTextAction())[0].performTextReplacement("")
        waitForIdle()
        goLive()
    }

    /**
     * Selects the entry, and keeps re-selecting it until In Scripture reports the whole of it.
     *
     * The interlinear index loads in the background, and `DictionaryViewModel` queries it once, at
     * the moment an entry is selected — so a selection made while it is still loading gets whatever
     * had been indexed by then and keeps it. Observed here across runs: 8, 18, 80, 181 and 211
     * verses for the same entry, which is why this screenshot was a different picture on every
     * recording, by 4.2% of the image.
     *
     * Re-selecting re-queries, so the complete count is the load's own positive signal. It is a
     * fixed number because the data is fixed; the wait ends on it and the check below fails loudly
     * if it never arrives rather than capturing something half-scanned.
     */
    private fun ComposeUiTest.selectUntilFullyScanned() {
        val deadline = System.currentTimeMillis() + RENDER_TIMEOUT_MS
        var seen: String? = null
        while (System.currentTimeMillis() < deadline) {
            onAllNodes(hasText("chêçêd", substring = true))[0].performClick()
            waitForIdle()
            seen = onAllNodesWithText(SCRIPTURE_COUNT_PREFIX, substring = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
            if (seen == FULLY_SCANNED) return
        }
        error("In Scripture never reached \"$FULLY_SCANNED\" — it stopped at \"$seen\"")
    }

    private companion object {
        const val SCRIPTURE_COUNT_PREFIX = "Found in"

        /** What H2617 settles on once the whole interlinear index is loaded. */
        const val FULLY_SCANNED = "Found in 211 verses"
    }
}
