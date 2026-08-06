@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.churchpresenter.app.churchpresenter.viewmodel.PresentationViewModel
import presentation.engine.LoadResult
import presentation.engine.model.Deck
import presentation.engine.model.DeckFormat
import presentation.engine.model.DeckSource
import presentation.engine.model.Fidelity
import presentation.engine.model.LayerSpec
import presentation.engine.model.RectPt
import presentation.engine.model.Slide
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The banner that explains why an embedded video is not playing.
 *
 * A deck with embedded video degrades gracefully without VLC — the slide shows its static poster
 * forever — so the banner is the *only* thing telling an operator why, and it has to appear during
 * preparation rather than leave them wondering mid-service.
 *
 * **`presentationTab` has taken a `vlcAvailable` parameter all along, documented as deciding exactly
 * this banner, and no test ever passed it.** That is why the whole composable was uncovered: not
 * because VLC availability is environment-dependent — it is a plain parameter here — but because the
 * scaffolding was sitting unused. (`MediaTab`'s equivalent is driven the same way by
 * `MediaTabVlcUnavailableTest`.)
 *
 * The deck is a synthetic [Deck] over a **real one-page PDF**: the view model rasterises whatever
 * `loadDeck` returns, so the source file has to be openable, while the `slides` list is ours to
 * shape — which is the only way to get a `LayerSpec.Media` layer without an actual PowerPoint
 * carrying an embedded video.
 */
class PresentationTabVlcBannerTest {

    private companion object {
        /**
         * The banner's title, which is the same whichever reason it gives underneath.
         *
         * **Only the title is asserted, deliberately.** The detail line picks between three strings
         * using `isVlcArchMismatch` / `isVlcLoadFailed`, and both of those derive from the JVM-wide
         * `isVlcAvailable` rather than from this tab's `vlcAvailable` parameter — so the detail says
         * "install VLC" on a machine that has it and "failed to load" on one that does not. Pinning
         * it passes locally and fails on CI, which is exactly what it did before this comment
         * existed. (`MediaTab` takes `vlcArchMismatch`/`vlcLoadFailed` as parameters and so can
         * assert all three — see `MediaTabVlcUnavailableTest`. `PresentationTab` does not; making it
         * symmetrical would be a production change worth its own PR.)
         */
        const val TITLE = "VLC media player is required for media playback"
    }

    private val temps = mutableListOf<File>()

    @AfterTest
    fun cleanUp() {
        temps.forEach { it.deleteRecursively() }
        temps.clear()
    }

    /** A real one-page PDF — the rasteriser has to be able to open it. */
    private fun pdf(): File {
        val dir = Files.createTempDirectory("cp-vlc-banner").toFile().also { temps += it }
        val file = File(dir, "deck.pdf")
        PDDocument().use { doc -> doc.addPage(PDPage()); doc.save(file) }
        return file
    }

    private fun deck(file: File, withVideo: Boolean) = Deck(
        sourceFile = file,
        format = DeckFormat.PDF,
        slideWidthPt = 720.0,
        slideHeightPt = 540.0,
        slides = listOf(
            Slide(
                index = 0,
                notes = "",
                transition = null,
                layers = if (withVideo) listOf(
                    LayerSpec.Media(
                        id = "media-0",
                        zIndex = 0,
                        boundsPt = RectPt(0.0, 0.0, 100.0, 100.0),
                        shapeIndex = 0,
                        contentRectPt = RectPt(0.0, 0.0, 100.0, 100.0),
                        mediaFile = null,
                    )
                ) else emptyList(),
                timeline = null,
                fidelity = Fidelity.NATIVE,
            )
        ),
        source = DeckSource.Pdf(file),
    )

    /**
     * Waits for a load that started at [generationBefore] to finish.
     *
     * Keyed on `loadGeneration` rather than on `slideFiles.isNotEmpty()`: the second deck in a test
     * starts with the first one's slides still on screen, so a "has slides" wait returns instantly
     * and the assertions run against the *previous* deck. That cost a failure that looked like a
     * production bug — the banner appearing not to come back — and was this helper all along.
     */
    private fun ComposeUiTest.awaitLoad(vm: PresentationViewModel, generationBefore: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (vm.loadGeneration != generationBefore && !vm.isLoading) { waitForIdle(); return }
            Thread.sleep(20)
        }
        throw AssertionError("the deck never finished rasterising")
    }

    private fun ComposeUiTest.countOf(text: String) =
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes(false).size

    /**
     * The banner's own dismiss button: the clickable "Clear" sitting in the banner's own row.
     *
     * Three things make this harder than it looks, and all three were found the hard way:
     *
     *  * `"Clear"` matches the `IconButton` **and** the `Icon` inside it, and only the outer one is
     *    clickable — hence `hasClickAction()`.
     *  * The tab has other `"Clear"` buttons. One belongs to a **presentation left in the shared
     *    test home by another suite** (`uploaded.pptx`), so how many exist depends on what ran
     *    before this class. Addressing "the only Clear" therefore passes alone and fails in a full
     *    run — which is what it did on CI while passing locally.
     *  * An ancestry matcher does not separate them either: the root is an ancestor of every button
     *    and does contain the banner's title.
     *
     * So it is located by row — the banner's title and its dismiss button share a `Row` with
     * `verticalAlignment = CenterVertically`, so the right button is the one whose centre falls
     * inside the title's vertical span. Exactly one must, and that is asserted rather than assumed.
     */
    private fun ComposeUiTest.dismissBanner() {
        val title = onAllNodesWithText(TITLE, substring = true)
            .fetchSemanticsNodes(false).single().boundsInRoot
        val clears = onAllNodes(hasContentDescription("Clear") and hasClickAction())
        val inTitleRow = clears.fetchSemanticsNodes(false).withIndex().filter { (_, node) ->
            node.boundsInRoot.center.y in title.top..title.bottom
        }
        assertEquals(
            1, inTitleRow.size,
            "expected exactly one dismiss button in the banner's row, found ${inTitleRow.size}",
        )
        clears[inTitleRow.single().index].performClick()
        waitForIdle()
    }

    /** Loads one synthetic deck through the real load path and waits for its slides. */
    private fun ComposeUiTest.load(vm: PresentationViewModel, withVideo: Boolean) {
        val file = pdf()
        val before = vm.loadGeneration
        vm.loadDeck = { LoadResult.Success(deck(file, withVideo)) }
        vm.addPresentation(file)
        awaitLoad(vm, before)
    }

    @Test
    fun `a deck with embedded video says why it will not play`() =
        presentationTab(vlcAvailable = false) { vm, _ ->
            load(vm, withVideo = true)

            assertEquals(1, countOf(TITLE))
        }

    @Test
    fun `with VLC present there is nothing to explain`() =
        // The positive twin for the gate: `vlcAvailable` defaults to true here.
        presentationTab { vm, _ ->
            load(vm, withVideo = true)

            assertEquals(0, countOf(TITLE))
        }

    @Test
    fun `a deck with no video is not warned about`() =
        // Most decks are slides only. Warning about VLC on those would be noise on every load for
        // an operator who has no video to play.
        presentationTab(vlcAvailable = false) { vm, _ ->
            load(vm, withVideo = false)

            assertEquals(0, countOf(TITLE))
        }

    @Test
    fun `dismissing it puts it away`() =
        presentationTab(vlcAvailable = false) { vm, _ ->
            load(vm, withVideo = true)
            assertEquals(1, countOf(TITLE))

            dismissBanner()

            assertEquals(0, countOf(TITLE))
        }

    @Test
    fun `loading another deck brings the warning back`() =
        // The dismissal is keyed on the load generation, so it means "not for this deck" rather than
        // "never again" — an operator who waved it away on a slides-only rehearsal still gets told
        // when they open the deck that actually has the video in it.
        presentationTab(vlcAvailable = false) { vm, _ ->
            load(vm, withVideo = true)
            dismissBanner()
            assertEquals(0, countOf(TITLE))

            load(vm, withVideo = true)

            assertEquals(1, countOf(TITLE), "a dismissal must not carry across decks")
        }
}
