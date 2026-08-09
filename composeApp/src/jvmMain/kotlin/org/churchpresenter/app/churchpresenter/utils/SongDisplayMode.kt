package org.churchpresenter.app.churchpresenter.utils

import org.churchpresenter.app.churchpresenter.data.settings.SongSettings

/** Widest group a slide may hold in line mode. Above this the setting stops meaning anything —
 * a verse is rarely longer — and the number field would let a typo hide the whole song. */
internal const val MAX_LINES_PER_SLIDE = 10

/**
 * True when any of the four song output surfaces — fullscreen, lower third, and their look-ahead
 * variants — is in per-line mode rather than whole-verse mode. This is what turns on arrow-key line
 * navigation, the on-screen nav hint and per-line highlighting. Extracted from three identical inline
 * OR-chains in SongsTab so the "is any surface in line mode" question lives in one tested place.
 */
internal fun isSongLineMode(settings: SongSettings): Boolean =
    settings.fullscreenDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE ||
        settings.lowerThirdDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE ||
        settings.lookAheadDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE ||
        settings.lowerThirdLookAheadDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE

/**
 * How many lines one slide holds on [isLowerThird]'s surface. Clamped rather than trusted: the value
 * arrives from a hand-editable settings.json, and 0 or a negative would make [songLineGroup] return
 * an empty slide — a blank screen mid-service.
 *
 * Deliberately one setting per *surface* rather than one per display-mode profile. Look-ahead has its
 * own verse/line choice but not its own group size: the group is a property of how much text fits on
 * that screen, which does not change because a preview line was switched on.
 */
internal fun songLinesPerSlide(settings: SongSettings, isLowerThird: Boolean): Int =
    (if (isLowerThird) settings.lowerThirdLinesPerSlide else settings.fullscreenLinesPerSlide)
        .coerceIn(1, MAX_LINES_PER_SLIDE)

/**
 * How far one arrow-key press moves the shared line cursor: one whole slide, not one line.
 *
 * There is a single cursor behind every output, so this has to be one number even when the projector
 * is set to three lines a slide and the lower third to one. **The fullscreen surface decides it**
 * whenever it is in line mode, and the lower third only when fullscreen is showing whole verses and
 * so has no slides to count.
 *
 * Taking the smallest group instead — which would guarantee no surface is ever stepped past a line
 * it never displayed — reads as broken at the keyboard: `lowerThirdDisplayMode` ships set to line
 * mode at one line a slide, so an operator who has never configured a lower third, and may not even
 * have one on screen, still had to press the arrow key three times to turn a three-line slide. One
 * press has to turn the slide the room is looking at.
 *
 * The cost is that a lower third set to a *smaller* group than fullscreen now skips the lines in
 * between. That is a genuinely contradictory pair of settings — two different slide sequences off one
 * cursor — and the main screen is the one to resolve it in favour of.
 */
internal fun songLineStep(settings: SongSettings): Int {
    val fullscreenInLineMode =
        settings.fullscreenDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE ||
            settings.lookAheadDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE
    val lowerThirdInLineMode =
        settings.lowerThirdDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE ||
            settings.lowerThirdLookAheadDisplayMode != Constants.SONG_DISPLAY_MODE_VERSE
    return when {
        fullscreenInLineMode -> songLinesPerSlide(settings, isLowerThird = false)
        lowerThirdInLineMode -> songLinesPerSlide(settings, isLowerThird = true)
        else -> 1
    }
}

/** The first line of the group [lineIndex] falls in, so a surface always shows whole groups —
 * lines 0..2, then 3..5 — instead of a window sliding one line at a time. */
internal fun songLineGroupStart(lineIndex: Int, linesPerSlide: Int): Int {
    val size = linesPerSlide.coerceAtLeast(1)
    if (lineIndex <= 0) return 0
    return (lineIndex / size) * size
}

/**
 * The slice of [lines] shown when the cursor sits at [lineIndex], snapped to its group. Empty when
 * there is nothing at that position, which callers read as "fall back to the whole verse".
 */
internal fun songLineGroup(lines: List<String>, lineIndex: Int, linesPerSlide: Int): List<String> {
    if (lines.isEmpty()) return emptyList()
    val size = linesPerSlide.coerceAtLeast(1)
    val start = songLineGroupStart(lineIndex, size)
    if (start >= lines.size) return emptyList()
    return lines.subList(start, minOf(start + size, lines.size))
}

/**
 * [lines] cut into the groups a surface will actually display. Used by auto-fit, which sizes the font
 * against the largest thing that ever appears on screen: fitting the whole verse when only part of it
 * is ever shown leaves the text far smaller than the screen allows.
 */
internal fun songLineGroups(lines: List<String>, linesPerSlide: Int): List<List<String>> {
    if (lines.isEmpty()) return emptyList()
    return lines.chunked(linesPerSlide.coerceAtLeast(1))
}
