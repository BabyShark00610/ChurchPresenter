@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.churchpresenter.app.churchpresenter.dialogs.filechooser.FileChooser
import org.churchpresenter.app.churchpresenter.presenter.Presenting
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
 * Export/import in the History view, and Export & Clear in the clear-all dialog — all three drive
 * `FileChooser.platformInstance`, a real native save/open dialog with nothing to click in a headless
 * test. Stood in for the same way `StatisticsContentTest` does for its own save dialog.
 *
 * None of the three report success or failure on screen, so what a test can observe is the
 * resulting file and the manager's own state, not a status message.
 *
 * See `QATabTestSupport.kt` for the harness.
 */
class QATabFileChooserTest {

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

    private fun givenChooserReturns(picked: String?) {
        mockkObject(FileChooser.Companion)
        every { FileChooser.platformInstance } returns FakeChooser(picked)
    }

    private fun ComposeUiTest.openHistory() {
        onNodeWithText(QALabel.HISTORY, substring = true).performClick()
        waitForIdle()
    }

    // ── Export from History ──────────────────────────────────────────────────────

    @Test
    fun `exporting history writes every question to the chosen file`() {
        val dest = File(Files.createTempDirectory("cp-qa-export").toFile(), "questions.txt")
        givenChooserReturns(dest.absolutePath)

        qaTab(seed = { askAll("Exported question"); toggleSession() }) { _, _, _ ->
            openHistory()
            clickQaLabel(QALabel.EXPORT_TO_FILE)
            waitForIdle()
            waitUntil(timeoutMillis = 2_000) { dest.exists() }

            assertTrue(dest.readText().contains("Exported question"), dest.readText())
        }
    }

    @Test
    fun `cancelling the export dialog leaves history untouched`() {
        givenChooserReturns(null)

        qaTab(seed = { askAll("stays in history"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.EXPORT_TO_FILE)
            waitForIdle()

            assertEquals(1, qa.history.size, "cancelling the save dialog must not touch history")
        }
    }

    // ── Import into the live queue ───────────────────────────────────────────────

    @Test
    fun `importing a file adds each line as a new question`() {
        val src = File(Files.createTempDirectory("cp-qa-import").toFile(), "import.txt")
        src.writeText("[2024-01-01 10:00] [Pending] Imported one\n[2024-01-01 10:01] [Approved] Imported two\n")
        givenChooserReturns(src.absolutePath)

        qaTab(seed = { askAll("prior session question"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.IMPORT_FROM_FILE)
            waitForIdle()
            waitUntil(timeoutMillis = 2_000) { qa.questions.size == 2 }

            assertTrue(qa.questions.any { it.text == "Imported one" }, "${qa.questions.map { it.text }}")
            assertTrue(qa.questions.any { it.text == "Imported two" })
        }
    }

    @Test
    fun `importing skips blank lines`() {
        val src = File(Files.createTempDirectory("cp-qa-import-blank").toFile(), "import.txt")
        src.writeText("[2024-01-01 10:00] [Pending] Kept line\n\n   \n")
        givenChooserReturns(src.absolutePath)

        qaTab(seed = { askAll("prior session question"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.IMPORT_FROM_FILE)
            waitForIdle()
            waitUntil(timeoutMillis = 2_000) { qa.questions.size == 1 }

            assertEquals("Kept line", qa.questions.single().text)
        }
    }

    @Test
    fun `cancelling the import dialog adds nothing`() {
        givenChooserReturns(null)

        qaTab(seed = { askAll("prior session question"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.IMPORT_FROM_FILE)
            waitForIdle()

            assertTrue(qa.questions.isEmpty())
        }
    }

    @Test
    fun `importing a file that cannot be read adds nothing and does not crash`() {
        val missing = File(Files.createTempDirectory("cp-qa-import-missing").toFile(), "gone.txt")
        givenChooserReturns(missing.absolutePath)

        qaTab(seed = { askAll("prior session question"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.IMPORT_FROM_FILE)
            waitForIdle()

            assertTrue(qa.questions.isEmpty(), "a failed read must add nothing")
        }
    }

    @Test
    fun `exporting to a folder that does not exist does not crash`() {
        val dest = File(Files.createTempDirectory("cp-qa-export-bad").toFile(), "no/such/folder/questions.txt")
        givenChooserReturns(dest.absolutePath)

        qaTab(seed = { askAll("Exported question"); toggleSession() }) { qa, _, _ ->
            openHistory()
            clickQaLabel(QALabel.EXPORT_TO_FILE)
            waitForIdle()

            assertFalse(dest.exists())
            assertEquals(1, qa.history.size, "the failed export must not touch history")
        }
    }

    // ── Export & Clear from the clear-all dialog ────────────────────────────────

    @Test
    fun `export and clear writes the live queue then clears it`() {
        val dest = File(Files.createTempDirectory("cp-qa-exportclear").toFile(), "questions.txt")
        givenChooserReturns(dest.absolutePath)

        qaTab(seed = { askAll("about to be cleared") }) { qa, presenter, reports ->
            clickQaLabel(QALabel.CLEAR_ALL)
            clickQaLabel(QALabel.EXPORT_AND_CLEAR)
            waitForIdle()
            waitUntil(timeoutMillis = 2_000) { qa.questions.isEmpty() }

            assertTrue(dest.readText().contains("about to be cleared"), dest.readText())
            assertEquals(null, presenter.displayedQuestion.value)
            assertFalse(presenter.showQRCodeOnDisplay.value)
            assertEquals(Presenting.NONE, reports.presenting.last())
        }
    }

    @Test
    fun `cancelling the export-and-clear save dialog leaves the questions untouched`() {
        givenChooserReturns(null)

        qaTab(seed = { askAll("must survive") }) { qa, _, _ ->
            clickQaLabel(QALabel.CLEAR_ALL)
            clickQaLabel(QALabel.EXPORT_AND_CLEAR)
            waitForIdle()

            assertEquals(1, qa.questions.size, "cancelling the save dialog must abort the clear")
        }
    }

    @Test
    fun `export and clear aborts when the save fails, leaving the queue intact`() {
        val dest = File(Files.createTempDirectory("cp-qa-exportclear-bad").toFile(), "no/such/folder/questions.txt")
        givenChooserReturns(dest.absolutePath)

        qaTab(seed = { askAll("must survive a failed export") }) { qa, _, _ ->
            clickQaLabel(QALabel.CLEAR_ALL)
            clickQaLabel(QALabel.EXPORT_AND_CLEAR)
            waitForIdle()

            assertFalse(dest.exists())
            assertEquals(1, qa.questions.size, "a failed export must abort the clear rather than lose questions")
        }
    }
}
