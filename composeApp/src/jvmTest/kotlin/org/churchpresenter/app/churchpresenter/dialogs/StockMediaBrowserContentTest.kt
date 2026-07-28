@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.stock_photo_browse_photos_title
import churchpresenter.composeapp.generated.resources.stock_photo_search_placeholder_photo
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.churchpresenter.app.churchpresenter.data.StockMediaClient
import org.churchpresenter.app.churchpresenter.viewmodel.StockMediaViewModel
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StockMediaBrowserContentTest {

    @BeforeTest
    fun stubClient() {
        mockkObject(StockMediaClient)
        coEvery { StockMediaClient.fetchThumbnailBytes(any(), any()) } returns null
    }

    @AfterTest
    fun cleanUp() {
        unmockkObject(StockMediaClient)
    }

    private fun settle() = repeat(2) { SwingUtilities.invokeAndWait { } }

    private fun item(id: String) = StockMediaClient.StockMediaItem(
        id = id,
        source = StockMediaClient.StockSource.PEXELS,
        isVideo = false,
        thumbnailUrl = "https://example.test/$id/thumb.jpg",
        downloadUrl = "https://example.test/$id/full.jpg",
    )

    private fun searchReturns(outcome: StockMediaClient.SearchOutcome) {
        coEvery { StockMediaClient.search(any(), any(), any(), any(), any(), any()) } returns outcome
    }

    private fun dialog(
        pexelsApiKey: String = "",
        block: ComposeUiTest.(dismissed: () -> Int, downloaded: () -> String?) -> Unit,
    ) {
        var dismissed = 0
        var downloaded: String? = null
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var key by remember { mutableStateOf(pexelsApiKey) }
                    var tab by remember { mutableStateOf(0) }
                    val pexelsVm = remember { StockMediaViewModel(StockMediaClient.StockMediaType.PHOTO, StockMediaClient.StockSource.PEXELS) }
                    val pixabayVm = remember { StockMediaViewModel(StockMediaClient.StockMediaType.PHOTO, StockMediaClient.StockSource.PIXABAY) }
                    StockMediaBrowserDialogContent(
                        titleRes = Res.string.stock_photo_browse_photos_title,
                        searchPlaceholderRes = Res.string.stock_photo_search_placeholder_photo,
                        pexelsViewModel = pexelsVm,
                        pixabayViewModel = pixabayVm,
                        pexelsApiKey = key,
                        onPexelsApiKeyChange = { key = it },
                        pixabayApiKey = "",
                        onPixabayApiKeyChange = {},
                        selectedTab = tab,
                        onSelectedTabChange = { tab = it },
                        onDismiss = { dismissed++ },
                        onDownloadedAndClose = { downloaded = it },
                    )
                }
            }
            waitForIdle()
            block({ dismissed }, { downloaded })
        }
    }

    @Test
    fun `with no api key the search box is replaced with a hint`() = dialog { _, _ ->
        onNodeWithText("Add your Pexels API key above to search.").assertExists()
        onNodeWithText("Search for photos…").assertDoesNotExist()
    }

    @Test
    fun `typing an api key reveals the search box`() = dialog { _, _ ->
        onAllNodes(hasSetTextAction())[0].performTextInput("a-key")
        waitForIdle()

        onNodeWithText("Search for photos…").assertExists()
    }

    @Test
    fun `a search with results shows them and offers Load more`() = dialog(pexelsApiKey = "a-key") { _, _ ->
        searchReturns(StockMediaClient.SearchOutcome.Success(listOf(item("1"), item("2")), hasMore = true))

        onNodeWithText("Search for photos…").performTextInput("worship")
        onNodeWithText("worship").performImeAction()
        settle()
        waitForIdle()

        onNodeWithText("Load more").assertExists()
    }

    @Test
    fun `an invalid api key error is shown after a failed search`() = dialog(pexelsApiKey = "bad-key") { _, _ ->
        searchReturns(StockMediaClient.SearchOutcome.InvalidKey)

        onNodeWithText("Search for photos…").performTextInput("worship")
        onNodeWithText("worship").performImeAction()
        settle()
        waitForIdle()

        onNodeWithText("Invalid API key").assertExists()
    }

    @Test
    fun `a network error is shown after a failed search`() = dialog(pexelsApiKey = "a-key") { _, _ ->
        searchReturns(StockMediaClient.SearchOutcome.NetworkError)

        onNodeWithText("Search for photos…").performTextInput("worship")
        onNodeWithText("worship").performImeAction()
        settle()
        waitForIdle()

        onNodeWithText("Network error — check your connection").assertExists()
    }

    @Test
    fun `a search with no matches shows the no-results message`() = dialog(pexelsApiKey = "a-key") { _, _ ->
        searchReturns(StockMediaClient.SearchOutcome.Success(emptyList(), hasMore = false))

        onNodeWithText("Search for photos…").performTextInput("no such thing")
        onNodeWithText("no such thing").performImeAction()
        settle()
        waitForIdle()

        onNodeWithText("No results found").assertExists()
    }

    @Test
    fun `switching tabs switches the key label shown`() = dialog { _, _ ->
        onNodeWithText("Pixabay").performClick()
        onNodeWithText("PIXABAY API KEY").assertExists()
    }

    @Test
    fun `Cancel dismisses the dialog`() = dialog { dismissed, _ ->
        onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed())
    }
}
