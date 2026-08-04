package org.churchpresenter.app.churchpresenter

import androidx.compose.ui.input.key.Key
import org.churchpresenter.app.churchpresenter.tabs.Tabs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainDesktopFunctionKeyTest {

    @Test
    fun `each function key opens its own tab`() {
        assertEquals(Tabs.BIBLE, tabForFunctionKey(Key.F6))
        assertEquals(Tabs.SONGS, tabForFunctionKey(Key.F7))
        assertEquals(Tabs.PICTURES, tabForFunctionKey(Key.F8))
        assertEquals(Tabs.PRESENTATION, tabForFunctionKey(Key.F9))
        assertEquals(Tabs.MEDIA, tabForFunctionKey(Key.F10))
        assertEquals(Tabs.LOWER_THIRD, tabForFunctionKey(Key.F11))
        assertEquals(Tabs.ANNOUNCEMENTS, tabForFunctionKey(Key.F12))
    }

    @Test
    fun `no two shortcuts land on the same tab`() {
        val mapped = listOf(Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
            .mapNotNull { tabForFunctionKey(it) }

        assertEquals(mapped.size, mapped.toSet().size, "two keys opening one tab leaves a tab unreachable: $mapped")
    }

    @Test
    fun `keys the app uses for other things are not tab shortcuts`() {
        listOf(Key.Escape, Key.PageUp, Key.PageDown, Key.Z, Key.Enter, Key.Spacebar).forEach {
            assertNull(tabForFunctionKey(it), "$it is handled elsewhere and must not also switch tabs")
        }
    }

    @Test
    fun `the function keys either side of the range are not claimed`() {
        assertNull(tabForFunctionKey(Key.F5))
        assertNull(tabForFunctionKey(Key.F1))
    }

    @Test
    fun `an ordinary letter is not a tab shortcut`() {
        assertNull(tabForFunctionKey(Key.A))
        assertNull(tabForFunctionKey(Key.D))
    }
}
