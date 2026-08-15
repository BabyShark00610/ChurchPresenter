package org.churchpresenter.app.churchpresenter.viewmodel

import kotlinx.coroutines.Dispatchers
import org.churchpresenter.app.churchpresenter.data.SongFileParser
import org.churchpresenter.app.churchpresenter.data.SongItem
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.SongSettings
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What goes live when NO section is selected.
 *
 * `selectedSectionIndex` is -1 whenever the operator is only browsing — a plain click in the song
 * list parks it there so looking at a song does not disturb what is on screen — and
 * `getSelectedLyricSection` falls back to the whole song for it. That fallback used to be built from
 * `SongItem.lyrics`, which is the FILE as written: section markers and chord brackets included. A
 * `[Chorus]` therefore reached the projector as if it were a line of the song, and in line mode it
 * was one more line to step onto.
 */
class SelectedSongFallbackTest {

    private lateinit var dir: File
    private val created = mutableListOf<SongsViewModel>()

    @BeforeTest
    fun createLibrary() {
        dir = Files.createTempDirectory("cp-selected-song-fallback").toFile()
        SongFileParser().writeSongFile(
            SongItem(
                number = "0001", title = "Marked Up", songbook = "Hymnal",
                lyrics = listOf(
                    "[Verse 1]", "[G]Verse one line", "Verse two line",
                    "[Chorus]", "Chorus line",
                ),
                secondaryLyrics = listOf(
                    "[Verse 1]", "Втора строка одна", "Втора строка два",
                    "[Chorus]", "Припев строка",
                ),
            ),
            File(File(dir, "Hymnal"), "0001 - Marked Up.song").absolutePath,
        )
    }

    @AfterTest
    fun cleanUp() {
        created.forEach { runCatching { it.dispose() } }
        created.clear()
        dir.deleteRecursively()
    }

    private fun viewModel(): SongsViewModel {
        val vm = SongsViewModel(
            AppSettings(songSettings = SongSettings(storageDirectory = dir.absolutePath, titleSlideEnabled = false)),
            dispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            enableFolderWatcher = false,
        )
        created.add(vm)
        if (vm.filteredSongItems.value.isEmpty()) throw AssertionError("songs did not load synchronously")
        vm.selectSong(0)
        vm.selectSection(-1) // browsing: exactly what a click in the song list leaves behind
        return vm
    }

    @Test
    fun `the fallback really is what a browsing selection returns`() {
        val section = viewModel().getSelectedLyricSection()
        assertTrue(section != null && section.lines.size > 2, "expected the whole song, not one verse")
    }

    @Test
    fun `no section marker reaches the screen`() {
        val lines = viewModel().getSelectedLyricSection()!!.lines
        assertTrue(
            lines.none { it.contains("[Verse") || it.contains("[Chorus") },
            "a header must never be presentable text, but got: $lines",
        )
    }

    @Test
    fun `no chord marker reaches the screen either`() {
        val lines = viewModel().getSelectedLyricSection()!!.lines
        assertTrue(lines.none { it.contains("[G]") }, "chords are stripped for the audience: $lines")
    }

    @Test
    fun `it is the song's words, in order`() {
        assertEquals(
            listOf("Verse one line", "Verse two line", "Chorus line"),
            viewModel().getSelectedLyricSection()!!.lines,
        )
    }

    @Test
    fun `the second language comes through the same way`() {
        assertEquals(
            listOf("Втора строка одна", "Втора строка два", "Припев строка"),
            viewModel().getSelectedLyricSection()!!.secondaryLines,
        )
    }

    @Test
    fun `the fallback still belongs to its song, so it keeps that song's look`() {
        assertEquals("Hymnal::0001", viewModel().getSelectedLyricSection()!!.songId)
    }
}
