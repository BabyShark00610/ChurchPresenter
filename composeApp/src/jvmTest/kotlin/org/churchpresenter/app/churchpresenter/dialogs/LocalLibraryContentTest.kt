@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.StockMediaClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalLibraryContentTest {

    private class Result {
        var selected: String? = null
        var dismissed = 0
    }

    private fun dialog(
        mediaType: StockMediaClient.StockMediaType = StockMediaClient.StockMediaType.VIDEO,
        downloadedFiles: List<File> = emptyList(),
        bundledFileNames: List<String> = emptyList(),
        block: ComposeUiTest.(Result) -> Unit,
    ) {
        val result = Result()
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    LocalLibraryDialogContent(
                        mediaType = mediaType,
                        downloadedFiles = downloadedFiles,
                        bundledFileNames = bundledFileNames,
                        onDismiss = { result.dismissed++ },
                        onMediaSelected = { result.selected = it },
                    )
                }
            }
            block(result)
        }
    }

    @Test
    fun `an empty library shows the empty-state message`() = dialog {
        onNodeWithText("Nothing downloaded yet — use Search to add photos or videos.").assertExists()
    }

    @Test
    fun `downloaded videos are listed by name`() = dialog(downloadedFiles = listOf(File("worship-loop.mp4"))) {
        onNodeWithText("worship-loop.mp4").assertExists()
        onNodeWithText("Nothing downloaded yet — use Search to add photos or videos.").assertDoesNotExist()
    }

    @Test
    fun `typing in the search box filters the list`() = dialog(
        downloadedFiles = listOf(File("worship-loop.mp4"), File("countdown.mp4")),
    ) {
        onNodeWithText("Filter by file name…").performTextInput("worship")

        onNodeWithText("worship-loop.mp4").assertExists()
        onNodeWithText("countdown.mp4").assertDoesNotExist()
    }

    @Test
    fun `a search matching nothing shows the empty state`() = dialog(downloadedFiles = listOf(File("worship-loop.mp4"))) {
        onNodeWithText("Filter by file name…").performTextInput("no such file")

        onNodeWithText("Nothing downloaded yet — use Search to add photos or videos.").assertExists()
    }

    @Test
    fun `clicking a downloaded entry selects it and dismisses the dialog`() = dialog(
        downloadedFiles = listOf(File("worship-loop.mp4")),
    ) { result ->
        onNodeWithText("worship-loop.mp4").performClick()

        assertEquals(File("worship-loop.mp4").absolutePath, result.selected)
        assertEquals(1, result.dismissed)
    }

    @Test
    fun `Cancel dismisses without selecting anything`() = dialog(downloadedFiles = listOf(File("worship-loop.mp4"))) { result ->
        onNodeWithText("Cancel").performClick()

        assertNull(result.selected)
        assertEquals(1, result.dismissed)
    }

    @Test
    fun `the photo library title is used for photos`() = dialog(mediaType = StockMediaClient.StockMediaType.PHOTO) {
        onNodeWithText("Image Library").assertExists()
    }

    @Test
    fun `the video library title is used for videos`() = dialog(mediaType = StockMediaClient.StockMediaType.VIDEO) {
        onNodeWithText("Video Library").assertExists()
    }
}
