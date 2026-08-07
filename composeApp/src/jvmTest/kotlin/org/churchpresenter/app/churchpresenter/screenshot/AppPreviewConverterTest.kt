@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.churchpresenter.app.churchpresenter.TestSingletons
import ui.App
import ui.ConverterTheme
import java.io.File
import kotlin.test.Test

/**
 * The bundled format converter, reached from the Help menu. It ships its own colour scheme rather
 * than following the app theme, so each tab is one image instead of a light/dark pair.
 */
class AppPreviewConverterTest {

    private fun converter(name: String, tab: String?) {
        TestSingletons.latchSkikoHostOs()
        TestSingletons.latchToTestHome()
        runSkikoComposeUiTest(size = Size(1100f, 800f), density = Density(1f)) {
            setContent {
                ConverterTheme {
                    Box(Modifier.size(1100.dp, 800.dp)) { App() }
                }
            }
            waitForIdle()
            tab?.let {
                onNodeWithText(it).performClick()
                waitForIdle()
            }
            captureTo(File("screenshots/previewApp/converter_$name.png"))
        }
    }

    @Test
    fun bibles() = converter("bibles", null)

    @Test
    fun songs() = converter("songs", "Songs")

    @Test
    fun duplicates() = converter("duplicates", "Duplicates")

    @Test
    fun rename() = converter("rename", "Rename")
}
