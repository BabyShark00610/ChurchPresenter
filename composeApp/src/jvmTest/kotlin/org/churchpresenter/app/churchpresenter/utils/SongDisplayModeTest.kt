package org.churchpresenter.app.churchpresenter.utils

import org.churchpresenter.app.churchpresenter.data.settings.SongSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `isSongLineMode` decides whether arrow-key line navigation, the nav hint and per-line highlighting
 * are active. It was three identical inline OR-chains in SongsTab; the rule is "line mode if ANY of
 * the four output surfaces is in line mode", so each surface is checked independently here.
 */
class SongDisplayModeTest {

    private val verse = Constants.SONG_DISPLAY_MODE_VERSE
    private val line = Constants.SONG_DISPLAY_MODE_LINE

    private fun settings(
        fullscreen: String = verse,
        lowerThird: String = verse,
        lookAhead: String = verse,
        lowerThirdLookAhead: String = verse,
    ) = SongSettings(
        fullscreenDisplayMode = fullscreen,
        lowerThirdDisplayMode = lowerThird,
        lookAheadDisplayMode = lookAhead,
        lowerThirdLookAheadDisplayMode = lowerThirdLookAhead,
    )

    @Test
    fun `all surfaces in verse mode is not line mode`() =
        assertFalse(isSongLineMode(settings()))

    @Test
    fun `the fullscreen surface alone in line mode is line mode`() =
        assertTrue(isSongLineMode(settings(fullscreen = line)))

    @Test
    fun `the lower-third surface alone in line mode is line mode`() =
        assertTrue(isSongLineMode(settings(lowerThird = line)))

    @Test
    fun `the look-ahead surface alone in line mode is line mode`() =
        assertTrue(isSongLineMode(settings(lookAhead = line)))

    @Test
    fun `the lower-third look-ahead surface alone in line mode is line mode`() =
        assertTrue(isSongLineMode(settings(lowerThirdLookAhead = line)))

    // ── songLinesPerSlide ──

    @Test
    fun `lines per slide is read from the surface asked for`() {
        val s = SongSettings(fullscreenLinesPerSlide = 3, lowerThirdLinesPerSlide = 2)
        assertEquals(3, songLinesPerSlide(s, isLowerThird = false))
        assertEquals(2, songLinesPerSlide(s, isLowerThird = true))
    }

    @Test
    fun `a hand-edited zero or negative lines per slide is clamped to one`() {
        // Straight from settings.json, so it is not necessarily in range. Zero would make
        // songLineGroup return nothing and put a blank slide on the screen.
        assertEquals(1, songLinesPerSlide(SongSettings(fullscreenLinesPerSlide = 0), false))
        assertEquals(1, songLinesPerSlide(SongSettings(fullscreenLinesPerSlide = -4), false))
    }

    @Test
    fun `lines per slide is clamped to the maximum`() =
        assertEquals(
            MAX_LINES_PER_SLIDE,
            songLinesPerSlide(SongSettings(fullscreenLinesPerSlide = 999), false),
        )

    // ── songLineStep ──

    @Test
    fun `with every surface in verse mode one press still advances one line`() =
        assertEquals(1, songLineStep(settings()))

    @Test
    fun `the step is the group of the only surface in line mode`() =
        assertEquals(
            4,
            songLineStep(settings(fullscreen = line).copy(fullscreenLinesPerSlide = 4)),
        )

    @Test
    fun `a surface in verse mode does not contribute its group to the step`() {
        // The lower third is on 1, but shows whole verses — it has no say in how far a press moves.
        val s = settings(fullscreen = line).copy(fullscreenLinesPerSlide = 5, lowerThirdLinesPerSlide = 1)
        assertEquals(5, songLineStep(s))
    }

    @Test
    fun `with two surfaces in line mode the smaller group wins`() {
        // Otherwise the surface on 1 would be stepped straight past lines it never displayed.
        val s = settings(fullscreen = line, lowerThird = line)
            .copy(fullscreenLinesPerSlide = 4, lowerThirdLinesPerSlide = 1)
        assertEquals(1, songLineStep(s))
    }

    @Test
    fun `a surface counts as being in line mode when only its look-ahead is`() =
        assertEquals(
            3,
            songLineStep(settings(lookAhead = line).copy(fullscreenLinesPerSlide = 3)),
        )

    // ── songLineGroupStart ──

    @Test
    fun `a cursor anywhere inside a group snaps back to that group's first line`() {
        // 0,1,2 all belong to the slide starting at 0 — which is what stops a larger group from
        // sliding one line at a time while a smaller surface steps.
        assertEquals(0, songLineGroupStart(0, 3))
        assertEquals(0, songLineGroupStart(1, 3))
        assertEquals(0, songLineGroupStart(2, 3))
        assertEquals(3, songLineGroupStart(3, 3))
        assertEquals(3, songLineGroupStart(5, 3))
        assertEquals(6, songLineGroupStart(6, 3))
    }

    @Test
    fun `a group of one leaves every cursor position where it is`() =
        (0..5).forEach { assertEquals(it, songLineGroupStart(it, 1)) }

    @Test
    fun `the -1 no-selection cursor starts at the first group`() =
        assertEquals(0, songLineGroupStart(-1, 3))

    // ── songLineGroup ──

    private val lines = listOf("one", "two", "three", "four", "five")

    @Test
    fun `a group of one is the single line at the cursor`() =
        assertEquals(listOf("three"), songLineGroup(lines, 2, 1))

    @Test
    fun `a group takes the whole slide's worth of lines from its boundary`() =
        assertEquals(listOf("one", "two"), songLineGroup(lines, 1, 2))

    @Test
    fun `the last group is short when the verse does not divide evenly`() =
        assertEquals(listOf("five"), songLineGroup(lines, 4, 2))

    @Test
    fun `a cursor past the end yields nothing rather than throwing`() =
        assertEquals(emptyList(), songLineGroup(lines, 99, 2))

    @Test
    fun `an empty verse yields nothing`() =
        assertEquals(emptyList(), songLineGroup(emptyList(), 0, 3))

    // ── songLineGroups ──

    @Test
    fun `a verse is cut into the slides that will actually be shown`() =
        assertEquals(
            listOf(listOf("one", "two"), listOf("three", "four"), listOf("five")),
            songLineGroups(lines, 2),
        )

    @Test
    fun `at one line per slide every line is its own slide`() =
        assertEquals(5, songLineGroups(lines, 1).size)

    @Test
    fun `a group wider than the verse leaves one slide holding all of it`() =
        assertEquals(listOf(lines), songLineGroups(lines, 20))

    @Test
    fun `an empty verse produces no slides`() =
        assertEquals(emptyList(), songLineGroups(emptyList(), 3))
}
