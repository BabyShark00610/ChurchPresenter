@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.takahirom.roborazzi.captureRoboImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.churchpresenter.app.churchpresenter.PresenterScreen
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.data.StrongsEntry
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.models.LyricSection
import org.churchpresenter.app.churchpresenter.models.Question
import org.churchpresenter.app.churchpresenter.models.QuestionStatus
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.presenter.AnnouncementsPresenter
import org.churchpresenter.app.churchpresenter.presenter.BiblePresenter
import org.churchpresenter.app.churchpresenter.presenter.DictionaryPresenter
import org.churchpresenter.app.churchpresenter.presenter.PicturePresenter
import org.churchpresenter.app.churchpresenter.presenter.PresentationPresenter
import org.churchpresenter.app.churchpresenter.presenter.QAPresenter
import org.churchpresenter.app.churchpresenter.presenter.ScenePresenter
import org.churchpresenter.app.churchpresenter.presenter.SongPresenter
import org.churchpresenter.app.churchpresenter.utils.Constants
import java.io.File
import kotlin.test.Test

class AppPreviewOutputTest {

    private val screen = Modifier.fillMaxSize()

    private fun settings(): AppSettings {
        TestSingletons.latchSkikoHostOs()
        TestSingletons.latchToTestHome()
        // Exactly the settings the tab previews compose with, so an output snapshot shows what
        // that preview's Screen 1 shows — no background of its own.
        return library()
    }

    private fun output(name: String, settings: AppSettings = settings(), content: @Composable () -> Unit) =
        runSkikoComposeUiTest(size = Size(1920f, 1080f), density = Density(1f)) {
            setContent {
                Box(screen) {
                    PresenterScreen(appSettings = settings) { content() }
                }
            }
            waitForIdle()
            onRoot().captureRoboImage("screenshots/output/$name.png")
        }

    private fun verse() = SelectedVerse(
        translationFileName = "kjv1769.spb",
        bibleAbbreviation = "KJV",
        bibleName = "King James Version",
        bookName = "Psalm",
        chapter = 23,
        verseNumber = 1,
        verseText = "The LORD is my shepherd; I shall not want.",
    )

    private fun slideBitmap(): ImageBitmap =
        PDDocument.load(File(LIBRARY_DIR, "Decks/Sermon.pdf")).use { doc ->
            PDFRenderer(doc).renderImageWithDPI(2, 120f).toComposeImageBitmap()
        }

    @Test
    fun `bible verse on screen`() = output("bible") {
        BiblePresenter(selectedVerses = listOf(verse()), appSettings = settings())
    }

    @Test
    fun `song lyrics on screen`() = output("song") {
        SongPresenter(
            lyricSection = LyricSection(
                header = "[Verse 1]",
                title = "Amazing Grace",
                songNumber = 12,
                type = Constants.SECTION_TYPE_VERSE,
                lines = listOf(
                    "Amazing grace! how sweet the sound",
                    "That saved a wretch like me!",
                    "I once was lost, but now am found,",
                    "Was blind, but now I see.",
                ),
            ),
            appSettings = settings(),
        )
    }

    @Test
    fun `picture on screen`() = output("picture") {
        PicturePresenter(
            imagePath = File(LIBRARY_DIR, "Baptism/04 Baptism Pool.png").absolutePath,
        )
    }

    @Test
    fun `presentation slide on screen`() = output("presentation") {
        PresentationPresenter(frame = null, slide = slideBitmap())
    }

    @Test
    fun `announcement on screen`() = output("announcement") {
        AnnouncementsPresenter(
            text = "Christ is risen — He is risen indeed!",
            appSettings = settings(),
        )
    }

    @Test
    fun `countdown on screen`() = output("timer_countdown") {
        AnnouncementsPresenter(text = "05:00", appSettings = settings())
    }

    @Test
    fun `canvas scene on screen`() = output("canvas") {
        ScenePresenter(scene = previewScenes().first())
    }

    @Test
    fun `question on screen`() = output("qa") {
        QAPresenter(
            question = Question(
                id = "q1",
                text = "How do we know the resurrection actually happened?",
                submitterName = "Sarah",
                timestamp = 1_770_000_000_000,
                status = QuestionStatus.APPROVED,
                voteCount = 12,
            ),
        )
    }

    @Test
    fun `dictionary entry on screen`() = output("dictionary") {
        DictionaryPresenter(
            entry = StrongsEntry(
                number = "H2617",
                word = "חֶסֶד",
                transliteration = "chêçêd",
                pronunciation = "kheh'-sed",
                definition = "kindness; by implication (towards God) piety; rarely (by opposition) reproof",
                kjvUsage = "favour, good deed(-liness, -ness), kindly, (loving-) kindness, merciful (kindness), mercy, pity",
            ),
            dictionarySettings = settings().dictionarySettings,
        )
    }

    private companion object {
        val LIBRARY_DIR: File = File("/tmp/ChurchPresenter")
    }
}
