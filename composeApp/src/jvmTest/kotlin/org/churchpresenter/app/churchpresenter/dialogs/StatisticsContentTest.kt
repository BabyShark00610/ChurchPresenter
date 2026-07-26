@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playSong
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playVerse
import org.churchpresenter.app.churchpresenter.dialogs.tabs.withStatsHome
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The statistics window: the two ranked sections, the CCLI callout above them, and the row of
 * actions beneath.
 *
 * `StatisticsDialog` can open two windows — its own `DialogWindow` and the nested CCLI report —
 * neither composable headless, so the body was lifted into `StatisticsContent` with the report kept
 * outside as a callback. That is what makes pressing the callout testable at all: it reports out
 * instead of opening anything.
 *
 * `StatisticsManager` resolves `user.home` in its field initialisers, so every test builds one inside
 * an isolated home ([withStatsHome]) and seeds it through the public recording API. Without that,
 * `Clear Statistics` here would wipe the developer's real play history.
 *
 * Left uncovered: both `DialogWindow` calls, and Export to XLS, which opens a native save dialog —
 * asserted present, never pressed.
 */
class StatisticsContentTest {

    private object Label {
        const val TOP_SONGS = "Top Songs"
        const val TOP_VERSES = "Top Verses"
        const val CLEAR = "Clear Statistics"
        const val EXPORT = "Export to XLS"
        const val CLOSE = "Close"
        const val CCLI_REPORT = "CCLI Report"
    }

    private class Actions {
        var openedReport = 0
        var dismissed = 0
    }

    @OptIn(ExperimentalTestApi::class)
    private fun statistics(
        seed: StatisticsManager.() -> Unit = {},
        block: ComposeUiTest.(stats: StatisticsManager, actions: Actions) -> Unit,
    ) = withStatsHome {
        val stats = StatisticsManager().apply(seed)
        val actions = Actions()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    StatisticsContent(
                        theme = ThemeMode.LIGHT,
                        statisticsManager = stats,
                        onOpenCcliReport = { actions.openedReport++ },
                        onDismiss = { actions.dismissed++ },
                    )
                }
            }
            block(stats, actions)
        }
    }

    private fun ComposeUiTest.countOf(text: String): Int =
        onAllNodes(hasText(text)).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    /** The song rows, top to bottom — every line naming a song number. */
    private fun ComposeUiTest.songRows(): List<String> =
        onAllNodes(hasText("#", substring = true))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .sortedBy { it.boundsInRoot.top }
            .mapNotNull { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            }

    // ── With nothing recorded ───────────────────────────────────────────────────

    @Test
    fun `both sections are still headed when nothing has been presented`() = statistics { _, _ ->
        onNodeWithText(Label.TOP_SONGS).assertIsDisplayed()
        onNodeWithText(Label.TOP_VERSES).assertIsDisplayed()
    }

    @Test
    fun `the CCLI callout is offered even with no statistics`() = statistics { _, _ ->
        assertTrue(countOf(Label.CCLI_REPORT) >= 1, "the callout names the report it opens")
    }

    // ── Ranked songs ────────────────────────────────────────────────────────────

    @Test
    fun `songs are listed with their number, title and play count`() =
        statistics({ playSong(number = 12, title = "Amazing Grace", songbook = "Hymnal", times = 4) }) { _, _ ->
            onNodeWithText("#12 Amazing Grace").assertIsDisplayed()
            assertTrue(countOf("4") >= 1, "the play count belongs on the row")
        }

    @Test
    fun `the most-played song is ranked first however it was recorded`() =
        statistics({
            playSong(number = 1, title = "Sung Once", songbook = "Hymnal", times = 1)
            playSong(number = 2, title = "Sung Often", songbook = "Hymnal", times = 9)
            playSong(number = 3, title = "Sung Twice", songbook = "Hymnal", times = 2)
        }) { _, _ ->
            assertEquals(
                listOf("#2 Sung Often", "#3 Sung Twice", "#1 Sung Once"),
                songRows(),
                "rank follows the play count, not the song number or the order recorded",
            )
        }

    @Test
    fun `each songbook is counted on its own`() =
        statistics({
            playSong(number = 1, title = "From The Hymnal", songbook = "Hymnal", times = 5)
            playSong(number = 1, title = "From The Chorus Book", songbook = "Chorus Book", times = 9)
        }) { _, _ ->
            // One songbook is shown at a time, chosen from a picker — not both lists at once.
            assertEquals(
                1,
                countOf("#1 From The Hymnal") + countOf("#1 From The Chorus Book"),
                "exactly one songbook's songs are listed at a time",
            )
        }

    // ── Ranked verses ───────────────────────────────────────────────────────────

    @Test
    fun `verses are listed by reference`() =
        statistics({ playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 3) }) { _, _ ->
            assertTrue(
                countOf("John 3:16") >= 1,
                "the verse must be named by its reference; showed nothing matching",
            )
        }

    @Test
    fun `songs and verses are ranked independently of one another`() =
        statistics({
            playSong(number = 1, title = "A Song", songbook = "Hymnal", times = 2)
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 7)
        }) { _, _ ->
            onNodeWithText("#1 A Song").assertIsDisplayed()
            assertTrue(countOf("John 3:16") >= 1, "both sections fill from their own recordings")
        }

    // ── The actions beneath ─────────────────────────────────────────────────────

    @Test
    fun `pressing the callout asks for the CCLI report rather than opening it here`() =
        statistics { _, actions ->
            onAllNodes(hasText(Label.CCLI_REPORT))[1].performClick()
            waitForIdle()
            assertEquals(1, actions.openedReport, "the button reports out; the window is the caller's to open")
        }

    @Test
    fun `clearing the statistics empties the manager and the sections with it`() =
        statistics({ playSong(number = 12, title = "Amazing Grace", songbook = "Hymnal", times = 4) }) { stats, _ ->
            onNodeWithText("#12 Amazing Grace").assertIsDisplayed()

            onNodeWithText(Label.CLEAR).performClick()
            waitForIdle()

            assertEquals(0, countOf("#12 Amazing Grace"), "the row must go, not merely the stored count")
            assertTrue(
                stats.getTopSongsBySongbook().isEmpty(),
                "and the manager itself must be empty, so it stays cleared once reopened",
            )
        }

    @Test
    fun `clearing with nothing recorded is harmless`() = statistics { stats, _ ->
        onNodeWithText(Label.CLEAR).performClick()
        waitForIdle()
        assertTrue(stats.getTopSongsBySongbook().isEmpty())
        onNodeWithText(Label.TOP_SONGS).assertIsDisplayed()
    }

    @Test
    fun `Close reports the dialog should shut`() = statistics { _, actions ->
        onNodeWithText(Label.CLOSE).performClick()
        waitForIdle()
        assertEquals(1, actions.dismissed)
    }

    @Test
    fun `the export button is offered`() =
        statistics({ playSong(number = 1, title = "A Song", songbook = "Hymnal") }) { _, _ ->
            // Never pressed: it opens a native save dialog.
            onNodeWithText(Label.EXPORT).assertIsDisplayed()
        }
}
