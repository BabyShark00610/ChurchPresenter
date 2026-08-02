package org.churchpresenter.app.churchpresenter.tabs

import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class BibleTabColumnWidthTest {

    @Test
    fun `saving column widths while windowed updates only the windowed layout`() {
        val before = AppSettings()
        val after = withBibleColumnWidths(before, isMaximized = false, bookWidthDp = 220, chapterWidthDp = 140)

        assertEquals(220, after.windowedLayout.bibleColWidthBook)
        assertEquals(140, after.windowedLayout.bibleColWidthChapter)
        assertEquals(before.maximizedLayout.bibleColWidthBook, after.maximizedLayout.bibleColWidthBook)
        assertEquals(before.maximizedLayout.bibleColWidthChapter, after.maximizedLayout.bibleColWidthChapter)
    }

    @Test
    fun `saving column widths while maximized updates only the maximized layout`() {
        val before = AppSettings()
        val after = withBibleColumnWidths(before, isMaximized = true, bookWidthDp = 220, chapterWidthDp = 140)

        assertEquals(220, after.maximizedLayout.bibleColWidthBook)
        assertEquals(140, after.maximizedLayout.bibleColWidthChapter)
        assertEquals(before.windowedLayout.bibleColWidthBook, after.windowedLayout.bibleColWidthBook)
        assertEquals(before.windowedLayout.bibleColWidthChapter, after.windowedLayout.bibleColWidthChapter)
    }

    @Test
    fun `saving column widths never touches the split panel width`() {
        val before = AppSettings()
        val after = withBibleColumnWidths(before, isMaximized = false, bookWidthDp = 220, chapterWidthDp = 140)

        assertEquals(before.windowedLayout.splitLivePanelWidth, after.windowedLayout.splitLivePanelWidth)
    }

    @Test
    fun `saving the split panel width while windowed updates only the windowed layout`() {
        val before = AppSettings()
        val after = withBibleSplitPanelWidth(before, isMaximized = false, widthDp = 360)

        assertEquals(360, after.windowedLayout.splitLivePanelWidth)
        assertEquals(before.maximizedLayout.splitLivePanelWidth, after.maximizedLayout.splitLivePanelWidth)
    }

    @Test
    fun `saving the split panel width while maximized updates only the maximized layout`() {
        val before = AppSettings()
        val after = withBibleSplitPanelWidth(before, isMaximized = true, widthDp = 360)

        assertEquals(360, after.maximizedLayout.splitLivePanelWidth)
        assertEquals(before.windowedLayout.splitLivePanelWidth, after.windowedLayout.splitLivePanelWidth)
    }

    @Test
    fun `saving the split panel width never touches the column widths`() {
        val before = AppSettings()
        val after = withBibleSplitPanelWidth(before, isMaximized = true, widthDp = 360)

        assertEquals(before.maximizedLayout.bibleColWidthBook, after.maximizedLayout.bibleColWidthBook)
        assertEquals(before.maximizedLayout.bibleColWidthChapter, after.maximizedLayout.bibleColWidthChapter)
    }
}
