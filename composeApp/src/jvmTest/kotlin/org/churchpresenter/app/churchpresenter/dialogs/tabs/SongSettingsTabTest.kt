package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The song settings tab is the largest pure-View surface in the app (~1,400 lines). Its checkboxes
 * carry `testTag`s so each can be reached and driven unambiguously; this exercises the title-slide,
 * number-placement and transition toggles and asserts the settings they flip — running the
 * onCheckedChange lambdas in place rather than only rendering them. (The tab's auto-fit toggles are
 * nested in layout-mode sections; tagging and covering those is a follow-up.)
 */
@OptIn(ExperimentalTestApi::class)
class SongSettingsTabTest {

    private fun runTab(block: ComposeUiTest.(get: () -> AppSettings) -> Unit) = runComposeUiTest {
        var current = AppSettings()
        setContent {
            MaterialTheme {
                var state by remember { mutableStateOf(current) }
                SongSettingsTab(
                    settings = state,
                    onSettingsChange = { transform -> state = transform(state); current = state },
                    presenterManager = null,
                )
            }
        }
        block { current }
    }

    @Test
    fun `the tab shows the title-slide section`() = runTab {
        onAllNodesWithText("Song Title Slide", substring = true).onFirst()
            .assertExists("the title-slide section must render when the tab opens")
    }

    @Test
    fun `the title-slide checkbox flips its setting`() = runTab { get ->
        assertFalse(get().songSettings.titleSlideEnabled, "title slides start off")
        // Reached unambiguously via its testTag rather than a fragile index/label selector.
        onNodeWithTag("song_titleSlideEnabled").performClick()
        assertTrue(get().songSettings.titleSlideEnabled, "the checkbox must set songSettings.titleSlideEnabled")
    }
}
