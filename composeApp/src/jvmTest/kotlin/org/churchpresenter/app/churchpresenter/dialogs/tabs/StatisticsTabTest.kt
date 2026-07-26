@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the Statistics tab: the two league tables it renders and the button that empties them.
 *
 * The tab is a read-only view over `StatisticsManager` with one destructive action, so there are no
 * settings to assert — every test either reads what is on screen or checks the manager afterwards.
 * Fixtures seed the manager through its recording API, the same one the app calls when something is
 * actually shown, so a change to how a play is counted shows up here rather than being papered over
 * by hand-written JSON.
 *
 * The tables group by songbook and by Bible, which is where the interesting branching is: an unnamed
 * group gets a plain heading, a named one gets the name in brackets, and either table can be empty on
 * its own while the other has content.
 */
class StatisticsTabTest {

    // ── Empty ───────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a tab with no history shows both headings and a dash under each`() = statisticsTab { _ ->
        onNodeWithText(StatsLabel.TOP_SONGS).assertExists("the songs heading must show even with no data")
        onNodeWithText(StatsLabel.TOP_VERSES).assertExists("as must the verses heading")
        onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(2)
    }

    /**
     * Clear is the tab's *only* interactive element — everything else is read-only text. Reaching it
     * through [clearButton], which resolves a single clickable node or fails, is what asserts that:
     * a second control appearing anywhere on the tab breaks this test rather than going unnoticed.
     */
    @Test
    fun `the clear button is the only control, and is offered with nothing to clear`() = statisticsTab { _ ->
        clearButton().assertExists("exactly one clickable must exist")
        onNodeWithText(StatsLabel.CLEAR).assertExists("and it must be the Clear button")
    }

    @Test
    fun `a populated tab still has only the clear button`() {
        statisticsTab(
            seed = {
                playSong(42, "Amazing Grace", "Hymnal", times = 3)
                playVerse("KJV", "John", 3, 16, times = 2)
            },
        ) { _ ->
            clearButton().assertExists("rows are read-only; they must add no controls")
        }
    }

    // ── Songs ───────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a played song is listed with its number, title and count`() {
        statisticsTab(seed = { playSong(42, "Amazing Grace", "Hymnal", times = 3) }) { _ ->
            val rows = rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal"))
            assertEquals(
                listOf(Triple("1.", "#42 Amazing Grace", "3")),
                rows,
                "the row must carry the rank, the numbered title and the play count",
            )
        }
    }

    @Test
    fun `songs are ranked by play count, most played first`() {
        statisticsTab(
            seed = {
                playSong(1, "Least", "Hymnal", times = 1)
                playSong(2, "Most", "Hymnal", times = 5)
                playSong(3, "Middle", "Hymnal", times = 3)
            },
        ) { _ ->
            val rows = rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal"))
            assertEquals(
                listOf(
                    Triple("1.", "#2 Most", "5"),
                    Triple("2.", "#3 Middle", "3"),
                    Triple("3.", "#1 Least", "1"),
                ),
                rows,
                "the table must be ordered by count, and ranked 1..n in that order",
            )
        }
    }

    /** Each songbook gets its own heading and its own ranking, restarting at 1. */
    @Test
    fun `each songbook gets its own section`() {
        statisticsTab(
            seed = {
                playSong(1, "Hymnal One", "Hymnal", times = 2)
                playSong(7, "Chorus One", "Chorus Book", times = 9)
            },
        ) { _ ->
            assertEquals(
                listOf(Triple("1.", "#1 Hymnal One", "2")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal")),
            )
            assertEquals(
                listOf(Triple("1.", "#7 Chorus One", "9")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Chorus Book")),
                "a second songbook must rank separately rather than joining the first",
            )
        }
    }

    /** A song with no songbook gets the bare heading rather than "Top Songs ()". */
    @Test
    fun `a song with no songbook is listed under the plain heading`() {
        statisticsTab(seed = { playSong(5, "Loose Song", songbook = "", times = 1) }) { _ ->
            onNodeWithText(StatsLabel.TOP_SONGS).assertExists("the heading must carry no empty brackets")
            assertTrue(
                renderedLines().none { it.startsWith("${StatsLabel.TOP_SONGS} (") },
                "no bracketed songbook heading may be rendered, was ${renderedLines()}",
            )
            assertEquals(
                listOf(Triple("1.", "#5 Loose Song", "1")),
                rowsUnder(StatsLabel.TOP_SONGS),
            )
        }
    }

    // ── Verses ──────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a shown verse is listed by reference with its count`() {
        statisticsTab(seed = { playVerse("KJV", "John", 3, 16, times = 4) }) { _ ->
            assertEquals(
                listOf(Triple("1.", "John 3:16", "4")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_VERSES, "KJV")),
                "the row must read as a reference, not as raw fields",
            )
        }
    }

    @Test
    fun `verses are ranked by count within their Bible`() {
        statisticsTab(
            seed = {
                playVerse("KJV", "John", 3, 16, times = 2)
                playVerse("KJV", "Psalm", 23, 1, times = 6)
            },
        ) { _ ->
            assertEquals(
                listOf(Triple("1.", "Psalm 23:1", "6"), Triple("2.", "John 3:16", "2")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_VERSES, "KJV")),
            )
        }
    }

    @Test
    fun `each Bible gets its own section`() {
        statisticsTab(
            seed = {
                playVerse("KJV", "John", 3, 16, times = 2)
                playVerse("ESV", "Romans", 8, 28, times = 3)
            },
        ) { _ ->
            assertEquals(
                listOf(Triple("1.", "John 3:16", "2")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_VERSES, "KJV")),
            )
            assertEquals(
                listOf(Triple("1.", "Romans 8:28", "3")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_VERSES, "ESV")),
            )
        }
    }

    @Test
    fun `a verse with no Bible name is listed under the plain heading`() {
        statisticsTab(seed = { playVerse("", "John", 3, 16, times = 1) }) { _ ->
            assertTrue(
                renderedLines().none { it.startsWith("${StatsLabel.TOP_VERSES} (") },
                "no bracketed Bible heading may be rendered, was ${renderedLines()}",
            )
            assertEquals(
                listOf(Triple("1.", "John 3:16", "1")),
                rowsUnder(StatsLabel.TOP_VERSES),
            )
        }
    }

    // ── One table full, the other empty ─────────────────────────────────────────────────────────

    /**
     * The two tables are independent, and each has its own empty state. With songs but no verses only
     * the verse table may show a dash — the pair of `isEmpty()` branches taken in opposite directions
     * at once, which a fixture seeding both would never reach.
     */
    @Test
    fun `songs without verses leaves only the verse table empty`() {
        statisticsTab(seed = { playSong(1, "Only Song", "Hymnal", times = 1) }) { _ ->
            onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(1)
            onNodeWithText(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal")).assertExists()
            onNodeWithText(StatsLabel.TOP_VERSES).assertExists("the verses heading still shows, with a dash")
        }
    }

    @Test
    fun `verses without songs leaves only the song table empty`() {
        statisticsTab(seed = { playVerse("KJV", "John", 3, 16, times = 1) }) { _ ->
            onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(1)
            onNodeWithText(StatsLabel.TOP_SONGS).assertExists("the songs heading still shows, with a dash")
            onNodeWithText(StatsLabel.heading(StatsLabel.TOP_VERSES, "KJV")).assertExists()
        }
    }

    // ── Clearing ────────────────────────────────────────────────────────────────────────────────

    /**
     * Clear empties the manager **and** refreshes the tab from it, which is two separate things: the
     * button reassigns the two `remember`ed lists after clearing, and without that the table would
     * keep showing history that no longer exists.
     */
    @Test
    fun `clearing empties the manager and repaints both tables`() {
        statisticsTab(
            seed = {
                playSong(42, "Amazing Grace", "Hymnal", times = 3)
                playVerse("KJV", "John", 3, 16, times = 2)
            },
        ) { stats ->
            onNodeWithText("#42 Amazing Grace").assertExists("fixture: the song is listed")
            onNodeWithText("John 3:16").assertExists("fixture: the verse is listed")

            onNodeWithText(StatsLabel.CLEAR).performClick()
            waitForIdle()

            assertTrue(stats.getTopSongsBySongbook().isEmpty(), "the manager must have been emptied of songs")
            assertTrue(stats.getTopVersesByBible().isEmpty(), "and of verses")
            onNodeWithText("#42 Amazing Grace").assertDoesNotExist()
            onNodeWithText("John 3:16").assertDoesNotExist()
            onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(2)
        }
    }

    @Test
    fun `clearing an already empty tab changes nothing`() = statisticsTab { stats ->
        onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(2)

        onNodeWithText(StatsLabel.CLEAR).performClick()
        waitForIdle()

        assertTrue(stats.getTopSongsBySongbook().isEmpty())
        onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(2)
        onNodeWithText(StatsLabel.CLEAR).assertExists("and the button must still be there")
    }
}
