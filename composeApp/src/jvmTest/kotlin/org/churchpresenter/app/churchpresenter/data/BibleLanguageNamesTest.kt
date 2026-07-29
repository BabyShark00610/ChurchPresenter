package org.churchpresenter.app.churchpresenter.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the pure merge seam. [BibleLanguageNames.table] itself is not tested here: it reads
 * whatever eBible catalogue happens to be cached on the machine, and the merge is the only part
 * with a decision in it.
 */
class BibleLanguageNamesTest {

    @Test
    fun `a Zefania-only code is named even with no catalogue at all`() {
        // The cold-start case: the Zefania tab opened before eBible was ever fetched.
        val names = BibleLanguageNames.resolve(emptyMap())

        assertEquals("German", names["GER"])
        assertEquals("Czech", names["CZE"])
        assertEquals("Afrikaans", names["AFR"])
    }

    @Test
    fun `the catalogue's own names are merged in alongside the curated ones`() {
        val names = BibleLanguageNames.resolve(mapOf("ENG" to "English", "SPA" to "Spanish"))

        assertEquals("English", names["ENG"])
        assertEquals("Czech", names["CZE"], "a curated entry survives the merge")
    }

    @Test
    fun `the catalogue wins where both name the same code`() {
        val names = BibleLanguageNames.resolve(mapOf("GER" to "Standard German"))

        assertEquals(
            "Standard German", names["GER"],
            "published data outranks the snapshot, which is what lets a fixed spelling take effect"
        )
    }

    @Test
    fun `an unknown code resolves to nothing rather than a placeholder`() {
        assertEquals(null, BibleLanguageNames.resolve(emptyMap())["ZZZ"])
    }

    @Test
    fun `every curated entry is an uppercase code with a non-blank name`() {
        // The codes are matched against uppercased folder names, so a lowercase key would be dead.
        BibleLanguageNames.resolve(emptyMap()).forEach { (code, name) ->
            assertEquals(code.uppercase(), code, "code '$code' is not uppercase")
            assertTrue(name.isNotBlank(), "code '$code' has a blank name")
        }
    }
}
