package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The media/slideshow settings tab. It's pure View wiring over [AppSettings] — every control reads a
 * field and, on change, hands back a copy with that one field replaced. The value is that those
 * copy-lambdas actually target the right nested field: a mis-wired one would silently write to the
 * wrong setting. Rather than assert pixels, this drives the real state round-trip (render → toggle →
 * recompose) and checks the intended field, and only that field, changed.
 */
@OptIn(ExperimentalTestApi::class)
class MediaSettingsTabTest {

    @Test
    fun `both settings sections render`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MediaSettingsTab(settings = AppSettings(), onSettingsChange = {})
            }
        }
        onNodeWithText("Media Slideshow Settings", substring = true)
            .assertExists("the slideshow section header must render")
        onNodeWithText("Transition Settings", substring = true)
            .assertExists("the transition section header must render")
    }

    @Test
    fun `toggling the loop checkbox flips only the looping flag`() = runComposeUiTest {
        var current = AppSettings()
        val before = current
        setContent {
            MaterialTheme {
                var state by remember { mutableStateOf(current) }
                MediaSettingsTab(
                    settings = state,
                    onSettingsChange = { transform -> state = transform(state); current = state },
                )
            }
        }

        // The Loop checkbox is the first toggleable control on the tab.
        onAllNodes(isToggleable())[0].performClick()

        assertEquals(
            !before.pictureSettings.isLooping,
            current.pictureSettings.isLooping,
            "clicking Loop must flip isLooping",
        )
        assertEquals(
            before.pictureSettings.copy(isLooping = current.pictureSettings.isLooping),
            current.pictureSettings,
            "no picture setting other than isLooping may change",
        )
    }
}
