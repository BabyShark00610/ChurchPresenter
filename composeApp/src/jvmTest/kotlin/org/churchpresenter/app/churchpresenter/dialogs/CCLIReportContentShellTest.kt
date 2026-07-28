@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import org.churchpresenter.app.churchpresenter.dialogs.filechooser.FileChooser
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playSong
import org.churchpresenter.app.churchpresenter.dialogs.tabs.playVerse
import org.churchpresenter.app.churchpresenter.dialogs.tabs.withStatsHome
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import java.io.File
import java.nio.file.Files
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.nio.file.Path as NioPath

/**
 * The report window's shell: the tab row over the three bodies, the quick-range presets, and what is
 * shown before anything has ever been recorded.
 *
 * `CCLIReportDialog` opens a `DialogWindow`, which cannot be composed headless, so the window's body
 * was lifted into `CCLIReportContent` — an extraction, no logic moved or changed — and that is what
 * these drive. `CCLIReportContentTest` covers the three report bodies underneath; this covers the
 * frame around them.
 *
 * `StatisticsManager` resolves `user.home` in its field initialisers and writes its two JSON files
 * there, so every test builds one inside an isolated home ([withStatsHome]) and seeds it through the
 * public recording API — the same path the app uses.
 *
 * The report loads its three result sets on a background dispatcher, so tests wait for the tab
 * counts to arrive rather than pausing: the count in the tab label is the positive signal that the
 * load finished.
 *
 * The export buttons open a native save dialog through `FileChooser.platformInstance`; the export
 * tests below replace it with a fake that "picks" a path without opening anything, the same pattern
 * `AboutContentTest` uses.
 *
 * Left uncovered: the `DialogWindow` call itself.
 */
class CCLIReportContentShellTest {

    private object Tab {
        const val ACTIVITY = "Activity"
        const val NO_EVENTS =
            "No event log yet. Song and verse presentations are tracked automatically starting now."
        const val CLOSE = "Close"
        const val EXPORT_CSV = "Export CCLI CSV"
        const val EXPORT_XLS = "Export XLS"
        const val FROM = "From:"
        const val TO = "To:"

        fun songs(n: Int) = "Songs ($n)"
        fun bible(n: Int) = "Bible ($n)"
    }

    private object Preset {
        const val LAST_30 = "Last 30 Days"
        const val LAST_90 = "Last 90 Days"
        const val THIS_YEAR = "This Year"
        const val LAST_YEAR = "Last Year"
        const val ALL_TIME = "All Time"
    }

    private class Closed { var count = 0 }

    @AfterTest
    fun tidy() {
        unmockkAll()
    }

    // ── Standing in for the native save dialog ──────────────────────────────────

    /** A save dialog that "returns" [picked] without opening anything. */
    private class FakeChooser(private val picked: String?) : FileChooser() {
        override suspend fun chooseImpl(
            path: NioPath,
            filters: List<FileNameExtensionFilter>,
            title: String,
            selectDirectory: Boolean,
            multiple: Boolean,
        ): List<NioPath>? = null

        override suspend fun saveImpl(
            location: NioPath,
            suggestedName: String,
            filters: List<FileNameExtensionFilter>,
            title: String,
        ): NioPath? = picked?.let { Path(it) }
    }

    private fun givenSaveChooserReturns(picked: String) {
        mockkObject(FileChooser.Companion)
        every { FileChooser.platformInstance } returns FakeChooser(picked)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun report(
        seed: StatisticsManager.() -> Unit = {},
        block: ComposeUiTest.(Closed) -> Unit,
    ) = withStatsHome {
        val stats = StatisticsManager().apply(seed)
        val closed = Closed()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    CCLIReportContent(
                        theme = ThemeMode.LIGHT,
                        statisticsManager = stats,
                        onDismiss = { closed.count++ },
                    )
                }
            }
            block(closed)
        }
    }

    private fun ComposeUiTest.countOf(text: String): Int =
        onAllNodes(hasText(text)).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    /** The tab counts only appear once the background load has returned. */
    private fun ComposeUiTest.awaitLoaded(songs: Int, verses: Int) =
        waitUntil("the report must finish loading and label its tabs", timeoutMillis = 5_000) {
            countOf(Tab.songs(songs)) == 1 && countOf(Tab.bible(verses)) == 1
        }

    // ── Before anything has been recorded ───────────────────────────────────────

    @Test
    fun `with no event log the report explains itself instead of showing empty tabs`() =
        report { _ ->
            onNodeWithText(Tab.NO_EVENTS).assertIsDisplayed()
            assertEquals(0, countOf(Tab.ACTIVITY), "there are no tabs to offer over an empty log")
        }

    @Test
    fun `the date range controls are offered even with no event log`() = report { _ ->
        onNodeWithText(Tab.FROM).assertIsDisplayed()
        onNodeWithText(Tab.TO).assertIsDisplayed()
        listOf(Preset.LAST_30, Preset.LAST_90, Preset.THIS_YEAR, Preset.LAST_YEAR, Preset.ALL_TIME)
            .forEach { onNodeWithText(it).assertIsDisplayed() }
    }

    // ── With recorded activity ──────────────────────────────────────────────────

    @Test
    fun `each tab counts what it holds`() =
        report({
            playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal", times = 3)
            playSong(number = 2, title = "Be Thou My Vision", songbook = "Hymnal")
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16, times = 2)
        }) { _ ->
            // Two distinct songs and one distinct verse, however many times each was shown.
            awaitLoaded(songs = 2, verses = 1)
            onNodeWithText(Tab.ACTIVITY).assertIsDisplayed()
        }

    @Test
    fun `the songs tab opens first and its table lists what was recorded`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal", times = 3) }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            assertTrue(
                countOf("Amazing Grace") >= 1,
                "the songs report is the one the dialog opens on, so its rows must already be there",
            )
        }

    @Test
    fun `choosing the Bible tab swaps the songs report for the verse report`() =
        report({
            playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal")
            playVerse(bible = "KJV", book = "John", chapter = 3, verse = 16)
        }) { _ ->
            awaitLoaded(songs = 1, verses = 1)
            onNodeWithText(Tab.bible(1)).performClick()
            waitForIdle()
            assertTrue(countOf("John 3:16") >= 1, "the verse report must be showing")
            assertEquals(0, countOf("Amazing Grace"), "the songs report must be gone, not merely behind it")
        }

    @Test
    fun `choosing the Activity tab shows the over-time report`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Tab.ACTIVITY).performClick()
            waitForIdle()
            onNodeWithText(CcliLabel.ACTIVITY_TITLE).assertIsDisplayed()
            assertEquals(0, countOf("Amazing Grace"), "the songs table belongs to the tab that was left")
        }

    // ── Quick ranges ────────────────────────────────────────────────────────────

    @Test
    fun `the last-year preset moves the range off the current year entirely`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Preset.LAST_YEAR).performClick()
            // The song was recorded just now, so a range ending last December must exclude it.
            waitUntil("the range change must reload the report", timeoutMillis = 5_000) {
                countOf(Tab.songs(0)) == 1
            }
            assertEquals(
                0,
                countOf("Amazing Grace"),
                "a song presented today cannot appear in last year's report",
            )
        }

    @Test
    fun `the all-time preset brings everything back into range`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Preset.LAST_YEAR).performClick()
            waitUntil("last year must exclude it first", timeoutMillis = 5_000) { countOf(Tab.songs(0)) == 1 }

            onNodeWithText(Preset.ALL_TIME).performClick()
            waitUntil("all time must take it back", timeoutMillis = 5_000) { countOf(Tab.songs(1)) == 1 }
            assertTrue(countOf("Amazing Grace") >= 1, "the row must be back in the table too")
        }

    @Test
    fun `the last-30-days preset keeps a just-recorded song in range`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Preset.LAST_30).performClick()
            waitUntil("a song recorded moments ago always falls inside the last 30 days", timeoutMillis = 5_000) {
                countOf(Tab.songs(1)) == 1
            }
        }

    @Test
    fun `the last-90-days preset keeps a just-recorded song in range`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Preset.LAST_90).performClick()
            waitUntil("a song recorded moments ago always falls inside the last 90 days", timeoutMillis = 5_000) {
                countOf(Tab.songs(1)) == 1
            }
        }

    @Test
    fun `This Year restores a song that Last Year's range had excluded`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Preset.LAST_YEAR).performClick()
            waitUntil("last year must exclude it first", timeoutMillis = 5_000) { countOf(Tab.songs(0)) == 1 }

            onNodeWithText(Preset.THIS_YEAR).performClick()
            waitUntil("this year must bring it back", timeoutMillis = 5_000) { countOf(Tab.songs(1)) == 1 }
            assertTrue(countOf("Amazing Grace") >= 1, "the row must be back in the table too")
        }

    // ── Date pickers ────────────────────────────────────────────────────────────

    @Test
    fun `opening the From day picker and choosing a new day updates its label`() = report { _ ->
        onNodeWithText("1").performClick()
        waitForIdle()
        onNodeWithText("15").performClick()
        waitForIdle()
        onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun `opening the From month picker and choosing a new month updates its short label`() = report { _ ->
        onAllNodesWithText("Jan")[0].performClick()
        waitForIdle()
        onNodeWithText("March").performClick()
        waitForIdle()
        assertTrue(countOf("Mar") >= 1, "the From button must now show the short form of the chosen month")
    }

    // ── Export ──────────────────────────────────────────────────────────────────

    @Test
    fun `exporting to CSV writes the filtered report and reports success`() {
        val dir = Files.createTempDirectory("cp-ccli-csv").toFile()
        val target = File(dir, "ccli_report.csv")
        givenSaveChooserReturns(target.path)
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Tab.EXPORT_CSV).performClick()
            waitUntil("the export must finish and report success", timeoutMillis = 5_000) {
                countOf(CcliLabel.EXPORT_SUCCESS) == 1
            }
            assertTrue(target.exists(), "the CSV file must have been written")
            assertTrue(target.readText().contains("Amazing Grace"), "the row must be in the exported CSV")
        }
    }

    @Test
    fun `exporting to CSV reports failure when the file cannot be written`() {
        val target = File(Files.createTempDirectory("cp-ccli-csv-missing").toFile(), "nope/ccli_report.csv")
        givenSaveChooserReturns(target.path)
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Tab.EXPORT_CSV).performClick()
            waitUntil("the export must finish and report failure", timeoutMillis = 5_000) {
                countOf(CcliLabel.EXPORT_ERROR) == 1
            }
            assertFalse(target.exists(), "no file can exist under a parent directory that was never created")
        }
    }

    @Test
    fun `exporting to XLS writes the filtered workbook and reports success`() {
        val dir = Files.createTempDirectory("cp-ccli-xls").toFile()
        val target = File(dir, "ccli_report.xls")
        givenSaveChooserReturns(target.path)
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Tab.EXPORT_XLS).performClick()
            waitUntil("the export must finish and report success", timeoutMillis = 5_000) {
                countOf(CcliLabel.EXPORT_SUCCESS) == 1
            }
            assertTrue(target.exists(), "the XLS workbook must have been written")
        }
    }

    @Test
    fun `exporting to XLS reports failure when the file cannot be written`() {
        val target = File(Files.createTempDirectory("cp-ccli-xls-missing").toFile(), "nope/ccli_report.xls")
        givenSaveChooserReturns(target.path)
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            awaitLoaded(songs = 1, verses = 0)
            onNodeWithText(Tab.EXPORT_XLS).performClick()
            waitUntil("the export must finish and report failure", timeoutMillis = 5_000) {
                countOf(CcliLabel.EXPORT_ERROR) == 1
            }
            assertFalse(target.exists(), "no file can exist under a parent directory that was never created")
        }
    }

    // ── Leaving ─────────────────────────────────────────────────────────────────

    @Test
    fun `Close reports the dialog should shut`() = report { closed ->
        onNodeWithText(Tab.CLOSE).performClick()
        waitForIdle()
        assertEquals(1, closed.count)
    }

    @Test
    fun `both export buttons are offered`() =
        report({ playSong(number = 1, title = "Amazing Grace", songbook = "Hymnal") }) { _ ->
            // Never pressed: each opens a native save dialog.
            onNodeWithText(Tab.EXPORT_CSV).assertIsDisplayed()
            onNodeWithText(Tab.EXPORT_XLS).assertIsDisplayed()
        }
}
