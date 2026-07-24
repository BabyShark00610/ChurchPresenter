package org.churchpresenter.app.churchpresenter.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [BibleBookAbbreviations] turns typed-in reference prefixes ("Gen", "1 Cor", "1cor.") into a
 * canonical book id so Planning Center plan text can be matched against the loaded Bible. The
 * resource-backed loading needs Compose string resources (headless-bound), but the parsing and
 * matching helpers underneath are pure, and they carry the tricky rules: a trailing period is
 * ignored, case and inner whitespace don't matter, and "1cor" must match a "1 Cor" variant.
 */
class BibleBookAbbreviationsTest {

    @Test
    fun `normalize lowercases, trims and drops a trailing period`() {
        assertEquals("gen", BibleBookAbbreviations.normalize("  Gen.  "))
        assertEquals("revelation", BibleBookAbbreviations.normalize("Revelation"))
    }

    @Test
    fun `normalize collapses runs of internal whitespace to a single space`() {
        assertEquals("1 cor", BibleBookAbbreviations.normalize("1   Cor"))
        assertEquals("song of songs", BibleBookAbbreviations.normalize("Song  of\tSongs"))
    }

    @Test
    fun `normalize only strips the trailing period, not internal ones`() {
        assertEquals("ph.il", BibleBookAbbreviations.normalize("Ph.il."))
    }

    @Test
    fun `parseVariants splits on the pipe and trims each variant`() {
        assertEquals(listOf("Gen", "Ge", "Gn"), BibleBookAbbreviations.parseVariants("Gen | Ge | Gn"))
    }

    @Test
    fun `parseVariants drops blank entries from stray or trailing pipes`() {
        assertEquals(listOf("Gen", "Ge"), BibleBookAbbreviations.parseVariants("Gen || Ge |  | "))
    }

    @Test
    fun `parseVariants of an empty string is an empty list`() {
        assertEquals(emptyList(), BibleBookAbbreviations.parseVariants(""))
        assertEquals(emptyList(), BibleBookAbbreviations.parseVariants("   |  "))
    }

    private val sample = mapOf(
        1 to listOf("Gen", "Ge", "Gn"),
        46 to listOf("1 Cor", "1Co"),
        19 to listOf("Ps", "Psalm"),
    )

    private fun resolve(text: String): Int? {
        val normalized = BibleBookAbbreviations.normalize(text)
        return BibleBookAbbreviations.findBookId(sample, normalized, normalized.replace(" ", ""))
    }

    @Test
    fun `findBookId matches a variant exactly, ignoring case and a trailing period`() {
        assertEquals(1, resolve("Gen"))
        assertEquals(1, resolve("gen."))
        assertEquals(1, resolve("GN"))
        assertEquals(19, resolve("Psalm"))
    }

    @Test
    fun `findBookId matches a numbered book whether or not a space follows the numeral`() {
        assertEquals(46, resolve("1 Cor"))
        assertEquals(46, resolve("1cor"))
        assertEquals(46, resolve("1CO"))
    }

    @Test
    fun `findBookId returns null for an unknown abbreviation`() {
        assertNull(resolve("Xyz"))
        assertNull(resolve("2 Cor"), "a numbered book that is not in the table must not match its sibling")
    }

    @Test
    fun `findBookId returns null against an empty table`() {
        assertNull(BibleBookAbbreviations.findBookId(emptyMap(), "gen", "gen"))
    }
}
