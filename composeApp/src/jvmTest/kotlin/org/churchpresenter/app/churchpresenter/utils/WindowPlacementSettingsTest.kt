package org.churchpresenter.app.churchpresenter.utils

import androidx.compose.ui.window.WindowPlacement
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.NO_SAVED_POSITION
import org.churchpresenter.app.churchpresenter.data.settings.withWindowGeometry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How the main window's placement and geometry survive a restart.
 *
 * These were two hand-written `when` blocks in `main.kt` pointing in opposite directions — enum to
 * string on close, string to enum on launch — with nothing tying them together. That is the shape
 * that produced the Studio theme bug: a pair where one side is compiler-checked and the other is not.
 * `every placement survives a save and reload` is the test that ties them, and it fails the moment
 * someone adds a case to one side only.
 *
 * The geometry half matters for a different reason, spelled out in
 * `a maximized window keeps the floating size it should return to`.
 */
class WindowPlacementSettingsTest {

    // ── The round trip ──────────────────────────────────────────────────────────

    @Test
    fun `every placement survives a save and reload`() {
        listOf(WindowPlacement.Floating, WindowPlacement.Fullscreen, WindowPlacement.Maximized)
            .forEach { placement ->
                assertEquals(
                    placement,
                    windowPlacementFromSettings(windowPlacementToSettings(placement)),
                    "$placement did not survive a restart",
                )
            }
    }

    @Test
    fun `no two placements share a stored value`() {
        val stored = listOf(WindowPlacement.Floating, WindowPlacement.Fullscreen, WindowPlacement.Maximized)
            .map { windowPlacementToSettings(it) }

        assertEquals(stored.size, stored.toSet().size, "two placements collapsed onto one: $stored")
    }

    @Test
    fun `an unrecognised value opens maximized rather than floating`() {
        // Floating would pair with saved coordinates that may be on a display no longer attached.
        assertEquals(WindowPlacement.Maximized, windowPlacementFromSettings("tiled"))
        assertEquals(WindowPlacement.Maximized, windowPlacementFromSettings(""))
    }

    @Test
    fun `the shipped default is understood`() {
        assertEquals(WindowPlacement.Maximized, windowPlacementFromSettings(AppSettings().windowPlacement))
    }

    // ── What gets persisted on close ────────────────────────────────────────────

    @Test
    fun `a floating window remembers where and how big it was`() {
        val saved = AppSettings().withWindowGeometry(
            placement = "floating", isFloating = true, width = 1280, height = 800, x = 120, y = 64,
        )

        assertEquals("floating", saved.windowPlacement)
        assertEquals(1280, saved.windowWidth)
        assertEquals(800, saved.windowHeight)
        assertEquals(120, saved.windowX)
        assertEquals(64, saved.windowY)
    }

    @Test
    fun `a maximized window keeps the floating size it should return to`() {
        val previous = AppSettings().copy(windowWidth = 1280, windowHeight = 800)

        val saved = previous.withWindowGeometry(
            placement = "maximized", isFloating = false, width = 3840, height = 2160, x = 0, y = 0,
        )

        assertEquals(1280, saved.windowWidth, "storing the screen size loses the user's layout for good")
        assertEquals(800, saved.windowHeight)
        assertEquals("maximized", saved.windowPlacement)
    }

    @Test
    fun `a non-floating window clears its position rather than storing the screen origin`() {
        val saved = AppSettings().copy(windowX = 120, windowY = 64).withWindowGeometry(
            placement = "fullscreen", isFloating = false, width = 3840, height = 2160, x = 0, y = 0,
        )

        assertEquals(NO_SAVED_POSITION, saved.windowX, "0,0 may be a screen that is no longer attached")
        assertEquals(NO_SAVED_POSITION, saved.windowY)
    }

    @Test
    fun `the cleared position is what the launch path treats as absent`() {
        val saved = AppSettings().withWindowGeometry(
            placement = "maximized", isFloating = false, width = 100, height = 100, x = 5, y = 5,
        )

        // main.kt restores a saved position only when windowX >= 0.
        assertEquals(true, saved.windowX < 0, "the sentinel must fail the launch path's own check")
    }

    @Test
    fun `nothing outside the window fields is disturbed`() {
        val before = AppSettings().copy(language = "de", theme = "STUDIO", setupWizardShown = true)

        val after = before.withWindowGeometry(
            placement = "floating", isFloating = true, width = 800, height = 600, x = 10, y = 10,
        )

        assertEquals("de", after.language)
        assertEquals("STUDIO", after.theme)
        assertEquals(true, after.setupWizardShown)
    }

    @Test
    fun `a floating window round-trips its geometry through settings`() {
        val saved = AppSettings().withWindowGeometry(
            placement = windowPlacementToSettings(WindowPlacement.Floating),
            isFloating = true, width = 1024, height = 768, x = 33, y = 44,
        )

        assertEquals(WindowPlacement.Floating, windowPlacementFromSettings(saved.windowPlacement))
        assertEquals(1024, saved.windowWidth)
        assertEquals(33, saved.windowX)
    }
}
