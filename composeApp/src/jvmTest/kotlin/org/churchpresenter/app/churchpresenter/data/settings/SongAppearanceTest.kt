package org.churchpresenter.app.churchpresenter.data.settings

import kotlinx.serialization.json.Json
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A song's own look: what it overrides, what it leaves alone, and how the two are merged.
 *
 * The load-bearing property throughout is that **null means inherit**, not "unset to a default".
 * A song that never chose a font has to keep following the shared one when that one changes, so the
 * tests below check the shared value comes through rather than that some particular value does.
 */
class SongAppearanceTest {

    private val defaults = SongSettings()

    // ── what counts as "nothing set" ──

    @Test
    fun `a fresh appearance overrides nothing`() = assertTrue(SongAppearance().isEmpty())

    @Test
    fun `one field is enough to make it worth storing`() =
        assertTrue(!SongAppearance(fontSize = 80).isEmpty())

    @Test
    fun `a section size alone is enough to make it worth storing`() =
        assertTrue(!SongAppearance().withSectionFontSize("Chorus", 90).isEmpty())

    // ── merging: fullscreen ──

    @Test
    fun `an empty appearance leaves the settings untouched`() {
        // Identity, not equality: the merge must not rebuild the object for nothing, because the
        // presenter caches its auto-fit on it.
        assertSame(defaults, defaults.withSongAppearance(SongAppearance(), isLowerThird = false))
        assertSame(defaults, defaults.withSongAppearance(null, isLowerThird = false))
    }

    @Test
    fun `a font override reaches the fullscreen lyric font`() {
        val merged = defaults.withSongAppearance(
            SongAppearance(fontType = "Georgia", fontSize = 88), isLowerThird = false,
        )
        assertEquals("Georgia", merged.lyricsFontType)
        assertEquals(88, merged.lyricsFontSize)
    }

    @Test
    fun `fields the song did not set keep the shared value`() {
        val merged = defaults.withSongAppearance(SongAppearance(fontSize = 88), isLowerThird = false)
        assertEquals(defaults.lyricsFontType, merged.lyricsFontType)
        assertEquals(defaults.lyricsColor, merged.lyricsColor)
        assertEquals(defaults.lyricsScrimEnabled, merged.lyricsScrimEnabled)
    }

    @Test
    fun `a song following the shared font moves when the shared font moves`() {
        // The whole reason the fields are nullable rather than copies of today's defaults.
        val shared = SongSettings(lyricsFontType = "Verdana")
        val merged = shared.withSongAppearance(SongAppearance(fontSize = 88), isLowerThird = false)
        assertEquals("Verdana", merged.lyricsFontType)
    }

    @Test
    fun `false is an override, not an absence`() {
        // `bold = false` has to survive the merge: it is the song saying "not bold" over a shared
        // setting that says bold, which a plain `?:` on a Boolean would get right and an `if (x)`
        // would not.
        val shared = SongSettings(lyricsBold = true)
        val merged = shared.withSongAppearance(SongAppearance(bold = false), isLowerThird = false)
        assertEquals(false, merged.lyricsBold)
    }

    @Test
    fun `the scrim comes across whole`() {
        val merged = defaults.withSongAppearance(
            SongAppearance(
                scrimEnabled = true, scrimColor = "#101010", scrimOpacity = 70,
                scrimSoftness = 20, scrimPadding = 60, scrimWidthPercent = 80,
            ),
            isLowerThird = false,
        )
        assertEquals(true, merged.lyricsScrimEnabled)
        assertEquals("#101010", merged.lyricsScrimColor)
        assertEquals(70, merged.lyricsScrimOpacity)
        assertEquals(20, merged.lyricsScrimSoftness)
        assertEquals(60, merged.lyricsScrimPadding)
        assertEquals(80, merged.lyricsScrimWidthPercent)
    }

    @Test
    fun `the slide split comes across`() {
        val merged = defaults.withSongAppearance(
            SongAppearance(displayMode = Constants.SONG_DISPLAY_MODE_LINE, linesPerSlide = 3),
            isLowerThird = false,
        )
        assertEquals(Constants.SONG_DISPLAY_MODE_LINE, merged.fullscreenDisplayMode)
        assertEquals(3, merged.fullscreenLinesPerSlide)
    }

    // ── merging: lower third ──

    @Test
    fun `the same override lands on the lower third's own fields`() {
        val a = SongAppearance(fontType = "Georgia", fontSize = 30, linesPerSlide = 2)
        val lower = defaults.withSongAppearance(a, isLowerThird = true)
        assertEquals("Georgia", lower.lyricsLowerThirdFontType)
        assertEquals(30, lower.lyricsLowerThirdFontSize)
        assertEquals(2, lower.lowerThirdLinesPerSlide)
    }

    @Test
    fun `styling the lower third leaves the fullscreen fields alone, and the reverse`() {
        val a = SongAppearance(fontType = "Georgia")
        assertEquals(defaults.lyricsFontType, defaults.withSongAppearance(a, true).lyricsFontType)
        assertEquals(
            defaults.lyricsLowerThirdFontType,
            defaults.withSongAppearance(a, false).lyricsLowerThirdFontType,
        )
    }

    @Test
    fun `look-ahead keeps its own profile either way`() {
        // It styles the preview of the NEXT slide — a property of the operator's screen, not of the
        // song, so a song's font must not reach it.
        val merged = defaults.withSongAppearance(
            SongAppearance(fontType = "Georgia", color = "#FF0000"), isLowerThird = false,
        )
        assertEquals(defaults.lookAheadFontType, merged.lookAheadFontType)
        assertEquals(defaults.lookAheadColor, merged.lookAheadColor)
    }

    // ── per-section sizes ──

    @Test
    fun `a section size is found by the name shown on screen, brackets or not`() {
        val a = SongAppearance().withSectionFontSize("[Chorus]", 90)
        assertEquals(90, a.sectionFontSize("Chorus"))
        assertEquals(90, a.sectionFontSize("[Chorus]"))
        assertEquals(90, a.sectionFontSize("{Chorus}"))
    }

    @Test
    fun `a section with no size of its own reports none`() =
        assertNull(SongAppearance().withSectionFontSize("Chorus", 90).sectionFontSize("Verse 1"))

    @Test
    fun `saving null clears a section back to the song's own size`() {
        val a = SongAppearance().withSectionFontSize("Chorus", 90).withSectionFontSize("Chorus", null)
        assertNull(a.sectionFontSize("Chorus"))
        assertTrue(a.sectionFontSizes.isEmpty())
    }

    @Test
    fun `a blank section name is never stored`() =
        assertTrue(SongAppearance().withSectionFontSize("   ", 90).sectionFontSizes.isEmpty())

    @Test
    fun `a null header matches nothing`() =
        assertNull(SongAppearance().withSectionFontSize("Chorus", 90).sectionFontSize(null))

    // ── persistence ──

    @Test
    fun `an appearance survives a save and load round trip`() {
        val format = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val saved = SongAppearance(fontType = "Georgia", bold = false)
            .withSectionFontSize("Chorus", 90)
        val loaded = format.decodeFromString<SongAppearance>(format.encodeToString(saved))
        assertEquals(saved, loaded)
    }

    @Test
    fun `a settings document written before this feature loads with no appearances`() {
        val format = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val loaded = format.decodeFromString<AppSettings>("""{"settingsVersion":6}""")
        assertTrue(loaded.songAppearances.isEmpty())
        assertNull(loaded.songAppearanceFor("Hymnal::12"))
    }

    @Test
    fun `an appearance that overrides nothing is not kept against the song`() {
        val settings = AppSettings()
            .withSongAppearance("Hymnal::12", SongAppearance(fontSize = 88))
            .withSongAppearance("Hymnal::12", SongAppearance())
        assertTrue(settings.songAppearances.isEmpty())
    }

    @Test
    fun `a blank song id is never a key`() =
        assertTrue(AppSettings().withSongAppearance("", SongAppearance(fontSize = 88)).songAppearances.isEmpty())
}
