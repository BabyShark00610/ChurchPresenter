package org.churchpresenter.app.churchpresenter.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The schedule card density ladder: Compact / Normal / Detailed. Pins the rung resolution (so a
 * value saved under the old continuous zoom still lands on a sensible rung), the clamped step, and
 * which rungs show the detail line vs the kind chip.
 */
class ScheduleZoomTest {

    // ── The enum itself ────────────────────────────────────────────────────────

    @Test fun `the ladder is Compact, Normal, Detailed in that order, at 70, 100, 150 percent`() {
        assertEquals(
            listOf(ScheduleDensity.COMPACT, ScheduleDensity.NORMAL, ScheduleDensity.DETAILED),
            ScheduleDensity.entries,
            "scheduleZoomIn/Out walk the ladder by ordinal, so the declared order IS the step order",
        )
        assertEquals(70, ScheduleDensity.COMPACT.percent)
        assertEquals(100, ScheduleDensity.NORMAL.percent)
        assertEquals(150, ScheduleDensity.DETAILED.percent)
    }

    // ── scheduleDensityFor ────────────────────────────────────────────────────

    @Test fun `a legacy low percent resolves to Compact`() {
        assertEquals(ScheduleDensity.COMPACT, scheduleDensityFor(70))
        assertEquals(ScheduleDensity.COMPACT, scheduleDensityFor(90))
    }

    @Test fun `a legacy mid percent resolves to Normal`() {
        assertEquals(ScheduleDensity.NORMAL, scheduleDensityFor(100))
        assertEquals(ScheduleDensity.NORMAL, scheduleDensityFor(91))
        assertEquals(ScheduleDensity.NORMAL, scheduleDensityFor(119))
    }

    @Test fun `a legacy high percent resolves to Detailed`() {
        assertEquals(ScheduleDensity.DETAILED, scheduleDensityFor(120))
        assertEquals(ScheduleDensity.DETAILED, scheduleDensityFor(150))
        assertEquals(ScheduleDensity.DETAILED, scheduleDensityFor(500))
    }

    @Test fun `percents at or below zero still resolve to Compact rather than throwing`() {
        assertEquals(ScheduleDensity.COMPACT, scheduleDensityFor(0))
        assertEquals(ScheduleDensity.COMPACT, scheduleDensityFor(-100))
    }

    // ── scheduleZoomIn / scheduleZoomOut ─────────────────────────────────────

    @Test fun `zooming in steps to the next rung up, from every starting rung`() {
        assertEquals(100, scheduleZoomIn(70), "Compact -> Normal")
        assertEquals(150, scheduleZoomIn(100), "Normal -> Detailed")
        assertEquals(150, scheduleZoomIn(150), "Detailed is the top rung, so it stays put")
    }

    @Test fun `zooming out steps to the next rung down, from every starting rung`() {
        assertEquals(70, scheduleZoomOut(70), "Compact is the bottom rung, so it stays put")
        assertEquals(70, scheduleZoomOut(100), "Normal -> Compact")
        assertEquals(100, scheduleZoomOut(150), "Detailed -> Normal")
    }

    @Test fun `zooming steps by rung, not by the raw legacy percent`() {
        // 80 and 140 are legacy values that resolve to Compact and Detailed respectively, but
        // neither is one of the ladder's own three percents — the step must still land exactly on
        // the neighbouring rung's percent, not drift from the raw input.
        assertEquals(100, scheduleZoomIn(80), "a legacy Compact-range percent still steps to Normal's 100")
        assertEquals(100, scheduleZoomOut(140), "a legacy Detailed-range percent still steps to Normal's 100")
    }

    @Test fun `zooming in then out from the middle rung returns to where it started`() {
        assertEquals(100, scheduleZoomOut(scheduleZoomIn(100)))
        assertEquals(100, scheduleZoomIn(scheduleZoomOut(100)))
    }

    // ── scheduleCanZoomIn / scheduleCanZoomOut ───────────────────────────────

    @Test fun `can-zoom flags respect the ladder ends`() {
        assertFalse(scheduleCanZoomOut(70), "already at Compact")
        assertTrue(scheduleCanZoomIn(70))
        assertFalse(scheduleCanZoomIn(150), "already at Detailed")
        assertTrue(scheduleCanZoomOut(150))
    }

    @Test fun `both can-zoom flags are true in the middle rung`() {
        assertTrue(scheduleCanZoomIn(100), "Normal can still step up to Detailed")
        assertTrue(scheduleCanZoomOut(100), "Normal can still step down to Compact")
    }

    // ── Display flags ─────────────────────────────────────────────────────────

    @Test fun `Compact hides the detail line, Normal and Detailed show it`() {
        assertFalse(scheduleShowDetailLine(70))
        assertTrue(scheduleShowDetailLine(100))
        assertTrue(scheduleShowDetailLine(150))
    }

    @Test fun `only Detailed shows the kind chip`() {
        assertFalse(scheduleShowKindDetails(70))
        assertFalse(scheduleShowKindDetails(100))
        assertTrue(scheduleShowKindDetails(150))
    }
}
