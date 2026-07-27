package org.churchpresenter.app.churchpresenter.presenter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleTranslationSettings
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Two verse layouts the full-screen single-verse tests in [PresenterRenderTest] don't reach:
 * lower-third mode and a multi-verse selection.
 *
 * Lower-third mode is an entirely separate styling path (every font/colour/size picker switches to
 * its `*LowerThird*` field); if it stopped composing the verse the band would go blank on air while
 * the operator's full-screen preview looked fine. Lower-third mode WITH a second Bible exercises the
 * secondary-translation branch of that same path — neither the full-screen bilingual test nor the
 * mono lower-third test reaches it — and both translations must appear in the band.
 *
 * Both assert on the verse text and reference that land on screen, so the layout path runs and
 * nothing races a crossfade.
 */
@OptIn(ExperimentalTestApi::class)
class BiblePresenterLayoutRenderTest {

    private val screen = Modifier.size(1920.dp, 1080.dp)

    private fun verse(
        text: String,
        number: Int,
        book: String = "John",
        chapter: Int = 3,
        abbreviation: String = "KJV",
        fileName: String = "",
    ) = SelectedVerse(
        translationFileName = fileName,
        bibleAbbreviation = abbreviation,
        bibleName = abbreviation,
        bookName = book,
        chapter = chapter,
        verseNumber = number,
        verseText = text,
    )

    @Test
    fun `lower-third mode still composes the verse and its reference`() = runComposeUiTest {
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = listOf(verse("For God so loved the world", 16)),
                    appSettings = AppSettings(),
                    isLowerThird = true,
                )
            }
        }
        onNodeWithText("For God so loved the world", substring = true).assertExists("the band must show the verse on air")
        onNodeWithText("John 3:16", substring = true).assertExists("the lower third still needs its reference")
    }

    @Test
    fun `lower-third mode carries both translations when a second bible is configured`() = runComposeUiTest {
        val bilingual = AppSettings(bibleSettings = BibleSettings(secondaryBible = "RST"))
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = listOf(
                        verse("For God so loved the world", 16),
                        verse("Ибо так возлюбил Бог мир", 16, book = "Иоанна", abbreviation = "RST"),
                    ),
                    appSettings = bilingual,
                    isLowerThird = true,
                )
            }
        }
        onNodeWithText("For God so loved the world", substring = true).assertExists("the band must carry the primary translation")
        onNodeWithText("Ибо так возлюбил Бог мир", substring = true)
            .assertExists("the lower-third secondary-translation style path must render the second language too")
    }

    @Test
    fun `full screen composes every translation in a parallel stack`() = runComposeUiTest {
        val settings = AppSettings(
            bibleSettings = BibleSettings().withTranslations(
                listOf("kjv.spb", "rst.spb", "lut.spb").map {
                    BibleTranslationSettings(fileName = it)
                },
            ),
        )
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = listOf(
                        verse("For God so loved the world", 16, fileName = "kjv.spb"),
                        verse("Ибо так возлюбил Бог мир", 16, abbreviation = "RST", fileName = "rst.spb"),
                        verse("Denn also hat Gott die Welt geliebt", 16, abbreviation = "LUT", fileName = "lut.spb"),
                    ),
                    appSettings = settings,
                )
            }
        }

        onNodeWithText("For God so loved the world", substring = true).assertExists()
        onNodeWithText("Ибо так возлюбил Бог мир", substring = true).assertExists()
        onNodeWithText("Denn also hat Gott die Welt geliebt", substring = true).assertExists()
    }

    @Test
    fun `missing middle verse still renders the later translation`() = runComposeUiTest {
        val settings = AppSettings(
            bibleSettings = BibleSettings().withTranslations(
                listOf("kjv.spb", "missing.spb", "lut.spb").map {
                    BibleTranslationSettings(fileName = it)
                },
            ),
        )
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = listOf(
                        verse("For God so loved the world", 16, fileName = "kjv.spb"),
                        verse("Denn also hat Gott die Welt geliebt", 16, fileName = "lut.spb"),
                    ),
                    appSettings = settings,
                )
            }
        }

        onNodeWithText("For God so loved the world", substring = true).assertExists()
        onNodeWithText("Denn also hat Gott die Welt geliebt", substring = true).assertExists()
    }

    @Test
    fun `four long translations stay inside the output bounds`() = runComposeUiTest {
        val files = listOf("one.spb", "two.spb", "three.spb", "four.spb")
        val settings = AppSettings(
            bibleSettings = BibleSettings().withTranslations(
                files.map { BibleTranslationSettings(fileName = it, textFontSize = 100, referenceFontSize = 70) },
            ),
        )
        val markers = listOf("FIRST", "SECOND", "THIRD", "FOURTH")
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = markers.mapIndexed { index, marker ->
                        verse(
                            text = "$marker ${"long verse text ".repeat(45)}",
                            number = 16,
                            fileName = files[index],
                        )
                    },
                    appSettings = settings,
                )
            }
        }

        markers.forEach { marker ->
            val bounds = onNodeWithText(marker, substring = true).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.top >= 0f, "$marker starts above the output: $bounds")
            assertTrue(bounds.bottom <= 1080f, "$marker is clipped below the output: $bounds")
        }
    }

    @Test
    fun `multi translation spacing separates adjacent blocks`() = runComposeUiTest {
        val settings = AppSettings(
            bibleSettings = BibleSettings(
                multiTranslationSpacing = 80,
                multiTranslationDivider = true,
            ).withTranslations(
                listOf("one.spb", "two.spb").map {
                    BibleTranslationSettings(
                        fileName = it,
                        textFontSize = 40,
                        referenceFontSize = 24,
                        showAbbreviation = true,
                    )
                },
            ),
        )
        setContent {
            Box(screen) {
                BiblePresenter(
                    selectedVerses = listOf(
                        verse("FIRST TEXT", 16, abbreviation = "ONE", fileName = "one.spb"),
                        verse("SECOND TEXT", 16, abbreviation = "TWO", fileName = "two.spb"),
                    ),
                    appSettings = settings,
                )
            }
        }

        val firstReferenceBottom = onNodeWithText("ONE John 3:16").fetchSemanticsNode().boundsInRoot.bottom
        val secondTextTop = onNodeWithText("SECOND TEXT").fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            secondTextTop - firstReferenceBottom >= 75f,
            "configured spacing must remain visible between translation blocks",
        )
    }
}
