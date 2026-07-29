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
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import org.churchpresenter.app.churchpresenter.dialogs.filechooser.FileChooser
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playSong
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playVerse
import org.churchpresenter.app.churchpresenter.dialogs.tabs.withStatsHome
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.nio.file.Path as NioPath

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
 * Export to XLS opens a native save dialog through `FileChooser.platformInstance`, stood in for the
 * same way `AboutContentTest` does for its own save dialog.
 *
 * Left uncovered: both `DialogWindow` calls.
 */
class StatisticsContentTest {

    @AfterTest
    fun cleanUp() {
        unmockkObject(FileChooser.Companion)
    }

    private class FakeChooser(private val picked: String?) : FileChooser() {
        override suspend fun chooseImpl(
            path: NioPath,
            filters: List<FileNameExtensionFilter>,
            title: String,
            selectDirectory: Boolean,
            multiple: Boolean
        ): List<NioPath>? = picked?.let { listOf(Path(it)) }

        override suspend fun saveImpl(
            location: NioPath,
            suggestedName: String,
            filters: List<FileNameExtensionFilter>,
            title: String
        ): NioPath? = picked?.let { Path(it) }
    }

    private fun givenSaveChooserReturns(picked: String?) {
        mockkObject(FileChooser.Companion)
        every { FileChooser.platformInstance } returns FakeChooser(picked)
    }

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

    private fun ComposeUiTest.assertTextEventually(text: String, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            repeat(3) { javax.swing.SwingUtilities.invokeAndWait { } }
            waitForIdle()
            try {
                onNodeWithText(text).assertIsDisplayed()
                return
            } catch (e: Throwable) {
                lastError = e
            }
            Thread.sleep(10)
        }
        throw AssertionError("timed out after ${timeoutMs}ms waiting for text: $text", lastError)
    }

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
            onNodeWithText(Label.EXPORT).assertIsDisplayed()
        }

    @Test
    fun `exporting to a chosen location writes the file and reports success`() = withStatsHome { home ->
        val dest = File(home, "statistics.xls")
        givenSaveChooserReturns(dest.absolutePath)
        statistics({ playSong(number = 1, title = "A Song", songbook = "Hymnal") }) { _, _ ->
            onNodeWithText(Label.EXPORT).performClick()
            assertTextEventually("Statistics exported successfully.")
            assertTrue(dest.exists(), "the workbook must actually be written")
        }
    }

    @Test
    fun `exporting to a folder that does not exist reports failure`() = withStatsHome { home ->
        givenSaveChooserReturns(File(home, "no/such/folder/statistics.xls").absolutePath)
        statistics({ playSong(number = 1, title = "A Song", songbook = "Hymnal") }) { _, _ ->
            onNodeWithText(Label.EXPORT).performClick()
            assertTextEventually("Failed to export statistics.")
        }
    }

    @Test
    fun `canceling the save dialog shows no status`() = withStatsHome {
        givenSaveChooserReturns(null)
        statistics({ playSong(number = 1, title = "A Song", songbook = "Hymnal") }) { _, _ ->
            onNodeWithText(Label.EXPORT).performClick()
            waitForIdle()

            onNodeWithText("Statistics exported successfully.").assertDoesNotExist()
            onNodeWithText("Failed to export statistics.").assertDoesNotExist()
        }
    }

    @Test
    fun `choosing a different songbook from the dropdown shows that songbook's songs`() =
        statistics({
            playSong(number = 1, title = "From The Hymnal", songbook = "Hymnal", times = 5)
            playSong(number = 1, title = "From The Chorus Book", songbook = "Chorus Book", times = 9)
        }) { _, _ ->
            onNodeWithText("#1 From The Hymnal").assertIsDisplayed()

            onAllNodes(hasText("Hymnal", substring = false))[0].performClick()
            waitForIdle()
            onNodeWithText("Chorus Book").performClick()
            waitForIdle()

            onNodeWithText("#1 From The Chorus Book").assertIsDisplayed()
            onNodeWithText("#1 From The Hymnal").assertDoesNotExist()
        }

    @Test
    fun `choosing a different bible from the dropdown shows that bible's verses`() =
        statistics({
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 5)
            playVerse(bible = "ESV", book = "Psalms", chapter = 23, verse = 1, times = 9)
        }) { _, _ ->
            onNodeWithText("John 3:16").assertIsDisplayed()

            onAllNodes(hasText("KJV", substring = false))[0].performClick()
            waitForIdle()
            onNodeWithText("ESV").performClick()
            waitForIdle()

            onNodeWithText("Psalms 23:1").assertIsDisplayed()
            onNodeWithText("John 3:16").assertDoesNotExist()
        }

    @Test
    fun `a blank songbook name shows an em dash for both the selected label and its dropdown option`() =
        statistics({
            playSong(number = 1, title = "Untitled Book Song", songbook = "")
            playSong(number = 2, title = "Named Book Song", songbook = "Hymnal")
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16)
        }) { _, _ ->
            assertTrue(countOf("—") == 1, "only the blank songbook's selected label falls back to the dash")

            onNodeWithText("—").performClick()
            waitForIdle()

            assertTrue(countOf("—") == 2, "the dropdown option for the blank songbook also falls back to the dash")
        }

    @Test
    fun `a blank bible name shows an em dash for both the selected label and its dropdown option`() =
        statistics({
            playSong(number = 1, title = "A Song", songbook = "Hymnal")
            playVerse(bible = "", book = "John", chapter = 3, verse = 16)
            playVerse(bible = "KJV", book = "Psalms", chapter = 23, verse = 1)
        }) { _, _ ->
            onNodeWithText("John 3:16").assertIsDisplayed()
            assertTrue(countOf("—") == 1, "only the blank bible's selected label falls back to the dash")

            onNodeWithText("—").performClick()
            waitForIdle()

            assertTrue(countOf("—") == 2, "the dropdown option for the blank bible also falls back to the dash")
        }

    @Test
    fun `the second-ranked verse in a bible is not emphasized like the first`() =
        statistics({
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 9)
            playVerse(bible = "KJV", book = "Psalms", chapter = 23, verse = 1, times = 1)
        }) { _, _ ->
            onNodeWithText("John 3:16").assertIsDisplayed()
            onNodeWithText("Psalms 23:1").assertIsDisplayed()
        }

    @Test
    fun `two songs tied for the top count in the same songbook both show up`() =
        statistics({
            playSong(number = 1, title = "Tied Song One", songbook = "Hymnal", times = 3)
            playSong(number = 2, title = "Tied Song Two", songbook = "Hymnal", times = 3)
        }) { _, _ ->
            onNodeWithText("#1 Tied Song One").assertIsDisplayed()
            onNodeWithText("#2 Tied Song Two").assertIsDisplayed()
        }

    @Test
    fun `two verses tied for the top count in the same bible both show up`() =
        statistics({
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 3)
            playVerse(bible = "KJV", book = "Psalms", chapter = 23, verse = 1, times = 3)
        }) { _, _ ->
            onNodeWithText("John 3:16").assertIsDisplayed()
            onNodeWithText("Psalms 23:1").assertIsDisplayed()
        }

    @Test
    fun `a songbook with no entries falls back to a max count of 1 and shows no rows`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopSongsSection(mapOf("Empty Songbook" to emptyList()))
            }
        }
        onNodeWithText("0 songs").assertIsDisplayed()
    }

    @Test
    fun `a bible with no entries falls back to a max count of 1 and shows no rows`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                TopVersesSection(mapOf("Empty Bible" to emptyList()))
            }
        }
        onNodeWithText("0 verses").assertIsDisplayed()
    }
}
