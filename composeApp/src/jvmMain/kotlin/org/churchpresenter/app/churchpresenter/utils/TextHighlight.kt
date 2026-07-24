package org.churchpresenter.app.churchpresenter.utils

/**
 * The `[start, end)` spans of [text] that match [query] case-insensitively, left to right and
 * non-overlapping — the segments a search result highlights. Extracted from BibleTab's
 * `buildAnnotatedString` loop so the match-finding can be tested apart from the Compose styling.
 *
 * - A blank query yields no spans; an empty query would otherwise make `indexOf` loop forever.
 * - Indices are clamped to `text.length`, because `lowercase()` can change string length in some
 *   locales and the spans are used to slice the original (non-lowercased) [text].
 */
internal fun highlightRanges(text: String, query: String): List<Pair<Int, Int>> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()
    val ranges = mutableListOf<Pair<Int, Int>>()
    var start = lowerText.indexOf(lowerQuery)
    while (start != -1) {
        val safeStart = start.coerceAtMost(text.length)
        val safeEnd = (start + lowerQuery.length).coerceAtMost(text.length)
        ranges.add(safeStart to safeEnd)
        start = lowerText.indexOf(lowerQuery, start + lowerQuery.length)
    }
    return ranges
}
