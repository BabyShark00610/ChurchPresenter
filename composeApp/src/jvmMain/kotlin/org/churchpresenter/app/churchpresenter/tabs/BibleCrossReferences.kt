package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.add_to_schedule
import churchpresenter.composeapp.generated.resources.bible_cross_references_close
import churchpresenter.composeapp.generated.resources.bible_cross_references_dismiss_hint
import churchpresenter.composeapp.generated.resources.bible_cross_references_keep_open
import churchpresenter.composeapp.generated.resources.bible_cross_references_none
import churchpresenter.composeapp.generated.resources.bible_cross_references_often_next
import churchpresenter.composeapp.generated.resources.bible_cross_references_passage
import churchpresenter.composeapp.generated.resources.bible_cross_references_source_count
import churchpresenter.composeapp.generated.resources.bible_cross_references_title
import churchpresenter.composeapp.generated.resources.book
import churchpresenter.composeapp.generated.resources.chapter
import churchpresenter.composeapp.generated.resources.close
import churchpresenter.composeapp.generated.resources.ic_close
import churchpresenter.composeapp.generated.resources.ic_link
import churchpresenter.composeapp.generated.resources.ic_playlist_add
import churchpresenter.composeapp.generated.resources.mode
import churchpresenter.composeapp.generated.resources.verse
import kotlinx.coroutines.flow.first
import org.churchpresenter.app.churchpresenter.composables.initialPassClickable
import org.churchpresenter.app.churchpresenter.composables.initialPassCombinedClickable
import org.churchpresenter.app.churchpresenter.data.formatCrossRefLabel
import org.churchpresenter.app.churchpresenter.viewmodel.BibleViewModel
import org.churchpresenter.app.churchpresenter.ui.theme.semantic
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The cross references beside a verse: the link chip that counts them, the popover it opens, and
 * the panel that popover docks into.
 *
 * Split out of `BibleTab.kt`, which owns the state these render. Nothing here resolves a
 * reference — rows arrive already translated into the loaded module's own naming and numbering.
 */

/** The floating popover a verse's link chip opens. Wide enough for a verse to read as prose. */
private val CROSS_REF_POPOVER_WIDTH = 380.dp

/** Past this the popover would cover the whole verse list rather than sit beside it. */
private val CROSS_REF_POPOVER_MAX_HEIGHT = 420.dp

/** One row of the cross-reference column, resolved against the loaded module. */
internal data class CrossRefRow(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val endVerse: Int?,
    /** True for "often next" — drawn from the operator's own go-lives rather than from TSK. */
    val learned: Boolean,
    /** The reference as this module writes it, e.g. "Rom 5:8". */
    val label: String,
    /** The start of the verse, or empty when the module does not have it. */
    val preview: String,
    /** False when the module has no such verse: the row is shown, but greyed and inert. */
    val available: Boolean,
    /** In passage mode, how many of the read verses point here. 0 for a single-verse row. */
    val count: Int = 0,
)

/**
 * Builds one row, translating the canonical reference into the loaded module's own words.
 *
 * A module that lacks the book still gets a row — labelled from the app's own abbreviations and
 * marked unavailable — because silently dropping it would leave the operator wondering why a
 * passage they know is cross-referenced shows nothing.
 */
internal fun crossRefRow(
    viewModel: BibleViewModel,
    fallbackAbbreviations: List<String>,
    bookId: Int,
    chapter: Int,
    verse: Int,
    endVerse: Int?,
    learned: Boolean,
    count: Int = 0,
): CrossRefRow {
    val moduleRef = viewModel.moduleRefFor(bookId, chapter, verse)
    val abbreviation = moduleRef?.abbreviation
        ?: fallbackAbbreviations.getOrNull(bookId - 1).orEmpty()
    return CrossRefRow(
        bookId = bookId,
        chapter = chapter,
        verse = verse,
        endVerse = endVerse,
        learned = learned,
        // The module's own numbering where it has an opinion: a Synodal psalm is labelled with the
        // number its operator will find in it, not the KJV number the dataset stores.
        label = formatCrossRefLabel(
            abbreviation,
            moduleRef?.chapter ?: chapter,
            moduleRef?.verse ?: verse,
            endVerse,
        ),
        preview = moduleRef?.text.orEmpty(),
        available = moduleRef != null,
        count = count,
    )
}

/**
 * One reference, as both the docked panel and the popover draw it.
 *
 * A card rather than a line: the reference heads it, its verse follows underneath **in full**, and
 * queueing it sits on the reference line where it can be hit without first navigating there.
 * Truncating the verse to one line, which is what the column did before, meant the panel could tell
 * you a reference existed but never what it said, so every candidate had to be opened to be judged.
 *
 * Going live is deliberately **not** a button here — it stays on the double-click, as it is
 * everywhere else in this tab. A one-tap "on screen now" sitting next to eight other references, at
 * 22dp, in a list that moves as the reading does, is the wrong thing to be able to hit by accident
 * during a service.
 *
 * The clickable is on the text column rather than the whole card, and the button is its sibling:
 * [initialPassCombinedClickable] handles the Initial pass, which is delivered parent-first, so a
 * button nested *inside* it never sees the click at all.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrossReferenceCard(
    row: CrossRefRow,
    selected: Boolean,
    striped: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onAddToSchedule: () -> Unit,
    /** Shown for the operator's own habits, hidden for the bundled dataset. */
    showLearnedDot: Boolean = row.learned,
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.surfaceVariant
        striped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    val markerColor = MaterialTheme.semantic.marker
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .background(background, RoundedCornerShape(9.dp))
            .drawBehind {
                if (selected) drawRect(color = markerColor, size = Size(4f, size.height))
            },
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                // An unavailable row is inert: clicking it could only fail, and a row that responds
                // to nothing reads as a broken app rather than as a reference this translation
                // happens not to carry.
                .then(
                    if (row.available) Modifier.initialPassCombinedClickable(
                        onClick = onClick,
                        onDoubleClick = onDoubleClick,
                    ) else Modifier
                )
                .padding(start = 9.dp, top = 7.dp, bottom = 7.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showLearnedDot) {
                    Box(
                        modifier = Modifier.size(4.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                }
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (row.available) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.count > 0) {
                    Text(
                        text = stringResource(Res.string.bible_cross_references_source_count, row.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (row.preview.isNotEmpty()) {
                Text(
                    text = row.preview,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 12.sp * 1.55f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (row.available) {
            // Named with the reference, not just the action: a panel of these is a column of
            // otherwise identical buttons, and which one is which is the whole point.
            val addStr = stringResource(Res.string.add_to_schedule)
            Box(modifier = Modifier.padding(top = 5.dp, end = 5.dp)) {
                CrossRefActionButton(
                    painter = painterResource(Res.drawable.ic_playlist_add),
                    tooltipText = addStr,
                    contentDescription = "$addStr ${row.label}",
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = onAddToSchedule,
                )
            }
        }
    }
}

/** The small, quiet icon button on a cross-reference card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrossRefActionButton(
    tooltipText: String,
    tint: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    painter: Painter? = null,
    /** Defaults to the tooltip; pass a longer one where several of these sit in a list. */
    contentDescription: String = tooltipText,
) {
    TooltipArea(
        tooltip = {
            Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    tooltipText,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(anchor = Alignment.BottomCenter, offset = DpOffset(0.dp, 4.dp)),
    ) {
        Box(
            modifier = Modifier.size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                // The initial pass, so the card's own click handler underneath does not also fire
                // and navigate away from the reference that was just queued.
                .initialPassClickable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(13.dp), tint = tint)
            } else if (painter != null) {
                Icon(painter, contentDescription = contentDescription, modifier = Modifier.size(13.dp), tint = tint)
            }
        }
    }
}

/**
 * The docked column of references beside the verse list.
 *
 * Two kinds of suggestion share one scrolling list rather than two panels: what the passage points
 * at (TSK) and what this operator usually shows next (their own go-lives). They are separated by a
 * label and distinguished by a leading dot, so it still reads as one list — during a service the eye
 * should find the reference, not navigate a layout.
 *
 * Each row carries a reference and its verse, both already resolved against the loaded module — so
 * they read in the module's language and its own numbering, and this composable renders rather than
 * resolves.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CrossReferencePanel(
    rows: List<CrossRefRow>,
    selectedIndex: Int,
    onClick: (Int) -> Unit,
    onDoubleClick: (Int) -> Unit,
    onAddToSchedule: (Int) -> Unit,
    onClose: () -> Unit,
    /** The span of the passage being read, e.g. "1:1-10", or null when describing one verse. */
    passageSpan: String?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val firstLearned = rows.indexOfFirst { it.learned }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        CrossReferenceHeader(
            // Naming the span makes it unambiguous which verses produced the list, which matters
            // precisely because the list changes shape as a reading goes on.
            title = passageSpan?.let { stringResource(Res.string.bible_cross_references_passage, it) }
                ?: stringResource(Res.string.bible_cross_references_title),
            onClose = onClose,
            closeTooltip = stringResource(Res.string.bible_cross_references_close),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (rows.isEmpty()) {
            CrossReferenceEmptyState(modifier = Modifier.fillMaxSize())
            return@Column
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(top = 4.dp, bottom = 4.dp, end = 8.dp),
            ) {
                itemsIndexed(rows) { idx, row ->
                    if (idx == firstLearned) {
                        Text(
                            text = stringResource(Res.string.bible_cross_references_often_next),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        )
                    }
                    CrossReferenceCard(
                        row = row,
                        selected = idx == selectedIndex,
                        striped = idx % 2 == 1,
                        onClick = { onClick(idx) },
                        onDoubleClick = { onDoubleClick(idx) },
                        onAddToSchedule = { onAddToSchedule(idx) },
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState),
            )
        }
    }
}

/** The chain-link title bar both the docked panel and the popover wear, so they read as one thing. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CrossReferenceHeader(
    title: String,
    onClose: () -> Unit,
    closeTooltip: String,
    onDock: (() -> Unit)? = null,
    dockTooltip: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_link),
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onDock != null) {
            CrossRefActionButton(
                painter = painterResource(Res.drawable.ic_link),
                tooltipText = dockTooltip,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onDock,
            )
        }
        CrossRefActionButton(
            painter = painterResource(Res.drawable.ic_close),
            tooltipText = closeTooltip,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onClose,
        )
    }
}

/** Says the verse has no references, rather than leaving the panel looking like it failed to load. */
@Composable
private fun CrossReferenceEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_link),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
        Text(
            text = stringResource(Res.string.bible_cross_references_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The floating list a verse's link chip opens, anchored to that chip.
 *
 * It overlays the verse list rather than taking a column of it: looking up what a verse points at is
 * a question asked in passing, and answering it should not reflow the passage being read. Keep-open
 * promotes it to the docked panel for an operator who wants it there for the rest of the service.
 */
@Composable
internal fun CrossReferencePopover(
    title: String,
    rows: List<CrossRefRow>,
    onDismiss: () -> Unit,
    onDock: () -> Unit,
    onOpen: (CrossRefRow) -> Unit,
    onGoLive: (CrossRefRow) -> Unit,
    onAddToSchedule: (CrossRefRow) -> Unit,
) {
    Popup(
        popupPositionProvider = remember { CrossRefPopoverPosition },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            // The popover is exactly as tall as it needs to be, and two ordinary ways of building
            // a scrolling list stop that: a `weight` makes its Column consume the whole height
            // offered to it whatever `fill` says, and a `VerticalScrollbar` sized to its parent
            // does the same from the inside. So the list is a plain scrolling Column with a max
            // height and no scrollbar — a verse has at most sixteen references, and holding a
            // popover open at full height for three of them is worse than losing the scrollbar.
            Column(modifier = Modifier.width(CROSS_REF_POPOVER_WIDTH)) {
                CrossReferenceHeader(
                    title = title,
                    onClose = onDismiss,
                    closeTooltip = stringResource(Res.string.close),
                    onDock = onDock,
                    dockTooltip = stringResource(Res.string.bible_cross_references_keep_open),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (rows.isEmpty()) {
                    CrossReferenceEmptyState(modifier = Modifier.fillMaxWidth().height(110.dp))
                } else {
                    // A plain scrolling Column, not a LazyColumn: a lazy list fills whatever
                    // height it is offered, which would hold the popover open at its maximum
                    // however few references it has. TSK gives a verse at most sixteen, so there
                    // is nothing to virtualise anyway. The scrollbar uses `matchParentSize` for
                    // the same reason — it must not be what decides the height.
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .heightIn(max = CROSS_REF_POPOVER_MAX_HEIGHT)
                            .verticalScroll(scrollState)
                            .padding(vertical = 4.dp),
                    ) {
                        rows.forEachIndexed { idx, row ->
                            CrossReferenceCard(
                                row = row,
                                selected = false,
                                striped = idx % 2 == 1,
                                onClick = { onOpen(row) },
                                onDoubleClick = { onGoLive(row) },
                                onAddToSchedule = { onAddToSchedule(row) },
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = stringResource(Res.string.bible_cross_references_dismiss_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/**
 * Hangs the popover under its chip, right edges aligned, and flips it above when there is no room
 * below — so a chip near the bottom of the verse list still opens a list that is fully on screen
 * rather than one clipped by the window edge.
 */
internal object CrossRefPopoverPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = 6
        val x = (anchorBounds.right - popupContentSize.width)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below
        else (anchorBounds.top - gap - popupContentSize.height)
            .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

/**
 * The chain-link chip at the end of a verse: how many references it has, and the way into them.
 *
 * It costs the verse a little width and only appears on verses that have something behind it, so an
 * operator can see at a glance which verses in a chapter are worth asking about.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CrossRefChip(
    count: Int,
    active: Boolean,
    tooltipText: String,
    onClick: () -> Unit,
) {
    val accent = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    TooltipArea(
        tooltip = {
            Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    tooltipText,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        tooltipPlacement = TooltipPlacement.ComponentRect(anchor = Alignment.BottomCenter, offset = DpOffset(0.dp, 4.dp)),
    ) {
        Row(
            modifier = Modifier
                .height(19.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    RoundedCornerShape(10.dp),
                )
                .border(
                    1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp),
                )
                // The initial pass, so the verse row's own press handler underneath does not also
                // fire and count this as a plain selection — or, twice in a row, as a go-live.
                .initialPassClickable(onClick)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_link),
                contentDescription = tooltipText,
                modifier = Modifier.size(9.dp),
                tint = accent,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = accent,
                maxLines = 1,
            )
        }
    }
}
