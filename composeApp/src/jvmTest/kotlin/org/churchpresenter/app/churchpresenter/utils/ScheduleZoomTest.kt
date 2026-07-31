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

    @Test fun `zooming in steps to the next rung up`() =
        assertEquals(150, scheduleZoomIn(100))

    @Test fun `zooming out steps to the next rung down`() =
        assertEquals(70, scheduleZoomOut(100))

    @Test fun `zooming in at the top rung stays put`() =
        assertEquals(150, scheduleZoomIn(150))

    @Test fun `zooming out at the bottom rung stays put`() =
        assertEquals(70, scheduleZoomOut(70))

    @Test fun `can-zoom flags respect the ladder ends`() {
        assertFalse(scheduleCanZoomOut(70), "already at Compact")
        assertTrue(scheduleCanZoomIn(70))
        assertFalse(scheduleCanZoomIn(150), "already at Detailed")
        assertTrue(scheduleCanZoomOut(150))
    }

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
