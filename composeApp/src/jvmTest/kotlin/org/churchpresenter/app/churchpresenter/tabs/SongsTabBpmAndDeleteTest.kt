@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.rightClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The metronome tempo tile, and deleting a song.
 *
 * Both are per-song and both were untested. The tempo is what drives the stage monitor's flashing dot
 * for the band, so it has to be stored **against the song it was set on** — a tempo that lands on the
 * wrong song id means the next song flashes at the last one's speed. It is also clamped: the field
 * accepts only digits, and a value beyond 300 is brought back rather than stored, because the dot's
 * flash interval is computed as `60000 / bpm`.
 *
 * Deleting asks first, and the confirmation names the song and the file it came from — this removes a
 * file from the library folder, so the operator has to be able to see which one before agreeing.
 * Cancelling has to leave it alone, which is asserted on the library rather than on the dialog closing.
 */
class SongsTabBpmAndDeleteTest {

    /** The song id the fixture's first listed song has, read from a throwaway composition. */
    private fun songIdOfFirstSong(): String {
        var id = ""
        songsTab { vm, _ -> id = vm.filteredSongItems.value[0].songId }
        return id
    }

    // ── Driving the two popups ──────────────────────────────────────────────────

    /** Opens the tempo editor by clicking its tile. */
    private fun ComposeUiTest.openBpmEditor() {
        onNodeWithText(BPM_CAPTION, substring = true).performClick()
        waitForIdle()
    }

    /** The tempo field — the only typed field once the editor is open besides the search box. */
    private fun ComposeUiTest.bpmField() = onAllNodes(hasSetTextAction())[1]

    private fun ComposeUiTest.clickPopup(label: String) {
        val nodes = onAllNodesWithText(label)
        nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        waitForIdle()
    }

    /** Right-clicks a row and chooses Delete, which opens the confirmation. */
    private fun ComposeUiTest.startDeleting(title: String) {
        onNodeWithText(title).performMouseInput { rightClick() }
        waitForIdle()
        clickPopup(DELETE)
    }

    // ── The tempo tile ──────────────────────────────────────────────────────────

    @Test
    fun `there is no tempo tile without a stage monitor`() {
        // The tempo exists only to drive the stage monitor's flashing dot, so it is not offered when
        // there is no such screen — a tile that saved a setting nothing reads would be a puzzle.
        songsTab(stageMonitor = false) { vm, _ ->
            vm.selectSong(0)
            waitForIdle()

            assertFalse(showsContaining(BPM_CAPTION), rendered().toString())
        }
    }

    @Test
    fun `a song with no tempo shows zero`() {
        songsTab(stageMonitor = true) { vm, _ ->
            vm.selectSong(0)
            waitForIdle()

            assertTrue(showsContaining("0 $BPM_CAPTION"), rendered().toString())
        }
    }

    @Test
    fun `a configured tempo is shown on the tile`() {
        val songId = songIdOfFirstSong()

        songsTab(songBpm = mapOf(songId to 120), stageMonitor = true) { vm, _ ->
            vm.selectSong(0)
            waitForIdle()

            assertTrue(showsContaining("120 $BPM_CAPTION"), rendered().toString())
        }
    }

    @Test
    fun `a tempo is saved against the song it was set on`() {
        songsTab(stageMonitor = true) { vm, reports ->
            vm.selectSong(0)
            waitForIdle()
            val songId = vm.filteredSongItems.value[0].songId

            openBpmEditor()
            bpmField().performTextClearance()
            bpmField().performTextInput("96")
            clickPopup(OK)

            assertEquals(
                mapOf(songId to 96),
                reports.settingsAfterChange?.songBpm,
                "a tempo on the wrong song id makes the next song flash at this one's speed",
            )
        }
    }

    @Test
    fun `cancelling the editor saves nothing`() {
        songsTab(stageMonitor = true) { vm, reports ->
            vm.selectSong(0)
            waitForIdle()

            openBpmEditor()
            bpmField().performTextClearance()
            bpmField().performTextInput("140")
            clickPopup(CANCEL)

            assertNull(reports.settingsAfterChange, "nothing should have been written")
        }
    }

    @Test
    fun `a tempo beyond the maximum is brought back rather than stored`() {
        songsTab(stageMonitor = true) { vm, reports ->
            vm.selectSong(0)
            waitForIdle()
            val songId = vm.filteredSongItems.value[0].songId

            openBpmEditor()
            bpmField().performTextClearance()
            bpmField().performTextInput("999")
            clickPopup(OK)

            // The flash interval is 60000/bpm, so an unclamped value drives the dot at a nonsense rate.
            assertEquals(300, reports.settingsAfterChange?.songBpm?.get(songId))
        }
    }

    @Test
    fun `the field takes digits only`() {
        songsTab(stageMonitor = true) { vm, reports ->
            vm.selectSong(0)
            waitForIdle()
            val songId = vm.filteredSongItems.value[0].songId

            openBpmEditor()
            bpmField().performTextClearance()
            bpmField().performTextInput("9a0b")
            clickPopup(OK)

            assertEquals(90, reports.settingsAfterChange?.songBpm?.get(songId), "letters are dropped")
        }
    }

    @Test
    fun `an emptied field turns the metronome off rather than failing`() {
        songsTab(stageMonitor = true) { vm, reports ->
            vm.selectSong(0)
            waitForIdle()
            val songId = vm.filteredSongItems.value[0].songId

            openBpmEditor()
            bpmField().performTextClearance()
            clickPopup(OK)

            assertEquals(0, reports.settingsAfterChange?.songBpm?.get(songId), "0 is off")
        }
    }

    // ── Deleting a song ─────────────────────────────────────────────────────────

    @Test
    fun `deleting asks first and names the song`() {
        songsTab { vm, _ ->
            startDeleting("Amazing Grace")

            assertTrue(showsContaining("Amazing Grace"), rendered().toString())
            // The file is named too — this removes it from the library folder.
            assertTrue(
                rendered().any { it.contains(".song") },
                "the operator has to see which file is going: ${rendered()}",
            )
            assertEquals(4, vm.filteredSongItems.value.size, "asking must not have deleted anything")
        }
    }

    @Test
    fun `cancelling a delete leaves the song in the library`() {
        songsTab { vm, _ ->
            startDeleting("Amazing Grace")

            clickPopup(CANCEL)

            assertEquals(4, vm.filteredSongItems.value.size)
            assertTrue(
                vm.filteredSongItems.value.any { it.title == "Amazing Grace" },
                "the song has to still be there",
            )
        }
    }

    @Test
    fun `confirming a delete removes the song and its file`() {
        songsTab { vm, _ ->
            val before = vm.filteredSongItems.value.single { it.title == "Amazing Grace" }
            val file = java.io.File(before.sourceFile)
            assertTrue(file.exists(), "the fixture should have written a real file")

            startDeleting("Amazing Grace")
            clickPopup(DELETE)

            assertFalse(
                vm.filteredSongItems.value.any { it.title == "Amazing Grace" },
                "it should be gone from the library: ${vm.filteredSongItems.value.map { it.title }}",
            )
            assertFalse(file.exists(), "and the file removed from the folder")
        }
    }

    @Test
    fun `deleting one song leaves the others alone`() {
        songsTab { vm, _ ->
            startDeleting("Amazing Grace")
            clickPopup(DELETE)

            assertEquals(3, vm.filteredSongItems.value.size)
            assertTrue(vm.filteredSongItems.value.any { it.title == "Be Thou My Vision" })
        }
    }

    private companion object {
        const val BPM_CAPTION = "BPM"
        const val OK = "OK"
        const val CANCEL = "Cancel"
        const val DELETE = "Delete"
    }
}
