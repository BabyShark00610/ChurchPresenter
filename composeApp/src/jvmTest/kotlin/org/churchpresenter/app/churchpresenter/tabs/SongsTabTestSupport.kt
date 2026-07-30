@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import org.churchpresenter.app.churchpresenter.data.SongFileParser
import org.churchpresenter.app.churchpresenter.data.SongItem
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.data.settings.SongSettings
import org.churchpresenter.app.churchpresenter.models.LyricSection
import org.churchpresenter.app.churchpresenter.viewmodel.SongsViewModel
import java.io.File
import java.nio.file.Files

/**
 * Harness and fixtures shared by the `SongsTab` test classes.
 *
 * **Why this tab is testable at all.** `tabs/` sat at 0% because a tab needs a real view model, and
 * `SongsViewModel` used to load its songs asynchronously on a shared dispatcher — so a test either
 * raced it or polled a wall clock for it. Since `ioDispatcher` became injectable (issue #56), the
 * view model loads *synchronously* from a temp songbook folder, which makes `SongsTab` ordinary
 * Compose: build the model, compose the tab, assert on what is on screen.
 *
 * Nothing is stubbed. Songs are written to disk with the real [SongFileParser] and read back through
 * the real load path, so the fixtures cannot drift from the file format the app actually writes.
 * `SongsTab` needs no `PresenterManager` and no host window — only a view model, an `AppSettings`,
 * and the section-selected callback.
 */

// ── Fixtures ────────────────────────────────────────────────────────────────────────────────────

/** One song as it will be written to disk. */
internal data class SongFixture(
    val number: String,
    val title: String,
    val songbook: String = "Hymnal",
    val author: String = "",
    val lyrics: List<String> = listOf("[Verse 1]", "a line of $title"),
)

internal val defaultSongs = listOf(
    SongFixture(number = "1", title = "Amazing Grace", author = "John Newton"),
    SongFixture(number = "2", title = "Be Thou My Vision", author = "Dallan Forgaill"),
    SongFixture(number = "12", title = "Amazing Love", author = "Charles Wesley"),
    SongFixture(number = "3", title = "How Great Thou Art", songbook = "Chorus Book"),
)

// ── Harness ─────────────────────────────────────────────────────────────────────────────────────

/** What the tab reported back, so a test asserts on the choice rather than on a stub. */
internal class TabReports {
    var selectedSection: LyricSection? = null
    val allSections = mutableListOf<List<LyricSection>>()
    var sectionIndex: Int? = null
    var lineIndex: Int? = null
    val scheduled = mutableListOf<String>()
    var settingsChanges = 0

    /**
     * The settings the tab's most recent change would produce.
     *
     * The tab never holds settings — it hands the host a transform — so the harness applies that
     * transform to the settings it was composed with and keeps the result, letting a test assert the
     * intended settings rather than that a callback fired.
     */
    var settingsAfterChange: AppSettings? = null
}

/**
 * Writes [songs] into a temp songbook folder, builds a real [SongsViewModel] over it, composes
 * `SongsTab`, and runs [block].
 *
 * The view model uses immediate dispatchers, so by the time [block] runs the songs are loaded — the
 * body can assert straight away without waiting for anything.
 */
@OptIn(ExperimentalTestApi::class)
internal fun songsTab(
    songs: List<SongFixture> = defaultSongs,
    songSettings: SongSettings = SongSettings(),
    /**
     * Which table columns are hidden, or null for the app's own default (tune, plays, author,
     * composer). Lives on [AppSettings] rather than [SongSettings], so it is its own parameter.
     */
    hiddenCols: Set<String>? = null,
    /** Metronome tempo per song id, which lives on [AppSettings] rather than [SongSettings]. */
    songBpm: Map<String, Int>? = null,
    /**
     * Whether a stage monitor screen is configured.
     *
     * The metronome-tempo tile is drawn only when one is — the tempo exists to drive that screen's
     * flashing dot — so a test for the tile has to turn this on.
     */
    stageMonitor: Boolean = false,
    /**
     * True when the tab is what is live on the output.
     *
     * It changes what the arrow keys do: while presenting they step *within* the live song and push
     * each step out, and while not presenting left/right move between songs instead.
     */
    isPresenting: Boolean = false,
    block: ComposeUiTest.(vm: SongsViewModel, reports: TabReports) -> Unit,
) {
    val dir = Files.createTempDirectory("cp-songs-tab").toFile()
    try {
        val parser = SongFileParser()
        songs.forEach { s ->
            val book = File(dir, s.songbook).apply { mkdirs() }
            parser.writeSongFile(
                SongItem(
                    number = s.number,
                    title = s.title,
                    songbook = s.songbook,
                    author = s.author,
                    lyrics = s.lyrics,
                ),
                File(book, "${s.number} - ${s.title}.song").absolutePath,
            )
        }
        val settings = AppSettings(songSettings = songSettings.copy(storageDirectory = dir.absolutePath))
            .let { if (hiddenCols != null) it.copy(songHiddenCols = hiddenCols) else it }
            .let { if (songBpm != null) it.copy(songBpm = songBpm) else it }
            .let {
                if (!stageMonitor) it
                else it.copy(
                    projectionSettings = it.projectionSettings.copy(
                        screenAssignments = listOf(
                            ScreenAssignment(displayMode = Constants.DISPLAY_MODE_STAGE_MONITOR),
                        ),
                    ),
                )
            }
        val vm = SongsViewModel(
            settings,
            dispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            enableFolderWatcher = false,
        )
        val reports = TabReports()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    SongsTab(
                        viewModel = vm,
                        appSettings = settings,
                        onSettingsChange = { transform ->
                            reports.settingsChanges++
                            reports.settingsAfterChange = transform(reports.settingsAfterChange ?: settings)
                        },
                        onAddToSchedule = { _, title, _, _ -> reports.scheduled += title },
                        onSongItemSelected = { reports.selectedSection = it },
                        onAllSectionsChanged = { reports.allSections += it },
                        onSectionIndexChanged = { reports.sectionIndex = it },
                        onLineIndexChanged = { reports.lineIndex = it },
                        isPresenting = isPresenting,
                    )
                }
            }
            block(vm, reports)
        }
    } finally {
        dir.deleteRecursively()
    }
}

// ── Labels, as the tab renders them ─────────────────────────────────────────────────────────────

internal object SongsLabel {
    const val SEARCH_PLACEHOLDER = "Search songs..."
    const val ALL_SONGBOOKS = "All Song Books"
    const val CONTAINS = "Contains"
    const val STARTS_WITH = "Starts With"
    const val EXACT_MATCH = "Exact Match"
    const val ADD_TO_SCHEDULE = "Add to Schedule"
    const val FAVORITES = "Favorites"
    const val NEW_SONG = "New Song"
}

// ── Reading and driving what was rendered ───────────────────────────────────────────────────────

/** Every string on screen. */
internal fun ComposeUiTest.rendered(): List<String> =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { t -> t.text } }

internal fun ComposeUiTest.shows(text: String): Boolean = rendered().any { it == text }

/**
 * Substring match, for the controls whose label and value land in one node.
 *
 * `DropdownSelector` merges its semantics, so the songbook filter renders as the single string
 * "SONG BOOKAll Song Books" rather than as a caption and a value — an exact match on either half
 * finds nothing.
 */
internal fun ComposeUiTest.showsContaining(fragment: String): Boolean =
    rendered().any { it.contains(fragment) }

/**
 * The search box: the tab's only freely-typed field.
 *
 * Addressed as the single node taking typed text rather than by its caption, because the placeholder
 * is a separate `Text` inside a `BasicTextField` decoration box and disappears once anything is typed.
 */
internal fun ComposeUiTest.searchBox() = onAllNodes(hasSetTextAction())[0]

internal fun ComposeUiTest.search(query: String) {
    searchBox().performTextReplacement(query)
    waitForIdle()
}

/**
 * The song titles currently listed, **in the order the tab shows them**.
 *
 * Ordered by vertical position rather than by walking the fixture list, so a change to how the tab
 * sorts its rows is visible here. (An earlier version of this helper filtered the fixtures by
 * presence, which silently made every ordering assertion a presence check.) Only titles belonging to
 * [from] are returned, so the surrounding chrome and the lyric pane do not leak in.
 */
internal fun ComposeUiTest.listedTitles(from: List<SongFixture> = defaultSongs): List<String> {
    val titles = from.map { it.title }.toSet()
    return onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .mapNotNull { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            if (text in titles) node.boundsInRoot.top to text!! else null
        }
        .sortedBy { it.first }
        .map { it.second }
        .distinct()
}
