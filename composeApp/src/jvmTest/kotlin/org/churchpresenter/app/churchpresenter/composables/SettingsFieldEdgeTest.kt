@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `StyledTextField`'s placeholder and disabled states.
 *
 * The placeholder is the only thing telling an operator what belongs in an empty box — the STT server
 * url, the website bar, the Q&A entry all rely on it — and it lives in a `decorationBox` that draws it
 * only while the value is empty, so it is its own branch rather than a property of the field.
 *
 * **Not covered: `NumberSettingsTextField`'s arrows.** Both `IconButton`s measure to a zero-size rect
 * in a headless composition — under the parameter's own default modifier
 * (`widthIn(min = 60.dp).width(IntrinsicSize.Max)`) and equally when the component is given a fixed
 * 200dp width — so no click can be delivered to either one and the range clamp behind them is
 * unreachable from a test. Worth a look in the real UI: if the arrow column really does collapse once
 * the text field takes the row's width, those arrows are undeliverable there too and the clamp they
 * guard never runs (every caller feeds the value straight into a setting). Flagged rather than
 * asserted around, since a test written against the collapsed layout would pin the bug in place.
 */
class SettingsFieldEdgeTest {

    @Test
    fun `an empty field shows its placeholder`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                StyledTextField(value = "", onValueChange = {}, placeholder = "e.g. John 3:16")
            }
        }
        onNodeWithText("e.g. John 3:16").assertExists("an empty field has to say what belongs in it")
    }

    @Test
    fun `a filled field shows its value instead of the placeholder`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                StyledTextField(value = "already typed", onValueChange = {}, placeholder = "e.g. John 3:16")
            }
        }
        onNodeWithText("e.g. John 3:16").assertDoesNotExist()
        onNodeWithText("already typed").assertExists()
    }

    @Test
    fun `a label is not a placeholder`() = runComposeUiTest {
        setContent { MaterialTheme { StyledTextField(value = "", onValueChange = {}, label = "URL") } }
        onNodeWithText("URL").assertExists("the label stays whatever the value is")
    }

    @Test
    fun `a multi-line field still shows its placeholder`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                StyledTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "type the announcement",
                    singleLine = false,
                    minLines = 3,
                )
            }
        }
        // The multi-line branch takes a different height modifier; the placeholder must survive it.
        onNodeWithText("type the announcement").assertExists()
    }

    @Test
    fun `a disabled field cannot be typed into but stays readable`() = runComposeUiTest {
        setContent {
            MaterialTheme { StyledTextField(value = "locked", onValueChange = {}, enabled = false) }
        }
        // A disabled BasicTextField drops its set-text action entirely rather than publishing a
        // disabled one, so "cannot be typed into" is the absence of any typable node.
        assertTrue(
            onAllNodes(hasSetTextAction()).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty(),
        )
        onNodeWithText("locked").assertExists("the operator still has to be able to read it")
    }
}
