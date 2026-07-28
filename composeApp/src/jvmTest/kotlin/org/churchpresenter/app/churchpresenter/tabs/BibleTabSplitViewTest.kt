@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.viewmodel.BibleViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Split-browse mode: the right-hand live-chapter panel that lets the operator read ahead in the
 * chapter that is on screen while browsing somewhere else on the left.
 *
 * See `BibleTabTestSupport.kt` for the harness.
 */
class BibleTabSplitViewTest {

    /** The harness with split-browse mode turned on. */
    private fun splitBibleTab(
        block: ComposeUiTest.(vm: BibleViewModel, reports: BibleReports) -> Unit,
    ) = bibleTab(
        settings = { it.copy(bibleSettings = it.bibleSettings.copy(splitBrowseMode = true)) },
        block = block,
    )

    @Test
    fun `the live panel is not shown unless split browse is on`() = bibleTab { _, _ ->
        assertEquals(
            1,
            countOnScreen("1. In the beginning God created the heaven and the earth."),
            "one copy of the verse — the browser's",
        )
    }

    @Test
    fun `split browse shows the live chapter beside the browser`() = splitBibleTab { _, _ ->
        // Seeded from the opening selection so the panel isn't blank before the first go-live.
        assertEquals(
            2,
            countOnScreen("1. In the beginning God created the heaven and the earth."),
            "the same verse in both the browser and the live panel",
        )
    }

    @Test
    fun `the live panel keeps showing the live chapter while the browser moves away`() =
        splitBibleTab { vm, _ ->
            onNodeWithText("John").performClick()
            waitForIdle()

            assertEquals(2, vm.selectedBookIndex.value, "the browser moved to John")
            assertTrue(
                showsExactly("1. In the beginning was the Word."),
                "John 1 is in the browser",
            )
            assertEquals(
                1,
                countOnScreen("1. In the beginning God created the heaven and the earth."),
                "and Genesis 1 is still in the live panel",
            )
        }

    @Test
    fun `clicking a verse in the live panel puts it on screen`() = splitBibleTab { _, reports ->
        livePanelVerse("3. And God said, Let there be light.").performClick()
        waitForIdle()

        val live = reports.live?.single()
        assertEquals("Genesis", live?.bookName)
        assertEquals(1, live?.chapter)
        assertEquals(3, live?.verseNumber, "the verse clicked in the live panel went live")
        assertEquals(
            listOf(Presenting.BIBLE),
            reports.presenting,
            "and the host was told the Bible is presenting",
        )
    }
}
