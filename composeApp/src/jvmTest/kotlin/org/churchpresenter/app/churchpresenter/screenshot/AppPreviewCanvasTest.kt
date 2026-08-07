@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import org.churchpresenter.app.churchpresenter.tabs.Tabs
import kotlin.test.Test

class AppPreviewCanvasTest {

    @Test
    fun `the canvas tab`() = appPreview("canvas", Tabs.CANVAS)
}
