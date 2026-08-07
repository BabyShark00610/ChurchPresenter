@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import org.churchpresenter.app.churchpresenter.tabs.Tabs
import kotlin.test.Test

class AppPreviewMediaTest {

    @Test
    fun `the media tab`() = appPreview("media", Tabs.MEDIA) {
        onAllNodes(hasText("Welcome Loop", substring = true))[0].performTouchInput { doubleClick() }
        waitForIdle()
        repeat(60) {
            Snapshot.sendApplyNotifications()
            waitForIdle()
            Thread.sleep(50)
        }
    }
}
