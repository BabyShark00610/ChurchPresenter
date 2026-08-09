@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.KeyboardShortcutSettings
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShortcutSettingsTabTest {

    private fun withOverride(action: ShortcutAction, chords: List<KeyChord>) = AppSettings(
        keyboardShortcutSettings = KeyboardShortcutSettings(overrides = mapOf(action.name to chords))
    )

    @Test
    fun `every scope heading is rendered`() = shortcutTab {
        listOf(
            "Menus", "Global", "Bible Tab", "Songs Tab", "Pictures Tab",
            "Presentation Tab", "Media Tab", "Canvas Tab",
        ).forEach { onNode(hasText(it)).assertExists() }
    }

    @Test
    fun `a row shows the shipped binding when nothing has been changed`() = shortcutTab {
        // Compared against the registry's own rendering rather than a literal: the label is
        // "Ctrl+Z" on Windows and Linux but "⌃Z" on macOS.
        chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")
    }

    @Test
    fun `a row shows the override once one is set`() =
        shortcutTab(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) {
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("J")
        }

    @Test
    fun `an unbound action reads Not set rather than empty`() =
        shortcutTab(withOverride(ShortcutAction.MEDIA_MUTE, emptyList())) {
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("Not set")
        }

    @Test
    fun `Clear unbinds the action`() = shortcutTab { get ->
        // An untouched row offers Clear; the row's own button is found by scrolling to its chip
        // first so the two-column layout does not leave it off-screen.
        revertButtonFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()

        val overrides = get().keyboardShortcutSettings.overrides
        assertTrue(overrides.containsKey(ShortcutAction.MEDIA_MUTE.name), "clearing must record an override")
        assertEquals(emptyList(), overrides[ShortcutAction.MEDIA_MUTE.name])
    }

    @Test
    fun `Reset removes the override entirely rather than writing the default back`() =
        shortcutTab(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) { get ->
            revertButtonFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()

            // Absent, not "present and equal to the default" — that distinction is what lets a
            // later release change a default for users who never touched the action.
            assertFalse(get().keyboardShortcutSettings.overrides.containsKey(ShortcutAction.MEDIA_MUTE.name))
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")
        }

    @Test
    fun `Reset All drops every override at once`() {
        val settings = AppSettings(
            keyboardShortcutSettings = KeyboardShortcutSettings(
                overrides = mapOf(
                    ShortcutAction.MEDIA_MUTE.name to listOf(KeyChord.of(Key.J)),
                    ShortcutAction.UNDO.name to emptyList(),
                )
            )
        )
        shortcutTab(settings) { get ->
            onNodeWithTag(SHORTCUT_RESET_ALL_TAG).performClick()

            assertEquals(emptyMap(), get().keyboardShortcutSettings.overrides)
            chipFor(ShortcutAction.MEDIA_MUTE).assertTextEquals("M")
        }
    }

    @Test
    fun `editing one action leaves the others untouched`() =
        shortcutTab(withOverride(ShortcutAction.MEDIA_MUTE, listOf(KeyChord.of(Key.J)))) { get ->
            chipFor(ShortcutAction.MEDIA_PLAY_PAUSE).performScrollTo()

            assertEquals(
                mapOf(ShortcutAction.MEDIA_MUTE.name to listOf(KeyChord.of(Key.J))),
                get().keyboardShortcutSettings.overrides
            )
        }

    @Test
    fun `the tab writes only to keyboardShortcutSettings`() {
        // Compared against the *seeded* instance, not a fresh AppSettings(): several of its
        // defaults are freshly generated UUIDs, so two constructions never compare equal.
        val initial = AppSettings()
        shortcutTab(initial) { get ->
            revertButtonFor(ShortcutAction.MEDIA_MUTE).performScrollTo().performClick()

            val after = get()
            assertEquals(initial.copy(keyboardShortcutSettings = after.keyboardShortcutSettings), after)
        }
    }

    @Test
    fun `every action has a row`() = shortcutTab {
        ShortcutAction.entries.forEach {
            chipFor(it).assertExists()
        }
    }
}
