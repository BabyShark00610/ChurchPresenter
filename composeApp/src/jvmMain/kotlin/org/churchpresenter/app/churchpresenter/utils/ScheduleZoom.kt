package org.churchpresenter.app.churchpresenter.utils

import kotlin.math.abs

/**
 * The schedule card zoom ladder. Zoom snaps to fixed rungs (so a value persisted before the ladder
 * existed still resolves to the nearest one), and below full size the card sheds its action buttons
 * and then collapses to a single line before it starts shrinking. Extracted from ScheduleTab so the
 * rung math and the two shed/collapse thresholds are tested without the Compose controls.
 */

internal val ZOOM_LEVELS = listOf(70, 80, 90, 98, 99, 100, 110, 120, 130, 140, 150)
private const val ZOOM_HIDE_ACTIONS_BELOW = 100
private const val ZOOM_SINGLE_LINE_AT_OR_BELOW = 98

/** The rung nearest [percent], so a value persisted before the ladder existed still resolves. */
internal fun nearestZoomIndex(percent: Int): Int =
    ZOOM_LEVELS.indices.minBy { abs(ZOOM_LEVELS[it] - percent) }

/** The next rung up from [percent] (clamped at the top rung). */
internal fun scheduleZoomIn(percent: Int): Int =
    ZOOM_LEVELS[(nearestZoomIndex(percent) + 1).coerceAtMost(ZOOM_LEVELS.lastIndex)]

/** The next rung down from [percent] (clamped at the bottom rung). */
internal fun scheduleZoomOut(percent: Int): Int =
    ZOOM_LEVELS[(nearestZoomIndex(percent) - 1).coerceAtLeast(0)]

/** Whether there is a higher rung to zoom into. */
internal fun scheduleCanZoomIn(percent: Int): Boolean = nearestZoomIndex(percent) < ZOOM_LEVELS.lastIndex

/** Whether there is a lower rung to zoom out to. */
internal fun scheduleCanZoomOut(percent: Int): Boolean = nearestZoomIndex(percent) > 0

/** At or above full size the cards keep their action buttons; below it they shed them. */
internal fun scheduleShowCardActions(percent: Int): Boolean = percent >= ZOOM_HIDE_ACTIONS_BELOW

/** At or below the collapse threshold the cards render a single line only. */
internal fun scheduleSingleLineCards(percent: Int): Boolean = percent <= ZOOM_SINGLE_LINE_AT_OR_BELOW
