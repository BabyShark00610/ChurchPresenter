package org.churchpresenter.app.churchpresenter.viewmodel

import org.churchpresenter.app.churchpresenter.data.SongItem
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.SongSettings
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The title slide as the song's first section.
 *
 * It used to be a row the operator had to find and click in the lyrics panel, pushed by a code path
 * of its own that skipped the play count and the Instance Link mirror, and left the presenter's list
 * one longer than the view model's — so the arrow key off the title slide landed on verse *two*.
 * Building it here, where a song becomes its list of slides, is what makes it just another section:
 * going live opens on it, one press down moves into the words, and the indices agree.
 */
class LyricSectionsTitleSlideTest {

    private val song = SongItem(
        number = "12",
        title = "Amazing Grace",
        songbook = "Hymnal",
        author = "Newton",
        lyrics = listOf("[Verse 1]", "V1 L1", "V1 L2", "[Verse 2]", "V2 L1"),
    )

    private fun sections(enabled: Boolean, showNumber: Boolean = true) =
        SongsViewModel(
            AppSettings(
                songSettings = SongSettings(
                    titleSlideEnabled = enabled,
                    titleSlideShowSongNumber = showNumber,
                )
            )
        ).getLyricSections(song)

    @Test
    fun `the first section is the title slide`() {
        val first = sections(enabled = true).first()
        assertEquals(Constants.SECTION_TYPE_TITLE_SLIDE, first.type)
    }

    @Test
    fun `the verses follow it, unchanged and in order`() {
        val withSlide = sections(enabled = true)
        val without = sections(enabled = false)
        assertEquals(without.size + 1, withSlide.size)
        assertEquals(without.map { it.lines }, withSlide.drop(1).map { it.lines })
    }

    @Test
    fun `switching it off leaves the song exactly as it was`() {
        assertTrue(sections(enabled = false).none { it.type == Constants.SECTION_TYPE_TITLE_SLIDE })
    }

    @Test
    fun `the title slide carries the number, the title and the credit`() {
        val first = sections(enabled = true).first()
        assertEquals(listOf("12 – Amazing Grace", "Newton"), first.lines)
    }

    @Test
    fun `the song number can be left off the heading`() {
        val first = sections(enabled = true, showNumber = false).first()
        assertEquals("Amazing Grace", first.lines.first())
    }

    @Test
    fun `the title slide is not what the end-of-song mark hangs off`() {
        // `isLastSection` is stamped before the prepend, so it stays on the real final verse. Were it
        // to land on the title slide, a one-verse song would show the mark on its opening slide.
        val withSlide = sections(enabled = true)
        assertTrue(withSlide.last().isLastSection)
        assertTrue(withSlide.none { it.type == Constants.SECTION_TYPE_TITLE_SLIDE && it.isLastSection })
    }

    @Test
    fun `it belongs to its song, so it presents on that song's own background`() {
        assertEquals(song.songId, sections(enabled = true).first().songId)
    }

    @Test
    fun `a song with no lyrics gets no title slide either`() {
        // Nothing to introduce — and prepending here would put a lone heading on screen for a song
        // the operator cannot advance through.
        val empty = SongsViewModel(AppSettings(songSettings = SongSettings(titleSlideEnabled = true)))
            .getLyricSections(song.copy(lyrics = emptyList()))
        assertTrue(empty.isEmpty())
    }
}
