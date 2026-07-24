package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * The reusable search box used across the song and Bible tabs.
 *
 * It shows a label and seeds its field from [initialText]; both must reach the screen or the
 * operator can't tell what they're searching or that a prior query is still applied.
 */
@OptIn(ExperimentalTestApi::class)
class SearchTextFieldTest {

    @Test
    fun `the label and the initial text are both shown`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                SearchTextField(label = "Search songs", initialText = "grace", onValueChange = {})
            }
        }
        onNodeWithText("Search songs", substring = true).assertExists("the field must name what it searches")
        onNodeWithText("grace", substring = true).assertExists("a pre-filled query must be visible, not hidden")
    }
}
