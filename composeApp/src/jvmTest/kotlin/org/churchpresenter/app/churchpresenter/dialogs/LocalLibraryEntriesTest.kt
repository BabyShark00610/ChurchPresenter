package org.churchpresenter.app.churchpresenter.dialogs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalLibraryEntriesTest {

    @Test
    fun `downloaded files come before bundled entries`() {
        val entries = libraryEntries(
            downloadedFiles = listOf(File("sunset.jpg")),
            bundledFileNames = listOf("mountains.jpg"),
            searchQuery = "",
        )

        assertEquals(listOf("sunset.jpg", "mountains.jpg"), entries.map { it.name })
    }

    @Test
    fun `a bundled entry already downloaded under the same name is not duplicated`() {
        val entries = libraryEntries(
            downloadedFiles = listOf(File("sunset.jpg")),
            bundledFileNames = listOf("sunset.jpg", "mountains.jpg"),
            searchQuery = "",
        )

        assertEquals(listOf("sunset.jpg", "mountains.jpg"), entries.map { it.name })
        assertTrue(entries.first { it.name == "sunset.jpg" } is DownloadedEntry, "the downloaded copy wins over the bundled one")
    }

    @Test
    fun `bundled entries are sorted by name`() {
        val entries = libraryEntries(
            downloadedFiles = emptyList(),
            bundledFileNames = listOf("zebra.jpg", "apple.jpg"),
            searchQuery = "",
        )

        assertEquals(listOf("apple.jpg", "zebra.jpg"), entries.map { it.name })
    }

    @Test
    fun `a blank search query keeps everything`() {
        val entries = libraryEntries(
            downloadedFiles = listOf(File("sunset.jpg")),
            bundledFileNames = listOf("mountains.jpg"),
            searchQuery = "   ",
        )

        assertEquals(2, entries.size)
    }

    @Test
    fun `a search query filters by name, case-insensitively`() {
        val entries = libraryEntries(
            downloadedFiles = listOf(File("Sunset.jpg")),
            bundledFileNames = listOf("Mountains.jpg"),
            searchQuery = "sun",
        )

        assertEquals(listOf("Sunset.jpg"), entries.map { it.name })
    }

    @Test
    fun `a search query matching nothing yields an empty list`() {
        val entries = libraryEntries(
            downloadedFiles = listOf(File("sunset.jpg")),
            bundledFileNames = emptyList(),
            searchQuery = "no such file",
        )

        assertTrue(entries.isEmpty())
    }

    @Test
    fun `a downloaded entry's key is its absolute path`() {
        val file = File("sunset.jpg")
        val entry = DownloadedEntry(file)

        assertEquals(file.absolutePath, entry.key)
    }

    @Test
    fun `two bundled entries with the same name have the same key`() {
        assertEquals(BundledEntry("sunset.jpg").key, BundledEntry("sunset.jpg").key)
    }

    @Test
    fun `a downloaded and a bundled entry never collide on key even with the same name`() {
        val downloaded = DownloadedEntry(File("sunset.jpg"))
        val bundled = BundledEntry("sunset.jpg")

        assertTrue(downloaded.key != bundled.key, "LazyVerticalGrid keys its items by this; a collision would confuse recomposition")
    }
}
