package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.data.settings.ProjectionSettings
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleTranslationSettings
import org.churchpresenter.app.churchpresenter.utils.Constants
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Bible settings tab: the widest tab in the app — two bible pickers, a swap, five checkboxes,
 * an alignment group, a transition slider, four margin fields, and then the same block of styling
 * controls repeated for four targets (primary text, primary reference, secondary text, secondary
 * reference), each in a full-screen and a lower-third variant.
 *
 * Settings are held in test state and fed straight back in, exactly as `OptionsDialog` does, so each
 * interaction is followed through to the text it changes. Nothing in the tab is modified for these
 * tests: no parameter, no test tag, no reflection, no mocks.
 *
 * Finding controls, in order of preference:
 * - by their own text (`B`/`I`/`U`/`S` style toggles, dropdown values, number-field contents);
 * - by content description (the swap button, vertical alignment, reference position);
 * - by position, for the two kinds that expose neither: the transition slider's track, and the
 *   horizontal-alignment icons whose `contentDescription` is null. Those are located relative to the
 *   measured bounds of the label in their own row and then ordered left-to-right, so a test names the
 *   button the way the operator sees it rather than by composition order.
 *
 * Number fields are given distinct starting values per test so each one can be found by its own
 * contents; sharing a default would make "the field showing 54" ambiguous.
 */
@OptIn(ExperimentalTestApi::class)
class BibleSettingsTabTest {

    private val temps = mutableListOf<File>()

    @AfterTest
    fun cleanup() = temps.forEach { it.deleteRecursively() }

    private fun tempDir(): File = Files.createTempDirectory("cp-bible-tab").toFile().also { temps.add(it) }

    /** A bible folder holding [titles] keyed by file name; a null title leaves the file untitled. */
    private fun bibleFolder(vararg files: Pair<String, String?>): File = tempDir().also { dir ->
        files.forEach { (name, title) ->
            File(dir, name).writeText(if (title != null) "##Title: $title\n" else "no header here\n")
        }
    }

    private class Harness {
        var current by mutableStateOf(AppSettings())
    }

    private fun ComposeUiTest.showTab(initial: AppSettings = AppSettings()): Harness {
        val harness = Harness().apply { current = initial }
        setContent {
            MaterialTheme {
                BibleSettingsTab(
                    settings = harness.current,
                    onSettingsChange = { transform -> harness.current = transform(harness.current) },
                )
            }
        }
        return harness
    }

    private fun ComposeUiTest.showBibleTab(bible: BibleSettings): Harness =
        showTab(AppSettings(bibleSettings = bible))

    /**
     * [settings] as they come back from settings.json: encoded exactly the way `SettingsManager`
     * writes the file and decoded the way it reads one. A control that updates state but whose field
     * does not survive the file would silently lose the operator's change on the next launch.
     */
    private fun persisted(settings: AppSettings): BibleSettings =
        Json { ignoreUnknownKeys = true }
            .decodeFromString(
                AppSettings.serializer(),
                Json { encodeDefaults = true }.encodeToString(AppSettings.serializer(), settings),
            )
            .bibleSettings

    // ── Structure ─────────────────────────────────────────────────────────────

    @Test
    fun `every section of the tab renders`() = runComposeUiTest {
        showTab()

        listOf(
            "Bible Selection",
            "Split Browse Mode",
            "Vertical alignment:",
            "Transition",
            "Text Margins",
            "Primary Bible Text",
            "Primary Bible Book Reference",
            "Secondary Bible Text",
            "Secondary Bible Book Reference",
        ).forEach { title ->
            onAllNodesWithText(title).onFirst().assertExists("the \"$title\" section must render")
        }
    }

    @Test
    fun `translation mode can be switched without changing legacy selection`() = runComposeUiTest {
        val harness = showBibleTab(
            BibleSettings(primaryBible = "first.spb", secondaryBible = "second.spb"),
        )

        // Both mode labels are asserted by name: they are the only place a user is told which mode
        // they are in, and the first of them was called "Legacy" until it was renamed for reading
        // as deprecated rather than as the ordinary two-Bible setup it describes.
        onNodeWithText("Dual translation").assertExists()
        onNodeWithText("Multi-translation").performClick()
        waitForIdle()

        assertTrue(harness.current.bibleSettings.multiTranslationMode)
        assertEquals("first.spb", harness.current.bibleSettings.primaryBible)
        assertEquals("second.spb", harness.current.bibleSettings.secondaryBible)
    }

    @Test
    fun `multi mode exposes no lower-third translation settings`() = runComposeUiTest {
        showBibleTab(
            BibleSettings().withTranslations(
                listOf(
                    BibleTranslationSettings(fileName = "first.spb"),
                    BibleTranslationSettings(fileName = "second.spb"),
                ),
            ),
        )

        onAllNodesWithText("Lower Third", substring = true).assertCountEquals(0)
        onNodeWithText("Show in Lower Third").assertDoesNotExist()
        onNodeWithText("Primary Bible").assertDoesNotExist()
        onNodeWithText("Secondary Bible").assertDoesNotExist()
        onNodeWithText("Translation 1").assertExists()
        onNodeWithText("Translation 2").assertExists()
        onNodeWithText("Multi-translation layout").assertExists()
        onNodeWithText("Space between translations").assertExists()
        onNodeWithText("Show divider between translations").assertExists()
    }

    @Test
    fun `each styling section offers the same block of controls`() = runComposeUiTest {
        showTab()

        // Four styling targets, so four of each styling row; Position belongs to the two reference
        // sections only.
        onAllNodesWithText("Color:").assertCountEquals(4)
        onAllNodesWithText("Font Type:").assertCountEquals(4)
        onAllNodesWithText("Font Size:").assertCountEquals(4)
        onAllNodesWithText("Horizontal alignment:").assertCountEquals(4)
        onAllNodesWithText("Position").assertCountEquals(2)
        // Two style-button groups per section: full screen and lower third.
        onAllNodesWithText("B").assertCountEquals(8)
        onAllNodesWithText("I").assertCountEquals(8)
        onAllNodesWithText("U").assertCountEquals(8)
        onAllNodesWithText("S").assertCountEquals(8)
    }

    @Test
    fun `the margins preview and its four fields render`() = runComposeUiTest {
        showBibleTab(BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44))

        onNodeWithText("Screen").assertExists("the margin preview names the screen it stands for")
        listOf("11", "22", "33", "44").forEach { value ->
            onAllNodesWithText(value).onFirst().assertExists("the margin field showing $value must render")
        }
    }

    // ── Bible selection ───────────────────────────────────────────────────────

    @Test
    fun `both bible pickers read None until a bible is chosen`() = runComposeUiTest {
        showTab()

        onAllNodesWithText("None").assertCountEquals(2)
    }

    @Test
    fun `the pickers list the bibles in the storage folder by their titles`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version", "untitled.spb" to null)
        showBibleTab(BibleSettings(storageDirectory = dir.path))

        onAllNodesWithText("None")[0].performScrollTo().performClick()
        waitForIdle()

        onAllNodesWithText("King James Version").onFirst()
            .assertExists("a bible with a ##Title header is offered under that title")
        onAllNodesWithText("untitled").onFirst()
            .assertExists("one without a title falls back to its file name")
    }

    @Test
    fun `choosing a bible in the primary picker stores its file name`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path))

        onAllNodesWithText("None")[0].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("King James Version").onLast().performClick()
        waitForIdle()

        assertEquals(
            "kjv.spb",
            harness.current.bibleSettings.primaryBible,
            "the file name is stored, not the title shown",
        )
        assertEquals("kjv.spb", persisted(harness.current).primaryBible, "and it must survive settings.json")
        assertEquals("", harness.current.bibleSettings.secondaryBible, "the other picker is untouched")
        onAllNodesWithText("King James Version").onFirst().assertExists("and the picker now reads it back")
    }

    @Test
    fun `choosing a bible in the secondary picker stores its file name`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path))

        onAllNodesWithText("None")[1].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("King James Version").onLast().performClick()
        waitForIdle()

        assertEquals("kjv.spb", harness.current.bibleSettings.secondaryBible)
        assertEquals("kjv.spb", persisted(harness.current).secondaryBible, "and it must survive settings.json")
        assertEquals("", harness.current.bibleSettings.primaryBible, "the other picker is untouched")
        onAllNodesWithText("King James Version").onFirst().assertExists("the picker reads the choice back")
        onAllNodesWithText("None").assertCountEquals(1, )
    }

    @Test
    fun `choosing None clears the bible that was selected`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path, primaryBible = "kjv.spb"))

        onAllNodesWithText("King James Version")[0].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("None").onLast().performClick()
        waitForIdle()

        assertEquals("", harness.current.bibleSettings.primaryBible, "None means no bible at all")
        assertEquals("", persisted(harness.current).primaryBible, "and it must survive settings.json")
        onAllNodesWithText("None").assertCountEquals(2, )
    }

    @Test
    fun `a bible the folder no longer holds is still shown by its stored name`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        showBibleTab(BibleSettings(storageDirectory = dir.path, primaryBible = "deleted.spb"))

        onAllNodesWithText("deleted.spb").onFirst()
            .assertExists("a missing bible reads back as its file name rather than vanishing")
    }

    @Test
    fun `the swap button is offered only once a secondary bible is set`() = runComposeUiTest {
        showTab()

        onAllNodesWithContentDescription("Swap").assertCountEquals(0)
    }

    @Test
    fun `swapping exchanges the primary and secondary bibles`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version", "niv.spb" to "New International")
        val harness = showBibleTab(
            BibleSettings(storageDirectory = dir.path, primaryBible = "kjv.spb", secondaryBible = "niv.spb")
        )

        onNodeWithContentDescription("Swap").performScrollTo().performClick()
        waitForIdle()

        assertEquals("niv.spb", harness.current.bibleSettings.primaryBible)
        assertEquals("kjv.spb", harness.current.bibleSettings.secondaryBible)
        assertEquals("niv.spb", persisted(harness.current).primaryBible, "and the swap must survive settings.json")
        onAllNodesWithText("New International").onFirst()
            .assertExists("the primary picker now reads the bible that was secondary")
    }

    // ── Checkboxes ────────────────────────────────────────────────────────────
    //
    // In composition order: show-in-lower-third, split browse, fade in, fade out, crossfade, then
    // the two "show book abbreviation" boxes in the reference sections.

    private object Box {
        const val SHOW_IN_LOWER_THIRD = 0
        const val SPLIT_BROWSE = 1
        const val FADE_IN = 2
        const val FADE_OUT = 3
        const val CROSSFADE = 4
        const val PRIMARY_ABBREVIATION = 5
        const val SECONDARY_ABBREVIATION = 6
    }

    @Test
    fun `the tab has exactly the seven checkboxes it is supposed to`() = runComposeUiTest {
        showTab()

        onAllNodes(isToggleable()).assertCountEquals(7)
    }

    @Test
    fun `each checkbox shows the setting it was given`() = runComposeUiTest {
        showBibleTab(
            BibleSettings(
                secondaryBibleLowerThirdEnabled = true,
                splitBrowseMode = false,
                fadeIn = true,
                fadeOut = false,
                crossfade = true,
                primaryShowAbbreviation = false,
                secondaryShowAbbreviation = true,
            )
        )

        onAllNodes(isToggleable())[Box.SHOW_IN_LOWER_THIRD].assertIsOn()
        onAllNodes(isToggleable())[Box.SPLIT_BROWSE].assertIsOff()
        onAllNodes(isToggleable())[Box.FADE_IN].assertIsOn()
        onAllNodes(isToggleable())[Box.FADE_OUT].assertIsOff()
        onAllNodes(isToggleable())[Box.CROSSFADE].assertIsOn()
        onAllNodes(isToggleable())[Box.PRIMARY_ABBREVIATION].assertIsOff()
        onAllNodes(isToggleable())[Box.SECONDARY_ABBREVIATION].assertIsOn()
    }

    /** Clicks checkbox [index] and returns the bible settings that produced. */
    private fun ComposeUiTest.toggle(index: Int, harness: Harness): BibleSettings {
        onAllNodes(isToggleable())[index].performScrollTo().performClick()
        waitForIdle()
        assertEquals(
            harness.current.bibleSettings,
            persisted(harness.current),
            "a checkbox change must survive a settings.json round trip",
        )
        return harness.current.bibleSettings
    }

    @Test
    fun `show in lower third toggles only its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val after = toggle(Box.SHOW_IN_LOWER_THIRD, harness)
        if (after.secondaryBibleLowerThirdEnabled) onAllNodes(isToggleable())[Box.SHOW_IN_LOWER_THIRD].assertIsOn()
        else onAllNodes(isToggleable())[Box.SHOW_IN_LOWER_THIRD].assertIsOff()

        assertEquals(!before.secondaryBibleLowerThirdEnabled, after.secondaryBibleLowerThirdEnabled)
        assertEquals(before.copy(secondaryBibleLowerThirdEnabled = after.secondaryBibleLowerThirdEnabled), after)
    }

    @Test
    fun `split browse mode toggles only its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val after = toggle(Box.SPLIT_BROWSE, harness)
        if (after.splitBrowseMode) onAllNodes(isToggleable())[Box.SPLIT_BROWSE].assertIsOn()
        else onAllNodes(isToggleable())[Box.SPLIT_BROWSE].assertIsOff()

        assertEquals(true, after.splitBrowseMode, "split browse starts off and turns on")
        assertEquals(before.copy(splitBrowseMode = true), after)
    }

    @Test
    fun `fade in toggles only its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val after = toggle(Box.FADE_IN, harness)
        if (after.fadeIn) onAllNodes(isToggleable())[Box.FADE_IN].assertIsOn()
        else onAllNodes(isToggleable())[Box.FADE_IN].assertIsOff()

        assertEquals(false, after.fadeIn, "fade in starts on and turns off")
        assertEquals(before.copy(fadeIn = false), after)
    }

    @Test
    fun `fade out toggles only its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val after = toggle(Box.FADE_OUT, harness)
        if (after.fadeOut) onAllNodes(isToggleable())[Box.FADE_OUT].assertIsOn()
        else onAllNodes(isToggleable())[Box.FADE_OUT].assertIsOff()

        assertEquals(false, after.fadeOut, "fade out starts on and turns off")
        assertEquals(before.copy(fadeOut = false), after)
    }

    @Test
    fun `crossfade toggles only its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val after = toggle(Box.CROSSFADE, harness)
        if (after.crossfade) onAllNodes(isToggleable())[Box.CROSSFADE].assertIsOn()
        else onAllNodes(isToggleable())[Box.CROSSFADE].assertIsOff()

        assertEquals(true, after.crossfade, "crossfade starts off and turns on")
        assertEquals(before.copy(crossfade = true), after)
    }

    @Test
    fun `each show-abbreviation box toggles its own reference only`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        val afterPrimary = toggle(Box.PRIMARY_ABBREVIATION, harness)
        onAllNodes(isToggleable())[Box.PRIMARY_ABBREVIATION].assertIsOn()
        assertEquals(before.copy(primaryShowAbbreviation = true), afterPrimary)

        val afterSecondary = toggle(Box.SECONDARY_ABBREVIATION, harness)
        onAllNodes(isToggleable())[Box.SECONDARY_ABBREVIATION].assertIsOn()
        assertEquals(
            before.copy(primaryShowAbbreviation = true, secondaryShowAbbreviation = true),
            afterSecondary,
        )
    }

    // ── Vertical alignment ────────────────────────────────────────────────────

    @Test
    fun `each vertical alignment button selects its own alignment`() = runComposeUiTest {
        val harness = showTab()

        listOf(
            "Align Top" to Constants.TOP,
            "Align Middle" to Constants.MIDDLE,
            "Align Bottom" to Constants.BOTTOM,
        ).forEach { (description, expected) ->
            onNodeWithContentDescription(description).performScrollTo().performClick()
            waitForIdle()
            assertEquals(
                expected,
                harness.current.bibleSettings.verticalAlignment,
                "\"$description\" must select $expected",
            )
            assertEquals(expected, persisted(harness.current).verticalAlignment, "and survive settings.json")
        }
    }

    // ── Transition ────────────────────────────────────────────────────────────

    /**
     * Taps the transition slider at [fraction] of its track. The track is a bare `Box` with pointer
     * input and no semantics; it runs from the row's fixed 120dp label to the value read-out, with
     * `Arrangement.spacedBy(10.dp)` before it, so both edges come from measured bounds.
     */
    private fun ComposeUiTest.tapTransitionSlider(readout: String, fraction: Float) {
        waitForIdle()
        val label = onNodeWithText("Transition Duration:").fetchSemanticsNode()
        val value = onNodeWithText(readout).fetchSemanticsNode()
        val left = label.boundsInRoot.right
        val right = value.boundsInRoot.left - 10f * label.layoutInfo.density.density
        assertTrue(right > left, "the slider track must have measurable width")

        onRoot().performTouchInput { click(Offset(left + (right - left) * fraction, value.boundsInRoot.center.y)) }
        waitForIdle()
    }

    @Test
    fun `the transition slider reads out its duration`() = runComposeUiTest {
        showBibleTab(BibleSettings(transitionDuration = 850f))

        onNodeWithText("850ms").assertExists()
    }

    @Test
    fun `tapping the far end of the transition slider selects the longest duration`() = runComposeUiTest {
        val harness = showTab()

        tapTransitionSlider("500ms", fraction = 1f)

        assertEquals(2000f, harness.current.bibleSettings.transitionDuration)
        assertEquals(2000f, persisted(harness.current).transitionDuration, "and it must survive settings.json")
        onNodeWithText("2000ms").assertExists()
    }

    @Test
    fun `tapping the near end of the transition slider selects the shortest duration`() = runComposeUiTest {
        val harness = showTab()

        tapTransitionSlider("500ms", fraction = 0f)

        assertEquals(100f, harness.current.bibleSettings.transitionDuration)
        onNodeWithText("100ms").assertExists()
    }

    @Test
    fun `the transition slider snaps to fifty-millisecond steps`() = runComposeUiTest {
        val harness = showTab()

        tapTransitionSlider("500ms", fraction = 1f / 3f)

        val duration = harness.current.bibleSettings.transitionDuration
        assertEquals(0f, duration % 50f, "every duration is a multiple of 50ms, was $duration")
        assertTrue(duration in 100f..2000f, "and inside the range, was $duration")
        assertEquals(duration, persisted(harness.current).transitionDuration, "and survive settings.json")
        onNodeWithText("${duration.toInt()}ms").assertExists("the read-out shows the snapped value")
    }

    // ── Text margins ──────────────────────────────────────────────────────────

    @Test
    fun `the top margin field writes the top margin`() = runComposeUiTest {
        // Distinct starting values so every field is findable by what it is showing.
        val harness = showBibleTab(
            BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44)
        )

        onNodeWithText("11").performScrollTo().performTextReplacement("60")
        waitForIdle()

        assertEquals(60, harness.current.bibleSettings.marginTop, "the top margin")
        assertEquals(60, persisted(harness.current).marginTop, "and it must survive settings.json")
        onAllNodesWithText("60").onFirst().assertExists("the field shows what was typed")
    }

    @Test
    fun `the left margin field writes the left margin`() = runComposeUiTest {
        // Distinct starting values so every field is findable by what it is showing.
        val harness = showBibleTab(
            BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44)
        )

        onNodeWithText("22").performScrollTo().performTextReplacement("70")
        waitForIdle()

        assertEquals(70, harness.current.bibleSettings.marginLeft, "the left margin")
        assertEquals(70, persisted(harness.current).marginLeft, "and it must survive settings.json")
        onAllNodesWithText("70").onFirst().assertExists("the field shows what was typed")
    }

    @Test
    fun `the right margin field writes the right margin`() = runComposeUiTest {
        // Distinct starting values so every field is findable by what it is showing.
        val harness = showBibleTab(
            BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44)
        )

        onNodeWithText("33").performScrollTo().performTextReplacement("80")
        waitForIdle()

        assertEquals(80, harness.current.bibleSettings.marginRight, "the right margin")
        assertEquals(80, persisted(harness.current).marginRight, "and it must survive settings.json")
        onAllNodesWithText("80").onFirst().assertExists("the field shows what was typed")
    }

    @Test
    fun `the bottom margin field writes the bottom margin`() = runComposeUiTest {
        // Distinct starting values so every field is findable by what it is showing.
        val harness = showBibleTab(
            BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44)
        )

        onNodeWithText("44").performScrollTo().performTextReplacement("90")
        waitForIdle()

        assertEquals(90, harness.current.bibleSettings.marginBottom, "the bottom margin")
        assertEquals(90, persisted(harness.current).marginBottom, "and it must survive settings.json")
        onAllNodesWithText("90").onFirst().assertExists("the field shows what was typed")
    }

    @Test
    fun `a margin beyond the allowed range is not stored`() = runComposeUiTest {
        val harness = showBibleTab(BibleSettings(marginTop = 11, marginLeft = 22, marginRight = 33, marginBottom = 44))

        onNodeWithText("11").performScrollTo().performTextReplacement("900")
        waitForIdle()

        assertEquals(11, harness.current.bibleSettings.marginTop, "500 is the largest margin the field accepts")
        assertEquals(11, persisted(harness.current).marginTop, "and nothing out of range reaches settings.json")
        onAllNodesWithText("900").onFirst()
            .assertExists("the field still shows the rejected entry, so the operator can correct it")
    }

    // ── Horizontal alignment: icons with no description of their own ──────────

    /**
     * The clickable icons sitting on the same row as [label], ordered left to right — which for an
     * alignment group is exactly left, centre, right, and for a position group above, below.
     */
    private fun ComposeUiTest.iconsBesideLabel(label: SemanticsNode): List<SemanticsNode> {
        val row = label.boundsInRoot
        return onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .filter { it.boundsInRoot.center.y in row.top..row.bottom && it.boundsInRoot.left >= row.right }
            .sortedBy { it.boundsInRoot.left }
    }

    @Test
    fun `the horizontal alignment icons are laid out right, centre then left`() = runComposeUiTest {
        val harness = showTab()

        // NOTE: HorizontalAlignmentButtons declares its buttons right, centre, left, and a plain LTR
        // Row lays them out in that order — so the LEFTMOST icon is the one that aligns text right.
        // This pins what the tab actually does today; see also the vertical group, declared
        // bottom, middle, top.
        val label = onAllNodesWithText("Full Screen")[0].fetchSemanticsNode()
        val icons = iconsBesideLabel(label)
        assertEquals(3, icons.size, "an alignment group offers three choices")

        listOf(Constants.RIGHT, Constants.CENTER, Constants.LEFT).forEachIndexed { index, expected ->
            onRoot().performTouchInput { click(icons[index].boundsInRoot.center) }
            waitForIdle()
            assertEquals(
                expected,
                harness.current.bibleSettings.primaryBibleHorizontalAlignment,
                "icon $index from the left must select $expected",
            )
            assertEquals(
                expected,
                persisted(harness.current).primaryBibleHorizontalAlignment,
                "and survive settings.json",
            )
        }
    }

    // ── The four styling targets ──────────────────────────────────────────────
    //
    // In composition order the right-hand column is primary text, primary reference, secondary text,
    // secondary reference, and each renders its full-screen block before its lower-third block. That
    // makes every repeated control addressable as (section * 2 + variant).

    private data class Target(
        val name: String,
        val bold: (BibleSettings) -> Boolean,
        val italic: (BibleSettings) -> Boolean,
        val underline: (BibleSettings) -> Boolean,
        val shadow: (BibleSettings) -> Boolean,
        val fontType: (BibleSettings) -> String,
        val fontSize: (BibleSettings) -> Int,
        val alignment: (BibleSettings) -> String,
        val withFontSize: (BibleSettings, Int) -> BibleSettings,
    )

    private val targets = listOf(
        Target("primary text, full screen",
            { it.primaryBibleBold }, { it.primaryBibleItalic }, { it.primaryBibleUnderline }, { it.primaryBibleShadow },
            { it.primaryBibleFontType }, { it.primaryBibleFontSize }, { it.primaryBibleHorizontalAlignment },
            { s, v -> s.copy(primaryBibleFontSize = v) }),
        Target("primary text, lower third",
            { it.primaryBibleLowerThirdBold }, { it.primaryBibleLowerThirdItalic }, { it.primaryBibleLowerThirdUnderline }, { it.primaryBibleLowerThirdShadow },
            { it.primaryBibleLowerThirdFontType }, { it.primaryBibleLowerThirdFontSize }, { it.primaryBibleLowerThirdHorizontalAlignment },
            { s, v -> s.copy(primaryBibleLowerThirdFontSize = v) }),
        Target("primary reference, full screen",
            { it.primaryReferenceBold }, { it.primaryReferenceItalic }, { it.primaryReferenceUnderline }, { it.primaryReferenceShadow },
            { it.primaryReferenceFontType }, { it.primaryReferenceFontSize }, { it.primaryReferenceHorizontalAlignment },
            { s, v -> s.copy(primaryReferenceFontSize = v) }),
        Target("primary reference, lower third",
            { it.primaryReferenceLowerThirdBold }, { it.primaryReferenceLowerThirdItalic }, { it.primaryReferenceLowerThirdUnderline }, { it.primaryReferenceLowerThirdShadow },
            { it.primaryReferenceLowerThirdFontType }, { it.primaryReferenceLowerThirdFontSize }, { it.primaryReferenceLowerThirdHorizontalAlignment },
            { s, v -> s.copy(primaryReferenceLowerThirdFontSize = v) }),
        Target("secondary text, full screen",
            { it.secondaryBibleBold }, { it.secondaryBibleItalic }, { it.secondaryBibleUnderline }, { it.secondaryBibleShadow },
            { it.secondaryBibleFontType }, { it.secondaryBibleFontSize }, { it.secondaryBibleHorizontalAlignment },
            { s, v -> s.copy(secondaryBibleFontSize = v) }),
        Target("secondary text, lower third",
            { it.secondaryBibleLowerThirdBold }, { it.secondaryBibleLowerThirdItalic }, { it.secondaryBibleLowerThirdUnderline }, { it.secondaryBibleLowerThirdShadow },
            { it.secondaryBibleLowerThirdFontType }, { it.secondaryBibleLowerThirdFontSize }, { it.secondaryBibleLowerThirdHorizontalAlignment },
            { s, v -> s.copy(secondaryBibleLowerThirdFontSize = v) }),
        Target("secondary reference, full screen",
            { it.secondaryReferenceBold }, { it.secondaryReferenceItalic }, { it.secondaryReferenceUnderline }, { it.secondaryReferenceShadow },
            { it.secondaryReferenceFontType }, { it.secondaryReferenceFontSize }, { it.secondaryReferenceHorizontalAlignment },
            { s, v -> s.copy(secondaryReferenceFontSize = v) }),
        Target("secondary reference, lower third",
            { it.secondaryReferenceLowerThirdBold }, { it.secondaryReferenceLowerThirdItalic }, { it.secondaryReferenceLowerThirdUnderline }, { it.secondaryReferenceLowerThirdShadow },
            { it.secondaryReferenceLowerThirdFontType }, { it.secondaryReferenceLowerThirdFontSize }, { it.secondaryReferenceLowerThirdHorizontalAlignment },
            { s, v -> s.copy(secondaryReferenceLowerThirdFontSize = v) }),
    )

    /** Clicks style toggle [label] (B, I, U or S) belonging to target [index]. */
    private fun ComposeUiTest.clickStyle(label: String, index: Int) {
        onAllNodesWithText(label)[index].performScrollTo().performClick()
        waitForIdle()
    }

    @Test
    fun `a style button changes nothing but its own flag`() = runComposeUiTest {
        val harness = showTab()
        val before = harness.current.bibleSettings

        clickStyle("B", 0)

        assertEquals(
            before.copy(primaryBibleBold = true),
            harness.current.bibleSettings,
            "bolding the primary bible text touches no other setting",
        )
    }

    // ── Shadow detail rows ────────────────────────────────────────────────────

    @Test
    fun `the shadow detail row appears only while that target has a shadow`() = runComposeUiTest {
        val harness = showTab()

        onAllNodesWithText("SIZE (%)", ignoreCase = true).assertCountEquals(0)

        clickStyle("S", 0)
        waitForIdle()
        onAllNodesWithText("SIZE (%)", ignoreCase = true).assertCountEquals(1)
        onAllNodesWithText("INTENSITY (%)", ignoreCase = true).assertCountEquals(1)
        assertTrue(harness.current.bibleSettings.primaryBibleShadow)

        clickStyle("S", 0)
        waitForIdle()
        assertTrue(!harness.current.bibleSettings.primaryBibleShadow, "and folds away again")
    }

    @Test
    fun `a font size outside the allowed range is not stored`() = runComposeUiTest {
        var settings = BibleSettings()
        targets.forEachIndexed { index, target -> settings = target.withFontSize(settings, 20 + index) }
        val harness = showBibleTab(settings)

        onNodeWithText("20").performScrollTo().performTextReplacement("400")
        waitForIdle()

        assertEquals(20, harness.current.bibleSettings.primaryBibleFontSize, "150 is the largest size accepted")
        assertEquals(20, persisted(harness.current).primaryBibleFontSize, "and nothing out of range is persisted")
        onAllNodesWithText("400").onFirst().assertExists("the field still shows the rejected entry")
    }

    // ── Font type dropdowns ───────────────────────────────────────────────────

    @Test
    fun `each font dropdown reads back the font it was given`() = runComposeUiTest {
        val fontOfTab = "Serif"
        var settings = BibleSettings(
            primaryBibleFontType = fontOfTab,
            primaryBibleLowerThirdFontType = fontOfTab,
            primaryReferenceFontType = fontOfTab,
            primaryReferenceLowerThirdFontType = fontOfTab,
            secondaryBibleFontType = fontOfTab,
            secondaryBibleLowerThirdFontType = fontOfTab,
            secondaryReferenceFontType = fontOfTab,
            secondaryReferenceLowerThirdFontType = fontOfTab,
        )
        showBibleTab(settings)

        onAllNodesWithText(fontOfTab).assertCountEquals(
            targets.size,
            )
    }

    // ── Reference position ────────────────────────────────────────────────────

    @Test
    fun `each reference position button selects above or below`() = runComposeUiTest {
        val harness = showTab()

        // Four position groups: primary reference (full screen, lower third), then secondary.
        onAllNodesWithContentDescription("Above").assertCountEquals(4)
        onAllNodesWithContentDescription("Below").assertCountEquals(4)

        onAllNodesWithContentDescription("Above")[0].performScrollTo().performClick()
        waitForIdle()
        assertEquals(Constants.POSITION_ABOVE, harness.current.bibleSettings.primaryReferencePosition)

        onAllNodesWithContentDescription("Below")[0].performScrollTo().performClick()
        waitForIdle()
        assertEquals(Constants.POSITION_BELOW, harness.current.bibleSettings.primaryReferencePosition)

        onAllNodesWithContentDescription("Above")[3].performScrollTo().performClick()
        waitForIdle()
        assertEquals(
            Constants.POSITION_ABOVE,
            harness.current.bibleSettings.secondaryReferenceLowerThirdPosition,
            "the last group belongs to the secondary reference's lower third",
        )
    }

    // ── Colour pickers ────────────────────────────────────────────────────────

    @Test
    fun `every colour field shows the colour it was given`() = runComposeUiTest {
        showBibleTab(BibleSettings(primaryBibleColor = "#123456"))

        onAllNodesWithText("#123456", ignoreCase = true).onFirst()
            .assertExists("the colour field reads back its hex")
    }

    @Test
    fun `picking a colour writes it to that target`() = runComposeUiTest {
        val harness = showBibleTab(BibleSettings(primaryBibleColor = "#123456"))

        onAllNodesWithText("#123456", ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("Choose Color", substring = true, ignoreCase = true)
            .assertExists("clicking the swatch opens the colour dialog")

        // Two nodes read "#123456" now: the swatch behind the dialog and the dialog's own hex box.
        // Only the latter takes text input.
        onNode(hasSetTextAction() and hasText("#123456", ignoreCase = true))
            .performTextReplacement("#ABCDEF")
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(
            "#ABCDEF",
            harness.current.bibleSettings.primaryBibleColor.uppercase(),
            "OK applies the typed hex to the target that opened the dialog",
        )
    }

    @Test
    fun `cancelling the colour dialog leaves the colour alone`() = runComposeUiTest {
        val harness = showBibleTab(BibleSettings(primaryBibleColor = "#123456"))

        onAllNodesWithText("#123456", ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("Cancel").performClick()
        waitForIdle()

        assertEquals("#123456", harness.current.bibleSettings.primaryBibleColor)
        onAllNodesWithText("Choose Color", substring = true, ignoreCase = true).assertCountEquals(0)
        onAllNodesWithText("#123456", ignoreCase = true).onFirst()
            .assertExists("the swatch still reads the colour it had")
    }

    // ── Colour pickers, all eight ─────────────────────────────────────────────

    private data class ColourTarget(
        val name: String,
        val hex: String,
        val get: (BibleSettings) -> String,
        val set: (BibleSettings, String) -> BibleSettings,
    )

    private val colourTargets = listOf(
        ColourTarget("primary text full screen", "#110000", { it.primaryBibleColor }, { s, v -> s.copy(primaryBibleColor = v) }),
        ColourTarget("primary text lower third", "#220000", { it.primaryBibleLowerThirdColor }, { s, v -> s.copy(primaryBibleLowerThirdColor = v) }),
        ColourTarget("primary reference full screen", "#330000", { it.primaryReferenceColor }, { s, v -> s.copy(primaryReferenceColor = v) }),
        ColourTarget("primary reference lower third", "#440000", { it.primaryReferenceLowerThirdColor }, { s, v -> s.copy(primaryReferenceLowerThirdColor = v) }),
        ColourTarget("secondary text full screen", "#550000", { it.secondaryBibleColor }, { s, v -> s.copy(secondaryBibleColor = v) }),
        ColourTarget("secondary text lower third", "#660000", { it.secondaryBibleLowerThirdColor }, { s, v -> s.copy(secondaryBibleLowerThirdColor = v) }),
        ColourTarget("secondary reference full screen", "#770000", { it.secondaryReferenceColor }, { s, v -> s.copy(secondaryReferenceColor = v) }),
        ColourTarget("secondary reference lower third", "#880000", { it.secondaryReferenceLowerThirdColor }, { s, v -> s.copy(secondaryReferenceLowerThirdColor = v) }),
    )

    /** Every colour field given its own distinct hex, so each is findable by what it shows. */
    private fun distinctColours(): BibleSettings =
        colourTargets.fold(BibleSettings()) { acc, target -> target.set(acc, target.hex) }

    @Test
    fun `every colour field shows its own colour`() = runComposeUiTest {
        showBibleTab(distinctColours())

        colourTargets.forEach { target ->
            onAllNodesWithText(target.hex, ignoreCase = true).onFirst()
                .assertExists("${target.name} must read back ${target.hex}")
        }
    }

    @Test
    fun `every reference position group moves its own reference`() = runComposeUiTest {
        val harness = showTab()

        listOf<Pair<String, (BibleSettings) -> String>>(
            "primary reference, full screen" to { it.primaryReferencePosition },
            "primary reference, lower third" to { it.primaryReferenceLowerThirdPosition },
            "secondary reference, full screen" to { it.secondaryReferencePosition },
            "secondary reference, lower third" to { it.secondaryReferenceLowerThirdPosition },
        ).forEachIndexed { index, (name, get) ->
            onAllNodesWithContentDescription("Above")[index].performScrollTo().performClick()
            waitForIdle()
            assertEquals(Constants.POSITION_ABOVE, get(harness.current.bibleSettings), "Above for $name")
            assertEquals(Constants.POSITION_ABOVE, get(persisted(harness.current)), "Above for $name, persisted")

            onAllNodesWithContentDescription("Below")[index].performScrollTo().performClick()
            waitForIdle()
            assertEquals(Constants.POSITION_BELOW, get(harness.current.bibleSettings), "Below for $name")
        }
    }

    // ── Auto-fit ──────────────────────────────────────────────────────────────
    //
    // The four Auto buttons only exist when the tab is given a PresenterManager, and they only
    // enable while a bible verse is actually live on a screen of the matching kind. A real
    // PresenterManager is used: its constructor opens nothing, so the state can simply be set.

    private fun presenterShowing(vararg verses: SelectedVerse): PresenterManager =
        PresenterManager().apply {
            setPresentingMode(Presenting.BIBLE)
            setSelectedVerses(verses.toList())
        }

    private fun verse(text: String) = SelectedVerse(
        bibleName = "King James Version",
        bookName = "John",
        chapter = 3,
        verseNumber = 16,
        verseText = text,
    )

    /** Both a full-screen and a lower-third output, which is what enables the two kinds of Auto. */
    private fun bothScreenKinds() = AppSettings().projectionSettings.copy(
        screenAssignments = listOf(
            ScreenAssignment(displayMode = Constants.DISPLAY_MODE_FULLSCREEN),
            ScreenAssignment(displayMode = Constants.DISPLAY_MODE_LOWER_THIRD_HORIZONTAL),
        )
    )

    private fun ComposeUiTest.showWithPresenter(
        bible: BibleSettings = BibleSettings(),
        presenter: PresenterManager?,
        projection: ProjectionSettings = bothScreenKinds(),
    ): Harness {
        val harness = Harness().apply {
            current = AppSettings(bibleSettings = bible, projectionSettings = projection)
        }
        setContent {
            MaterialTheme {
                BibleSettingsTab(
                    settings = harness.current,
                    onSettingsChange = { transform -> harness.current = transform(harness.current) },
                    presenterManager = presenter,
                )
            }
        }
        return harness
    }

    @Test
    fun `the Auto buttons appear only when the tab is given a presenter`() = runComposeUiTest {
        showTab()

        onAllNodesWithText("Auto").assertCountEquals(0)
    }

    @Test
    fun `all four Auto buttons are offered once a presenter is present`() = runComposeUiTest {
        showWithPresenter(presenter = presenterShowing(verse("For God so loved the world")))

        onAllNodesWithText("Auto").assertCountEquals(4)
    }

    @Test
    fun `Auto is disabled while no bible verse is live`() = runComposeUiTest {
        showWithPresenter(presenter = PresenterManager())

        onAllNodesWithText("Auto").assertCountEquals(4)
        repeat(4) { index -> onAllNodesWithText("Auto")[index].assertIsNotEnabled() }
    }

    @Test
    fun `Auto is disabled when no screen of that kind is configured`() = runComposeUiTest {
        showWithPresenter(
            presenter = presenterShowing(verse("For God so loved the world")),
            projection = AppSettings().projectionSettings.copy(screenAssignments = emptyList()),
        )

        repeat(4) { index -> onAllNodesWithText("Auto")[index].assertIsNotEnabled() }
    }

    @Test
    fun `Auto fits the primary full-screen size to the live verse`() = runComposeUiTest {
        val harness = showWithPresenter(
            bible = BibleSettings(primaryBibleFontSize = 70),
            presenter = presenterShowing(verse("For God so loved the world that he gave his only begotten Son.")),
        )

        onAllNodesWithText("Auto")[0].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.primaryBibleFontSize
        assertEquals(fitted, persisted(harness.current).primaryBibleFontSize, "and survive settings.json")
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != 70, "Auto must compute a size of its own, was still 70")
        assertTrue(fitted > 0, "and a usable one, was $fitted")
        // NOTE: Auto writes whatever fills the 1920x1080 canvas and is NOT clamped to the 8..150
        // the font-size field enforces on typed input — a short verse fits at well over 150. This
        // pins today's behaviour rather than endorsing it.
    }

    @Test
    fun `Auto fits the primary lower-third size to the live verse`() = runComposeUiTest {
        val harness = showWithPresenter(
            bible = BibleSettings(primaryBibleLowerThirdFontSize = 70),
            presenter = presenterShowing(verse("For God so loved the world that he gave his only begotten Son.")),
        )

        onAllNodesWithText("Auto")[1].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.primaryBibleLowerThirdFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != 70, "Auto must compute a size of its own, was still 70")
        assertTrue(fitted in 8..150, "and keep it inside the range the field accepts, was $fitted")
    }

    @Test
    fun `the secondary Auto buttons need a second verse to be live`() = runComposeUiTest {
        val harness = showWithPresenter(
            bible = BibleSettings(secondaryBibleFontSize = 70),
            presenter = presenterShowing(
                verse("For God so loved the world"),
                verse("Denn also hat Gott die Welt geliebt, dass er seinen Sohn gab."),
            ),
        )

        onAllNodesWithText("Auto")[2].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.secondaryBibleFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != 70, "the secondary Auto fits the second verse, was still 70")
        assertTrue(fitted in 8..150, "and stays inside the range, was $fitted")
    }

    @Test
    fun `Auto fits the secondary lower-third size to the live verse`() = runComposeUiTest {
        val harness = showWithPresenter(
            bible = BibleSettings(secondaryBibleLowerThirdFontSize = 70),
            presenter = presenterShowing(
                verse("For God so loved the world"),
                verse("Denn also hat Gott die Welt geliebt, dass er seinen eingeborenen Sohn gab."),
            ),
        )

        onAllNodesWithText("Auto")[3].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.secondaryBibleLowerThirdFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != 70, "the fourth Auto fits the second verse into the lower third, was still 70")
        assertTrue(fitted > 0, "and a usable size, was $fitted")
        assertEquals(
            fitted,
            persisted(harness.current).secondaryBibleLowerThirdFontSize,
            "and it must survive settings.json",
        )
    }

    @Test
    fun `the secondary Auto buttons stay disabled with only one verse live`() = runComposeUiTest {
        showWithPresenter(presenter = presenterShowing(verse("For God so loved the world")))

        onAllNodesWithText("Auto")[2].assertIsNotEnabled()
        onAllNodesWithText("Auto")[3].assertIsNotEnabled()
    }

    // ── One test per styling control ──────────────────────────────────────────
    //
    // Eight targets — four sections, each with a full-screen and a lower-third block — and the same
    // controls repeated for every one of them. Each test below drives exactly one control and is
    // named for it, so a failure says which button on which target broke.

    /** Clicks style toggle [letter] on target [index] and checks only that target's flag flipped. */
    private fun ComposeUiTest.assertStyleToggles(index: Int, letter: String, flag: (Target) -> (BibleSettings) -> Boolean) {
        val harness = showTab()
        val target = targets[index]
        val before = flag(target)(harness.current.bibleSettings)

        val shadowRowsBefore = onAllNodesWithText("SIZE (%)", ignoreCase = true).fetchSemanticsNodes().size

        onAllNodesWithText(letter)[index].performScrollTo().performClick()
        waitForIdle()

        assertEquals(!before, flag(target)(harness.current.bibleSettings), "$letter on ${target.name}")
        assertEquals(
            !before,
            flag(target)(persisted(harness.current)),
            "$letter on ${target.name} must survive a settings.json round trip",
        )
        if (letter == "S") {
            // The visible result of a shadow toggle is its detail row folding in or out.
            assertEquals(
                shadowRowsBefore + if (before) -1 else 1,
                onAllNodesWithText("SIZE (%)", ignoreCase = true).fetchSemanticsNodes().size,
                "the shadow detail row for ${target.name} must follow the toggle",
            )
        }
    }

    @Test
    fun `bold on the primary text, full screen`() = runComposeUiTest { assertStyleToggles(0, "B") { it.bold } }

    @Test
    fun `bold on the primary text, lower third`() = runComposeUiTest { assertStyleToggles(1, "B") { it.bold } }

    @Test
    fun `bold on the primary reference, full screen`() = runComposeUiTest { assertStyleToggles(2, "B") { it.bold } }

    @Test
    fun `bold on the primary reference, lower third`() = runComposeUiTest { assertStyleToggles(3, "B") { it.bold } }

    @Test
    fun `bold on the secondary text, full screen`() = runComposeUiTest { assertStyleToggles(4, "B") { it.bold } }

    @Test
    fun `bold on the secondary text, lower third`() = runComposeUiTest { assertStyleToggles(5, "B") { it.bold } }

    @Test
    fun `bold on the secondary reference, full screen`() = runComposeUiTest { assertStyleToggles(6, "B") { it.bold } }

    @Test
    fun `bold on the secondary reference, lower third`() = runComposeUiTest { assertStyleToggles(7, "B") { it.bold } }

    @Test
    fun `italic on the primary text, full screen`() = runComposeUiTest { assertStyleToggles(0, "I") { it.italic } }

    @Test
    fun `italic on the primary text, lower third`() = runComposeUiTest { assertStyleToggles(1, "I") { it.italic } }

    @Test
    fun `italic on the primary reference, full screen`() = runComposeUiTest { assertStyleToggles(2, "I") { it.italic } }

    @Test
    fun `italic on the primary reference, lower third`() = runComposeUiTest { assertStyleToggles(3, "I") { it.italic } }

    @Test
    fun `italic on the secondary text, full screen`() = runComposeUiTest { assertStyleToggles(4, "I") { it.italic } }

    @Test
    fun `italic on the secondary text, lower third`() = runComposeUiTest { assertStyleToggles(5, "I") { it.italic } }

    @Test
    fun `italic on the secondary reference, full screen`() = runComposeUiTest { assertStyleToggles(6, "I") { it.italic } }

    @Test
    fun `italic on the secondary reference, lower third`() = runComposeUiTest { assertStyleToggles(7, "I") { it.italic } }

    @Test
    fun `underline on the primary text, full screen`() = runComposeUiTest { assertStyleToggles(0, "U") { it.underline } }

    @Test
    fun `underline on the primary text, lower third`() = runComposeUiTest { assertStyleToggles(1, "U") { it.underline } }

    @Test
    fun `underline on the primary reference, full screen`() = runComposeUiTest { assertStyleToggles(2, "U") { it.underline } }

    @Test
    fun `underline on the primary reference, lower third`() = runComposeUiTest { assertStyleToggles(3, "U") { it.underline } }

    @Test
    fun `underline on the secondary text, full screen`() = runComposeUiTest { assertStyleToggles(4, "U") { it.underline } }

    @Test
    fun `underline on the secondary text, lower third`() = runComposeUiTest { assertStyleToggles(5, "U") { it.underline } }

    @Test
    fun `underline on the secondary reference, full screen`() = runComposeUiTest { assertStyleToggles(6, "U") { it.underline } }

    @Test
    fun `underline on the secondary reference, lower third`() = runComposeUiTest { assertStyleToggles(7, "U") { it.underline } }

    @Test
    fun `shadow on the primary text, full screen`() = runComposeUiTest { assertStyleToggles(0, "S") { it.shadow } }

    @Test
    fun `shadow on the primary text, lower third`() = runComposeUiTest { assertStyleToggles(1, "S") { it.shadow } }

    @Test
    fun `shadow on the primary reference, full screen`() = runComposeUiTest { assertStyleToggles(2, "S") { it.shadow } }

    @Test
    fun `shadow on the primary reference, lower third`() = runComposeUiTest { assertStyleToggles(3, "S") { it.shadow } }

    @Test
    fun `shadow on the secondary text, full screen`() = runComposeUiTest { assertStyleToggles(4, "S") { it.shadow } }

    @Test
    fun `shadow on the secondary text, lower third`() = runComposeUiTest { assertStyleToggles(5, "S") { it.shadow } }

    @Test
    fun `shadow on the secondary reference, full screen`() = runComposeUiTest { assertStyleToggles(6, "S") { it.shadow } }

    @Test
    fun `shadow on the secondary reference, lower third`() = runComposeUiTest { assertStyleToggles(7, "S") { it.shadow } }

    /** Types a new size into target [index]'s font-size field. */
    private fun ComposeUiTest.assertFontSizeField(index: Int) {
        var settings = BibleSettings()
        targets.forEachIndexed { i, t -> settings = t.withFontSize(settings, 20 + i) }
        val harness = showBibleTab(settings)
        val target = targets[index]

        onNodeWithText((20 + index).toString()).performScrollTo().performTextReplacement("120")
        waitForIdle()

        assertEquals(120, target.fontSize(harness.current.bibleSettings), "font size for ${target.name}")
        assertEquals(120, target.fontSize(persisted(harness.current)), "and it must survive settings.json")
        onAllNodesWithText("120").onFirst().assertExists("the field must show what was typed")
    }

    @Test
    fun `the font size field of the primary text, full screen`() = runComposeUiTest { assertFontSizeField(0) }

    @Test
    fun `the font size field of the primary text, lower third`() = runComposeUiTest { assertFontSizeField(1) }

    @Test
    fun `the font size field of the primary reference, full screen`() = runComposeUiTest { assertFontSizeField(2) }

    @Test
    fun `the font size field of the primary reference, lower third`() = runComposeUiTest { assertFontSizeField(3) }

    @Test
    fun `the font size field of the secondary text, full screen`() = runComposeUiTest { assertFontSizeField(4) }

    @Test
    fun `the font size field of the secondary text, lower third`() = runComposeUiTest { assertFontSizeField(5) }

    @Test
    fun `the font size field of the secondary reference, full screen`() = runComposeUiTest { assertFontSizeField(6) }

    @Test
    fun `the font size field of the secondary reference, lower third`() = runComposeUiTest { assertFontSizeField(7) }

    /** Picks a different font in target [index]'s dropdown. */
    private fun ComposeUiTest.assertFontDropdown(index: Int) {
        val start = "Serif"
        val chosen = "SansSerif"   // a Java logical family, and it matches the seeded "Serif" query
        val harness = showBibleTab(
            BibleSettings(
                primaryBibleFontType = start, primaryBibleLowerThirdFontType = start,
                primaryReferenceFontType = start, primaryReferenceLowerThirdFontType = start,
                secondaryBibleFontType = start, secondaryBibleLowerThirdFontType = start,
                secondaryReferenceFontType = start, secondaryReferenceLowerThirdFontType = start,
            )
        )
        val target = targets[index]

        onAllNodesWithText(start)[index].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText(chosen).onLast().performScrollTo().performClick()
        waitForIdle()

        assertEquals(chosen, target.fontType(harness.current.bibleSettings), "font for ${target.name}")
        assertEquals(chosen, target.fontType(persisted(harness.current)), "and it must survive settings.json")
        onAllNodesWithText(chosen).onFirst().assertExists("the closed dropdown must read the new font")
    }

    @Test
    fun `the font dropdown of the primary text, full screen`() = runComposeUiTest { assertFontDropdown(0) }

    @Test
    fun `the font dropdown of the primary text, lower third`() = runComposeUiTest { assertFontDropdown(1) }

    @Test
    fun `the font dropdown of the primary reference, full screen`() = runComposeUiTest { assertFontDropdown(2) }

    @Test
    fun `the font dropdown of the primary reference, lower third`() = runComposeUiTest { assertFontDropdown(3) }

    @Test
    fun `the font dropdown of the secondary text, full screen`() = runComposeUiTest { assertFontDropdown(4) }

    @Test
    fun `the font dropdown of the secondary text, lower third`() = runComposeUiTest { assertFontDropdown(5) }

    @Test
    fun `the font dropdown of the secondary reference, full screen`() = runComposeUiTest { assertFontDropdown(6) }

    @Test
    fun `the font dropdown of the secondary reference, lower third`() = runComposeUiTest { assertFontDropdown(7) }

    /** Opens target [index]'s colour swatch, types a hex and accepts it. */
    private fun ComposeUiTest.assertColourPicker(index: Int) {
        val harness = showBibleTab(distinctColours())
        val target = colourTargets[index]
        val chosen = "#0000" + "%02X".format(index + 17)

        onAllNodesWithText(target.hex, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        // The swatch behind the dialog carries the same hex; only the dialog's box takes input.
        onNode(hasSetTextAction() and hasText(target.hex, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(
            chosen.uppercase(),
            target.get(harness.current.bibleSettings).uppercase(),
            "colour for ${target.name}",
        )
        assertEquals(
            chosen.uppercase(),
            target.get(persisted(harness.current)).uppercase(),
            "and it must survive settings.json",
        )
        onAllNodesWithText(chosen, ignoreCase = true).onFirst()
            .assertExists("the swatch must read back the colour that was picked")
    }

    @Test
    fun `the colour picker of the primary text, full screen`() = runComposeUiTest { assertColourPicker(0) }

    @Test
    fun `the colour picker of the primary text, lower third`() = runComposeUiTest { assertColourPicker(1) }

    @Test
    fun `the colour picker of the primary reference, full screen`() = runComposeUiTest { assertColourPicker(2) }

    @Test
    fun `the colour picker of the primary reference, lower third`() = runComposeUiTest { assertColourPicker(3) }

    @Test
    fun `the colour picker of the secondary text, full screen`() = runComposeUiTest { assertColourPicker(4) }

    @Test
    fun `the colour picker of the secondary text, lower third`() = runComposeUiTest { assertColourPicker(5) }

    @Test
    fun `the colour picker of the secondary reference, full screen`() = runComposeUiTest { assertColourPicker(6) }

    @Test
    fun `the colour picker of the secondary reference, lower third`() = runComposeUiTest { assertColourPicker(7) }


    // ── Branches the happy path never reaches ─────────────────────────────────

    @Test
    fun `pointing the tab at a different folder relists the bibles`() = runComposeUiTest {
        val first = bibleFolder("kjv.spb" to "King James Version")
        val second = bibleFolder("nasb.spb" to "New American Standard")
        val harness = showBibleTab(BibleSettings(storageDirectory = first.path))

        onAllNodesWithText("None")[0].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("King James Version").onFirst().assertExists("the first folder's bible is offered")
        onAllNodesWithText("None").onLast().performClick()   // re-pick None, closing the menu
        waitForIdle()

        // The storage folder is a setting like any other; changing it must refresh the pickers.
        harness.current = harness.current.copy(
            bibleSettings = harness.current.bibleSettings.copy(storageDirectory = second.path)
        )
        waitForIdle()
        onAllNodesWithText("None")[0].performScrollTo().performClick()
        waitForIdle()

        onAllNodesWithText("New American Standard").onFirst()
            .assertExists("the new folder's bible is offered")
        onAllNodesWithText("King James Version").assertCountEquals(0, )
    }

    @Test
    fun `a secondary bible the folder no longer holds is still shown by its stored name`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        showBibleTab(BibleSettings(storageDirectory = dir.path, secondaryBible = "gone.spb"))

        onAllNodesWithText("gone.spb").onFirst()
            .assertExists("a missing secondary bible reads back as its file name")
    }

    @Test
    fun `choosing None clears the secondary bible`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path, secondaryBible = "kjv.spb"))

        onAllNodesWithText("King James Version")[0].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("None").onLast().performClick()
        waitForIdle()

        assertEquals("", harness.current.bibleSettings.secondaryBible)
        assertEquals("", persisted(harness.current).secondaryBible, "and it must survive settings.json")
        onAllNodesWithText("None").assertCountEquals(2, )
    }

    /** Every style flag on, so Auto measures bold, italic and underlined text rather than plain. */
    /** A starting size no fit will land on by chance, so "Auto did nothing" cannot pass. */
    private val unfittedSize = 9

    private fun fullyStyled() = BibleSettings(
        primaryBibleFontSize = unfittedSize,
        primaryBibleLowerThirdFontSize = unfittedSize,
        secondaryBibleFontSize = unfittedSize,
        secondaryBibleLowerThirdFontSize = unfittedSize,
        primaryBibleBold = true, primaryBibleItalic = true, primaryBibleUnderline = true,
        primaryReferenceBold = true, primaryReferenceItalic = true,
        secondaryBibleBold = true, secondaryBibleItalic = true, secondaryBibleUnderline = true,
        secondaryReferenceBold = true, secondaryReferenceItalic = true,
    )

    private fun twoVerses() = arrayOf(
        verse("For God so loved the world that he gave his only begotten Son."),
        verse("Denn also hat Gott die Welt geliebt, dass er seinen eingeborenen Sohn gab."),
    )

    @Test
    fun `Auto measures the primary full-screen size with its styles applied`() = runComposeUiTest {
        val harness = showWithPresenter(bible = fullyStyled(), presenter = presenterShowing(*twoVerses()))

        onAllNodesWithText("Auto")[0].performScrollTo().performClick()
        waitForIdle()

        // Two verses are live, so the primary only gets half the height — a different branch from
        // the single-verse case, and the styles feed real weights into the measurement.
        val fitted = harness.current.bibleSettings.primaryBibleFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted > 0, "a bold, italic, underlined verse still fits somewhere, was $fitted")
        assertEquals(fitted, persisted(harness.current).primaryBibleFontSize, "and survives settings.json")
    }

    @Test
    fun `Auto measures the primary lower-third size with its styles applied`() = runComposeUiTest {
        val harness = showWithPresenter(bible = fullyStyled(), presenter = presenterShowing(*twoVerses()))

        onAllNodesWithText("Auto")[1].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.primaryBibleLowerThirdFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != unfittedSize, "Auto must replace the starting size, was still $fitted")
        assertEquals(fitted, persisted(harness.current).primaryBibleLowerThirdFontSize)
    }

    @Test
    fun `Auto measures the secondary full-screen size with its styles applied`() = runComposeUiTest {
        val harness = showWithPresenter(bible = fullyStyled(), presenter = presenterShowing(*twoVerses()))

        onAllNodesWithText("Auto")[2].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.secondaryBibleFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != unfittedSize, "Auto must replace the starting size, was still $fitted")
        assertEquals(fitted, persisted(harness.current).secondaryBibleFontSize)
    }

    @Test
    fun `Auto measures the secondary lower-third size with its styles applied`() = runComposeUiTest {
        val harness = showWithPresenter(bible = fullyStyled(), presenter = presenterShowing(*twoVerses()))

        onAllNodesWithText("Auto")[3].performScrollTo().performClick()
        waitForIdle()

        val fitted = harness.current.bibleSettings.secondaryBibleLowerThirdFontSize
        onAllNodesWithText(fitted.toString()).onFirst().assertExists("the field must show the fitted size")
        assertTrue(fitted != unfittedSize, "Auto must replace the starting size, was still $fitted")
        assertEquals(fitted, persisted(harness.current).secondaryBibleLowerThirdFontSize)
    }

    @Test
    fun `Auto stays disabled while the live verse has no text`() = runComposeUiTest {
        showWithPresenter(presenter = presenterShowing(verse("")))

        // Live on a bible, but with nothing to measure: the buttons must not offer to fit blank text.
        repeat(4) { index -> onAllNodesWithText("Auto")[index].assertIsNotEnabled() }
    }

    @Test
    fun `the secondary Auto buttons stay disabled without a screen of their kind`() = runComposeUiTest {
        showWithPresenter(
            presenter = presenterShowing(*twoVerses()),
            projection = AppSettings().projectionSettings.copy(screenAssignments = emptyList()),
        )

        // Two verses are live — so the secondary content qualifies — but there is nowhere to show it.
        onAllNodesWithText("Auto")[2].assertIsNotEnabled()
        onAllNodesWithText("Auto")[3].assertIsNotEnabled()
    }

    @Test
    fun `re-applying equal settings leaves the tab exactly as it was`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path, primaryBible = "kjv.spb"))
        onAllNodesWithText("King James Version").onFirst().assertExists()

        // A distinct but equal instance: the tab must treat this as no change at all, which is the
        // path Compose takes when it decides a section can be skipped rather than recomposed.
        repeat(3) {
            harness.current = harness.current.copy(bibleSettings = harness.current.bibleSettings.copy())
            waitForIdle()
        }

        onAllNodesWithText("King James Version").onFirst()
            .assertExists("the picker still reads its bible after an equal update")
        onAllNodes(isToggleable()).assertCountEquals(7)
        onAllNodesWithText("B").assertCountEquals(8)
    }

    @Test
    fun `Auto stays disabled while something other than a bible is live`() = runComposeUiTest {
        val presenter = PresenterManager().apply {
            setSelectedVerses(twoVerses().toList())
            setPresentingMode(Presenting.LYRICS)
        }
        showWithPresenter(presenter = presenter)

        // Verses are loaded, but song lyrics are on screen — fitting the bible text now would resize
        // against content nobody is looking at.
        repeat(4) { index -> onAllNodesWithText("Auto")[index].assertIsNotEnabled() }
    }

    // ── Shadow detail rows: eight of them, three controls each ────────────────

    /** Every target's shadow switched on, with distinct values so each control is findable. */
    private fun everyShadowShowing() = BibleSettings(
        primaryBibleShadow = true, primaryBibleShadowColor = "#A10011", primaryBibleShadowSize = 201, primaryBibleShadowOpacity = 41,
        primaryBibleLowerThirdShadow = true, primaryBibleLowerThirdShadowColor = "#A10012", primaryBibleLowerThirdShadowSize = 202, primaryBibleLowerThirdShadowOpacity = 42,
        primaryReferenceShadow = true, primaryReferenceShadowColor = "#A10013", primaryReferenceShadowSize = 203, primaryReferenceShadowOpacity = 43,
        primaryReferenceLowerThirdShadow = true, primaryReferenceLowerThirdShadowColor = "#A10014", primaryReferenceLowerThirdShadowSize = 204, primaryReferenceLowerThirdShadowOpacity = 44,
        secondaryBibleShadow = true, secondaryBibleShadowColor = "#A10015", secondaryBibleShadowSize = 205, secondaryBibleShadowOpacity = 45,
        secondaryBibleLowerThirdShadow = true, secondaryBibleLowerThirdShadowColor = "#A10016", secondaryBibleLowerThirdShadowSize = 206, secondaryBibleLowerThirdShadowOpacity = 46,
        secondaryReferenceShadow = true, secondaryReferenceShadowColor = "#A10017", secondaryReferenceShadowSize = 207, secondaryReferenceShadowOpacity = 47,
        secondaryReferenceLowerThirdShadow = true, secondaryReferenceLowerThirdShadowColor = "#A10018", secondaryReferenceLowerThirdShadowSize = 208, secondaryReferenceLowerThirdShadowOpacity = 48,
    )

    @Test
    fun `the shadow colour of the primary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10011"
        val chosen = "#0B0011"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.primaryBibleShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).primaryBibleShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the primary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("201").performScrollTo().performTextReplacement("301")
        waitForIdle()

        assertEquals(301, harness.current.bibleSettings.primaryBibleShadowSize)
        assertEquals(301, persisted(harness.current).primaryBibleShadowSize, "settings.json")
        onAllNodesWithText("301").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the primary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("41").performScrollTo().performTextReplacement("81")
        waitForIdle()

        assertEquals(81, harness.current.bibleSettings.primaryBibleShadowOpacity)
        assertEquals(81, persisted(harness.current).primaryBibleShadowOpacity, "settings.json")
        onAllNodesWithText("81").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the primary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10012"
        val chosen = "#0B0012"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.primaryBibleLowerThirdShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).primaryBibleLowerThirdShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the primary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("202").performScrollTo().performTextReplacement("302")
        waitForIdle()

        assertEquals(302, harness.current.bibleSettings.primaryBibleLowerThirdShadowSize)
        assertEquals(302, persisted(harness.current).primaryBibleLowerThirdShadowSize, "settings.json")
        onAllNodesWithText("302").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the primary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("42").performScrollTo().performTextReplacement("82")
        waitForIdle()

        assertEquals(82, harness.current.bibleSettings.primaryBibleLowerThirdShadowOpacity)
        assertEquals(82, persisted(harness.current).primaryBibleLowerThirdShadowOpacity, "settings.json")
        onAllNodesWithText("82").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the primary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10013"
        val chosen = "#0B0013"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.primaryReferenceShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).primaryReferenceShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the primary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("203").performScrollTo().performTextReplacement("303")
        waitForIdle()

        assertEquals(303, harness.current.bibleSettings.primaryReferenceShadowSize)
        assertEquals(303, persisted(harness.current).primaryReferenceShadowSize, "settings.json")
        onAllNodesWithText("303").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the primary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("43").performScrollTo().performTextReplacement("83")
        waitForIdle()

        assertEquals(83, harness.current.bibleSettings.primaryReferenceShadowOpacity)
        assertEquals(83, persisted(harness.current).primaryReferenceShadowOpacity, "settings.json")
        onAllNodesWithText("83").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the primary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10014"
        val chosen = "#0B0014"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.primaryReferenceLowerThirdShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).primaryReferenceLowerThirdShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the primary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("204").performScrollTo().performTextReplacement("304")
        waitForIdle()

        assertEquals(304, harness.current.bibleSettings.primaryReferenceLowerThirdShadowSize)
        assertEquals(304, persisted(harness.current).primaryReferenceLowerThirdShadowSize, "settings.json")
        onAllNodesWithText("304").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the primary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("44").performScrollTo().performTextReplacement("84")
        waitForIdle()

        assertEquals(84, harness.current.bibleSettings.primaryReferenceLowerThirdShadowOpacity)
        assertEquals(84, persisted(harness.current).primaryReferenceLowerThirdShadowOpacity, "settings.json")
        onAllNodesWithText("84").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the secondary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10015"
        val chosen = "#0B0015"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.secondaryBibleShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).secondaryBibleShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the secondary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("205").performScrollTo().performTextReplacement("305")
        waitForIdle()

        assertEquals(305, harness.current.bibleSettings.secondaryBibleShadowSize)
        assertEquals(305, persisted(harness.current).secondaryBibleShadowSize, "settings.json")
        onAllNodesWithText("305").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the secondary text, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("45").performScrollTo().performTextReplacement("85")
        waitForIdle()

        assertEquals(85, harness.current.bibleSettings.secondaryBibleShadowOpacity)
        assertEquals(85, persisted(harness.current).secondaryBibleShadowOpacity, "settings.json")
        onAllNodesWithText("85").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the secondary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10016"
        val chosen = "#0B0016"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.secondaryBibleLowerThirdShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).secondaryBibleLowerThirdShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the secondary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("206").performScrollTo().performTextReplacement("306")
        waitForIdle()

        assertEquals(306, harness.current.bibleSettings.secondaryBibleLowerThirdShadowSize)
        assertEquals(306, persisted(harness.current).secondaryBibleLowerThirdShadowSize, "settings.json")
        onAllNodesWithText("306").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the secondary text, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("46").performScrollTo().performTextReplacement("86")
        waitForIdle()

        assertEquals(86, harness.current.bibleSettings.secondaryBibleLowerThirdShadowOpacity)
        assertEquals(86, persisted(harness.current).secondaryBibleLowerThirdShadowOpacity, "settings.json")
        onAllNodesWithText("86").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the secondary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10017"
        val chosen = "#0B0017"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.secondaryReferenceShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).secondaryReferenceShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the secondary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("207").performScrollTo().performTextReplacement("307")
        waitForIdle()

        assertEquals(307, harness.current.bibleSettings.secondaryReferenceShadowSize)
        assertEquals(307, persisted(harness.current).secondaryReferenceShadowSize, "settings.json")
        onAllNodesWithText("307").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the secondary reference, full screen`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("47").performScrollTo().performTextReplacement("87")
        waitForIdle()

        assertEquals(87, harness.current.bibleSettings.secondaryReferenceShadowOpacity)
        assertEquals(87, persisted(harness.current).secondaryReferenceShadowOpacity, "settings.json")
        onAllNodesWithText("87").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow colour of the secondary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())
        val was = "#A10018"
        val chosen = "#0B0018"

        onAllNodesWithText(was, ignoreCase = true)[0].performScrollTo().performClick()
        waitForIdle()
        onNode(hasSetTextAction() and hasText(was, ignoreCase = true)).performTextReplacement(chosen)
        waitForIdle()
        onNodeWithText("OK").performClick()
        waitForIdle()

        assertEquals(chosen.uppercase(), harness.current.bibleSettings.secondaryReferenceLowerThirdShadowColor.uppercase())
        assertEquals(chosen.uppercase(), persisted(harness.current).secondaryReferenceLowerThirdShadowColor.uppercase(), "settings.json")
        onAllNodesWithText(chosen, ignoreCase = true).onFirst().assertExists("the swatch reads it back")
    }
    @Test
    fun `the shadow size of the secondary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("208").performScrollTo().performTextReplacement("308")
        waitForIdle()

        assertEquals(308, harness.current.bibleSettings.secondaryReferenceLowerThirdShadowSize)
        assertEquals(308, persisted(harness.current).secondaryReferenceLowerThirdShadowSize, "settings.json")
        onAllNodesWithText("308").onFirst().assertExists("the field shows what was typed")
    }
    @Test
    fun `the shadow intensity of the secondary reference, lower third`() = runComposeUiTest {
        val harness = showBibleTab(everyShadowShowing())

        onNodeWithText("48").performScrollTo().performTextReplacement("88")
        waitForIdle()

        assertEquals(88, harness.current.bibleSettings.secondaryReferenceLowerThirdShadowOpacity)
        assertEquals(88, persisted(harness.current).secondaryReferenceLowerThirdShadowOpacity, "settings.json")
        onAllNodesWithText("88").onFirst().assertExists("the field shows what was typed")
    }

    // ── Horizontal alignment: one test per group ──────────────────────────────

    /**
     * Clicks all three icons of the alignment group beside occurrence [labelIndex] of [label] and
     * checks each writes [get]. The icons carry no content description, so they are found by
     * position and then read left to right — which for this app is right, centre, left.
     */
    private fun ComposeUiTest.assertAlignmentGroup(label: String, labelIndex: Int, get: (BibleSettings) -> String) {
        val harness = showTab()
        // Bring the row on screen first: a node still below the viewport reports bounds that sweep
        // in every clickable on the tab.
        onAllNodesWithText(label)[labelIndex].performScrollTo()
        waitForIdle()
        val icons = iconsBesideLabel(onAllNodesWithText(label)[labelIndex].fetchSemanticsNode())
        assertEquals(3, icons.size, "an alignment group offers three choices")

        listOf(Constants.RIGHT, Constants.CENTER, Constants.LEFT).forEachIndexed { index, expected ->
            onRoot().performTouchInput { click(icons[index].boundsInRoot.center) }
            waitForIdle()
            assertEquals(expected, get(harness.current.bibleSettings), "icon $index selects $expected")
            assertEquals(expected, get(persisted(harness.current)), "and it survives settings.json")
        }
    }

    @Test
    fun `the horizontal alignment of the primary text, full screen`() = runComposeUiTest {
        assertAlignmentGroup("Full Screen", 0) { it.primaryBibleHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the primary text, lower third`() = runComposeUiTest {
        assertAlignmentGroup("Lower Third", 0) { it.primaryBibleLowerThirdHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the primary reference, full screen`() = runComposeUiTest {
        assertAlignmentGroup("Full Screen", 2) { it.primaryReferenceHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the primary reference, lower third`() = runComposeUiTest {
        assertAlignmentGroup("Lower Third", 2) { it.primaryReferenceLowerThirdHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the secondary text, full screen`() = runComposeUiTest {
        assertAlignmentGroup("Full Screen", 3) { it.secondaryBibleHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the secondary text, lower third`() = runComposeUiTest {
        assertAlignmentGroup("Lower Third", 3) { it.secondaryBibleLowerThirdHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the secondary reference, full screen`() = runComposeUiTest {
        assertAlignmentGroup("Full Screen", 5) { it.secondaryReferenceHorizontalAlignment }
    }
    @Test
    fun `the horizontal alignment of the secondary reference, lower third`() = runComposeUiTest {
        assertAlignmentGroup("Lower Third", 5) { it.secondaryReferenceLowerThirdHorizontalAlignment }
    }

    @Test
    fun `Auto stays disabled when the bible is live but nothing is selected`() = runComposeUiTest {
        val presenter = PresenterManager().apply { setPresentingMode(Presenting.BIBLE) }
        showWithPresenter(presenter = presenter)

        // Bible mode with an empty selection — between passages, say.
        repeat(4) { index -> onAllNodesWithText("Auto")[index].assertIsNotEnabled() }
    }

    @Test
    fun `the secondary Auto buttons stay disabled when the second verse is blank`() = runComposeUiTest {
        showWithPresenter(
            presenter = presenterShowing(verse("For God so loved the world"), verse("")),
        )

        // A secondary bible is selected but has no text for this verse: nothing to fit.
        onAllNodesWithText("Auto")[2].assertIsNotEnabled()
        onAllNodesWithText("Auto")[3].assertIsNotEnabled()
        // The primary still has text, so its buttons remain live.
        onAllNodesWithText("Auto")[0].assertIsEnabled()
    }

    @Test
    fun `the primary picker can choose a bible that is not the first in the folder`() = runComposeUiTest {
        // Two bibles, and the one chosen is the second: the lookup from the shown title back to a
        // file name has to walk past a non-matching entry to find it.
        val dir = bibleFolder("kjv.spb" to "King James Version", "niv.spb" to "New International")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path))

        onAllNodesWithText("None")[0].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("New International").onLast().performClick()
        waitForIdle()

        assertEquals("niv.spb", harness.current.bibleSettings.primaryBible)
        assertEquals("niv.spb", persisted(harness.current).primaryBible, "and it must survive settings.json")
        onAllNodesWithText("New International").onFirst().assertExists("the picker reads the choice back")
    }

    @Test
    fun `the secondary picker can choose a bible that is not the first in the folder`() = runComposeUiTest {
        val dir = bibleFolder("kjv.spb" to "King James Version", "niv.spb" to "New International")
        val harness = showBibleTab(BibleSettings(storageDirectory = dir.path))

        onAllNodesWithText("None")[1].performScrollTo().performClick()
        waitForIdle()
        onAllNodesWithText("New International").onLast().performClick()
        waitForIdle()

        assertEquals("niv.spb", harness.current.bibleSettings.secondaryBible)
        assertEquals("niv.spb", persisted(harness.current).secondaryBible, "and it must survive settings.json")
        onAllNodesWithText("New International").onFirst().assertExists("the picker reads the choice back")
        onNodeWithContentDescription("Swap").assertExists("a secondary bible brings out the swap button")
    }
}
