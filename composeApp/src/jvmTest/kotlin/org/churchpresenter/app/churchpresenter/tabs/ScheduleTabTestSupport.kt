@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.viewmodel.ScheduleViewModel
import java.io.File
import java.nio.file.Files

/**
 * Harness and fixtures shared by the `ScheduleTab` test classes.
 *
 * The tab is driven through a real [ScheduleViewModel] — the same one the app builds — so what is
 * exercised is the wiring between the two: which view-model call a button makes, and what the list
 * renders from the resulting state. The view model's own rules (undo history, move semantics,
 * remote following) are already covered by the `ScheduleViewModel*` suites, so nothing here
 * re-tests those; these tests assert the schedule the operator ends up with.
 *
 * `user.home` is isolated per test because the view model resolves its autosave path at
 * construction and `newSchedule()` deletes that file. [TestSingletons.latchToTestHome] pins the
 * JVM-wide loggers to the real test home first, so they do not latch onto a temp dir that is then
 * deleted.
 */

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/** What the tab reported back, so a test asserts on the choice rather than on a stub. */
internal class ScheduleReports {
    val presenting = mutableListOf<Presenting>()
    val clicked = mutableListOf<ScheduleItem>()
    val presented = mutableListOf<ScheduleItem>()
    val editedLabels = mutableListOf<ScheduleItem.LabelItem>()
    val selectionChanges = mutableListOf<String?>()
    var addLabelRequests = 0
    var addWebsiteRequests = 0
    val zoomChanges = mutableListOf<Int>()
}

/**
 * Builds a real [ScheduleViewModel] under an isolated `user.home`, seeds it with [seed], composes
 * `ScheduleTab` over it, and runs [block].
 *
 * The view model is created before composition and passed in, so a test can seed it without racing
 * the tab's first frame — and so `block` can read the schedule back from the same instance the tab
 * is driving.
 */
@OptIn(ExperimentalTestApi::class)
internal fun scheduleTab(
    itemZoomPercent: Int = 100,
    seed: ScheduleViewModel.() -> Unit = {},
    block: ComposeUiTest.(vm: ScheduleViewModel, reports: ScheduleReports) -> Unit,
) {
    TestSingletons.latchToTestHome()
    val realHome = System.getProperty("user.home")
    val tempHome: File = Files.createTempDirectory("cp-schedule-tab").toFile()
    System.setProperty("user.home", tempHome.absolutePath)
    val vm = ScheduleViewModel()
    try {
        vm.seed()
        val reports = ScheduleReports()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    ScheduleTab(
                        scheduleViewModel = vm,
                        itemZoomPercent = itemZoomPercent,
                        onItemZoomChange = { reports.zoomChanges += it },
                        onPresenting = { reports.presenting += it },
                        onItemClick = { reports.clicked += it },
                        onEditLabel = { reports.editedLabels += it },
                        onSelectedItemChanged = { reports.selectionChanges += it },
                        onAddLabel = { reports.addLabelRequests++ },
                        onAddWebsite = { reports.addWebsiteRequests++ },
                        onPresentSong = { reports.presented += it },
                        onPresentBible = { reports.presented += it },
                        onPresentWebsite = { reports.presented += it },
                        onPresentAnnouncement = { reports.presented += it },
                        onPresentMedia = { reports.presented += it },
                        onPresentLowerThird = { reports.presented += it },
                        onPresentDictionary = { reports.presented += it },
                    )
                }
            }
            block(vm, reports)
        }
    } finally {
        runCatching { vm.dispose() }
        realHome?.let { System.setProperty("user.home", it) }
        tempHome.deleteRecursively()
    }
}

// ── Fixtures ────────────────────────────────────────────────────────────────────────────────────

/** A service order with one of each item type the row renderer draws differently. */
internal fun ScheduleViewModel.seedService() {
    addLabel("Welcome", "#FFFFFF", "#203040")
    addSong(songNumber = 42, title = "Amazing Grace", songbook = "Hymnal")
    addBibleVerse(
        bookName = "John", chapter = 3, verseNumber = 16,
        verseText = "For God so loved the world.",
    )
    addWebsite(url = "https://example.org", title = "Notices")
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object ScheduleLabel {
    const val TITLE = "Schedule"
    const val NEW = "New Schedule"
    const val UNDO = "Undo (Ctrl+Z)"
    const val REDO = "Redo (Ctrl+Shift+Z)"
    const val ADD_LABEL = "Add Label"
    const val REMOVE_SELECTED = "Remove from Schedule"
    const val CLEAR = "Clear Schedule"
    const val ZOOM_IN = "Zoom In"
    const val ZOOM_OUT = "Zoom Out"
    const val DROP_HINT = "Drag files here to add to schedule"
    const val MOVE_UP = "Move Up"
    const val MOVE_DOWN = "Move Down"
    const val GO_LIVE = "Go Live"
    const val REMOVE = "Remove"
    const val NOTE = "Note"
    const val EDIT_LABEL = "Edit Label"
    const val NOTE_SAVE = "Save note"
    const val NOTE_CLEAR = "Clear note"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────

/** Every string on screen. */
internal fun ComposeUiTest.renderedText(): List<String> =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { t -> t.text } }

internal fun ComposeUiTest.showsExactly(text: String): Boolean = renderedText().any { it == text }

internal fun ComposeUiTest.showsContainingText(fragment: String): Boolean =
    renderedText().any { it.contains(fragment) }

/**
 * A toolbar or row button, addressed by the content description [TooltipIconButton] gives it —
 * which is its tooltip text, so the label a user would see is also the test's selector.
 */
internal fun ComposeUiTest.button(label: String) = onNodeWithContentDescription(label)

/**
 * The [n]th button with this label, top to bottom.
 *
 * Row buttons repeat once per schedule item, so a test that means "the second item's Go Live"
 * addresses it by position — which is also what the operator is doing.
 */
internal fun ComposeUiTest.buttonAt(label: String, n: Int) = onAllNodesWithContentDescription(label)[n]

internal fun ComposeUiTest.buttonCount(label: String): Int =
    onAllNodesWithContentDescription(label)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .size

/**
 * The note editor, which only exists once a row's note has been opened.
 *
 * Addressed as the node taking typed text rather than by its placeholder: the placeholder is drawn
 * separately and disappears as soon as anything is typed.
 */
internal fun ComposeUiTest.noteField() = onAllNodes(hasSetTextAction())[0]

/**
 * Where [labels] appear on screen, top to bottom, ignoring any that are absent.
 *
 * Ordered by vertical position rather than by walking the schedule, so a change to the order the
 * tab draws rows in is visible here. Takes explicit labels because a row is not one node: a song
 * draws its number, title and songbook separately, so there is no single node carrying the item's
 * `displayText`.
 */
internal fun ComposeUiTest.orderOf(vararg labels: String): List<String> {
    val wanted = labels.toSet()
    return onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .mapNotNull { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)
                ?.joinToString("") { it.text } ?: return@mapNotNull null
            if (text in wanted) node.boundsInRoot.top to text else null
        }
        .sortedBy { it.first }
        .map { it.second }
        .distinct()
}
