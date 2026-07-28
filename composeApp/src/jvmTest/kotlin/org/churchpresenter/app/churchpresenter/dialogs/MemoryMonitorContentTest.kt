@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryMonitorContentTest {

    private val mb = 1024L * 1024L

    private fun dialog(
        heapUsed: Long = 100L * mb,
        heapCommitted: Long = 200L * mb,
        heapMax: Long = 500L * mb,
        nonHeapUsed: Long = 50L * mb,
        nonHeapCommitted: Long = 80L * mb,
        gcCount: Long = 5L,
        gcTimeMs: Long = 120L,
        history: List<Long> = listOf(10L * mb, 20L * mb, 30L * mb),
        block: ComposeUiTest.(forceGcCalls: () -> Int) -> Unit,
    ) {
        var forceGcCalls = 0
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    MemoryMonitorDialogContent(
                        heapUsed = heapUsed,
                        heapCommitted = heapCommitted,
                        heapMax = heapMax,
                        nonHeapUsed = nonHeapUsed,
                        nonHeapCommitted = nonHeapCommitted,
                        gcCount = gcCount,
                        gcTimeMs = gcTimeMs,
                        history = history,
                        onForceGc = { forceGcCalls++ },
                    )
                }
            }
            block { forceGcCalls }
        }
    }

    @Test
    fun `heap figures are shown formatted in megabytes`() = dialog {
        onNodeWithText("100 MB").assertExists()
        onNodeWithText("200 MB").assertExists()
        onNodeWithText("500 MB").assertExists()
    }

    @Test
    fun `non-heap figures are shown formatted in megabytes`() = dialog {
        onNodeWithText("50 MB").assertExists()
        onNodeWithText("80 MB").assertExists()
    }

    @Test
    fun `an unknown heap max is shown as a dash rather than 0 MB`() = dialog(heapMax = 0L) {
        onNodeWithText("—").assertExists()
    }

    @Test
    fun `gc count and time are shown together`() = dialog {
        onNodeWithText("5 (120 ms)").assertExists()
    }

    @Test
    fun `clicking Force GC calls the handler`() = dialog { forceGcCalls ->
        onNodeWithText("Force GC").performClick()
        assertEquals(1, forceGcCalls())
    }
}
