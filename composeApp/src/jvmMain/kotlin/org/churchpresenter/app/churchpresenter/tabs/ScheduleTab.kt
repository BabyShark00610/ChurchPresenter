package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.shape.RoundedCornerShape
import org.churchpresenter.app.churchpresenter.composables.finalPassCombinedClickable
import org.churchpresenter.app.churchpresenter.composables.initialPassCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.file_chooser_open_schedule
import churchpresenter.composeapp.generated.resources.file_chooser_save_schedule
import churchpresenter.composeapp.generated.resources.file_filter_schedule
import churchpresenter.composeapp.generated.resources.ic_add
import churchpresenter.composeapp.generated.resources.ic_arrow_down
import churchpresenter.composeapp.generated.resources.ic_arrow_up
import churchpresenter.composeapp.generated.resources.ic_close
import churchpresenter.composeapp.generated.resources.ic_delete
import churchpresenter.composeapp.generated.resources.ic_drag_dots
import churchpresenter.composeapp.generated.resources.ic_edit
import churchpresenter.composeapp.generated.resources.ic_folder
import churchpresenter.composeapp.generated.resources.ic_label
import churchpresenter.composeapp.generated.resources.ic_play
import churchpresenter.composeapp.generated.resources.ic_check
import churchpresenter.composeapp.generated.resources.ic_note
import churchpresenter.composeapp.generated.resources.ic_redo
import churchpresenter.composeapp.generated.resources.ic_remove
import churchpresenter.composeapp.generated.resources.ic_save
import churchpresenter.composeapp.generated.resources.ic_undo
import churchpresenter.composeapp.generated.resources.pause_duration_ms
import churchpresenter.composeapp.generated.resources.planning_center_import_title
import churchpresenter.composeapp.generated.resources.schedule
import churchpresenter.composeapp.generated.resources.schedule_density_compact
import churchpresenter.composeapp.generated.resources.schedule_density_detailed
import churchpresenter.composeapp.generated.resources.schedule_density_normal
import churchpresenter.composeapp.generated.resources.schedule_item_count
import churchpresenter.composeapp.generated.resources.schedule_note_placeholder
import churchpresenter.composeapp.generated.resources.tooltip_note
import churchpresenter.composeapp.generated.resources.tooltip_note_clear
import churchpresenter.composeapp.generated.resources.tooltip_note_done
import churchpresenter.composeapp.generated.resources.tooltip_redo
import churchpresenter.composeapp.generated.resources.tooltip_undo
import churchpresenter.composeapp.generated.resources.autosave_restore_confirm
import churchpresenter.composeapp.generated.resources.autosave_restore_discard
import churchpresenter.composeapp.generated.resources.autosave_restore_message
import churchpresenter.composeapp.generated.resources.autosave_restore_title
import churchpresenter.composeapp.generated.resources.schedule_add_files
import churchpresenter.composeapp.generated.resources.schedule_drop_hint
import churchpresenter.composeapp.generated.resources.schedule_drop_to_remove
import churchpresenter.composeapp.generated.resources.tooltip_add_label
import churchpresenter.composeapp.generated.resources.tooltip_clear_schedule
import churchpresenter.composeapp.generated.resources.tooltip_edit_label
import churchpresenter.composeapp.generated.resources.tooltip_go_live
import churchpresenter.composeapp.generated.resources.tooltip_move_down
import churchpresenter.composeapp.generated.resources.tooltip_schedule_zoom_in
import churchpresenter.composeapp.generated.resources.tooltip_schedule_zoom_out
import churchpresenter.composeapp.generated.resources.tooltip_move_up
import churchpresenter.composeapp.generated.resources.tooltip_new_schedule
import churchpresenter.composeapp.generated.resources.tooltip_open_schedule
import churchpresenter.composeapp.generated.resources.tooltip_remove
import churchpresenter.composeapp.generated.resources.tooltip_remove_from_schedule
import churchpresenter.composeapp.generated.resources.tooltip_save_schedule
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.churchpresenter.app.churchpresenter.composables.ConditionalTooltipArea
import org.churchpresenter.app.churchpresenter.composables.TooltipIconButton
import org.churchpresenter.app.churchpresenter.data.settings.PlanningCenterSettings
import org.churchpresenter.app.churchpresenter.dialogs.PlanningCenterImportDialog
import org.churchpresenter.app.churchpresenter.dialogs.filechooser.FileChooser
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import org.churchpresenter.app.churchpresenter.utils.Utils
import org.churchpresenter.app.churchpresenter.utils.DroppedFileAction
import org.churchpresenter.app.churchpresenter.utils.IMAGE_EXTENSIONS
import org.churchpresenter.app.churchpresenter.utils.DragItemGeometry
import org.churchpresenter.app.churchpresenter.utils.ScheduleDensity
import org.churchpresenter.app.churchpresenter.utils.classifyDroppedFile
import org.churchpresenter.app.churchpresenter.utils.dragDropTarget
import org.churchpresenter.app.churchpresenter.utils.scheduleCanZoomIn
import org.churchpresenter.app.churchpresenter.utils.scheduleCanZoomOut
import org.churchpresenter.app.churchpresenter.utils.scheduleDensityFor
import org.churchpresenter.app.churchpresenter.utils.scheduleShowDetailLine
import org.churchpresenter.app.churchpresenter.utils.scheduleShowKindDetails
import org.churchpresenter.app.churchpresenter.utils.scheduleZoomIn
import org.churchpresenter.app.churchpresenter.utils.scheduleZoomOut
import org.churchpresenter.app.churchpresenter.viewmodel.ScheduleViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.announcementTimerSubtext
import org.churchpresenter.app.churchpresenter.viewmodel.scheduleItemDetailText
import org.churchpresenter.app.churchpresenter.viewmodel.scheduleItemGlyph
import org.churchpresenter.app.churchpresenter.viewmodel.scheduleItemKindLabel
import org.churchpresenter.app.churchpresenter.viewmodel.scheduleItemPaletteIndex
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Actions exposed to the parent composable so toolbar/menu can drive the schedule
 * without holding a reference to [ScheduleViewModel].
 */
data class ScheduleTabActions(
    val newSchedule: () -> Unit = {},
    val openSchedule: () -> Unit = {},
    val saveSchedule: () -> Unit = {},
    val saveScheduleAs: () -> Unit = {},
    val removeSelected: () -> Unit = {},
    /** Removes a specific item by id, regardless of current UI selection — used to apply an
     *  approved remote "remove from schedule" request (mobile companion or Instance Link). */
    val removeById: (id: String) -> Unit = {},
    val clearSchedule: () -> Unit = {},
    val moveSelectedToTop: () -> Unit = {},
    val moveSelectedUp: () -> Unit = {},
    val moveSelectedDown: () -> Unit = {},
    val moveSelectedToBottom: () -> Unit = {},
    val addLabel: (text: String, textColor: String, backgroundColor: String) -> Unit = { _, _, _ -> },
    val updateLabel: (id: String, text: String, textColor: String, backgroundColor: String) -> Unit = { _, _, _, _ -> },
    val addBibleVerse: (bookName: String, chapter: Int, verseNumber: Int, verseText: String, verseRange: String, bookId: Int) -> Unit = { _, _, _, _, _, _ -> },
    val addSong: (songNumber: Int, title: String, songbook: String, songId: String) -> Unit = { _, _, _, _ -> },
    val addPicture: (folderPath: String, folderName: String, imageCount: Int) -> Unit = { _, _, _ -> },
    val addPresentation: (filePath: String, fileName: String, slideCount: Int, fileType: String) -> Unit = { _, _, _, _ -> },
    val addMedia: (mediaUrl: String, mediaTitle: String, mediaType: String) -> Unit = { _, _, _ -> },
    val addLowerThird: (presetId: String, presetLabel: String, pauseAtFrame: Boolean, pauseDurationMs: Long) -> Unit = { _, _, _, _ -> },
    val addAnnouncement: (text: String, textColor: String, backgroundColor: String, fontSize: Int, fontType: String, bold: Boolean, italic: Boolean, underline: Boolean, shadow: Boolean, shadowColor: String, shadowSize: Int, shadowOpacity: Int, horizontalAlignment: String, position: String, animationType: String, animationDuration: Int, loopCount: Int, isTimer: Boolean, timerHours: Int, timerMinutes: Int, timerSeconds: Int, timerTextColor: String, timerExpiredText: String, timerMode: String, targetHour: Int, targetMinute: Int, targetSecond: Int, liveClockFormat: String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    val addWebsite: (url: String, title: String) -> Unit = { _, _ -> },
    val updateWebsiteTitle: (url: String, title: String) -> Unit = { _, _ -> },
    val addScene: (sceneId: String, sceneName: String) -> Unit = { _, _ -> },
    val addDictionary: (number: String, word: String, transliteration: String, definition: String) -> Unit = { _, _, _, _ -> }
)

/** The card density percentage a schedule opens at; the rung ladder itself lives in ScheduleZoom.kt. */
private const val ZOOM_DEFAULT = 100

/** How far the pointer must travel on a card's icon before it becomes a reorder drag. */
private val DRAG_HANDLE_THRESHOLD = 4.dp

/** Height of the drop-here-to-remove zone at the bottom of the list, shown while dragging. */
private val DELETE_ZONE_HEIGHT = 56.dp

/** Corner radius shared by every card (section or item) in the list. */
private val CARD_SHAPE = RoundedCornerShape(9.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleTab(
    modifier: Modifier = Modifier,
    /** Optional pre-created view model. Pass one from a parent composable to survive panel
     *  collapse/expand (i.e. when this composable leaves the composition inside AnimatedVisibility).
     *  If null a local view model is created and owned by this composable. */
    scheduleViewModel: ScheduleViewModel? = null,
    onPresenting: (Presenting) -> Unit = { Presenting.NONE },
    onItemClick: (ScheduleItem) -> Unit = {},
    onEditLabel: (ScheduleItem.LabelItem) -> Unit = {},
    onPresentSong: ((ScheduleItem.SongItem) -> Unit)? = null,
    onPresentBible: ((ScheduleItem.BibleVerseItem) -> Unit)? = null,
    onPresentPresentation: ((ScheduleItem.PresentationItem) -> Unit)? = null,
    onPresentPictures: ((ScheduleItem.PictureItem) -> Unit)? = null,
    onPresentMedia: ((ScheduleItem.MediaItem) -> Unit)? = null,
    onPresentAnnouncement: ((ScheduleItem.AnnouncementItem) -> Unit)? = null,
    onPresentLowerThird: ((ScheduleItem.LowerThirdItem) -> Unit)? = null,
    onPresentWebsite: ((ScheduleItem.WebsiteItem) -> Unit)? = null,
    onPresentDictionary: ((ScheduleItem.DictionaryItem) -> Unit)? = null,
    onPresentScene: ((ScheduleItem.SceneItem) -> Unit)? = null,
    onActionsReady: (ScheduleTabActions) -> Unit = {},
    onSelectedItemChanged: (String?) -> Unit = {},
    onScheduleChanged: ((List<ScheduleItem>) -> Unit)? = null,
    onAddLabel: () -> Unit = {},
    onAddWebsite: () -> Unit = {},
    theme: ThemeMode = ThemeMode.SYSTEM,
    itemZoomPercent: Int = ZOOM_DEFAULT,
    onItemZoomChange: (Int) -> Unit = {},
    planningCenterSettings: PlanningCenterSettings = PlanningCenterSettings(),
    onPlanningCenterTokensRefreshed: (accessToken: String, refreshToken: String, expiresAtEpochMs: Long) -> Unit = { _, _, _ -> },
    onPlanningCenterConnected: (accessToken: String, refreshToken: String, expiresAtEpochMs: Long, personName: String) -> Unit = { _, _, _, _ -> },
    onPlanningCenterDisconnect: () -> Unit = {}
) {
    val onScheduleChangedState = rememberUpdatedState(onScheduleChanged)
    // Use the provided view model, or fall back to a locally-owned one.
    val viewModel = scheduleViewModel ?: remember { ScheduleViewModel(onScheduleChanged = { items -> onScheduleChangedState.value?.invoke(items) }) }
    val scope = rememberCoroutineScope()

    var showAutoRestoreDialog by remember { mutableStateOf(viewModel.shouldPromptAutoRestore()) }
    if (showAutoRestoreDialog) {
        val savedAt = remember { viewModel.autoSaveSavedAt() }
        val timeStr = remember(savedAt) {
            SimpleDateFormat("h:mm a").format(Date(savedAt))
        }
        AlertDialog(
            onDismissRequest = { showAutoRestoreDialog = false },
            title = { Text(stringResource(Res.string.autosave_restore_title)) },
            text = { Text(stringResource(Res.string.autosave_restore_message, timeStr)) },
            confirmButton = {
                Button(
                    shape = RoundedCornerShape(6.dp),
                    onClick = {
                    viewModel.restoreAutoSave()
                    showAutoRestoreDialog = false
                }) { Text(stringResource(Res.string.autosave_restore_confirm)) }
            },
            dismissButton = {
                TextButton(
                    shape = RoundedCornerShape(6.dp),
                    onClick = {
                    viewModel.clearAutoSave()
                    showAutoRestoreDialog = false
                }) { Text(stringResource(Res.string.autosave_restore_discard)) }
            }
        )
    }

    // State holders — lambdas capture the State object, not the string value,
    // so they always read the latest value via .value without re-registering.
    val strSaveScheduleAs = rememberUpdatedState(stringResource(Res.string.file_chooser_save_schedule))
    val strOpenSchedule   = rememberUpdatedState(stringResource(Res.string.file_chooser_open_schedule))
    val strFileFilter     = rememberUpdatedState(stringResource(Res.string.file_filter_schedule))

    // Register actions once — no recomposition cycle.
    LaunchedEffect(Unit) {
        onActionsReady(
            ScheduleTabActions(
                newSchedule      = { viewModel.newSchedule() },
                openSchedule     = { scope.launch { viewModel.loadSchedule(strOpenSchedule.value, strFileFilter.value) } },
                saveSchedule     = { scope.launch { viewModel.saveSchedule(strSaveScheduleAs.value, strFileFilter.value) } },
                saveScheduleAs   = { scope.launch { viewModel.saveScheduleAs(strSaveScheduleAs.value, strFileFilter.value) } },
                removeSelected   = { viewModel.selectedItemId?.let { viewModel.removeItem(it) } },
                removeById       = { id -> viewModel.removeItem(id) },
                clearSchedule    = { viewModel.clearSchedule() },
                moveSelectedToTop    = { viewModel.selectedItemId?.let { viewModel.moveItemToTop(it) } },
                moveSelectedUp       = { viewModel.selectedItemId?.let { viewModel.moveItemUp(it) } },
                moveSelectedDown     = { viewModel.selectedItemId?.let { viewModel.moveItemDown(it) } },
                moveSelectedToBottom = { viewModel.selectedItemId?.let { viewModel.moveItemToBottom(it) } },
                addLabel    = { text, textColor, bg -> viewModel.addLabel(text, textColor, bg) },
                updateLabel = { id, text, textColor, bg -> viewModel.updateLabel(id, text, textColor, bg) },
                addBibleVerse    = { bookName, chapter, verseNumber, verseText, verseRange, bookId -> viewModel.addBibleVerse(bookName, chapter, verseNumber, verseText, verseRange, bookId) },
                addSong          = { songNumber, title, songbook, songId -> viewModel.addSong(songNumber, title, songbook, songId) },
                addPicture       = { folderPath, folderName, imageCount -> viewModel.addPicture(folderPath, folderName, imageCount) },
                addPresentation  = { filePath, fileName, slideCount, fileType -> viewModel.addPresentation(filePath, fileName, slideCount, fileType) },
                addMedia         = { mediaUrl, mediaTitle, mediaType -> viewModel.addMedia(mediaUrl, mediaTitle, mediaType) },
                addLowerThird    = { presetId, presetLabel, pauseAtFrame, pauseDurationMs -> viewModel.addLowerThird(presetId, presetLabel, pauseAtFrame, pauseDurationMs) },
                addAnnouncement  = { text, textColor, backgroundColor, fontSize, fontType, bold, italic, underline, shadow, shadowColor, shadowSize, shadowOpacity, horizontalAlignment, position, animationType, animationDuration, loopCount, isTimer, timerHours, timerMinutes, timerSeconds, timerTextColor, timerExpiredText, timerMode, targetHour, targetMinute, targetSecond, liveClockFormat ->
                    viewModel.addAnnouncement(text, textColor, backgroundColor, fontSize, fontType, bold, italic, underline, shadow, shadowColor, shadowSize, shadowOpacity, horizontalAlignment, position, animationType, animationDuration, loopCount, isTimer, timerHours, timerMinutes, timerSeconds, timerTextColor, timerExpiredText, timerMode, targetHour, targetMinute, targetSecond, liveClockFormat)
                },
                addWebsite       = { url, title -> viewModel.addWebsite(url, title) },
                updateWebsiteTitle = { url, title -> viewModel.updateWebsiteTitle(url, title) },
                addScene         = { sceneId, sceneName -> viewModel.addScene(sceneId, sceneName) },
                addDictionary    = { number, word, transliteration, definition -> viewModel.addDictionary(number, word, transliteration, definition) }
            )
        )
    }

    // Notify parent when selection changes
    LaunchedEffect(viewModel.selectedItemId) {
        onSelectedItemChanged(viewModel.selectedItemId)
    }

    val scheduleItems = viewModel.scheduleItems
    val selectedItemId = viewModel.selectedItemId
    var showPlanningCenterImport by remember { mutableStateOf(false) }
    val density = scheduleDensityFor(itemZoomPercent)

    Column(modifier = modifier.fillMaxSize()) {

        ScheduleHeader(
            itemCount = scheduleItems.count { it !is ScheduleItem.LabelItem },
            density = density,
            onZoomOut = { onItemZoomChange(scheduleZoomOut(itemZoomPercent)) },
            onZoomIn = { onItemZoomChange(scheduleZoomIn(itemZoomPercent)) },
            canZoomOut = scheduleCanZoomOut(itemZoomPercent),
            canZoomIn = scheduleCanZoomIn(itemZoomPercent),
            onNewSchedule = { viewModel.newSchedule() },
            onOpenSchedule = { scope.launch { viewModel.loadSchedule(strOpenSchedule.value, strFileFilter.value) } },
            onSaveSchedule = { scope.launch { viewModel.saveSchedule(strSaveScheduleAs.value, strFileFilter.value) } },
            canUndo = viewModel.canUndo,
            canRedo = viewModel.canRedo,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onAddLabel = onAddLabel,
            onImportPlanningCenter = { showPlanningCenterImport = true },
            onRemoveSelected = { viewModel.selectedItemId?.let { viewModel.removeItem(it) } },
            onClearSchedule = { viewModel.clearSchedule() }
        )

        // Schedule items list with drag-and-drop support
        val viewModelState = rememberUpdatedState(viewModel)
        var listHeightPx by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { listHeightPx = it.height }
        ) {
            val listState = rememberLazyListState()

            // Drag-to-reorder state: shift+click+drag anywhere on a card, or plain drag on its icon
            var draggingFromIndex by remember { mutableStateOf(-1) }
            var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
            var isDragActive by remember { mutableStateOf(false) }
            var dragCursorY by remember { mutableStateOf(0f) }
            var dragItemHeight by remember { mutableStateOf(50f) }
            var isOverDeleteZone by remember { mutableStateOf(false) }

            val baseDensity = LocalDensity.current

            // Reorder gesture, shared by the whole-card shift+drag and the per-card icon handle.
            // The handle path arms only after real movement so a plain click still selects the card.
            val dragThresholdPx = with(baseDensity) { DRAG_HANDLE_THRESHOLD.toPx() }
            val deleteZonePx = with(baseDensity) { DELETE_ZONE_HEIGHT.toPx() }
            fun Modifier.reorderGesture(index: Int, requireShift: Boolean): Modifier =
                pointerInput(index, requireShift) {
                    awaitPointerEventScope {
                        while (true) {
                            val pressEvent = awaitPointerEvent(PointerEventPass.Initial)
                            if (pressEvent.type != PointerEventType.Press) continue
                            if (requireShift && !pressEvent.keyboardModifiers.isShiftPressed) continue
                            if (requireShift) pressEvent.changes.forEach { it.consume() }

                            var lastPos = pressEvent.changes.first().position
                            var travelled = if (requireShift) dragThresholdPx else 0f
                            var armed = false
                            var dragging = true
                            // Idempotent: called on the normal drop path AND from finally, so a
                            // cancelled gesture (node disposed mid-drag) can never strand the
                            // state — a stuck isDragActive freezes the whole sidebar.
                            fun endDrag() {
                                if (draggingFromIndex == index) draggingFromIndex = -1
                                dropTargetIndex = null
                                isDragActive = false
                                isOverDeleteZone = false
                                dragCursorY = 0f
                            }
                            try {
                            while (dragging) {
                                // Another card already owns this drag (a shift+press on the grip
                                // reaches both gestures) — don't arm a second one over it
                                if (!armed && travelled >= dragThresholdPx && !isDragActive) {
                                    val itemInfo = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == index }
                                    draggingFromIndex = index
                                    isDragActive = true
                                    dropTargetIndex = index
                                    dragItemHeight = itemInfo?.size?.toFloat() ?: 50f
                                    dragCursorY = if (itemInfo != null) {
                                        itemInfo.offset + itemInfo.size / 2f
                                    } else lastPos.y
                                    armed = true
                                }
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (armed) event.changes.forEach { it.consume() }
                                // A drop outside every card's bounds can arrive as something
                                // other than Release, so treat "nothing pressed" as the end too
                                val finished = event.type == PointerEventType.Release ||
                                    event.changes.none { it.pressed }
                                if (finished) {
                                    if (armed && draggingFromIndex == index) {
                                        val droppedId = scheduleItems.getOrNull(index)?.id
                                        if (isOverDeleteZone && droppedId != null) {
                                            viewModel.removeItem(droppedId)
                                            if (viewModel.selectedItemId == droppedId) {
                                                viewModel.clearSelection()
                                            }
                                        } else {
                                            val to = dropTargetIndex ?: index
                                            if (index != to) viewModel.moveItem(index, to)
                                        }
                                    }
                                    dragging = false
                                } else if (event.type == PointerEventType.Move) {
                                    val pos = event.changes.firstOrNull()?.position ?: continue
                                    val deltaY = (pos - lastPos).y
                                    lastPos = pos
                                    if (!armed) {
                                        travelled += abs(deltaY)
                                        continue
                                    }
                                    dragCursorY += deltaY
                                    // Over the delete zone the card is being removed, not reordered
                                    val hit = dragDropTarget(
                                        cursorY = dragCursorY,
                                        listHeightPx = listHeightPx,
                                        deleteZonePx = deleteZonePx,
                                        visibleItems = listState.layoutInfo.visibleItemsInfo.map {
                                            DragItemGeometry(it.index, it.offset, it.size)
                                        },
                                    )
                                    isOverDeleteZone = hit.overDeleteZone
                                    if (!hit.overDeleteZone) {
                                        hit.targetIndex?.let { dropTargetIndex = it }
                                    }
                                }
                            }
                            } finally {
                                if (armed) endDrag()
                            }
                        }
                    }
                }

            // Register AWT DropTarget on the window for file drag-and-drop
            DisposableEffect(Unit) {
                val awtWindow = java.awt.Window.getWindows().firstOrNull { it.isShowing }
                val dropTarget = awtWindow?.let { win ->
                    DropTarget(win, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
                        override fun drop(event: DropTargetDropEvent) {
                            event.acceptDrop(DnDConstants.ACTION_COPY)
                            try {
                                val transferable = event.transferable
                                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                    @Suppress("UNCHECKED_CAST")
                                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                                    val vm = viewModelState.value
                                    handleDroppedFiles(files, vm)
                                }
                                event.dropComplete(true)
                            } catch (e: Exception) {
                                event.dropComplete(false)
                            }
                        }
                    }, true)
                }
                onDispose {
                    if (dropTarget != null) {
                        awtWindow.dropTarget = null
                    }
                }
            }

            if (scheduleItems.isEmpty()) {
                // Empty state hint
                Text(
                    text = stringResource(Res.string.schedule_drop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            }

            val rows = scheduleItems.toList()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .padding(top = 6.dp, bottom = 10.dp, end = 4.dp)
            ) {
                // A fixed copy, not the live list. `itemsIndexed` reads `items[index]` inside the key
                // and content-type lambdas it builds, and those run when the lazy layout rebuilds its
                // key map — which can be after a delete or an undo has already shortened the list,
                // leaving it indexing past the end and taking the whole tab down. Copying at
                // composition gives those lambdas a list that cannot shrink under them.
                itemsIndexed(rows, key = { _, item -> item.id }) { index, item ->
                    val isDraggingThis = isDragActive && draggingFromIndex == index
                    val nextIsSection = rows.getOrNull(index + 1) is ScheduleItem.LabelItem

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(bottom = if (nextIsSection) 12.dp else 3.dp)
                            .alpha(if (isDraggingThis) 0.35f else 1f)
                            .reorderGesture(index, requireShift = true)
                    ) {
                        ScheduleItemRow(
                            item = item,
                            dragHandleModifier = Modifier.reorderGesture(index, requireShift = false),
                            density = density,
                            isSelected = item.id == selectedItemId,
                            note = viewModel.getNote(item.id),
                            onSelect = {
                                if (!isDragActive) {
                                    viewModel.selectItem(item.id)
                                    onItemClick(item)
                                }
                            },
                            onMoveUp   = { viewModel.moveItemUp(item.id) },
                            onMoveDown = { viewModel.moveItemDown(item.id) },
                            onRemove = {
                                viewModel.removeItem(item.id)
                                if (selectedItemId == item.id) viewModel.clearSelection()
                            },
                            onPresent = {
                                viewModel.presentItem(
                                    item = item,
                                    onPresenting = onPresenting,
                                    onPresentSong = onPresentSong,
                                    onPresentBible = onPresentBible,
                                    onPresentPresentation = onPresentPresentation,
                                    onPresentPictures = onPresentPictures,
                                    onPresentMedia = onPresentMedia,
                                    onPresentAnnouncement = onPresentAnnouncement,
                                    onPresentLowerThird = onPresentLowerThird,
                                    onPresentWebsite = onPresentWebsite,
                                    onPresentDictionary = onPresentDictionary,
                                    onPresentScene = onPresentScene
                                )
                            },
                            onEditLabel = {
                                if (item is ScheduleItem.LabelItem) onEditLabel(item)
                            },
                            onNoteChanged = { viewModel.setNote(item.id, it) }
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState)
            )

            // Drop-here-to-remove zone, only while a card is being dragged
            if (isDragActive) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(DELETE_ZONE_HEIGHT)
                        .zIndex(5f)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = if (isOverDeleteZone) 0.9f else 0.25f),
                            RoundedCornerShape(4.dp)
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Text(
                        text = stringResource(Res.string.schedule_drop_to_remove),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            // Floating drag preview — elevated card follows cursor
            if (isDragActive) {
                val dragItem = scheduleItems.getOrNull(draggingFromIndex)
                dragItem?.let { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(10f)
                            .graphicsLayer {
                                translationY = dragCursorY - dragItemHeight / 2
                                scaleX = 1.04f
                                scaleY = 1.04f
                                shadowElevation = 20f
                            }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CARD_SHAPE)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scheduleItemGlyph(item),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = item.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Add Files button at the bottom — dashed outline (a drop-target look), not a filled
        // button, matching the reference design.
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ScheduleAddFilesButton(
                onClick = {
                    scope.launch {
                        val files = FileChooser.platformInstance.chooseMultiple(
                            path = null,
                            title = "Add Files to Schedule",
                            filters = emptyList(),
                            selectDirectory = false
                        )
                        if (files != null) {
                            handleDroppedFiles(files.map(Path::toFile), viewModel)
                        }
                    }
                }
            )
        }

        PlanningCenterImportDialog(
            isVisible = showPlanningCenterImport,
            theme = theme,
            settings = planningCenterSettings,
            onDismiss = { showPlanningCenterImport = false },
            onTokensRefreshed = onPlanningCenterTokensRefreshed,
            onAddSong = { songNumber, title, songbook, songId ->
                viewModel.addSong(songNumber, title, songbook, songId)
            },
            onAddLabel = { text, textColor, backgroundColor ->
                viewModel.addLabel(text, textColor, backgroundColor)
            },
            onAddPresentation = { filePath, fileName, slideCount, fileType ->
                viewModel.addPresentation(filePath, fileName, slideCount, fileType)
            },
            onAddPicture = { folderPath, folderName, imageCount ->
                viewModel.addPicture(folderPath, folderName, imageCount)
            },
            onAddMedia = { mediaUrl, mediaTitle, mediaType ->
                viewModel.addMedia(mediaUrl, mediaTitle, mediaType)
            },
            onAddAnnouncement = { text ->
                viewModel.addAnnouncement(text = text)
            },
            onAddBibleVerse = { bookName, chapter, verseNumber, verseText, verseRange, bookId ->
                viewModel.addBibleVerse(bookName, chapter, verseNumber, verseText, verseRange, bookId)
            },
            onConnected = onPlanningCenterConnected,
            onDisconnect = onPlanningCenterDisconnect
        )
    }
}

/**
 * The panel header: title, item count, the Compact/Normal/Detailed density toggle, and the
 * toolbar (file ops, undo/redo, add-section, import, remove/clear) grouped into pill containers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleHeader(
    itemCount: Int,
    density: ScheduleDensity,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onNewSchedule: () -> Unit,
    onOpenSchedule: () -> Unit,
    onSaveSchedule: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAddLabel: () -> Unit,
    onImportPlanningCenter: () -> Unit,
    onRemoveSelected: () -> Unit,
    onClearSchedule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.schedule),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(Res.string.schedule_item_count, itemCount),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PillGroup {
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_remove),
                    text = stringResource(Res.string.tooltip_schedule_zoom_out),
                    onClick = onZoomOut,
                    enabled = canZoomOut,
                    buttonSize = 24.dp,
                    iconSize = 13.dp,
                    iconTint = if (canZoomOut) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                Text(
                    text = when (density) {
                        ScheduleDensity.COMPACT -> stringResource(Res.string.schedule_density_compact)
                        ScheduleDensity.NORMAL -> stringResource(Res.string.schedule_density_normal)
                        ScheduleDensity.DETAILED -> stringResource(Res.string.schedule_density_detailed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_add),
                    text = stringResource(Res.string.tooltip_schedule_zoom_in),
                    onClick = onZoomIn,
                    enabled = canZoomIn,
                    buttonSize = 24.dp,
                    iconSize = 13.dp,
                    iconTint = if (canZoomIn) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            PillGroup {
                // New Schedule clears the whole list — the same weight as Clear Schedule at the
                // tail of this toolbar, so it stays a plain icon button rather than the kind of
                // prominent "+" button a genuinely additive action would get.
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_add),
                    text = stringResource(Res.string.tooltip_new_schedule),
                    onClick = onNewSchedule,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_folder),
                    text = stringResource(Res.string.tooltip_open_schedule),
                    onClick = onOpenSchedule,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_save),
                    text = stringResource(Res.string.tooltip_save_schedule),
                    onClick = onSaveSchedule,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillDivider()
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_undo),
                    text = stringResource(Res.string.tooltip_undo),
                    onClick = onUndo,
                    enabled = canUndo,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = if (canUndo) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_redo),
                    text = stringResource(Res.string.tooltip_redo),
                    onClick = onRedo,
                    enabled = canRedo,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = if (canRedo) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                PillDivider()
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_label),
                    text = stringResource(Res.string.tooltip_add_label),
                    onClick = onAddLabel,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TooltipIconButton(
                    painter = rememberVectorPainter(Icons.Default.CloudDownload),
                    text = stringResource(Res.string.planning_center_import_title),
                    onClick = onImportPlanningCenter,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillDivider()
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_close),
                    text = stringResource(Res.string.tooltip_remove_from_schedule),
                    onClick = onRemoveSelected,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_delete),
                    text = stringResource(Res.string.tooltip_clear_schedule),
                    onClick = onClearSchedule,
                    buttonSize = 26.dp,
                    iconSize = 14.dp,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** A rounded, bordered pill housing a row of compact icon buttons — the toolbar's visual grouping. */
@Composable
private fun PillGroup(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) { content() }
}

@Composable
private fun PillDivider() {
    VerticalDivider(
        modifier = Modifier.height(14.dp).padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
}

/**
 * Same shape as [TooltipIconButton], but anchors its tooltip well to the left of the button
 * instead of above or below it. Row-action buttons sit mid-row, only ~3dp from the next card,
 * with several sibling buttons packed 1dp apart on both sides — the app-wide default
 * (below-center) spills onto the card underneath, an above-center placement spills onto the row
 * above's own buttons, and even a small leftward nudge lands on the very next sibling button
 * (each is only ~27-30dp wide). A big enough leftward offset clears the whole button cluster
 * (five or six buttons, under 180dp total) regardless of which one triggered it, landing on the
 * row's own (non-interactive) title text instead — the only placement that can't cover a control
 * in this row or any other. A local copy avoids widening [TooltipIconButton]'s own signature with
 * an experimental-API parameter, which would force every one of its call sites across the app to
 * opt in for no benefit to them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScheduleRowActionButton(
    painter: Painter,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 27.dp,
    iconSize: Dp = 13.dp,
    iconTint: Color? = null,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
    ConditionalTooltipArea(
        // `anchor` is the point on the BUTTON to align to; `alignment` is which point ON THE
        // TOOLTIP touches it. Leaving `alignment` at its default (BottomCenter) — as an earlier
        // version of this did — puts the tooltip's bottom-center on the button's left-center
        // point, so it still extends upward and sideways back over the row instead of clearing
        // it. `alignment = CenterStart` puts the tooltip's own left edge there instead, so the
        // whole tooltip extends purely leftward from a small gap off the button, staying level
        // with this row (never another row's controls) the entire time. (`CenterEnd` looks like
        // the intuitive choice but is backwards — per Compose's ComponentRect math it anchors the
        // tooltip's LEFT edge to the point, extending the tooltip rightward on top of the button.)
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.CenterStart,
            alignment = Alignment.CenterStart,
            offset = DpOffset((-8).dp, 0.dp)
        ),
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = MaterialTheme.shapes.extraSmall,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        IconButton(onClick = onClick, modifier = modifier.size(buttonSize), colors = colors) {
            Image(
                painter = painter,
                contentDescription = text,
                modifier = Modifier.size(iconSize),
                colorFilter = iconTint?.let { ColorFilter.tint(it) }
            )
        }
    }
}

/** The dashed drop-target-styled "Add Files…" button at the foot of the panel. */
@Composable
private fun ScheduleAddFilesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                       else MaterialTheme.colorScheme.outlineVariant
    val contentColor = if (hovered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
             else MaterialTheme.colorScheme.surfaceContainerHigh
    val strokeWidthPx = with(LocalDensity.current) { 1.dp.toPx() }
    val cornerRadiusPx = with(LocalDensity.current) { 8.dp.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .hoverable(interactionSource)
            .clip(shape)
            .background(bg, shape)
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = strokeWidthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))),
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_add),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = stringResource(Res.string.schedule_add_files),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Adds each dropped file to the schedule as whatever [classifyDroppedFile] says it is.
 *
 * `internal` rather than private so the drop handling can be tested directly: the drop itself
 * arrives through an AWT `DropTarget` on the real window, which a headless test cannot deliver.
 */
internal fun handleDroppedFiles(files: List<File>, viewModel: ScheduleViewModel) {
    for (file in files) {
        if (file.isDirectory) {
            // Folder dropped — count image files inside and add as pictures
            val imageCount = file.listFiles()?.count { child ->
                child.isFile && child.extension.lowercase() in IMAGE_EXTENSIONS
            } ?: 0
            if (imageCount > 0) {
                viewModel.addPicture(file.absolutePath, file.name, imageCount)
            }
            continue
        }

        val ext = file.extension.lowercase()
        when (classifyDroppedFile(ext)) {
            DroppedFileAction.PRESENTATION ->
                viewModel.addPresentation(file.absolutePath, file.nameWithoutExtension, 0, ext)
            DroppedFileAction.MEDIA ->
                viewModel.addMedia(file.absolutePath, file.nameWithoutExtension, "local")
            DroppedFileAction.PICTURE -> {
                // Single image dropped — add parent folder as picture source
                val parentFolder = file.parentFile
                val imageCount = parentFolder?.listFiles()?.count { child ->
                    child.isFile && child.extension.lowercase() in IMAGE_EXTENSIONS
                } ?: 1
                viewModel.addPicture(
                    parentFolder?.absolutePath ?: file.absolutePath,
                    parentFolder?.name ?: file.name,
                    imageCount
                )
            }
            DroppedFileAction.LOWER_THIRD ->
                viewModel.addLowerThird(file.nameWithoutExtension, file.nameWithoutExtension, false, 0L)
            DroppedFileAction.NONE -> {}
        }
    }
}

/** Vertical padding inside a card at each density rung. */
private fun ScheduleDensity.rowPadding(): Dp = when (this) {
    ScheduleDensity.COMPACT -> 4.dp
    ScheduleDensity.NORMAL -> 7.dp
    ScheduleDensity.DETAILED -> 9.dp
}

/** Minimum card height at each density rung. */
private fun ScheduleDensity.rowMinHeight(): Dp = when (this) {
    ScheduleDensity.COMPACT -> 32.dp
    ScheduleDensity.NORMAL -> 42.dp
    ScheduleDensity.DETAILED -> 54.dp
}

/**
 * One row of a small rotating theme palette for a type-icon chip's background/foreground — not a
 * distinct hue per content type (that would mean hardcoded colors), but enough variety that
 * adjacent kinds in a service read as visually different.
 */
@Composable
private fun scheduleChipColors(paletteIndex: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (paletteIndex % 4) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        2 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.errorContainer to scheme.onErrorContainer
    }
}

@Composable
private fun ScheduleItemRow(
    item: ScheduleItem,
    /** Applied to the grip dots, which double as the drag-to-reorder handle. */
    dragHandleModifier: Modifier = Modifier,
    density: ScheduleDensity,
    isSelected: Boolean,
    note: String,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onPresent: () -> Unit,
    onEditLabel: () -> Unit = {},
    onNoteChanged: (String) -> Unit = {}
) {
    val interactionSource = remember(item.id) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val actionsAlpha by animateFloatAsState(if (hovered) 1f else 0f, label = "scheduleRowActionsAlpha")

    var noteExpanded by remember(item.id) { mutableStateOf(false) }
    var noteText by remember(item.id) { mutableStateOf(note) }

    // Sync local noteText when external note changes (e.g. after undo/redo)
    LaunchedEffect(note) {
        if (noteText != note) noteText = note
    }

    val isSection = item is ScheduleItem.LabelItem
    val sectionAccent = if (item is ScheduleItem.LabelItem) Utils.parseHexColor(item.textColor) else Color.Unspecified

    val cardBg = when {
        isSection -> Utils.parseHexColor(item.backgroundColor)
        isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    // A label's text/background are both user-chosen (Add Label dialog). WCAG's contrast ratio
    // is luminance-only, so two colors close in hue (navy text on a mid-blue background, say)
    // can clear the AA 4.5:1 minimum for normal text on paper while still reading as low-contrast
    // to the eye — going to the AAA threshold (7:1) catches those near-miss same-hue pairs too.
    // Only the rendered title falls back to white/black; the accent bar and border stay the
    // user's actual color, and the stored colors are untouched either way.
    val sectionText = if (isSection) Utils.ensureContrast(sectionAccent, cardBg, minRatio = 7.0) else sectionAccent
    val cardBorder = when {
        isSection -> sectionAccent.copy(alpha = 0.35f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val leftAccent = when {
        isSection -> sectionAccent
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clip(CARD_SHAPE)
            .background(cardBg, CARD_SHAPE)
            .border(1.dp, cardBorder, CARD_SHAPE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 3.dp,
                    end = 6.dp,
                    top = density.rowPadding(),
                    bottom = density.rowPadding()
                )
                .heightIn(min = density.rowMinHeight()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar (decorative) + grip dots + type icon — the grip and icon together
            // form the drag-to-reorder handle, matching their combined touch target before this
            // redesign rather than shrinking it down to the 4dp-wide grip glyph alone.
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(leftAccent, RoundedCornerShape(2.dp))
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .then(dragHandleModifier)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_drag_dots),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                    )
                    if (!isSection) {
                        val (chipBg, chipFg) = scheduleChipColors(scheduleItemPaletteIndex(item))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(chipBg, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scheduleItemGlyph(item),
                                style = MaterialTheme.typography.bodyMedium,
                                color = chipFg
                            )
                        }
                    }
                }
            }

            // Content takes the row's full remaining width — the action buttons below overlay on
            // top of it (only while hovered) rather than reserving their own permanent column, so
            // text is never squeezed to make room for controls that are usually hidden.
            Box(modifier = Modifier.weight(1f)) {
                // Selecting/presenting lives here (Initial-pass: survives the LazyColumn
                // scroll-gesture-eats-clicks issue on ARM Mac). The overlaid action row below is a
                // separate sibling in this Box (painted after, so it sits on top) — its own
                // buttons keep their normal click handling untouched.
                //
                // fillMaxSize, not fillMaxWidth: the Box's height is whichever sibling is
                // taller, which can be the action-button row rather than this Column's own
                // wrapped text (a single Compact-density title line is shorter than the row of
                // 27-30dp buttons). fillMaxWidth alone left this Column's clickable bounds at
                // just its text's own height, so clicking the card below the text but still
                // inside the row's visible bounds hit nothing.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Selection applies to section rows too — otherwise a label can never
                        // become `selectedItemId`, and the toolbar's "Remove from Schedule"
                        // button (which acts on the current selection) can't touch it. Only
                        // double-click-to-present stays disabled for sections: there's nothing
                        // to go live with.
                        .initialPassCombinedClickable(
                            onClick = { onSelect() },
                            onDoubleClick = if (!isSection) { { onPresent() } } else null
                        )
                ) {
                    if (isSection) {
                        Text(
                            text = item.displayText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = sectionText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        ScheduleItemContent(item = item, density = density, isSelected = isSelected)
                    }

                    // Note preview when collapsed
                    if (note.isNotEmpty() && !noteExpanded) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Hover-revealed action buttons — always present (so they stay reachable by
                // keyboard and testable), only their opacity tracks hover. A scrim fading from
                // transparent to the card's own background sits behind them so they stay legible
                // over whatever text they're overlapping. Also carries its own Final-pass
                // select/present click: a nested button's Main-pass click wins first, and only a
                // click that lands here without hitting a button falls through to select the card.
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .alpha(actionsAlpha)
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.35f to cardBg.copy(alpha = 0.82f),
                                1f to cardBg.copy(alpha = 0.82f)
                            )
                        )
                        .padding(start = 20.dp)
                        .finalPassCombinedClickable(
                            onClick = { onSelect() },
                            onDoubleClick = if (!isSection) { { onPresent() } } else null
                        ),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScheduleRowActionButton(
                        painter = painterResource(Res.drawable.ic_arrow_up),
                        text = stringResource(Res.string.tooltip_move_up),
                        onClick = onMoveUp,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ScheduleRowActionButton(
                        painter = painterResource(Res.drawable.ic_arrow_down),
                        text = stringResource(Res.string.tooltip_move_down),
                        onClick = onMoveDown,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ScheduleRowActionButton(
                        painter = painterResource(Res.drawable.ic_note),
                        text = stringResource(Res.string.tooltip_note),
                        onClick = { noteExpanded = !noteExpanded },
                        iconTint = if (note.isNotEmpty() || noteExpanded) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ScheduleRowActionButton(
                        painter = painterResource(Res.drawable.ic_close),
                        text = stringResource(Res.string.tooltip_remove),
                        onClick = onRemove,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                    // Slightly larger than the other four, matching the reference design — but
                    // NOT permanently filled with color: the reference only does that for
                    // whichever item is currently live, a concept this app doesn't track per
                    // schedule item, so every row would otherwise show a "live-looking" button
                    // that never actually means anything.
                    if (isSection) {
                        ScheduleRowActionButton(
                            painter = painterResource(Res.drawable.ic_edit),
                            text = stringResource(Res.string.tooltip_edit_label),
                            onClick = onEditLabel,
                            modifier = Modifier.padding(start = 2.dp),
                            buttonSize = 30.dp,
                            iconSize = 15.dp,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ScheduleRowActionButton(
                            painter = painterResource(Res.drawable.ic_play),
                            text = stringResource(Res.string.tooltip_go_live),
                            onClick = onPresent,
                            modifier = Modifier.padding(start = 2.dp),
                            buttonSize = 30.dp,
                            iconSize = 15.dp,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Saved note preview, or the inline editor — either way it always gets its own row so
        // opening/closing it never fights the hover-alpha of the action buttons above.
        if (note.isNotEmpty() && !noteExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp, end = 8.dp, bottom = 7.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f).padding(top = 2.dp, bottom = 2.dp)
                )
                ScheduleRowActionButton(
                    painter = painterResource(Res.drawable.ic_edit),
                    text = stringResource(Res.string.tooltip_note),
                    onClick = { noteExpanded = true },
                    iconSize = 11.dp,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        AnimatedVisibility(visible = noteExpanded) {
            val noteInteractionSource = remember { MutableInteractionSource() }
            val noteFieldFocused by noteInteractionSource.collectIsFocusedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp, end = 8.dp, bottom = 7.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(7.dp))
                    .border(
                        width = 1.dp,
                        color = if (noteFieldFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(7.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 3,
                    interactionSource = noteInteractionSource,
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteText.isEmpty()) {
                                Text(
                                    stringResource(Res.string.schedule_note_placeholder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_check),
                    text = stringResource(Res.string.tooltip_note_done),
                    onClick = {
                        onNoteChanged(noteText)
                        noteExpanded = false
                    },
                    buttonSize = 32.dp,
                    iconSize = 15.dp,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_close),
                    text = stringResource(Res.string.tooltip_note_clear),
                    onClick = {
                        noteText = ""
                        onNoteChanged("")
                    },
                    modifier = Modifier.padding(end = 4.dp),
                    buttonSize = 32.dp,
                    iconSize = 15.dp,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** The title line and, at Normal/Detailed density, the per-type detail line(s) below it. */
@Composable
private fun ScheduleItemContent(item: ScheduleItem, density: ScheduleDensity, isSelected: Boolean) {
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val detailColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (item is ScheduleItem.SongItem && item.songNumber > 0) {
            Text(
                text = item.songNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            // A song's own displayText prepends its number ("42 - Amazing Grace") for contexts
            // without room for the two as separate elements; here the number already has its own
            // Text above, so the title alone avoids showing it twice.
            text = if (item is ScheduleItem.SongItem) item.title else item.displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }

    if (!scheduleShowDetailLine(density.percent)) return

    when (item) {
        is ScheduleItem.SongItem -> if (item.songbook.isNotBlank()) {
            Text(
                text = item.songbook,
                style = MaterialTheme.typography.bodySmall,
                color = detailColor,
                maxLines = 1,
                // Songbook names are often long paths/editions whose distinguishing part is at the
                // END, so clip the front instead of the tail
                overflow = TextOverflow.StartEllipsis
            )
        }
        is ScheduleItem.BibleVerseItem -> Text(
            text = scheduleItemDetailText(item).orEmpty(),
            style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        is ScheduleItem.PictureItem -> Text(
            text = scheduleItemDetailText(item).orEmpty(),
            style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        is ScheduleItem.PresentationItem -> if (!scheduleShowKindDetails(density.percent)) {
            Text(
                text = scheduleItemDetailText(item).orEmpty(),
                style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        is ScheduleItem.MediaItem -> if (!scheduleShowKindDetails(density.percent)) {
            Text(
                text = scheduleItemDetailText(item).orEmpty(),
                style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        is ScheduleItem.LowerThirdItem -> if (item.pauseAtFrame) {
            Text(
                text = stringResource(Res.string.pause_duration_ms, item.pauseDurationMs),
                style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1
            )
        }
        is ScheduleItem.AnnouncementItem -> {
            val timerSubtext = announcementTimerSubtext(item)
            if (item.isTimer && timerSubtext != null) {
                Text(
                    text = timerSubtext,
                    style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1
                )
            }
        }
        is ScheduleItem.WebsiteItem -> Text(
            text = item.url,
            style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        is ScheduleItem.DictionaryItem -> Text(
            text = item.transliteration,
            style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        is ScheduleItem.LabelItem, is ScheduleItem.SceneItem -> { /* no secondary text */ }
    }

    // Detailed density adds the uppercase type chip (and, for file-backed items, the path)
    if (scheduleShowKindDetails(density.percent)) {
        val path = when (item) {
            is ScheduleItem.PresentationItem -> item.filePath
            is ScheduleItem.MediaItem -> item.mediaUrl
            else -> null
        }
        Row(
            modifier = Modifier.padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (chipBg, chipFg) = scheduleChipColors(scheduleItemPaletteIndex(item))
            Box(
                modifier = Modifier.background(chipBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stringResource(scheduleItemKindLabel(item)).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = chipFg
                )
            }
            if (path != null) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall,
                    color = detailColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
