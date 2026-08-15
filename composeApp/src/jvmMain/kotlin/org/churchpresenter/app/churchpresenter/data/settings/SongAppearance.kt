package org.churchpresenter.app.churchpresenter.data.settings

import kotlinx.serialization.Serializable

/**
 * What one song looks like when it differs from every other song.
 *
 * Every field is nullable and every null means "whatever [SongSettings] says" — the song only carries
 * what someone deliberately changed for it. That is what lets the shared settings keep working as
 * defaults: raise the global lyric size and every song that never set its own follows, which a
 * record of concrete values copied from the defaults could not do.
 *
 * Held per-machine in `AppSettings`, keyed by `SongItem.songId`, for the same reason as the tempo and
 * the capo: the `.song` file is a document shared between churches, while a font that exists here may
 * not exist there.
 *
 * The background is deliberately NOT here — it predates this and lives in `AppSettings.songBackgrounds`,
 * already keyed the same way and already tested. The editor writes both; merging the two stores would
 * buy tidiness at the price of a migration.
 */
@Serializable
data class SongAppearance(
    val fontType: String? = null,
    val fontSize: Int? = null,
    val fontSizeAutoFit: Boolean? = null,
    val color: String? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val shadow: Boolean? = null,
    val scrimEnabled: Boolean? = null,
    val scrimColor: String? = null,
    val scrimOpacity: Int? = null,
    val scrimSoftness: Int? = null,
    val scrimPadding: Int? = null,
    val scrimWidthPercent: Int? = null,
    val displayMode: String? = null,
    val linesPerSlide: Int? = null,
    /**
     * Font size for one named section, keyed by its header as written without the brackets —
     * `"Verse 1"`, `"Chorus"`. By name rather than by position so that inserting a verse does not
     * silently re-point every override below it, and so the chorus keeps its size at each of the
     * places the repeat rule puts it.
     *
     * A section with an entry here is drawn at that size flat: auto-fit is a whole-song calculation
     * and cannot honour a per-section size and its own uniform answer at once.
     */
    val sectionFontSizes: Map<String, Int> = emptyMap(),
) {
    /** True when the song carries no override at all, so it need not be stored. */
    fun isEmpty(): Boolean = this == EMPTY

    /** [sectionFontSizes] with [section]'s size set to [size], or removed when null. */
    fun withSectionFontSize(section: String, size: Int?): SongAppearance {
        val key = sectionKey(section)
        if (key.isBlank()) return this
        return copy(
            sectionFontSizes = if (size == null) sectionFontSizes - key
            else sectionFontSizes + (key to size)
        )
    }

    /** The size [section] is drawn at, or null to leave it to the song's size and auto-fit. */
    fun sectionFontSize(section: String?): Int? {
        val key = sectionKey(section ?: return null)
        return if (key.isBlank()) null else sectionFontSizes[key]
    }

    companion object {
        val EMPTY = SongAppearance()

        /**
         * A section header reduced to the name people see: `"[Verse 1]"` and `"{Verse 1}"` both
         * become `"Verse 1"`. The lyrics panel and the editor's preview strip the brackets the same
         * way, so an override set against what is on screen finds the section it was set against.
         */
        fun sectionKey(header: String): String = header.trim().trim('[', ']', '{', '}').trim()
    }
}

/**
 * These settings with [appearance]'s overrides folded in, aimed at whichever surface is drawing.
 *
 * Done as a merge producing a whole [SongSettings] rather than as a check at each of the presenter's
 * ~15 read sites: the presenter binds `songSettings` once and reads everything through it, so one
 * merge at that binding covers the lyric font, the colour, the style flags, the scrim and the group
 * size together, and nothing can be forgotten later by adding a read and not an override.
 *
 * [isLowerThird] picks which half of every paired field the override lands on. A song's look is one
 * choice by the operator; which surface it reaches is decided here, at the point of use, so that the
 * same stored value styles the projector and the lower third without the editor having to offer two
 * of everything.
 *
 * Look-ahead's own font profile is deliberately left alone — it styles the *preview* of the next
 * slide, which is a property of the operator's screen rather than of the song.
 */
internal fun SongSettings.withSongAppearance(
    appearance: SongAppearance?,
    isLowerThird: Boolean,
): SongSettings {
    if (appearance == null || appearance.isEmpty()) return this
    return if (isLowerThird) copy(
        lyricsLowerThirdFontType = appearance.fontType ?: lyricsLowerThirdFontType,
        lyricsLowerThirdFontSize = appearance.fontSize ?: lyricsLowerThirdFontSize,
        lyricsLowerThirdFontSizeAutoFit = appearance.fontSizeAutoFit ?: lyricsLowerThirdFontSizeAutoFit,
        lyricsLowerThirdColor = appearance.color ?: lyricsLowerThirdColor,
        lyricsLowerThirdBold = appearance.bold ?: lyricsLowerThirdBold,
        lyricsLowerThirdItalic = appearance.italic ?: lyricsLowerThirdItalic,
        lyricsLowerThirdUnderline = appearance.underline ?: lyricsLowerThirdUnderline,
        lyricsLowerThirdShadow = appearance.shadow ?: lyricsLowerThirdShadow,
        lyricsLowerThirdScrimEnabled = appearance.scrimEnabled ?: lyricsLowerThirdScrimEnabled,
        lyricsLowerThirdScrimColor = appearance.scrimColor ?: lyricsLowerThirdScrimColor,
        lyricsLowerThirdScrimOpacity = appearance.scrimOpacity ?: lyricsLowerThirdScrimOpacity,
        lyricsLowerThirdScrimSoftness = appearance.scrimSoftness ?: lyricsLowerThirdScrimSoftness,
        lyricsLowerThirdScrimPadding = appearance.scrimPadding ?: lyricsLowerThirdScrimPadding,
        lyricsLowerThirdScrimWidthPercent = appearance.scrimWidthPercent ?: lyricsLowerThirdScrimWidthPercent,
        lowerThirdDisplayMode = appearance.displayMode ?: lowerThirdDisplayMode,
        lowerThirdLinesPerSlide = appearance.linesPerSlide ?: lowerThirdLinesPerSlide,
    ) else copy(
        lyricsFontType = appearance.fontType ?: lyricsFontType,
        lyricsFontSize = appearance.fontSize ?: lyricsFontSize,
        lyricsFontSizeAutoFit = appearance.fontSizeAutoFit ?: lyricsFontSizeAutoFit,
        lyricsColor = appearance.color ?: lyricsColor,
        lyricsBold = appearance.bold ?: lyricsBold,
        lyricsItalic = appearance.italic ?: lyricsItalic,
        lyricsUnderline = appearance.underline ?: lyricsUnderline,
        lyricsShadow = appearance.shadow ?: lyricsShadow,
        lyricsScrimEnabled = appearance.scrimEnabled ?: lyricsScrimEnabled,
        lyricsScrimColor = appearance.scrimColor ?: lyricsScrimColor,
        lyricsScrimOpacity = appearance.scrimOpacity ?: lyricsScrimOpacity,
        lyricsScrimSoftness = appearance.scrimSoftness ?: lyricsScrimSoftness,
        lyricsScrimPadding = appearance.scrimPadding ?: lyricsScrimPadding,
        lyricsScrimWidthPercent = appearance.scrimWidthPercent ?: lyricsScrimWidthPercent,
        fullscreenDisplayMode = appearance.displayMode ?: fullscreenDisplayMode,
        fullscreenLinesPerSlide = appearance.linesPerSlide ?: fullscreenLinesPerSlide,
    )
}
