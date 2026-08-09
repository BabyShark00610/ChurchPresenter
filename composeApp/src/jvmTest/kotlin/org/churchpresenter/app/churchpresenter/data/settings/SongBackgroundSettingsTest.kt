package org.churchpresenter.app.churchpresenter.data.settings

import kotlinx.serialization.json.Json
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A song's own background: `AppSettings.songBackgroundFor` / `withSongBackground`, the store the
 * presenter consults before falling back to the shared song background.
 *
 * Absence is load-bearing here — no entry means "present on the shared background", and it is the
 * only way to say that, so removal is tested as carefully as writing.
 */
class SongBackgroundSettingsTest {

    private val image = BackgroundConfig(
        backgroundType = Constants.BACKGROUND_IMAGE,
        backgroundImage = "C:/media/sunrise.jpg",
    )

    @Test
    fun `a song with no entry has no background of its own`() =
        assertNull(AppSettings().songBackgroundFor("Hymnal::12"))

    @Test
    fun `a stored background comes back for its own song`() {
        val settings = AppSettings().withSongBackground("Hymnal::12", image)
        assertEquals(image, settings.songBackgroundFor("Hymnal::12"))
    }

    @Test
    fun `storing one song's background leaves every other song alone`() {
        val settings = AppSettings().withSongBackground("Hymnal::12", image)
        assertNull(settings.songBackgroundFor("Hymnal::13"))
    }

    @Test
    fun `storing again replaces rather than accumulates`() {
        val other = image.copy(backgroundImage = "C:/media/dusk.jpg")
        val settings = AppSettings()
            .withSongBackground("Hymnal::12", image)
            .withSongBackground("Hymnal::12", other)
        assertEquals(other, settings.songBackgroundFor("Hymnal::12"))
        assertEquals(1, settings.songBackgrounds.size)
    }

    @Test
    fun `saving null removes the entry, which is how a song goes back to the shared background`() {
        // Not "an entry holding a Default background" — that type means fall through to the GLOBAL
        // default, which is a different screen. Only absence sends the song back to the song slot.
        val settings = AppSettings()
            .withSongBackground("Hymnal::12", image)
            .withSongBackground("Hymnal::12", null)
        assertNull(settings.songBackgroundFor("Hymnal::12"))
        assertTrue(settings.songBackgrounds.isEmpty())
    }

    @Test
    fun `removing a song that was never stored changes nothing`() {
        val settings = AppSettings()
            .withSongBackground("Hymnal::12", image)
            .withSongBackground("Hymnal::99", null)
        assertEquals(image, settings.songBackgroundFor("Hymnal::12"))
        assertEquals(1, settings.songBackgrounds.size)
    }

    @Test
    fun `a blank song id is never stored and never matches`() {
        // SongItem.songId is "songbook::number" or "songbook::title"; a song with neither would
        // otherwise collide with every other such song on one shared entry.
        val settings = AppSettings().withSongBackground("", image)
        assertTrue(settings.songBackgrounds.isEmpty())
        assertNull(settings.songBackgroundFor(""))
    }

    @Test
    fun `a stored background survives a save and load round trip`() {
        // The field is new, so it has to actually serialize — a Map<String, BackgroundConfig> is the
        // first of its shape in AppSettings, and losing it would silently reset every song on
        // restart rather than fail loudly.
        val format = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val saved = AppSettings().withSongBackground("Hymnal::12", image)
        val loaded = format.decodeFromString<AppSettings>(format.encodeToString(saved))
        assertEquals(image, loaded.songBackgroundFor("Hymnal::12"))
    }

    @Test
    fun `a settings document written before this feature loads with no song backgrounds`() {
        // Purely additive, so no migration and no version bump — an older file must still decode.
        val format = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val loaded = format.decodeFromString<AppSettings>("""{"settingsVersion":6}""")
        assertTrue(loaded.songBackgrounds.isEmpty())
        assertNull(loaded.songBackgroundFor("Hymnal::12"))
    }
}
