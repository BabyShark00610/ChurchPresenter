package org.churchpresenter.app.churchpresenter

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [centeredOnMainWindow] decides where every modal dialog in the app opens. Get it wrong and a
 * dialog appears off-screen, on the wrong monitor, or drifting outside the main window instead of
 * centred on it — the kind of thing a user notices immediately and a test easily misses, since
 * every call site just trusts the returned [WindowPosition] without checking it.
 */
class WindowUtilsTest {

    @Test
    fun `no main window falls back to the platform default`() {
        val result = centeredOnMainWindow(null, dialogWidth = 400.dp, dialogHeight = 300.dp)
        assertEquals(WindowPosition.PlatformDefault, result)
    }

    @Test
    fun `a main window still at the platform default falls back too`() {
        val state = WindowState(position = WindowPosition.PlatformDefault, size = DpSize(800.dp, 600.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 400.dp, dialogHeight = 300.dp)
        assertEquals(WindowPosition.PlatformDefault, result)
    }

    @Test
    fun `a main window positioned only by alignment falls back too`() {
        val state = WindowState(position = WindowPosition(Alignment.Center), size = DpSize(800.dp, 600.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 400.dp, dialogHeight = 300.dp)
        assertEquals(WindowPosition.PlatformDefault, result)
    }

    @Test
    fun `a known main window position centres the dialog on it`() {
        val state = WindowState(position = WindowPosition(100.dp, 100.dp), size = DpSize(800.dp, 600.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 400.dp, dialogHeight = 300.dp)
        assertEquals(WindowPosition(300.dp, 250.dp), result)
    }

    @Test
    fun `a dialog the same size as the window lands exactly on its corner`() {
        val state = WindowState(position = WindowPosition(50.dp, 20.dp), size = DpSize(800.dp, 600.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 800.dp, dialogHeight = 600.dp)
        assertEquals(WindowPosition(50.dp, 20.dp), result)
    }

    @Test
    fun `a dialog wider than the main window is clamped to its left edge, not pushed negative`() {
        val state = WindowState(position = WindowPosition(50.dp, 50.dp), size = DpSize(400.dp, 600.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 900.dp, dialogHeight = 300.dp)
        val absolute = result as WindowPosition.Absolute
        assertEquals(0.dp, absolute.x)
        assertEquals(200.dp, absolute.y)
    }

    @Test
    fun `a dialog taller than the main window is clamped to its top edge, not pushed negative`() {
        val state = WindowState(position = WindowPosition(50.dp, 50.dp), size = DpSize(800.dp, 300.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 400.dp, dialogHeight = 900.dp)
        val absolute = result as WindowPosition.Absolute
        assertEquals(250.dp, absolute.x)
        assertEquals(0.dp, absolute.y)
    }

    @Test
    fun `a main window at the screen origin still centres correctly`() {
        val state = WindowState(position = WindowPosition(0.dp, 0.dp), size = DpSize(1920.dp, 1080.dp))
        val result = centeredOnMainWindow(state, dialogWidth = 940.dp, dialogHeight = 700.dp)
        assertEquals(WindowPosition(490.dp, 190.dp), result)
    }
}
