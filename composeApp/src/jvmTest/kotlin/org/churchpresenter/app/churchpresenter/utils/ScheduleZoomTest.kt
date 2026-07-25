package org.churchpresenter.app.churchpresenter.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The schedule card zoom ladder. Zoom snaps to fixed rungs and the two rungs just below 100 strip
 * the card (drop actions, then collapse to one line) before it shrinks. These pin the rung math,
 * the clamped step, and the two shed/collapse thresholds.
 */
class ScheduleZoomTest {

    @Test fun `an exact percent resolves to its own rung`() =
        assertEquals(5, nearestZoomIndex(100)) // ZOOM_LEVELS[5] == 100

    @Test fun `a between-rungs percent resolves to the nearest rung`() =
        assertEquals(1, nearestZoomIndex(84)) // closer to 80 than 90

    @Test fun `a percent below the ladder resolves to the lowest rung`() =
        assertEquals(0, nearestZoomIndex(40))

    @Test fun `a percent above the ladder resolves to the highest rung`() =
        assertEquals(ZOOM_LEVELS.lastIndex, nearestZoomIndex(500))

    @Test fun `zooming in steps to the next rung up`() =
        assertEquals(110, scheduleZoomIn(100))

    @Test fun `zooming out steps to the next rung down`() =
        assertEquals(99, scheduleZoomOut(100))

    @Test fun `zooming in at the top rung stays put`() =
        assertEquals(150, scheduleZoomIn(150))

    @Test fun `zooming out at the bottom rung stays put`() =
        assertEquals(70, scheduleZoomOut(70))

    @Test fun `can-zoom flags respect the ladder ends`() {
        assertFalse(scheduleCanZoomOut(70), "already at the smallest rung")
        assertTrue(scheduleCanZoomIn(70))
        assertFalse(scheduleCanZoomIn(150), "already at the largest rung")
        assertTrue(scheduleCanZoomOut(150))
    }

    @Test fun `card actions show at full size and hide just below it`() {
        assertTrue(scheduleShowCardActions(100))
        assertTrue(scheduleShowCardActions(110))
        assertFalse(scheduleShowCardActions(99))
    }

    @Test fun `cards collapse to a single line at or below the collapse rung`() {
        assertTrue(scheduleSingleLineCards(98))
        assertTrue(scheduleSingleLineCards(70))
        assertFalse(scheduleSingleLineCards(99))
    }
}
