package org.churchpresenter.app.churchpresenter.utils

import androidx.compose.ui.input.key.Key
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.models.ShortcutScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ShortcutMapTest {

    private fun mapWith(vararg overrides: Pair<ShortcutAction, List<KeyChord>>) =
        ShortcutMap.from(KeyboardShortcutSettings(overrides.associate { it.first.name to it.second }))

    @Test
    fun `an action with no override keeps its shipped binding`() {
        assertEquals(ShortcutAction.UNDO.defaults, ShortcutMap.DEFAULT.chordsFor(ShortcutAction.UNDO))
        assertTrue(ShortcutMap.DEFAULT.matches(ShortcutAction.UNDO, keyDown(Key.Z, ctrl = true)))
    }

    @Test
    fun `an override replaces the shipped binding rather than adding to it`() {
        val map = mapWith(ShortcutAction.UNDO to listOf(KeyChord.of(Key.U, ctrl = true)))

        assertTrue(map.matches(ShortcutAction.UNDO, keyDown(Key.U, ctrl = true)))
        assertFalse(map.matches(ShortcutAction.UNDO, keyDown(Key.Z, ctrl = true)))
    }

    @Test
    fun `an empty override list means unbound, which is not the same as absent`() {
        val map = mapWith(ShortcutAction.CLEAR_OUTPUT to emptyList())

        assertTrue(map.chordsFor(ShortcutAction.CLEAR_OUTPUT).isEmpty())
        assertFalse(map.matches(ShortcutAction.CLEAR_OUTPUT, keyDown(Key.Escape)))
        // Absent, by contrast, still resolves to the default.
        assertTrue(ShortcutMap.DEFAULT.matches(ShortcutAction.CLEAR_OUTPUT, keyDown(Key.Escape)))
    }

    @Test
    fun `an override naming an action this build does not have is ignored`() {
        val map = ShortcutMap.from(
            KeyboardShortcutSettings(overrides = mapOf("AN_ACTION_FROM_A_LATER_RELEASE" to listOf(KeyChord.of(Key.J))))
        )

        // The unknown entry must not disturb anything else, and must not throw on load.
        assertEquals(ShortcutAction.UNDO.defaults, map.chordsFor(ShortcutAction.UNDO))
    }

    @Test
    fun `an action with several chords answers to all of them`() {
        val map = ShortcutMap.DEFAULT

        assertTrue(map.matches(ShortcutAction.PRESENTATION_NEXT, keyDown(Key.DirectionRight)))
        assertTrue(map.matches(ShortcutAction.PRESENTATION_NEXT, keyDown(Key.DirectionDown)))
        assertTrue(map.matches(ShortcutAction.CANVAS_DELETE_SOURCE, keyDown(Key.Delete)))
        assertTrue(map.matches(ShortcutAction.CANVAS_DELETE_SOURCE, keyDown(Key.Backspace)))
    }

    @Test
    fun `actionFor only answers within the scope asked for`() {
        val map = ShortcutMap.DEFAULT

        assertEquals(ShortcutAction.MEDIA_PLAY_PAUSE, map.actionFor(keyDown(Key.Spacebar), ShortcutScope.MEDIA))
        assertEquals(ShortcutAction.PICTURES_PLAY_PAUSE, map.actionFor(keyDown(Key.Spacebar), ShortcutScope.PICTURES))
        assertNull(map.actionFor(keyDown(Key.Spacebar), ShortcutScope.GLOBAL))
    }

    @Test
    fun `the same key in two different tab scopes is not a conflict`() {
        // Space is play/pause in Media, Pictures and Presentation, and always has been.
        assertNull(
            ShortcutMap.DEFAULT.conflictFor(KeyChord.of(Key.Spacebar), ShortcutAction.MEDIA_PLAY_PAUSE)
        )
    }

    @Test
    fun `Delete across the menu and the Canvas tab is not a conflict`() {
        // Menu accelerators are dispatched by the MenuBar before focus-based handlers see the
        // event, so this pair coexists in the shipped app.
        assertNull(
            ShortcutMap.DEFAULT.conflictFor(KeyChord.of(Key.Delete), ShortcutAction.CANVAS_DELETE_SOURCE)
        )
    }

    @Test
    fun `two global actions on one chord do conflict, and the clash is named`() {
        val conflict = ShortcutMap.DEFAULT.conflictFor(KeyChord.of(Key.Z, ctrl = true), ShortcutAction.CLEAR_OUTPUT)

        assertEquals(ShortcutAction.UNDO, conflict)
    }

    @Test
    fun `a global binding conflicts with a tab binding, since both handlers see the event`() {
        val conflict = ShortcutMap.DEFAULT.conflictFor(KeyChord.of(Key.Escape), ShortcutAction.BIBLE_NEXT_VERSE)

        assertEquals(ShortcutAction.CLEAR_OUTPUT, conflict)
    }

    @Test
    fun `an action never conflicts with itself`() {
        assertNull(
            ShortcutMap.DEFAULT.conflictFor(KeyChord.of(Key.Z, ctrl = true), ShortcutAction.UNDO)
        )
    }

    @Test
    fun `isCustomized tracks whether the binding has moved off the default`() {
        assertFalse(ShortcutMap.DEFAULT.isCustomized(ShortcutAction.UNDO))
        assertTrue(mapWith(ShortcutAction.UNDO to listOf(KeyChord.of(Key.U))).isCustomized(ShortcutAction.UNDO))
        assertTrue(mapWith(ShortcutAction.UNDO to emptyList()).isCustomized(ShortcutAction.UNDO))
        // An override that happens to equal the default is not a customization.
        assertFalse(mapWith(ShortcutAction.UNDO to ShortcutAction.UNDO.defaults).isCustomized(ShortcutAction.UNDO))
    }

    @Test
    fun `DEFAULT is a shared instance so it costs nothing to read repeatedly`() {
        assertSame(ShortcutMap.DEFAULT, ShortcutMap.DEFAULT)
    }
}
