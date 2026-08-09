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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Line navigation when a slide holds more than one line.
 *
 * `navigateNextLine`/`navigatePreviousLine` take the step as a parameter — the number of lines the
 * slide shows — so one press moves to the next SLIDE rather than the next line. The default of 1 is
 * the original behaviour and is what every pre-existing caller and test relies on, so both the
 * stepped and the unstepped forms are exercised here.
 */
class SongsViewModelLineStepTest {

    private lateinit var dir: File
    private val created = mutableListOf<SongsViewModel>()

    @BeforeTest
    fun createLibrary() {
        dir = Files.createTempDirectory("cp-songs-line-step-test").toFile()
        // Verse 1 has five lines — deliberately not a multiple of 2 or 3, so the short final group
        // is covered rather than assumed.
        writeSong(
            lyrics = listOf(
                "[Verse 1]", "L1", "L2", "L3", "L4", "L5",
                "[Verse 2]", "V2 L1", "V2 L2",
            ),
        )
    }

    @AfterTest
    fun cleanUp() {
        created.forEach { runCatching { it.dispose() } }
        created.clear()
        dir.deleteRecursively()
    }

    private fun writeSong(lyrics: List<String>) {
        val target = File(File(dir, "Hymnal"), "0001 - Stepping.song")
        SongFileParser().writeSongFile(
            SongItem(number = "0001", title = "Stepping", songbook = "Hymnal", lyrics = lyrics),
            target.absolutePath,
        )
    }

    private fun viewModel(): SongsViewModel {
        val vm = SongsViewModel(
            AppSettings(songSettings = SongSettings(storageDirectory = dir.absolutePath)),
            dispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            enableFolderWatcher = false,
        )
        created.add(vm)
        if (vm.filteredSongItems.value.isEmpty()) throw AssertionError("songs did not load synchronously")
        vm.selectSong(0)
        return vm
    }

    @Test
    fun `the fixture really has a five-line first verse`() {
        val sections = viewModel().getLyricSections()
        assertEquals(listOf("L1", "L2", "L3", "L4", "L5"), sections[0].lines)
    }

    @Test
    fun `a step of two advances two lines at a time`() {
        val vm = viewModel()

        assertTrue(vm.navigateNextLine(2))
        assertEquals(2, vm.selectedLineIndex.value)
        assertTrue(vm.navigateNextLine(2))
        assertEquals(4, vm.selectedLineIndex.value)
    }

    @Test
    fun `no step given still moves one line, as every existing caller expects`() {
        val vm = viewModel()

        assertTrue(vm.navigateNextLine())

        assertEquals(1, vm.selectedLineIndex.value)
    }

    @Test
    fun `a step landing on the short final group crosses to the next section instead`() {
        // From line 4 a step of 2 would run off the end of a 5-line verse. The slide showing L5 is
        // already on screen at that point, so the press has to move on rather than clamp and appear
        // to do nothing.
        val vm = viewModel()
        vm.navigateNextLine(2) // -> 2
        vm.navigateNextLine(2) // -> 4, the slide holding just L5

        assertTrue(vm.navigateNextLine(2))

        assertEquals(1, vm.selectedSectionIndex.value)
        assertEquals(0, vm.selectedLineIndex.value)
    }

    @Test
    fun `a step of two steps back two lines`() {
        val vm = viewModel()
        vm.navigateNextLine(2)
        vm.navigateNextLine(2) // -> 4

        assertTrue(vm.navigatePreviousLine(2))

        assertEquals(2, vm.selectedLineIndex.value)
    }

    @Test
    fun `stepping back never lands before the first line`() {
        val vm = viewModel()
        vm.navigateNextLine(3) // -> 3

        assertTrue(vm.navigatePreviousLine(3))

        assertEquals(0, vm.selectedLineIndex.value)
    }

    @Test
    fun `stepping back into a previous section lands on that section's last SLIDE`() {
        // Not its last LINE: with a step of 2 the final slide of a five-line verse starts at L5's
        // own index 4, and landing anywhere else would show a slide the operator never stepped
        // forward through.
        val vm = viewModel()
        vm.navigateNextSection() // section 1, line 0

        assertTrue(vm.navigatePreviousLine(2))

        assertEquals(0, vm.selectedSectionIndex.value)
        assertEquals(4, vm.selectedLineIndex.value)
    }

    @Test
    fun `a step of zero is treated as one rather than freezing navigation`() {
        // The step is derived from a hand-editable setting; a zero would otherwise leave the arrow
        // key doing nothing at all.
        val vm = viewModel()

        assertTrue(vm.navigateNextLine(0))

        assertEquals(1, vm.selectedLineIndex.value)
    }

    @Test
    fun `stepping forward off the end of the last section reports there is nowhere to go`() {
        // Verse 2 holds two lines, so at a step of 2 its first slide is also its last: one press
        // from the start of the song's final section must already have nowhere left to go.
        val vm = viewModel()
        vm.navigateNextSection() // section 1, line 0 — the whole of Verse 2 on one slide

        assertFalse(vm.navigateNextLine(2))

        assertEquals(1, vm.selectedSectionIndex.value, "and the failed press moves nothing")
        assertEquals(0, vm.selectedLineIndex.value)
    }
}
