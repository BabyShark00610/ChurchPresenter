@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Recomposition behaviour, and the one thing about this tab worth knowing before reading it as live
 * data: **it is a snapshot, not a feed.**
 *
 * The two tables come from `remember { statisticsManager.getTop…() }` with no key, so they are read
 * once when the tab is first composed and never again — except by the Clear button, which reassigns
 * them explicitly. A song shown while the dialog is open does not appear until the dialog is
 * reopened. That is asserted below rather than left to be discovered, because it is exactly the sort
 * of thing that reads as a bug in the field ("I sang it, why isn't it counted?") when it is really
 * the refresh policy.
 */
class StatisticsTabRecompositionTest {

    /** Renders the tab over [stats] with a recomposition trigger the test controls. */
    private fun rerenderable(
        stats: StatisticsManager,
        block: ComposeUiTest.(recompose: () -> Unit) -> Unit,
    ) = runComposeUiTest {
        var tick by mutableStateOf(0)
        setContent {
            MaterialTheme {
                @Suppress("UNUSED_EXPRESSION") tick
                StatisticsTab(statisticsManager = stats)
            }
        }
        block { tick += 1; waitForIdle() }
    }

    @Test
    fun `the tab survives a recomposition that changes nothing`() = withStatsHome {
        val stats = StatisticsManager().apply { playSong(42, "Amazing Grace", "Hymnal", times = 3) }
        rerenderable(stats) { recompose ->
            onNodeWithText("#42 Amazing Grace").assertExists()

            recompose()

            onNodeWithText("#42 Amazing Grace").assertExists("the table must survive a re-render")
            onNodeWithText(StatsLabel.CLEAR).assertExists()
            assertEquals(
                listOf(Triple("1.", "#42 Amazing Grace", "3")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal")),
            )
        }
    }

    /**
     * A play recorded while the tab is on screen does **not** appear, even when the tab recomposes.
     * The lists are remembered without a key, so only Clear ever re-reads them.
     */
    @Test
    fun `a play recorded while the tab is open does not appear until it is reopened`() = withStatsHome {
        val stats = StatisticsManager().apply { playSong(1, "First", "Hymnal", times = 1) }

        rerenderable(stats) { recompose ->
            onNodeWithText("#1 First").assertExists()
            onNodeWithText("#2 Second").assertDoesNotExist()

            stats.playSong(2, "Second", "Hymnal", times = 5)
            recompose()

            assertTrue(
                stats.getTopSongsBySongbook().getValue("Hymnal").any { it.title == "Second" },
                "fixture: the manager itself must have taken the new play",
            )
            onNodeWithText("#2 Second")
                .assertDoesNotExist() // the tab holds the snapshot it took when it opened
            onNodeWithText("#1 First").assertExists("and still shows what it read then")
        }
    }

    /** Reopening the tab is what picks the new play up — the same manager, a fresh composition. */
    @Test
    fun `reopening the tab picks up plays recorded since`() = withStatsHome {
        val stats = StatisticsManager().apply { playSong(1, "First", "Hymnal", times = 1) }

        runComposeUiTest {
            setContent { MaterialTheme { StatisticsTab(statisticsManager = stats) } }
            onNodeWithText("#1 First").assertExists()
        }

        stats.playSong(2, "Second", "Hymnal", times = 5)

        runComposeUiTest {
            setContent { MaterialTheme { StatisticsTab(statisticsManager = stats) } }
            onNodeWithText("#2 Second").assertExists("a fresh composition must read the manager again")
            assertEquals(
                listOf(Triple("1.", "#2 Second", "5"), Triple("2.", "#1 First", "1")),
                rowsUnder(StatsLabel.heading(StatsLabel.TOP_SONGS, "Hymnal")),
                "and rank the two together",
            )
        }
    }

    /** Clear is the one action that re-reads the manager without the tab being reopened. */
    @Test
    fun `clearing re-reads the manager even though nothing else does`() = withStatsHome {
        val stats = StatisticsManager().apply { playSong(1, "First", "Hymnal", times = 1) }

        rerenderable(stats) { recompose ->
            stats.playSong(2, "Second", "Hymnal", times = 5)
            recompose()
            onNodeWithText("#2 Second").assertDoesNotExist() // still the old snapshot

            onNodeWithText(StatsLabel.CLEAR).performClick()
            waitForIdle()

            // Clear wipes the manager and re-reads it, so both songs go at once.
            onNodeWithText("#1 First").assertDoesNotExist()
            onNodeWithText("#2 Second").assertDoesNotExist()
            onAllNodesWithText(StatsLabel.EMPTY).assertCountEquals(2)
        }
    }

    /** The shape `OptionsDialog` uses: a parent that forwards its own parameter straight through. */
    @Test
    fun `the tab renders when reached through a parent that forwards its manager`() = withStatsHome {
        val stats = StatisticsManager().apply {
            playSong(42, "Amazing Grace", "Hymnal", times = 3)
            playVerse("KJV", "John", 3, 16, times = 2)
        }
        runComposeUiTest {
            var tick by mutableStateOf(0)
            setContent {
                MaterialTheme {
                    @Suppress("UNUSED_EXPRESSION") tick
                    StatisticsHost(statisticsManager = stats)
                }
            }

            onNodeWithText("#42 Amazing Grace").assertExists()
            onNodeWithText("John 3:16").assertExists()

            tick += 1
            waitForIdle()

            onNodeWithText("#42 Amazing Grace").assertExists("both tables must survive a forwarded re-render")
            onNodeWithText("John 3:16").assertExists()
        }
    }
}

/**
 * Stands in for `OptionsDialog`: takes the manager as its own parameter and forwards it, so the
 * compiler propagates its caller's change flags into the tab.
 */
@Composable
private fun StatisticsHost(statisticsManager: StatisticsManager) {
    StatisticsTab(statisticsManager = statisticsManager)
}
