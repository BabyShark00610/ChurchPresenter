@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleTabHoldPillTest {

    private fun ComposeUiTest.clickable() =
        onAllNodes(hasText("⌘/Ctrl Shift") and hasClickAction())
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    @Test
    fun `clicking the hold pill toggles Bible Hold and notifies Instance Link`() {
        val presenter = PresenterManager()
        var sentHold: Boolean? = null
        bibleTab(presenter = presenter, onInstanceLinkSendBibleHold = { sentHold = it }) { _, _ ->
            assertFalse(presenter.bibleHold.value)

            onNodeWithText("⌘/Ctrl Shift").performClick()
            waitForIdle()

            assertTrue(presenter.bibleHold.value)
            assertEquals(true, sentHold)

            onNodeWithText("⌘/Ctrl Shift").performClick()
            waitForIdle()

            assertFalse(presenter.bibleHold.value)
            assertEquals(false, sentHold)
        }
    }

    @Test
    fun `with no presenter manager the pill is not clickable`() = bibleTab { _, _ ->
        assertFalse(clickable())
    }

    @Test
    fun `in split browse mode the pill is not clickable even with a presenter manager`() {
        val presenter = PresenterManager()
        bibleTab(
            settings = { it.copy(bibleSettings = it.bibleSettings.copy(splitBrowseMode = true)) },
            presenter = presenter,
        ) { _, _ ->
            assertFalse(clickable())
        }
    }

    @Test
    fun `with no Instance Link callback wired, the click still toggles Bible Hold locally`() {
        val presenter = PresenterManager()
        bibleTab(presenter = presenter) { _, _ ->
            onNodeWithText("⌘/Ctrl Shift").performClick()
            waitForIdle()

            assertTrue(presenter.bibleHold.value)
        }
    }
}
