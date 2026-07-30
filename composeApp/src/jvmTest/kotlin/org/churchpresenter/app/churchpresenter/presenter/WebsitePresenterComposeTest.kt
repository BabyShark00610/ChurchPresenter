@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.presenter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.churchpresenter.app.churchpresenter.utils.Constants
import kotlin.test.Test

class WebsitePresenterComposeTest {

    @Test
    fun `EmbeddedWebView with a blank url renders nothing and does not crash`() = runComposeUiTest {
        setContent { EmbeddedWebView(url = "") }
    }

    @Test
    fun `EmbeddedWebView with no CefManager engine renders nothing and does not crash`() = runComposeUiTest {
        // CefManager.init() is never called in tests, so CefManager.initialized stays false and
        // this always takes the early-return path — proving that path is safe on its own.
        setContent { EmbeddedWebView(url = "https://example.com") }
    }

    @Test
    fun `rememberWebNavController does not crash and yields a usable controller`() = runComposeUiTest {
        lateinit var controller: WebNavController
        setContent { controller = rememberWebNavController() }
        controller.goBack()
    }

    @Test
    fun `WebsitePresenter in key mode is a plain white output`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(200.dp, 200.dp)) {
                WebsitePresenter(url = "https://example.com", outputRole = Constants.OUTPUT_ROLE_KEY, modifier = Modifier.testTag("ws"))
            }
        }
        val pixels = onNodeWithTag("ws").captureToImage().toPixelMap()
        assertColorAt(pixels, 100, 100, Color.White)
    }

    @Test
    fun `WebsitePresenter in normal mode with no audio device does not crash`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(200.dp, 200.dp)) {
                WebsitePresenter(url = "https://example.com", audioDeviceId = "")
            }
        }
    }

    @Test
    fun `WebsitePresenter in normal mode with an audio device does not crash`() = runComposeUiTest {
        setContent {
            Box(Modifier.size(200.dp, 200.dp)) {
                WebsitePresenter(url = "https://example.com", audioDeviceId = "some-sink")
            }
        }
    }
}
