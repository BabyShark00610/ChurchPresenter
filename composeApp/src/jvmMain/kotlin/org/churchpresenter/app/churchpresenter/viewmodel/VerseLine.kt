package org.churchpresenter.app.churchpresenter.viewmodel

/**
 * A displayed verse line is stored as `"<number>. <text>"` — e.g. `"16. For God so loved the world"`.
 * These helpers pull the pieces back out in one place, so the `"N. text"` convention lives behind a
 * name instead of being re-parsed inline (with `substringBefore(". ")` / `substringAfter(". ")`) at a
 * dozen sites across the Bible tab and its view model. Behaviour is identical to those inline forms.
 */

/** The leading verse number, or `null` when the line doesn't start with an integer followed by `". "`. */
internal fun verseNumberOf(verseLine: String): Int? = verseLine.substringBefore(". ").toIntOrNull()

/** The verse text after `"N. "`; [fallback] (default: the whole line) is returned when there is no `". "`. */
internal fun verseTextOf(verseLine: String, fallback: String = verseLine): String =
    verseLine.substringAfter(". ", fallback)

/** Index of the first line whose verse number is in [liveVerseNumbers], or `-1` if none match. */
internal fun indexOfFirstLiveVerse(verses: List<String>, liveVerseNumbers: Set<Int>): Int =
    verses.indexOfFirst { verseNumberOf(it)?.let { n -> n in liveVerseNumbers } == true }

/** The reference label for a verse line — `"John 3:16"`, or `"John 3"` when the line carries no number. */
internal fun formatVerseReference(verseLine: String, bookName: String, chapter: Int): String {
    val verseNum = verseNumberOf(verseLine)
    return if (verseNum != null) "$bookName $chapter:$verseNum" else "$bookName $chapter"
}

/**
 * The `(start, end)` verse span shown for a passage, for CCLI/training logging. Parsed as the min and
 * max of the numbers in a range string like `"1-3"` or `"2,4,5"`; falls back to [fallbackVerse] as a
 * single verse when the range is blank or has no numbers. `end` is `null` for a single verse (when the
 * max doesn't exceed the min).
 */
internal fun verseSpan(verseRange: String, fallbackVerse: Int): Pair<Int, Int?> {
    val nums = verseRange
        .takeIf { it.isNotBlank() }
        ?.split(",", "-")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?.takeIf { it.isNotEmpty() }
        ?: listOf(fallbackVerse)
    val start = nums.min()
    val end = nums.max().takeIf { it > start }
    return start to end
}
