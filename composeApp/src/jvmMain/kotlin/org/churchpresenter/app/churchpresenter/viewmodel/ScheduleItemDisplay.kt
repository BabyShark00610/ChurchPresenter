package org.churchpresenter.app.churchpresenter.viewmodel

import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.utils.Constants

/**
 * How a schedule item is labelled in the list — its type glyph, its grey detail line, and an
 * announcement timer's preview. Extracted from ScheduleTab (the glyph `when` was duplicated at two
 * sites) so the per-type mapping is exhaustive over the sealed [ScheduleItem] and tested in one place.
 */

/** The single-glyph type indicator shown on a schedule row and its drag preview. */
internal fun scheduleItemGlyph(item: ScheduleItem): String = when (item) {
    is ScheduleItem.SongItem -> "♪"
    is ScheduleItem.BibleVerseItem -> "✝"
    is ScheduleItem.LabelItem -> "🏷"
    is ScheduleItem.PictureItem -> "📷"
    is ScheduleItem.PresentationItem -> "📊"
    is ScheduleItem.MediaItem -> "🎬"
    is ScheduleItem.LowerThirdItem -> "▼"
    is ScheduleItem.AnnouncementItem -> "📢"
    is ScheduleItem.WebsiteItem -> "🌐"
    is ScheduleItem.SceneItem -> "🎬"
    is ScheduleItem.DictionaryItem -> "📖"
}

/**
 * The grey secondary line under a schedule row, or null for types that show none (or whose detail is
 * rendered with a string resource in the View, e.g. lower-third pause duration). A long Bible verse
 * is truncated to 100 chars with an ellipsis.
 */
internal fun scheduleItemDetailText(item: ScheduleItem): String? = when (item) {
    is ScheduleItem.BibleVerseItem ->
        item.verseText.take(100) + if (item.verseText.length > 100) "..." else ""
    is ScheduleItem.PictureItem -> item.folderPath
    is ScheduleItem.PresentationItem -> "${item.fileType.uppercase()} - ${item.filePath}"
    is ScheduleItem.MediaItem -> "${item.mediaType.uppercase()} - ${item.mediaUrl}"
    else -> null
}

/**
 * The h:m:s preview for an announcement timer, or null when there is nothing fixed to preview: a
 * count-up timer and the live clock display only have a value once triggered. A clock-target timer
 * previews the target time-of-day; a plain duration timer previews its minutes:seconds.
 */
internal fun announcementTimerSubtext(item: ScheduleItem.AnnouncementItem): String? = when (item.timerMode) {
    Constants.TIMER_MODE_CLOCK ->
        "%02d:%02d:%02d".format(item.targetHour, item.targetMinute, item.targetSecond)
    Constants.TIMER_MODE_COUNT_UP, Constants.TIMER_MODE_CLOCK_DISPLAY -> null
    else -> "%02d:%02d".format(item.timerMinutes, item.timerSeconds)
}
