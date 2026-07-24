package org.churchpresenter.app.churchpresenter.server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.churchpresenter.app.churchpresenter.data.StrongsEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The pure lookup/formatting helpers behind the dictionary REST endpoints. The suspend loaders read
 * bundled resources and the interlinear index, but the language normalisation, root-number
 * extraction, verse-scope ordering, and the wire DTOs are all pure and carry the tricky rules —
 * a mis-parsed reference here reorders the "Appears in" list or leaks the wrong entry to a client.
 */
class StrongsDictionaryRepositoryTest {

    private val repo = StrongsDictionaryRepository

    @Test
    fun `only Russian maps to ru, everything else falls back to en`() {
        assertEquals("ru", repo.normalizeLang("ru"))
        assertEquals("ru", repo.normalizeLang("RU"))
        assertEquals("en", repo.normalizeLang("en"))
        assertEquals("en", repo.normalizeLang("fr"))
        assertEquals("en", repo.normalizeLang(null))
        assertEquals("en", repo.normalizeLang(""))
    }

    private fun entry(number: String, definition: String) = StrongsEntry(
        number = number, word = "w", transliteration = "t", pronunciation = "p", definition = definition,
    )

    @Test
    fun `rootOf returns the first Strong's reference in the definition other than the entry itself`() {
        assertEquals("H1234", repo.rootOf(entry("H430", "a form of H430; from H1234; God")))
        assertEquals("G2222", repo.rootOf(entry("G26", "from G2222 (life)")))
    }

    @Test
    fun `rootOf is empty when the definition cites no other number`() {
        assertEquals("", repo.rootOf(entry("H430", "plural of H430")), "the entry's own number is not its root")
        assertEquals("", repo.rootOf(entry("H1", "primitive root; a father")))
    }

    @Test
    fun `orderRefsByScope leaves the list untouched when no book is given`() {
        val refs = listOf("001001001", "043003016", "019023001")
        assertEquals(refs, repo.orderRefsByScope(refs, book = null, chapter = null, verse = null))
    }

    @Test
    fun `orderRefsByScope floats references in the requested book to the front, keeping order`() {
        val refs = listOf("001001001", "043003016", "001050020", "019023001")
        assertEquals(
            listOf("001001001", "001050020", "043003016", "019023001"),
            repo.orderRefsByScope(refs, book = 1, chapter = null, verse = null),
        )
    }

    @Test
    fun `orderRefsByScope narrows to a chapter`() {
        val refs = listOf("043003016", "043003017", "043004001", "001001001")
        assertEquals(
            listOf("043003016", "043003017", "043004001", "001001001"),
            repo.orderRefsByScope(refs, book = 43, chapter = 3, verse = null),
        )
    }

    @Test
    fun `orderRefsByScope narrows to a single verse that then leads the list`() {
        val refs = listOf("043003017", "043003016", "043004001")
        val ordered = repo.orderRefsByScope(refs, book = 43, chapter = 3, verse = 16)
        assertEquals(listOf("043003016", "043003017", "043004001"), ordered)
    }

    @Test
    fun `StrongsEntryDto round-trips`() {
        val json = Json { encodeDefaults = true }
        val dto = StrongsEntryDto("H430", "elohiym", "el-o-heem'", "el-o-HEEM", "God", "God", 2606, "H433")
        assertEquals(dto, json.decodeFromString<StrongsEntryDto>(json.encodeToString(dto)))
    }

    @Test
    fun `DictionaryVerseDto and DictionaryVersesResponse round-trip`() {
        val json = Json { encodeDefaults = true }
        val verse = DictionaryVerseDto("Genesis", 1, 1, "Genesis 1:1", "In the beginning")
        assertEquals(verse, json.decodeFromString<DictionaryVerseDto>(json.encodeToString(verse)))

        val response = DictionaryVersesResponse("H430", total = 2606, verses = listOf(verse))
        assertEquals(response, json.decodeFromString<DictionaryVersesResponse>(json.encodeToString(response)))
    }
}
