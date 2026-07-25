@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.utils.Utils
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import kotlin.test.assertFalse

/**
 * Harness and node locators shared by the `SongSettingsTab` test classes.
 *
 * The tab is the largest pure-View surface in the app (~1,450 lines) and it renders the *same*
 * widgets over and over — eight styled-text blocks each with a font-size field, a font dropdown, a
 * colour field, four style buttons and a shadow row. None of it is refactored for testability, so
 * every locator here works off what the production tree already publishes: `testTag`s on the
 * checkboxes, content descriptions on the stepper and position/vertical-alignment icons, and the
 * text the widgets actually display.
 *
 * Two locator styles are used, deliberately:
 *
 *  * **By value** — for text fields and colour fields. The fixture gives the field under test a
 *    value no other field on the tab holds (e.g. `songNumberFontSize = 111`), then finds it by that
 *    value. Reads like the UI, and is immune to controls being added or reordered around it.
 *  * **By ordinal** — for the button groups that publish neither a tag nor any text of their own
 *    (horizontal-alignment icons, B/I/U/S style buttons, the segmented mode rows). The index is the
 *    widget's position in composition order, named through the `*Group` objects below.
 *    `SongSettingsTabStructureTest` pins every one of those counts, so if a control is added or
 *    moved that test fails first and says so, instead of an ordinal test failing somewhere obscure.
 *
 * Two production quirks shape the locators. `SlimSlider`'s track publishes no semantics at all, so
 * the transition-duration slider can only be driven by injecting a click at a computed coordinate
 * (see the slider test). And the horizontal-alignment icons pass `contentDescription = null`, unlike
 * their vertical-alignment and position siblings, which is why they need the "a Button with neither
 * text nor a description" matcher below.
 *
 * Known coverage gaps — the four spots in `SongSettingsTab.kt` these tests deliberately do not
 * reach, all of them unreachable rather than untested:
 *
 *  * `else -> Constants.FIRST_PAGE` in each of the four show dropdowns' `onValueChange`. The
 *    dropdown's own `options` list holds exactly the three strings the `when` matches, so no click
 *    can produce a fourth value. (The mirror-image `else -> firstPageStr` on the *render* side is
 *    reachable — a settings file can hold anything — and is covered.)
 *  * `if (lyricsText.isBlank()) return@TextButton` in both auto-fit buttons. The button's `enabled`
 *    condition already requires a non-blank line, so the guard cannot fire from the UI;
 *    `the auto-fit buttons leave the size alone when the live section is blank` asserts that.
 *  * `count == 1` in `segmentedItemShape`. Every segmented row on this tab has two or three items.
 *  * The `when`-on-String jump tables compile to a hash switch followed by an `equals` check; the
 *    "hash matched but the string differs" arms need a hash-colliding value to reach.
 */
@OptIn(ExperimentalTestApi::class)
internal fun songTab(
    initial: AppSettings = AppSettings(),
    presenterManager: PresenterManager? = null,
    block: ComposeUiTest.(get: () -> AppSettings) -> Unit,
) = runComposeUiTest {
    var current = initial
    setContent {
        MaterialTheme {
            var state by remember { mutableStateOf(current) }
            SongSettingsTab(
                settings = state,
                onSettingsChange = { transform -> state = transform(state); current = state },
                presenterManager = presenterManager,
            )
        }
    }
    block { current }
}

// ── Ordinal maps ────────────────────────────────────────────────────────────────────────────────

/** Ordinal of each B/I/U/S button group, in composition order. */
internal object StyleGroup {
    const val TITLE_FULLSCREEN = 0
    const val TITLE_LOWER_THIRD = 1
    const val LYRICS_FULLSCREEN = 2
    const val LYRICS_LOWER_THIRD = 3
    const val LOOK_AHEAD = 4
    const val LOOK_AHEAD_NEXT = 5
    const val LT_LOOK_AHEAD = 6
    const val LT_LOOK_AHEAD_NEXT = 7
    const val COUNT = 8
}

/** Ordinal of each horizontal-alignment button group, in composition order. */
internal object HAlignGroup {
    const val SONG_NUMBER_FULLSCREEN = 0
    const val SONG_NUMBER_LOWER_THIRD = 1
    const val TITLE_FULLSCREEN = 2
    const val TITLE_LOWER_THIRD = 3
    const val LYRICS_FULLSCREEN = 4
    const val LYRICS_LOWER_THIRD = 5
    const val LOOK_AHEAD = 6
    const val LT_LOOK_AHEAD = 7
    const val COUNT = 8
}

/** Ordinal of each above/below position button pair, in composition order. */
internal object PositionGroup {
    const val SONG_NUMBER_FULLSCREEN = 0
    const val SONG_NUMBER_LOWER_THIRD = 1
    const val TITLE_FULLSCREEN = 2
    const val TITLE_LOWER_THIRD = 3
    const val COUNT = 4
}

/** Ordinal of each None/First Page/Every Page dropdown, in composition order. */
internal object ShowDropdown {
    const val NUMBER_FULLSCREEN = 0
    const val NUMBER_LOWER_THIRD = 1
    const val TITLE_FULLSCREEN = 2
    const val TITLE_LOWER_THIRD = 3
    const val COUNT = 4
}

/** Ordinal of each segmented display-mode / language row, in composition order. */
internal object ModeRow {
    const val FULLSCREEN = 0
    const val LOWER_THIRD = 1
    const val LOOK_AHEAD = 2
    const val LT_LOOK_AHEAD = 3
    const val COUNT = 4
}

/** Position of a button inside one horizontal-alignment group — the row is laid out right-first. */
internal object HAlign {
    const val RIGHT = 0
    const val CENTER = 1
    const val LEFT = 2
}

// ── Locators ────────────────────────────────────────────────────────────────────────────────────

/**
 * The horizontal-alignment icon buttons: the only `Role.Button` nodes on the tab carrying neither a
 * content description (the steppers and the position/vertical-alignment icons have one) nor text
 * (the two auto-fit buttons do).
 */
private val horizontalAlignButton =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription) and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.Text)

/** Every `NumberSettingsTextField` on the tab — the stepper fields leave ImeAction at its default. */
internal fun ComposeUiTest.numberFields(): SemanticsNodeInteractionCollection =
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Default))

/** Every `FontSettingsDropdown` on the tab — its editor commits the picked font on ImeAction.Done. */
internal fun ComposeUiTest.fontFields(): SemanticsNodeInteractionCollection =
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done))

/** Every `ColorPickerField` on the tab — each displays its stored value as a `#RRGGBB` string. */
internal fun ComposeUiTest.colorFields(): SemanticsNodeInteractionCollection =
    onAllNodes(hasClickAction() and hasText("#", substring = true))

/** Every None / First Page / Every Page dropdown on the tab. */
internal fun ComposeUiTest.showDropdowns(): SemanticsNodeInteractionCollection =
    onAllNodes(hasClickAction() and (hasText("None") or hasText("First Page") or hasText("Every Page")))

internal fun ComposeUiTest.horizontalAlignButtons(): SemanticsNodeInteractionCollection =
    onAllNodes(horizontalAlignButton)

/** One button of one horizontal-alignment group — [which] is [HAlign.RIGHT]/`CENTER`/`LEFT`. */
internal fun ComposeUiTest.horizontalAlignButton(group: Int, which: Int): SemanticsNodeInteraction =
    horizontalAlignButtons()[group * 3 + which]

internal fun ComposeUiTest.positionButton(group: Int, above: Boolean): SemanticsNodeInteraction =
    onAllNodesWithContentDescription(if (above) "Above" else "Below")[group]

/** One B/I/U/S button — [label] is `"B"`, `"I"`, `"U"` or `"S"`. */
internal fun ComposeUiTest.styleButton(group: Int, label: String): SemanticsNodeInteraction =
    onAllNodes(hasClickAction() and hasText(label))[group]

/** One segmented button, e.g. `segmentedButton("1 Line", ModeRow.LOWER_THIRD)`. */
internal fun ComposeUiTest.segmentedButton(text: String, row: Int): SemanticsNodeInteraction =
    onAllNodesWithText(text)[row]

/** The "Auto" push-buttons next to the lyrics font sizes; only rendered with a PresenterManager. */
internal fun ComposeUiTest.autoFitButtons(): SemanticsNodeInteractionCollection =
    onAllNodes(hasClickAction() and hasText("Auto"))

// ── Actions ─────────────────────────────────────────────────────────────────────────────────────

/** Retypes the number field currently displaying [showing]. */
internal fun ComposeUiTest.retypeNumberField(showing: Int, to: Int) {
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Default) and hasText(showing.toString()))
        .onFirstNode("no number field is showing $showing")
        .performTextReplacement(to.toString())
    waitForIdle()
}

/**
 * Clicks a control whose only feedback is how it is painted, and asserts it repainted.
 *
 * The alignment, position and B/I/U/S buttons publish no `Selected` or `ToggleableState` semantics —
 * a chosen one differs from its neighbours only by border and fill colour. Comparing the node's
 * rendered pixels before and after is therefore the only way to show the UI followed the setting,
 * and it asserts *that* the painting changed rather than what colour it changed to, so it holds
 * across the three target platforms' rendering differences.
 *
 * [node] must be a control the click actually turns on; re-clicking an already-selected button
 * repaints nothing and this would rightly fail.
 */
internal fun ComposeUiTest.clickAndAssertRepaint(node: SemanticsNodeInteraction, what: String) {
    val before = node.performScrollTo().renderedPixels()
    node.performClick()
    waitForIdle()
    assertFalse(
        node.renderedPixels().contentEquals(before),
        "$what must visibly change once it is the selected option",
    )
}

/** The pixels a node currently paints, for controls that publish no state to assert on. */
internal fun SemanticsNodeInteraction.renderedPixels(): IntArray = captureToImage().toPixelMap().buffer

/** Asserts some font dropdown on the tab is displaying [family]. */
internal fun ComposeUiTest.assertFontFieldShows(family: String, what: String) {
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done) and hasText(family))
        .onFirstNode("$what must display $family")
        .assertExists("$what must display $family")
}

/** Asserts some colour field on the tab is displaying [hex], whatever case it was stored in. */
internal fun ComposeUiTest.assertColorFieldShows(hex: String, what: String) {
    onAllNodes(hasClickAction() and hasText(hex, ignoreCase = true))
        .onFirstNode("$what must display $hex")
        .assertExists("$what must display $hex")
}

/** Asserts some number field on the tab is displaying [value]. */
internal fun ComposeUiTest.assertNumberFieldShows(value: Int, what: String) {
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Default) and hasText(value.toString()))
        .onFirstNode("$what must display $value")
        .assertExists("$what must display $value")
}

/**
 * Picks [to] in the font dropdown currently displaying [showing]. Typing filters the menu; the
 * dropdown commits on the IME action when the filter leaves exactly one candidate, so [to] must be
 * a font name that is a substring of no other installed family — see [uniquelyNamedFont].
 */
internal fun ComposeUiTest.pickFont(showing: String, to: String) {
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done) and hasText(showing))
        .onFirstNode("no font dropdown is showing $showing")
        .performTextReplacement(to)
    waitForIdle()
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done) and hasText(to))
        .onFirstNode("the font dropdown should now hold the typed filter $to")
        .performImeAction()
    waitForIdle()
}

/**
 * Types [filter] into the font dropdown currently displaying [showing] without committing it. The
 * dropdown treats typing purely as a menu filter, so nothing is written back to the settings.
 */
internal fun ComposeUiTest.pickFontFilterOnly(showing: String, filter: String) {
    onAllNodes(hasSetTextAction() and hasImeAction(ImeAction.Done) and hasText(showing))
        .onFirstNode("no font dropdown is showing $showing")
        .performTextReplacement(filter)
    waitForIdle()
}

/** Opens the colour field currently displaying [showingHex] and returns with its dialog up. */
internal fun ComposeUiTest.openColorField(showingHex: String) {
    onAllNodes(hasClickAction() and hasText(showingHex))
        .onFirstNode("no colour field is showing $showingHex")
        .performScrollTo()
        .performClick()
    waitForIdle()
}

/** In an open colour dialog: types [hex] and confirms. The hex box is the only editable "#" field. */
internal fun ComposeUiTest.confirmColorDialogWith(hex: String) {
    onAllNodes(hasSetTextAction() and hasText("#", substring = true))
        .onFirstNode("the colour dialog must offer a hex field")
        .performTextReplacement(hex)
    waitForIdle()
    onNodeWithText("OK").performClick()
    waitForIdle()
}

/** Opens the colour field showing [fromHex], types [toHex] and confirms — the whole round trip. */
internal fun ComposeUiTest.recolor(fromHex: String, toHex: String) {
    openColorField(fromHex)
    confirmColorDialogWith(toHex)
}

/**
 * Opens the [group]-th show dropdown and picks [option] from its menu.
 *
 * The open menu's item and the field behind it both carry the option's text, so the item is picked
 * out by having *only* that text — the field always also carries its own "FULL SCREEN"/"LOWER THIRD"
 * caption.
 */
internal fun ComposeUiTest.chooseShowOption(group: Int, option: String) {
    showDropdowns()[group].performScrollTo().performClick()
    waitForIdle()
    onNode(hasTextExactly(option) and hasClickAction()).performClick()
    waitForIdle()
}

// ── Fixtures ────────────────────────────────────────────────────────────────────────────────────

/**
 * A font family whose name appears in no other installed family's name, so typing it into a
 * `FontSettingsDropdown` filters the menu down to exactly one candidate — which is the only state
 * from which the dropdown commits on the IME action.
 */
internal fun uniquelyNamedFont(): String {
    val fonts = Utils.getAvailableSystemFonts()
    return fonts.first { candidate -> fonts.count { it.contains(candidate, ignoreCase = true) } == 1 }
}

/** A font name no installed family matches, used to park a dropdown on a value only it holds. */
internal const val SENTINEL_FONT = "ZzUnusedTestFont"

private fun SemanticsNodeInteractionCollection.onFirstNode(message: String): SemanticsNodeInteraction {
    val count = fetchSemanticsNodes(atLeastOneRootRequired = false).size
    check(count > 0) { message }
    return get(0)
}
