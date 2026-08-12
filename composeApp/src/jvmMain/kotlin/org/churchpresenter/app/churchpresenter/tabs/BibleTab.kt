package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondary
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.add_to_schedule
import churchpresenter.composeapp.generated.resources.bible_cross_references_count
import churchpresenter.composeapp.generated.resources.bible_cross_references_popover_title
import churchpresenter.composeapp.generated.resources.bible_no_primary_hint
import churchpresenter.composeapp.generated.resources.bible_no_primary_step1
import churchpresenter.composeapp.generated.resources.bible_no_primary_step2
import churchpresenter.composeapp.generated.resources.bible_no_primary_title
import churchpresenter.composeapp.generated.resources.bible_search_mode_auto
import churchpresenter.composeapp.generated.resources.bible_search_mode_reference
import churchpresenter.composeapp.generated.resources.bible_search_mode_text
import churchpresenter.composeapp.generated.resources.bible_search_mode_tooltip
import churchpresenter.composeapp.generated.resources.bible_smart_search_hint
import churchpresenter.composeapp.generated.resources.bible_translation_order
import churchpresenter.composeapp.generated.resources.bible_verse_selection_hint
import churchpresenter.composeapp.generated.resources.book
import churchpresenter.composeapp.generated.resources.chapter
import churchpresenter.composeapp.generated.resources.clear
import churchpresenter.composeapp.generated.resources.contains_phrase
import churchpresenter.composeapp.generated.resources.copy_verse
import churchpresenter.composeapp.generated.resources.current_book
import churchpresenter.composeapp.generated.resources.entire_bible
import churchpresenter.composeapp.generated.resources.exact_match
import churchpresenter.composeapp.generated.resources.found_results
import churchpresenter.composeapp.generated.resources.go_live
import churchpresenter.composeapp.generated.resources.hold_live
import churchpresenter.composeapp.generated.resources.ic_copy
import churchpresenter.composeapp.generated.resources.ic_playlist_add
import churchpresenter.composeapp.generated.resources.ic_search
import churchpresenter.composeapp.generated.resources.mode
import churchpresenter.composeapp.generated.resources.no_results_found
import churchpresenter.composeapp.generated.resources.scope
import churchpresenter.composeapp.generated.resources.search
import churchpresenter.composeapp.generated.resources.swap_bibles
import churchpresenter.composeapp.generated.resources.tab_focus_lost
import churchpresenter.composeapp.generated.resources.verse
import java.awt.Cursor
import java.awt.Window as AwtWindow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.composables.DropdownSelector
import org.churchpresenter.app.churchpresenter.composables.FocusLostBanner
import org.churchpresenter.app.churchpresenter.composables.focusRescuePressHook
import org.churchpresenter.app.churchpresenter.composables.initialPassClickable
import org.churchpresenter.app.churchpresenter.composables.rememberFocusLostRescue
import org.churchpresenter.app.churchpresenter.composables.rememberTokenGate
import org.churchpresenter.app.churchpresenter.data.BibleBookAbbreviations
import org.churchpresenter.app.churchpresenter.data.CrossReferenceRepository
import org.churchpresenter.app.churchpresenter.data.formatCrossRefLabel
import org.churchpresenter.app.churchpresenter.data.aggregateCrossRefs
import org.churchpresenter.app.churchpresenter.data.mergeCrossRefs
import org.churchpresenter.app.churchpresenter.data.sharedCrossReferences
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import org.churchpresenter.app.churchpresenter.data.VerseSequenceLog
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.moveBibleTranslation
import org.churchpresenter.app.churchpresenter.data.settings.swapBibleTranslations
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.utils.LocalShortcuts
import org.churchpresenter.app.churchpresenter.utils.UsageEvent
import org.churchpresenter.app.churchpresenter.utils.UsageEvents
import org.churchpresenter.app.churchpresenter.utils.highlightRanges
import org.churchpresenter.app.churchpresenter.utils.isMultiTranslationPresentation
import org.churchpresenter.app.churchpresenter.utils.isSplitScreenBible
import org.churchpresenter.app.churchpresenter.viewmodel.BibleEngineClient
import org.churchpresenter.app.churchpresenter.viewmodel.BibleSearchMode
import org.churchpresenter.app.churchpresenter.viewmodel.BibleViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import org.churchpresenter.app.churchpresenter.viewmodel.STTManager
import org.churchpresenter.app.churchpresenter.viewmodel.bibleSttStatus
import org.churchpresenter.app.churchpresenter.viewmodel.filteredSelectionIndices
import org.churchpresenter.app.churchpresenter.viewmodel.formatVerseReference
import org.churchpresenter.app.churchpresenter.viewmodel.nextLiveVerseNumber
import org.churchpresenter.app.churchpresenter.viewmodel.verseNumberOf
import org.churchpresenter.app.churchpresenter.viewmodel.verseSpan
import org.churchpresenter.app.churchpresenter.viewmodel.verseTextOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CROSS_REF_MIN_WIDTH = 200.dp

private val CROSS_REF_MAX_WIDTH = 500.dp

private const val CROSS_REF_RANGE_ANCHORS = 3

private const val CROSS_REF_STATIC_LIMIT = 8

internal fun withBibleColumnWidths(settings: AppSettings, isMaximized: Boolean, bookWidthDp: Int, chapterWidthDp: Int): AppSettings =
    if (isMaximized) settings.copy(maximizedLayout = settings.maximizedLayout.copy(bibleColWidthBook = bookWidthDp, bibleColWidthChapter = chapterWidthDp))
    else settings.copy(windowedLayout = settings.windowedLayout.copy(bibleColWidthBook = bookWidthDp, bibleColWidthChapter = chapterWidthDp))

internal fun withBibleSplitPanelWidth(settings: AppSettings, isMaximized: Boolean, widthDp: Int): AppSettings =
    if (isMaximized) settings.copy(maximizedLayout = settings.maximizedLayout.copy(splitLivePanelWidth = widthDp))
    else settings.copy(windowedLayout = settings.windowedLayout.copy(splitLivePanelWidth = widthDp))

internal fun withBibleCrossRefPanelWidth(settings: AppSettings, isMaximized: Boolean, widthDp: Int): AppSettings =
    if (isMaximized) settings.copy(maximizedLayout = settings.maximizedLayout.copy(bibleColWidthCrossRef = widthDp))
    else settings.copy(windowedLayout = settings.windowedLayout.copy(bibleColWidthCrossRef = widthDp))

internal fun withBibleCrossReferencePanel(settings: AppSettings, docked: Boolean): AppSettings =
    settings.copy(bibleSettings = settings.bibleSettings.copy(crossReferencesPanel = docked))

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BibleTab(
    modifier: Modifier = Modifier,

    hostWindow: AwtWindow? = null,
    viewModel: BibleViewModel,
    appSettings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit = {},
    onAddToSchedule: ((bookName: String, chapter: Int, verseNumber: Int, verseText: String, verseRange: String, bookId: Int) -> Unit)? = null,
    selectedVerseItem: ScheduleItem.BibleVerseItem? = null,
    onVerseSelected: (List<SelectedVerse>) -> Unit = {},

    onInstanceLinkSendVerse: ((bookName: String, chapter: Int, verseNumber: Int, verseText: String, verseRange: String) -> Unit)? = null,

    onInstanceLinkSendBibleHold: ((hold: Boolean) -> Unit)? = null,
    onPresenting: (Presenting) -> Unit = { Presenting.NONE },
    isPresenting: Boolean = false,
    presenterManager: PresenterManager? = null,
    statisticsManager: StatisticsManager? = null,

    verseSequenceLog: VerseSequenceLog? = null,

    crossReferences: CrossReferenceRepository? = null,
    sttManager: STTManager? = null,
    bibleEngineClient: BibleEngineClient? = null,
    dialogDismissSignal: Int = 0,
) {

    val isFirstComposition = remember { mutableStateOf(true) }
    val translationSelectionKey = appSettings.bibleSettings.translationSelectionKey()
    LaunchedEffect(
        appSettings.bibleSettings.storageDirectory,
        translationSelectionKey,
    ) {
        if (isFirstComposition.value) {
            isFirstComposition.value = false
        } else {
            viewModel.updateSettings(appSettings)
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedVerseItem) {
        selectedVerseItem?.let { item ->
            if (!viewModel.isFullyLoadedFlow.value) {
                viewModel.isFullyLoadedFlow.first { it }
            }
            val found = viewModel.selectVerseByDetails(item.bookName, item.chapter, item.verseNumber, item.verseRange, bookId = item.bookId)
            if (found) {
                focusRequester.requestFocus()
            }
        }
    }

    val sttConnected = sttManager?.connected?.value == true
    val engineSettings = appSettings.bibleEngineSettings
    val detectedReferences by viewModel.detectedReferences
    val autoFollowEnabled by viewModel.autoFollowEnabled
    val textMatchLevel by viewModel.textMatchLevel
    val continuationSpeed by viewModel.continuationSpeed

    val books by viewModel.books
    val loadErrors by viewModel.loadErrors
    val selectedBookIndex by viewModel.selectedBookIndex
    val selectedChapter by viewModel.selectedChapter
    val selectedVerseIndex by viewModel.selectedVerseIndex
    val verses by viewModel.verses
    val searchQuery by viewModel.searchQuery
    val searchResults by viewModel.searchResults
    val isSearchMode by viewModel.isSearchMode
    val searchMode by viewModel.searchMode
    val filteredBooks by viewModel.filteredBooks
    val filteredChapters by viewModel.filteredChapters
    val filteredVerses by viewModel.filteredVerses

    val scopeOptions = listOf(
        stringResource(Res.string.entire_bible),
        stringResource(Res.string.current_book),
    )
    val selectedScopeIndex by viewModel.selectedScopeIndex
    val selectedScope = scopeOptions.getOrElse(selectedScopeIndex) { scopeOptions.first() }

    val modeOptions = listOf(
        stringResource(Res.string.contains_phrase),
        stringResource(Res.string.exact_match),
    )
    val selectedModeIndex by viewModel.selectedModeIndex
    val selectedMode = modeOptions.getOrElse(selectedModeIndex) { modeOptions.first() }

    LaunchedEffect(dialogDismissSignal) { focusRequester.requestFocus() }

    val verseSelectionToken by viewModel.verseSelectionToken

    val currentIsPresenting by rememberUpdatedState(isPresenting)

    val splitBrowseMode = appSettings.bibleSettings.splitBrowseMode

    val isSplitActive = splitBrowseMode

    val crossRefsEnabled = appSettings.bibleSettings.crossReferencesPanel
    val crossRefRepository = crossReferences ?: sharedCrossReferences
    var crossRefRows by remember { mutableStateOf<List<CrossRefRow>>(emptyList()) }
    var selectedCrossRefIdx by remember { mutableStateOf(-1) }

    var crossRefCounts by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    var crossRefPopoverIndex by remember { mutableStateOf(-1) }

    var crossRefPopoverAnchor by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var crossRefPopoverLabel by remember { mutableStateOf("") }
    var crossRefPopoverRows by remember { mutableStateOf<List<CrossRefRow>>(emptyList()) }

    var crossRefNavigatedTo by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    var crossRefAnchorEpoch by remember { mutableStateOf(0) }

    val fallbackAbbreviationResources =
        BibleBookAbbreviations.abbreviationResourceIds.map { stringResource(it) }
    val fallbackAbbreviations = remember(fallbackAbbreviationResources) {
        fallbackAbbreviationResources.map { BibleBookAbbreviations.parseVariants(it).firstOrNull().orEmpty() }
    }

    val loadedModule = viewModel.primaryBible.value

    var crossRefAnchors by remember { mutableStateOf<List<Triple<Int, Int, Int>>>(emptyList()) }

    var crossRefAnchorIsLive by remember { mutableStateOf(false) }

    var crossRefRun by remember { mutableStateOf<List<Triple<Int, Int, Int>>>(emptyList()) }

    fun anchorLiveVerse(ref: Triple<Int, Int, Int>) {
        val previous = crossRefRun.lastOrNull()
        val continues = previous != null &&
            previous.first == ref.first && previous.second == ref.second && ref.third > previous.third
        crossRefRun = if (continues) crossRefRun + ref else listOf(ref)
        crossRefAnchors = listOf(ref)
        crossRefAnchorIsLive = true
        crossRefNavigatedTo = null
    }

    LaunchedEffect(
        selectedBookIndex, selectedChapter, selectedVerseIndex, verses,
        verseSelectionToken, crossRefAnchorEpoch, loadedModule,
    ) {
        val selectedNumbers = viewModel.getSelectedVerseNumbers().ifEmpty {
            listOfNotNull(verses.getOrNull(selectedVerseIndex)?.let(::verseNumberOf))
        }

        crossRefAnchors = selectedNumbers.take(CROSS_REF_RANGE_ANCHORS).mapNotNull { number ->
            viewModel.canonicalRefForDisplay(selectedBookIndex, selectedChapter, number)
                ?.let { (book, chapter, verse) -> verse?.let { Triple(book, chapter, it) } }
        }
        crossRefAnchorIsLive = false
    }

    val crossRefPassageMode = crossRefAnchorIsLive && crossRefRun.size > 1

    LaunchedEffect(
        crossRefsEnabled, crossRefAnchors, crossRefPassageMode, crossRefRun,

        crossRefAnchorEpoch, loadedModule, fallbackAbbreviations,
    ) {
        if (!crossRefsEnabled || crossRefAnchors.isEmpty()) {
            crossRefRows = emptyList()
            crossRefNavigatedTo = null
            return@LaunchedEffect
        }

        if (crossRefAnchors.size == 1 && crossRefAnchors.first() == crossRefNavigatedTo) return@LaunchedEffect
        crossRefNavigatedTo = null

        val learned = crossRefAnchors.first().let { (book, chapter, verse) ->
            verseSequenceLog?.successors(book, chapter, verse).orEmpty()
        }.map { crossRefRow(viewModel, fallbackAbbreviations, it.bookId, it.chapter, it.verse, null, learned = true) }

        crossRefRepository.ensureLoaded()
        val sources = if (crossRefPassageMode) crossRefRun else crossRefAnchors
        val perVerse = sources.map { (book, chapter, verse) -> crossRefRepository.forVerse(book, chapter, verse) }
        val references = if (crossRefPassageMode) {
            aggregateCrossRefs(perVerse, limit = CROSS_REF_STATIC_LIMIT).map {
                crossRefRow(
                    viewModel, fallbackAbbreviations, it.bookId, it.chapter, it.startVerse,
                    it.endVerse, learned = false, count = it.sourceCount,
                )
            }
        } else {
            mergeCrossRefs(perVerse, limit = CROSS_REF_STATIC_LIMIT).map {
                crossRefRow(viewModel, fallbackAbbreviations, it.bookId, it.chapter, it.verse, it.endVerse, learned = false)
            }
        }

        val learnedKeys = learned.map { Triple(it.bookId, it.chapter, it.verse) }.toSet()
        crossRefRows = learned + references.filter { Triple(it.bookId, it.chapter, it.verse) !in learnedKeys }
        selectedCrossRefIdx = -1
    }

    LaunchedEffect(selectedBookIndex, selectedChapter, verses, loadedModule, crossRefRepository) {
        crossRefRepository.ensureLoaded()
        crossRefCounts = buildMap {
            verses.forEach { line ->
                val number = verseNumberOf(line) ?: return@forEach
                val canonical = viewModel.canonicalRefForDisplay(selectedBookIndex, selectedChapter, number)
                val verse = canonical?.third ?: return@forEach
                val count = crossRefRepository.forVerse(canonical.first, canonical.second, verse).size
                if (count > 0) put(number, count)
            }
        }
    }

    LaunchedEffect(crossRefPopoverAnchor, loadedModule, fallbackAbbreviations) {
        val anchor = crossRefPopoverAnchor
        if (anchor == null) {
            crossRefPopoverRows = emptyList()
            return@LaunchedEffect
        }
        crossRefRepository.ensureLoaded()
        crossRefPopoverRows = crossRefRepository.forVerse(anchor.first, anchor.second, anchor.third)
            .map { crossRefRow(viewModel, fallbackAbbreviations, it.bookId, it.chapter, it.verse, it.endVerse, learned = false) }
    }

    val crossRefCountStr = stringResource(Res.string.bible_cross_references_count)
    val crossRefPopoverTitleStr = stringResource(Res.string.bible_cross_references_popover_title)

    fun openCrossRef(row: CrossRefRow) {
        crossRefNavigatedTo = Triple(row.bookId, row.chapter, row.verse)
        crossRefPopoverIndex = -1
        crossRefPopoverAnchor = null
        viewModel.selectVerseByCanonicalRef(row.bookId, row.chapter, row.verse)
        focusRequester.requestFocus()
    }

    fun goLiveCrossRef(row: CrossRefRow) {
        crossRefNavigatedTo = Triple(row.bookId, row.chapter, row.verse)
        crossRefPopoverIndex = -1
        crossRefPopoverAnchor = null
        viewModel.selectVerseByCanonicalRef(row.bookId, row.chapter, row.verse, goLiveSource = "crossref")
        focusRequester.requestFocus()
    }

    fun scheduleCrossRef(row: CrossRefRow) {
        viewModel.addCanonicalRefToSchedule(row.bookId, row.chapter, row.verse) {
                bookName, chapter, verseNumber, verseText, verseRange, bookId ->
            onAddToSchedule?.invoke(bookName, chapter, verseNumber, verseText, verseRange, bookId)
        }
        focusRequester.requestFocus()
    }

    var liveChapterVerses by remember { mutableStateOf<List<String>>(emptyList()) }
    var liveBookName by remember { mutableStateOf("") }
    var liveChapterNum by remember { mutableStateOf(0) }
    var liveVerseNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var liveNavTargetVerse by remember { mutableStateOf(0) }
    var liveNavToken       by remember { mutableStateOf(0) }

    val fallbackDisplayedVerses = remember { mutableStateOf<List<SelectedVerse>>(emptyList()) }
    val displayedVerses by (presenterManager?.displayedVerses ?: fallbackDisplayedVerses)

    val scope = rememberCoroutineScope()

    LaunchedEffect(displayedVerses, splitBrowseMode) {
        if (!splitBrowseMode || displayedVerses.isEmpty()) return@LaunchedEffect
        val first = displayedVerses.first()
        liveBookName = first.bookName
        liveChapterNum = first.chapter
        liveVerseNumbers = setOf(displayedVerses.first().verseNumber)
        liveNavTargetVerse = liveVerseNumbers.minOrNull() ?: 0
        liveChapterVerses = viewModel.getChapterVerses(first.bookName, first.chapter)
    }

    LaunchedEffect(splitBrowseMode, verses.size) {
        if (!splitBrowseMode) return@LaunchedEffect
        if (liveChapterVerses.isNotEmpty() || displayedVerses.isNotEmpty()) return@LaunchedEffect
        val first = viewModel.getSelectedVerses().firstOrNull() ?: return@LaunchedEffect
        liveBookName = first.bookName
        liveChapterNum = first.chapter
        liveVerseNumbers = setOf(first.verseNumber)
        liveNavTargetVerse = first.verseNumber
        liveChapterVerses = viewModel.getChapterVerses(first.bookName, first.chapter)
    }

    LaunchedEffect(liveNavToken) {
        if (liveNavToken == 0 || liveNavTargetVerse == 0) return@LaunchedEffect
        val verses = viewModel.getVersesForDisplay(liveBookName, liveChapterNum, liveNavTargetVerse)
        if (verses.isNotEmpty()) {
            val primary = verses.first()
            statisticsManager?.recordVerseDisplay(
                primary.bibleName, primary.bookName, primary.chapter, primary.verseNumber
            )
            onVerseSelected(verses)
            onInstanceLinkSendVerse?.invoke(primary.bookName, primary.chapter, primary.verseNumber, primary.verseText, primary.verseRange)
            presenterManager?.let { if (it.bibleHold.value) { it.setBibleHold(false); onInstanceLinkSendBibleHold?.invoke(false) } }
            onPresenting(Presenting.BIBLE)
            viewModel.logLiveReference(
                displayBookIndex = viewModel.selectedBookIndex.value,
                chapter    = primary.chapter,
                verseStart = primary.verseNumber,
                verseEnd   = null,
                source     = "manual",
                autoFollow = viewModel.autoFollowEnabled.value,
            )
        }
    }

    fun goLiveWithHistory(source: String = "manual", matchType: String? = null) {
        val selectedVerses = viewModel.getSelectedVerses()
        selectedVerses.firstOrNull()?.let { v ->
            if (viewModel.multiVerseEnabled.value) {
                val verseNumbers = viewModel.getSelectedVerseNumbers()
                val rangeStr = viewModel.formatVerseRange(verseNumbers)
                viewModel.addToHistory(v.bookName, v.chapter, v.verseNumber, v.verseText, rangeStr)
            } else {
                viewModel.addToHistory(v.bookName, v.chapter, v.verseNumber, v.verseText)
            }
        }

        val primaryVerse = selectedVerses.firstOrNull()

        if (primaryVerse != null) {
            val translationCount = appSettings.bibleSettings.translationList().size
            val outputs = appSettings.projectionSettings.screenAssignments
            if (isMultiTranslationPresentation(translationCount, outputs)) {
                UsageEvents.record(UsageEvent.BIBLE_MULTI_TRANSLATION)
            }
            if (isSplitScreenBible(translationCount, outputs)) {
                UsageEvents.record(UsageEvent.BIBLE_SPLIT_SCREEN)
            }
        }
        if (primaryVerse != null && statisticsManager != null) {
            if (viewModel.multiVerseEnabled.value) {
                for (vNum in viewModel.getSelectedVerseNumbers()) {
                    statisticsManager.recordVerseDisplay(primaryVerse.bibleName, primaryVerse.bookName, primaryVerse.chapter, vNum)
                }
            } else {
                statisticsManager.recordVerseDisplay(primaryVerse.bibleName, primaryVerse.bookName, primaryVerse.chapter, primaryVerse.verseNumber)
            }
        }

        if (selectedVerses.isNotEmpty()) {
            onVerseSelected(selectedVerses)
        }
        primaryVerse?.let { v ->
            onInstanceLinkSendVerse?.invoke(v.bookName, v.chapter, v.verseNumber, v.verseText, v.verseRange)
        }
        if (primaryVerse != null) {

            val (verseStart, verseEnd) = verseSpan(primaryVerse.verseRange, primaryVerse.verseNumber)
            viewModel.logLiveReference(
                displayBookIndex = viewModel.selectedBookIndex.value,
                chapter    = primaryVerse.chapter,
                verseStart = verseStart,
                verseEnd   = verseEnd,
                source     = source,
                autoFollow = viewModel.autoFollowEnabled.value,
                matchType  = matchType,
            )

            viewModel.logGoLiveCorrection(viewModel.selectedBookIndex.value, primaryVerse.chapter, verseStart)

            viewModel.canonicalRefForDisplay(
                viewModel.selectedBookIndex.value, primaryVerse.chapter, verseStart,
            )?.let { (book, chapter, verse) ->
                if (verse != null) {
                    verseSequenceLog?.recordGoLive(book, chapter, verse)

                    anchorLiveVerse(Triple(book, chapter, verse))
                }
            }
        }
        if (viewModel.multiVerseEnabled.value) {
            viewModel.clearMultiVerseSelection()
        }
        presenterManager?.let { if (it.bibleHold.value) { it.setBibleHold(false); onInstanceLinkSendBibleHold?.invoke(false) } }
        onPresenting(Presenting.BIBLE)
    }

    val autoFollowLiveToken by viewModel.autoFollowLiveToken

    val autoFollowTokenGate = rememberTokenGate(autoFollowLiveToken)
    LaunchedEffect(autoFollowLiveToken) {
        if (!autoFollowTokenGate.consume()) return@LaunchedEffect
        goLiveWithHistory(source = viewModel.autoFollowLiveSource.value, matchType = viewModel.autoFollowLiveMatchType.value)
    }

    LaunchedEffect(verseSelectionToken) {

        if (viewModel.multiVerseEnabled.value && currentIsPresenting) return@LaunchedEffect

        if (splitBrowseMode) return@LaunchedEffect
        if (verses.isNotEmpty() && selectedVerseIndex >= 0 && selectedVerseIndex < verses.size) {
            val selectedVerses = viewModel.getSelectedVerses()
            if (selectedVerses.isNotEmpty()) {
                onVerseSelected(selectedVerses)

                if (currentIsPresenting && autoFollowLiveToken == autoFollowTokenGate.lastHandled) {
                    val primary = selectedVerses.first()
                    viewModel.logLiveReference(
                        displayBookIndex = viewModel.selectedBookIndex.value,
                        chapter    = primary.chapter,
                        verseStart = primary.verseNumber,
                        verseEnd   = null,
                        source     = "manual",
                        autoFollow = viewModel.autoFollowEnabled.value,
                    )
                }
            }
        }
    }

    LaunchedEffect(verses.size) {
        if (!currentIsPresenting && !splitBrowseMode && verses.isNotEmpty()) {
            val selectedVerses = viewModel.getSelectedVerses()
            if (selectedVerses.isNotEmpty()) onVerseSelected(selectedVerses)
        }
    }

    val prevBookRef = remember { mutableStateOf(selectedBookIndex) }
    val prevChapterRef = remember { mutableStateOf(selectedChapter) }
    LaunchedEffect(selectedBookIndex, selectedChapter) {
        val bookChanged = selectedBookIndex != prevBookRef.value
        val chapterChanged = selectedChapter != prevChapterRef.value
        prevBookRef.value = selectedBookIndex
        prevChapterRef.value = selectedChapter
        val wasSequentialAdvance = viewModel.consumeSequentialChapterAdvance()
        if ((bookChanged || chapterChanged) && !splitBrowseMode && currentIsPresenting && !wasSequentialAdvance) {
            presenterManager?.setBibleHold(true)
        }
    }

    var historyExpanded by remember { mutableStateOf(true) }
    var selectedHistoryIdx by remember { mutableStateOf(-1) }
    var selectedDetectionIdx by remember { mutableStateOf(0) }
    LaunchedEffect(detectedReferences.size) { selectedDetectionIdx = 0 }

    LaunchedEffect(sttConnected) {
        if (sttConnected) {
            val url = appSettings.sttSettings.serverUrl
            if (appSettings.sttSettings.lastConnectedUrl != url) {
                onSettingsChange { it.copy(sttSettings = it.sttSettings.copy(lastConnectedUrl = url)) }
            }
        }
    }

    var searchFieldFocused by remember { mutableStateOf(false) }
    val shortcuts = LocalShortcuts.current

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        if (searchFieldFocused) return false

        val movingUp = shortcuts.matches(ShortcutAction.BIBLE_PREVIOUS_VERSE, event)
        val movingDown = shortcuts.matches(ShortcutAction.BIBLE_NEXT_VERSE, event)

        if (splitBrowseMode && liveChapterVerses.isNotEmpty() && (movingUp || movingDown)) {
            val refVerse = if (liveNavTargetVerse > 0) liveNavTargetVerse
                           else liveVerseNumbers.minOrNull() ?: 1
            val nextVerseNum = nextLiveVerseNumber(
                liveChapterVerses, refVerse, moveUp = movingUp,
            )
            if (nextVerseNum != null) {
                liveNavTargetVerse = nextVerseNum
                liveNavToken++
            }
            return true
        }

        return when {
            movingUp -> viewModel.navigatePreviousVerse()
            movingDown -> viewModel.navigateNextVerse()
            shortcuts.matches(ShortcutAction.BIBLE_PREVIOUS_CHAPTER, event) -> viewModel.navigatePreviousChapter()
            shortcuts.matches(ShortcutAction.BIBLE_NEXT_CHAPTER, event) -> viewModel.navigateNextChapter()
            else -> false
        }
    }

    val density = LocalDensity.current
    val onSettingsChangeState = rememberUpdatedState(onSettingsChange)

    val windowState = LocalMainWindowState.current
    val isMaximized = windowState?.placement != WindowPlacement.Floating
    val currentLayout = if (isMaximized) appSettings.maximizedLayout else appSettings.windowedLayout

    var colWBook by remember(currentLayout.bibleColWidthBook, isMaximized) {
        mutableStateOf(with(density) { currentLayout.bibleColWidthBook.dp.toPx() })
    }
    var colWChapter by remember(currentLayout.bibleColWidthChapter, isMaximized) {
        mutableStateOf(with(density) { currentLayout.bibleColWidthChapter.dp.toPx() })
    }

    fun saveColWidths() {
        val bookDp = with(density) { colWBook.toDp().value.toInt() }
        val chapterDp = with(density) { colWChapter.toDp().value.toInt() }
        onSettingsChangeState.value { s -> withBibleColumnWidths(s, isMaximized, bookDp, chapterDp) }
    }

    var colWSplit by remember(currentLayout.splitLivePanelWidth, isMaximized) {
        mutableStateOf(with(density) { currentLayout.splitLivePanelWidth.dp.toPx() })
    }

    fun saveColWSplit() {
        val widthDp = with(density) { colWSplit.toDp().value.toInt() }
        onSettingsChangeState.value { s -> withBibleSplitPanelWidth(s, isMaximized, widthDp) }
    }

    var colWCrossRef by remember(currentLayout.bibleColWidthCrossRef, isMaximized) {
        mutableStateOf(with(density) { currentLayout.bibleColWidthCrossRef.dp.toPx() })
    }

    fun saveColWCrossRef() {
        val widthDp = with(density) { colWCrossRef.toDp().value.toInt() }
        onSettingsChangeState.value { s -> withBibleCrossRefPanelWidth(s, isMaximized, widthDp) }
    }

    @Composable
    fun SearchModeChip(modifier: Modifier = Modifier) {
        val (label, container, content) = when (searchMode) {
            BibleSearchMode.AUTO -> Triple(
                Res.string.bible_search_mode_auto,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary
            )
            BibleSearchMode.REFERENCE -> Triple(
                Res.string.bible_search_mode_reference,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.onSecondary
            )
            BibleSearchMode.TEXT -> Triple(
                Res.string.bible_search_mode_text,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.onTertiary
            )
        }
        TooltipArea(
            tooltip = {
                Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.extraSmall, tonalElevation = 4.dp) {
                    Text(
                        text = stringResource(Res.string.bible_search_mode_tooltip),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            tooltipPlacement = TooltipPlacement.ComponentRect(anchor = Alignment.BottomCenter, offset = DpOffset(0.dp, 4.dp))
        ) {
            Surface(
                onClick = { viewModel.cycleSearchMode(); focusRequester.requestFocus() },
                modifier = modifier,
                shape = MaterialTheme.shapes.small,
                color = container,
                contentColor = content
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }

    @Composable
    fun DragHandle(onDragEnd: () -> Unit = ::saveColWidths, onDrag: (Float) -> Unit) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> onDrag(delta) },
                    onDragStopped = { onDragEnd() }
                )
        )
    }

    val focusRescue = rememberFocusLostRescue(hostWindow, focusRequester)
    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { focusRescue.onFocusChanged(it.hasFocus) }
            .focusRescuePressHook(focusRescue)
            .focusable()
            .onPreviewKeyEvent { handleKeyEvent(it) }
    ) {

        if (loadErrors.isNotEmpty()) {
            BibleLoadErrorBanner(
                errors = loadErrors,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )
        }

        val searchPlaceholder = stringResource(Res.string.bible_smart_search_hint)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)) {
            val searchIsNarrow = maxWidth < 440.dp

            if (searchIsNarrow) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BibleSearchField(
                        value = searchQuery,
                        placeholder = searchPlaceholder,
                        onValueChange = { viewModel.onSmartQueryChanged(it) },
                        onClear = { viewModel.clearSearch(); focusRequester.requestFocus() },
                        onSubmit = { viewModel.submitSmartQuery(); focusRequester.requestFocus() },
                        onFocusChanged = { searchFieldFocused = it },
                        modeChip = { SearchModeChip() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DropdownSelector(
                            label = stringResource(Res.string.scope),
                            items = scopeOptions,
                            selected = selectedScope,
                            onSelectedChange = { newValue ->
                                viewModel.updateSelectedScopeIndex(scopeOptions.indexOf(newValue).coerceAtLeast(0))
                            }
                        )
                        DropdownSelector(
                            label = stringResource(Res.string.mode),
                            items = modeOptions,
                            selected = selectedMode,
                            onSelectedChange = { newValue ->
                                viewModel.updateSelectedModeIndex(modeOptions.indexOf(newValue).coerceAtLeast(0))
                            }
                        )
                        Box(
                            modifier = Modifier.size(42.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                    viewModel.submitSmartQuery(); focusRequester.requestFocus()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(Res.drawable.ic_search), contentDescription = stringResource(Res.string.search), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    BibleSearchField(
                        value = searchQuery,
                        placeholder = searchPlaceholder,
                        onValueChange = { viewModel.onSmartQueryChanged(it) },
                        onClear = { viewModel.clearSearch(); focusRequester.requestFocus() },
                        onSubmit = { viewModel.submitSmartQuery(); focusRequester.requestFocus() },
                        onFocusChanged = { searchFieldFocused = it },
                        modeChip = { SearchModeChip() },
                        modifier = Modifier.weight(1f)
                    )
                    DropdownSelector(
                        label = stringResource(Res.string.scope),
                        items = scopeOptions,
                        selected = selectedScope,
                        onSelectedChange = { newValue ->
                            viewModel.updateSelectedScopeIndex(scopeOptions.indexOf(newValue).coerceAtLeast(0))
                        }
                    )
                    DropdownSelector(
                        label = stringResource(Res.string.mode),
                        items = modeOptions,
                        selected = selectedMode,
                        onSelectedChange = { newValue ->
                            viewModel.updateSelectedModeIndex(modeOptions.indexOf(newValue).coerceAtLeast(0))
                        }
                    )
                    Box(
                        modifier = Modifier.size(42.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                viewModel.submitSmartQuery(); focusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(Res.drawable.ic_search), contentDescription = stringResource(Res.string.search), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (engineSettings.enabled && sttConnected) {
            val engineStartFailed = bibleEngineClient?.startFailed?.value == true

            val engineSttDown = bibleEngineClient?.engineSttConnected?.value == false
            val sttConnectError = sttManager.connectError.value == true
            val noBibleSelected = appSettings.bibleSettings.primaryBible.isBlank() &&
                appSettings.bibleSettings.secondaryBible.isBlank() &&
                viewModel.primaryBible.value == null
            BibleDetectionPanel(
                status = bibleSttStatus(
                    engineStartFailed = engineStartFailed,
                    noBibleSelected = noBibleSelected,
                    sttConnected = sttConnected,
                    engineConnected = bibleEngineClient?.connected?.value == true,
                    engineSttDown = engineSttDown,
                    sttReceiving = sttManager.inProgressText.value.isNotBlank() || sttManager.segments.isNotEmpty(),
                    hasDetectedReferences = detectedReferences.isNotEmpty(),
                    sttReconnecting = sttManager.reconnecting.value == true,
                    sttConnectError = sttConnectError,
                    sttConnecting = sttManager.connecting.value == true,
                ),
                statusIsError = engineStartFailed || noBibleSelected || sttConnectError || engineSttDown,
                autoFollowEnabled = autoFollowEnabled,
                textMatchLevel = textMatchLevel,
                continuationSpeed = continuationSpeed,
                detections = detectedReferences,
                selectedIndex = selectedDetectionIdx,
                showFlagButtons = engineSettings.helpDevMode,
                canFlagLive = displayedVerses.isNotEmpty(),
                onAutoFollowChange = { next ->
                    viewModel.setAutoFollow(next)
                    onSettingsChange { it.copy(bibleEngineSettings = it.bibleEngineSettings.copy(autoFollow = next)) }
                },
                onTextMatchLevelChange = { next ->
                    viewModel.setTextMatchLevel(next)
                    onSettingsChange { it.copy(bibleEngineSettings = it.bibleEngineSettings.copy(textMatchLevel = next.name.lowercase())) }
                },
                onContinuationSpeedChange = { next ->
                    viewModel.setContinuationSpeed(next)
                    onSettingsChange { it.copy(bibleEngineSettings = it.bibleEngineSettings.copy(continuationSpeed = next.name.lowercase())) }
                },
                onFlag = { kind ->
                    val live = displayedVerses
                    if (kind == "missed_passage") viewModel.logOperatorFlag(kind = kind)
                    else if (live.isNotEmpty()) viewModel.logOperatorFlag(
                        kind = kind,
                        bookName = live.first().bookName,
                        chapter = live.first().chapter,
                        verseStart = live.minOf { it.verseNumber },
                        verseEnd = live.maxOf { it.verseNumber }.takeIf { live.size > 1 },
                        matchType = viewModel.autoFollowLiveMatchType.value,
                    )
                },
                onClearDetections = { viewModel.clearDetectedReferences() },
                onDetectionClick = { idx ->
                    selectedDetectionIdx = idx
                    detectedReferences.getOrNull(idx)?.let { viewModel.applyDetectedReference(it) }
                    focusRequester.requestFocus()
                },
                onDetectionDoubleClick = { idx ->
                    selectedDetectionIdx = idx
                    detectedReferences.getOrNull(idx)
                        ?.let { viewModel.applyDetectedReference(it, goLiveSource = "detection") }
                    focusRequester.requestFocus()
                },
            )
        }

        if (appSettings.bibleSettings.primaryBible.isBlank() && viewModel.primaryBible.value == null) {

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 360.dp),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📖",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = stringResource(Res.string.bible_no_primary_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = stringResource(Res.string.bible_no_primary_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (appSettings.bibleSettings.storageDirectory.isBlank()) {
                            Text(
                                text = stringResource(Res.string.bible_no_primary_step1),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = stringResource(Res.string.bible_no_primary_step2),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else if (isSearchMode && searchResults.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(31.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.found_results, searchResults.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 8.dp)) {
                        itemsIndexed(searchResults) { _, result ->

                            val resultText = result.verseText
                            val highlightedText = buildAnnotatedString {
                                var lastIndex = 0

                                for ((safeStart, safeEnd) in highlightRanges(resultText, searchQuery)) {
                                    append(resultText.substring(lastIndex.coerceAtMost(safeStart), safeStart))
                                    withStyle(style = SpanStyle(
                                        background = MaterialTheme.colorScheme.primaryContainer,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )) {
                                        append(resultText.substring(safeStart, safeEnd))
                                    }
                                    lastIndex = safeEnd
                                }
                                if (lastIndex < resultText.length) append(resultText.substring(lastIndex))
                            }
                            Text(
                                text = highlightedText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .initialPassClickable {
                                        viewModel.selectSearchResult(result)
                                        viewModel.clearSearch()
                                        focusRequester.requestFocus()
                                    }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(scrollState = listState)
                    )
                }
            }
        } else if (isSearchMode && searchQuery.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.no_results_found, searchQuery),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {

            FocusLostBanner(focusRescue, stringResource(Res.string.tab_focus_lost))

            val accentColor = MaterialTheme.colorScheme.primary
            BibleColumnHeaderRow(
                bookWidth = with(density) { colWBook.toDp() },
                chapterWidth = with(density) { colWChapter.toDp() },
                crossRefsEnabled = crossRefsEnabled,
                holdAvailable = presenterManager != null && !splitBrowseMode,
                holdLive = presenterManager?.bibleHold?.value ?: false,
                sttToggleVisible = appSettings.sttSettings.lastConnectedUrl.isNotBlank() &&
                    appSettings.sttSettings.lastConnectedUrl == appSettings.sttSettings.serverUrl &&
                    sttManager != null,
                sttConnected = sttConnected,
                translations = appSettings.bibleSettings.translationList(),
                storageDirectory = appSettings.bibleSettings.storageDirectory,
                translationSelectionKey = translationSelectionKey,
                onCrossReferencesToggle = {
                    onSettingsChange { s -> withBibleCrossReferencePanel(s, !crossRefsEnabled) }

                    crossRefPopoverIndex = -1
                    crossRefPopoverAnchor = null
                    focusRequester.requestFocus()
                },
                onHoldLiveToggle = {
                    val next = !(presenterManager?.bibleHold?.value ?: false)
                    presenterManager?.setBibleHold(next)
                    onInstanceLinkSendBibleHold?.invoke(next)
                    focusRequester.requestFocus()
                },
                onSttToggle = {
                    if (sttConnected) sttManager?.disconnect()
                    else sttManager?.connect(appSettings.sttSettings.serverUrl)
                    focusRequester.requestFocus()
                },
                onSwapTranslations = {
                    onSettingsChange { s -> s.swapBibleTranslations() }
                    focusRequester.requestFocus()
                },
                onMoveTranslation = { index, offset ->
                    onSettingsChange { app -> app.moveBibleTranslation(index, offset) }
                    focusRequester.requestFocus()
                },
                onAddToSchedule = {
                    viewModel.addCurrentVerseToSchedule { bookName, chapter, verseNumber, verseText, verseRange, bookId ->
                        onAddToSchedule?.invoke(bookName, chapter, verseNumber, verseText, verseRange, bookId)
                    }
                    focusRequester.requestFocus()
                },
                onGoLive = { goLiveWithHistory(); focusRequester.requestFocus() },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 4.dp)) {

                Column(modifier = Modifier.width(with(density) { colWBook.toDp() }).fillMaxHeight()) {
                    BibleBrowserColumn(
                        items = filteredBooks,
                        selectedIndex = filteredBooks.indexOf(books.getOrNull(selectedBookIndex) ?: "").coerceAtLeast(0),
                        singleLine = true,
                        onItemSelected = { index ->
                            val bookName = filteredBooks.getOrNull(index)
                            bookName?.let {
                                val realIndex = books.indexOf(it)
                                if (realIndex >= 0) viewModel.selectBook(realIndex)
                            }
                        }
                    )
                }

                DragHandle { amount ->
                    colWBook = (colWBook + amount).coerceIn(
                        with(density) { 80.dp.toPx() },
                        with(density) { 400.dp.toPx() }
                    )
                }

                Column(modifier = Modifier.width(with(density) { colWChapter.toDp() }).fillMaxHeight()) {
                    BibleBrowserColumn(
                        items = filteredChapters,
                        selectedIndex = filteredChapters.indexOf(selectedChapter.toString()).coerceAtLeast(0),
                        centerText = true,
                        rowHeight = 31.dp,
                        onItemSelected = { index ->
                            val chapterStr = filteredChapters.getOrNull(index)
                            chapterStr?.toIntOrNull()?.let { chapter -> viewModel.selectChapter(chapter) }
                        }
                    )
                }

                DragHandle { amount ->
                    colWChapter = (colWChapter + amount).coerceIn(
                        with(density) { 60.dp.toPx() },
                        with(density) { 300.dp.toPx() }
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {

                    val crossRefReserve =
                        if (crossRefsEnabled) colWCrossRef + with(density) { 5.dp.toPx() } else 0f
                    val effectiveSplitWidth = if (isSplitActive)
                        colWSplit.coerceAtMost(
                            (constraints.maxWidth - crossRefReserve - with(density) { (100.dp + 6.dp).toPx() }).coerceAtLeast(0f)
                        )
                    else 0f
                    Row(modifier = Modifier.fillMaxSize()) {

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            var showVerseContextMenu by remember { mutableStateOf(false) }
                            var verseContextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

                            Box(modifier = Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            if (event.type == PointerEventType.Press && event.button?.isSecondary == true) {
                                                val pos = event.changes.first().position
                                                verseContextMenuOffset = with(density) { DpOffset(pos.x.toDp(), pos.y.toDp()) }
                                            }
                                        }
                                    }
                                }
                            ) {
                                val multiIndicesInFiltered = filteredSelectionIndices(
                                    viewModel.selectedVerseIndices, verses, filteredVerses,
                                )

                                BibleVerseColumn(
                                    verses = filteredVerses,
                                    selectedIndex = if (filteredVerses.isEmpty()) -1 else {
                                        val currentVerse = verses.getOrNull(selectedVerseIndex)
                                        filteredVerses.indexOf(currentVerse).coerceAtLeast(0)
                                    },
                                    selectedIndices = multiIndicesInFiltered,
                                    accentColor = accentColor,
                                    onItemSelected = { index ->
                                        val verseText = filteredVerses.getOrNull(index)
                                        verseText?.let {
                                            val realIndex = verses.indexOf(it)
                                            if (realIndex >= 0) viewModel.selectVerse(realIndex)
                                        }

                                        crossRefNavigatedTo = null
                                        crossRefAnchorEpoch++

                                        crossRefPopoverIndex = -1
                                        crossRefPopoverAnchor = null
                                        focusRequester.requestFocus()
                                    },
                                    refCountFor = { index ->
                                        filteredVerses.getOrNull(index)
                                            ?.let(::verseNumberOf)
                                            ?.let { crossRefCounts[it] } ?: 0
                                    },
                                    refCountTooltip = { count ->
                                        crossRefCountStr.format(count)
                                    },
                                    openRefIndex = if (crossRefsEnabled) -1 else crossRefPopoverIndex,
                                    onRefsClicked = { index ->
                                        val verseText = filteredVerses.getOrNull(index)
                                        val realIndex = verseText?.let { verses.indexOf(it) } ?: -1
                                        if (realIndex >= 0) viewModel.selectVerse(realIndex)
                                        crossRefNavigatedTo = null
                                        crossRefAnchorEpoch++
                                        val number = verseText?.let(::verseNumberOf)
                                        val canonical = number?.let {
                                            viewModel.canonicalRefForDisplay(selectedBookIndex, selectedChapter, it)
                                        }?.let { (book, chapter, verse) -> verse?.let { Triple(book, chapter, it) } }

                                        if (crossRefsEnabled || canonical == null || crossRefPopoverIndex == index) {
                                            crossRefPopoverIndex = -1
                                            crossRefPopoverAnchor = null
                                        } else {
                                            crossRefPopoverIndex = index
                                            crossRefPopoverAnchor = canonical
                                            crossRefPopoverLabel = viewModel
                                                .moduleRefFor(canonical.first, canonical.second, canonical.third)
                                                ?.let { formatCrossRefLabel(it.abbreviation, it.chapter, it.verse, null) }
                                                ?: ""
                                        }
                                        focusRequester.requestFocus()
                                    },
                                    refPopover = {
                                        CrossReferencePopover(
                                            title = crossRefPopoverTitleStr.format(
                                                crossRefPopoverLabel, crossRefPopoverRows.size,
                                            ),
                                            rows = crossRefPopoverRows,
                                            onDismiss = {
                                                crossRefPopoverIndex = -1
                                                crossRefPopoverAnchor = null
                                                focusRequester.requestFocus()
                                            },
                                            onDock = {
                                                onSettingsChange { s -> withBibleCrossReferencePanel(s, true) }
                                                crossRefPopoverIndex = -1
                                                crossRefPopoverAnchor = null
                                                focusRequester.requestFocus()
                                            },
                                            onOpen = ::openCrossRef,
                                            onGoLive = ::goLiveCrossRef,
                                            onAddToSchedule = ::scheduleCrossRef,
                                        )
                                    },
                                    onItemDoubleClicked = { _ -> goLiveWithHistory() },
                                    onItemCtrlClicked = { index ->
                                        val verseText = filteredVerses.getOrNull(index)
                                        verseText?.let {
                                            val realIndex = verses.indexOf(it)
                                            if (realIndex >= 0) viewModel.ctrlClickVerse(realIndex)
                                        }
                                    },
                                    onItemShiftClicked = { index ->
                                        val verseText = filteredVerses.getOrNull(index)
                                        verseText?.let {
                                            val realIndex = verses.indexOf(it)
                                            if (realIndex >= 0) viewModel.shiftClickVerse(realIndex)
                                        }
                                    },
                                    onRightClicked = { index ->
                                        val verseText = filteredVerses.getOrNull(index)
                                        verseText?.let {
                                            val realIndex = verses.indexOf(it)
                                            if (realIndex >= 0) viewModel.selectVerse(realIndex)
                                        }
                                        showVerseContextMenu = true
                                    }
                                )

                                DropdownMenu(
                                    expanded = showVerseContextMenu,
                                    onDismissRequest = { showVerseContextMenu = false },
                                    offset = verseContextMenuOffset
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.copy_verse)) },
                                        leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            val verseStr = verses.getOrNull(selectedVerseIndex) ?: ""
                                            val verseText = verseTextOf(verseStr)
                                            val bookName = books.getOrNull(selectedBookIndex) ?: ""
                                            val reference = formatVerseReference(verseStr, bookName, selectedChapter)
                                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                            clipboard.setContents(java.awt.datatransfer.StringSelection("$reference\n$verseText"), null)
                                            showVerseContextMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.add_to_schedule)) },
                                        leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_playlist_add), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary) },
                                        onClick = {
                                            viewModel.addCurrentVerseToSchedule { bookName, chapter, verseNumber, verseText, verseRange, bookId ->
                                                onAddToSchedule?.invoke(bookName, chapter, verseNumber, verseText, verseRange, bookId)
                                            }
                                            focusRequester.requestFocus()
                                            showVerseContextMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.go_live)) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                        onClick = { goLiveWithHistory(); focusRequester.requestFocus(); showVerseContextMenu = false }
                                    )
                                }
                            }
                        }

                        if (crossRefsEnabled) {
                            DragHandle(onDragEnd = ::saveColWCrossRef) { amount ->
                                colWCrossRef = (colWCrossRef - amount).coerceIn(
                                    with(density) { CROSS_REF_MIN_WIDTH.toPx() },
                                    with(density) { CROSS_REF_MAX_WIDTH.toPx() },
                                )
                            }
                            CrossReferencePanel(
                                rows = crossRefRows,
                                selectedIndex = selectedCrossRefIdx,
                                onClick = { idx ->
                                    selectedCrossRefIdx = idx
                                    crossRefRows.getOrNull(idx)?.let(::openCrossRef)
                                },
                                onDoubleClick = { idx ->
                                    selectedCrossRefIdx = idx
                                    crossRefRows.getOrNull(idx)?.let(::goLiveCrossRef)
                                },
                                onAddToSchedule = { idx -> crossRefRows.getOrNull(idx)?.let(::scheduleCrossRef) },
                                onClose = {
                                    onSettingsChange { s -> withBibleCrossReferencePanel(s, false) }
                                    focusRequester.requestFocus()
                                },
                                passageSpan = if (crossRefPassageMode) {
                                    val chapter = crossRefRun.first().second
                                    "$chapter:${crossRefRun.first().third}-${crossRefRun.last().third}"
                                } else null,
                                modifier = Modifier.width(with(density) { colWCrossRef.toDp() }).fillMaxHeight(),
                            )
                        }

                        if (isSplitActive) {
                            DragHandle(onDragEnd = ::saveColWSplit) { amount ->
                                colWSplit = (colWSplit - amount).coerceIn(with(density) { 150.dp.toPx() }, with(density) { 600.dp.toPx() })
                            }
                            Column(modifier = Modifier.width(with(density) { effectiveSplitWidth.toDp() }).fillMaxHeight()) {
                                LiveChapterPanel(
                                    verses = liveChapterVerses,
                                    liveVerseNumbers = liveVerseNumbers,
                                    onVerseClicked = { verseNum ->
                                        scope.launch {
                                            val verses = viewModel.getVersesForDisplay(liveBookName, liveChapterNum, verseNum)
                                            if (verses.isNotEmpty()) {
                                                val primary = verses.first()
                                                statisticsManager?.recordVerseDisplay(primary.bibleName, primary.bookName, primary.chapter, primary.verseNumber)
                                                onVerseSelected(verses)
                                                onInstanceLinkSendVerse?.invoke(primary.bookName, primary.chapter, primary.verseNumber, primary.verseText, primary.verseRange)
                                                presenterManager?.let { if (it.bibleHold.value) { it.setBibleHold(false); onInstanceLinkSendBibleHold?.invoke(false) } }
                                                onPresenting(Presenting.BIBLE)

                                                viewModel.logLiveReference(
                                                    displayBookIndex = viewModel.displayIndexForBookName(liveBookName)
                                                        .takeIf { it >= 0 } ?: viewModel.selectedBookIndex.value,
                                                    chapter    = primary.chapter,
                                                    verseStart = primary.verseNumber,
                                                    verseEnd   = null,
                                                    source     = "manual",
                                                    autoFollow = viewModel.autoFollowEnabled.value,
                                                )
                                                viewModel.canonicalRefForBookName(
                                                    liveBookName, primary.chapter, primary.verseNumber,
                                                )?.let(::anchorLiveVerse)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                    }
                    }

                    BibleHistoryPanel(
                        entries = viewModel.history,
                        expanded = historyExpanded,
                        selectedIndex = selectedHistoryIdx,
                        onToggleExpanded = { historyExpanded = !historyExpanded },
                        onClear = { viewModel.clearHistory() },
                        onEntryClick = { idx ->
                            selectedHistoryIdx = idx
                            viewModel.history.getOrNull(idx)?.let {
                                viewModel.selectVerseByDetails(it.bookName, it.chapter, it.verseNumber, it.verseRange)
                            }
                            focusRequester.requestFocus()
                        },
                        onEntryDoubleClick = { idx ->
                            selectedHistoryIdx = idx
                            viewModel.history.getOrNull(idx)?.let {
                                viewModel.selectVerseByDetails(
                                    it.bookName, it.chapter, it.verseNumber, it.verseRange,
                                    goLiveSource = "history",
                                )
                            }
                            focusRequester.requestFocus()
                        },
                    )

                }

            }
        }
    }
}
