package org.churchpresenter.app.churchpresenter.viewmodel

import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How schedule rows are labelled — the type glyph, the grey detail line, and the timer preview.
 * The glyph `when` was duplicated at two sites in ScheduleTab; the detail line and timer preview
 * carried real formatting (verse truncation, uppercase type tags, the count-up/clock "no preview"
 * rule) that was never tested.
 */
class ScheduleItemDisplayTest {

    // ── glyph (exhaustive over the sealed type) ────────────────────────────────

    @Test
    fun `every schedule item type has its own glyph`() {
        val cases = listOf(
            ScheduleItem.SongItem(id = "1", songNumber = 1, title = "t", songbook = "b") to "♪",
            ScheduleItem.BibleVerseItem(id = "2", bookName = "John", chapter = 3, verseNumber = 16, verseText = "x") to "✝",
            ScheduleItem.LabelItem(id = "3", text = "l", textColor = "#fff", backgroundColor = "#000") to "🏷",
            ScheduleItem.PictureItem(id = "4", folderPath = "/p", folderName = "p", imageCount = 1) to "📷",
            ScheduleItem.PresentationItem(id = "5", filePath = "/d.pptx", fileName = "d", slideCount = 1, fileType = "pptx") to "📊",
            ScheduleItem.MediaItem(id = "6", mediaUrl = "/m.mp4", mediaTitle = "m", mediaType = "local") to "🎬",
            ScheduleItem.LowerThirdItem(id = "7", presetId = "p", presetLabel = "p", pauseAtFrame = false, pauseDurationMs = 0L) to "▼",
            ScheduleItem.AnnouncementItem(id = "8", text = "a") to "📢",
            ScheduleItem.WebsiteItem(id = "9", url = "https://x") to "🌐",
            ScheduleItem.SceneItem(id = "10", sceneId = "s", sceneName = "s") to "🎬",
            ScheduleItem.DictionaryItem(id = "11", number = "1", word = "w", transliteration = "t", definition = "d") to "📖",
        )
        for ((item, glyph) in cases) {
            assertEquals(glyph, scheduleItemGlyph(item), item::class.simpleName)
        }
    }

    // ── detail line ────────────────────────────────────────────────────────────

    @Test fun `a short bible verse shows in full`() =
        assertEquals("For God so loved", scheduleItemDetailText(
            ScheduleItem.BibleVerseItem(id = "1", bookName = "John", chapter = 3, verseNumber = 16, verseText = "For God so loved")))

    @Test fun `a long bible verse is truncated to 100 chars with an ellipsis`() {
        val verse = "a".repeat(150)
        val detail = scheduleItemDetailText(
            ScheduleItem.BibleVerseItem(id = "1", bookName = "John", chapter = 3, verseNumber = 16, verseText = verse))
        assertEquals(103, detail!!.length, "100 chars + ellipsis")
        assertTrue(detail.endsWith("..."))
    }

    @Test fun `a presentation detail is its uppercased type and path`() =
        assertEquals("PPTX - /decks/a.pptx", scheduleItemDetailText(
            ScheduleItem.PresentationItem(id = "1", filePath = "/decks/a.pptx", fileName = "a", slideCount = 3, fileType = "pptx")))

    @Test fun `a media detail is its uppercased type and url`() =
        assertEquals("LOCAL - /clips/a.mp4", scheduleItemDetailText(
            ScheduleItem.MediaItem(id = "1", mediaUrl = "/clips/a.mp4", mediaTitle = "a", mediaType = "local")))

    @Test fun `a type with no simple detail line returns null`() =
        assertNull(scheduleItemDetailText(ScheduleItem.SongItem(id = "1", songNumber = 1, title = "t", songbook = "b")))

    // ── announcement timer preview ─────────────────────────────────────────────

    private fun timer(mode: String) = ScheduleItem.AnnouncementItem(
        id = "1", text = "a", isTimer = true, timerMode = mode,
        timerMinutes = 5, timerSeconds = 9, targetHour = 9, targetMinute = 30, targetSecond = 5,
    )

    @Test fun `a clock-target timer previews the target time of day`() =
        assertEquals("09:30:05", announcementTimerSubtext(timer(Constants.TIMER_MODE_CLOCK)))

    @Test fun `a duration timer previews minutes and seconds`() =
        assertEquals("05:09", announcementTimerSubtext(timer(Constants.TIMER_MODE_DURATION)))

    @Test fun `a count-up timer has no fixed preview`() =
        assertNull(announcementTimerSubtext(timer(Constants.TIMER_MODE_COUNT_UP)))

    @Test fun `a live clock display has no fixed preview`() =
        assertNull(announcementTimerSubtext(timer(Constants.TIMER_MODE_CLOCK_DISPLAY)))
}
