package org.churchpresenter.app.churchpresenter.utils

/**
 * The schedule card density ladder: three fixed rungs (Compact / Normal / Detailed) rather than a
 * continuous zoom. The persisted setting stays a plain `Int` (`AppSettings.scheduleItemZoomPercent`,
 * pre-dating this ladder) so a value saved under the old 11-rung 70-150 scheme still resolves to the
 * nearest rung here. Extracted from ScheduleTab so the rung math is tested without the Compose
 * controls.
 */

internal enum class ScheduleDensity(val percent: Int) {
    COMPACT(70),
    NORMAL(100),
    DETAILED(150),
}

/** The rung nearest [percent], so a value persisted before or after this ladder still resolves. */
internal fun scheduleDensityFor(percent: Int): ScheduleDensity = when {
    percent <= 90 -> ScheduleDensity.COMPACT
    percent < 120 -> ScheduleDensity.NORMAL
    else -> ScheduleDensity.DETAILED
}

/** The next rung up from [percent] (clamped at Detailed). */
internal fun scheduleZoomIn(percent: Int): Int {
    val density = scheduleDensityFor(percent)
    val next = ScheduleDensity.entries.getOrElse(density.ordinal + 1) { density }
    return next.percent
}

/** The next rung down from [percent] (clamped at Compact). */
internal fun scheduleZoomOut(percent: Int): Int {
    val density = scheduleDensityFor(percent)
    val prev = ScheduleDensity.entries.getOrElse(density.ordinal - 1) { density }
    return prev.percent
}

/** Whether there is a higher rung to move into. */
internal fun scheduleCanZoomIn(percent: Int): Boolean =
    scheduleDensityFor(percent) != ScheduleDensity.DETAILED

/** Whether there is a lower rung to move out to. */
internal fun scheduleCanZoomOut(percent: Int): Boolean =
    scheduleDensityFor(percent) != ScheduleDensity.COMPACT

/** Normal and Detailed show the row's secondary (grey) detail line; Compact shows title only. */
internal fun scheduleShowDetailLine(percent: Int): Boolean =
    scheduleDensityFor(percent) != ScheduleDensity.COMPACT

/** Only Detailed shows the type-kind chip and (when present) the file path. */
internal fun scheduleShowKindDetails(percent: Int): Boolean =
    scheduleDensityFor(percent) == ScheduleDensity.DETAILED
