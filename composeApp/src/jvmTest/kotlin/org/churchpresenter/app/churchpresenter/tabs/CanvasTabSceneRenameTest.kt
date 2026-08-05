@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Renaming a scene from the Canvas tab's scene list.
 *
 * The row swaps its label for an inline editor and back, and the only thing that actually commits the
 * new name is the tick beside the field — there is no Enter handler and no commit on focus loss. So a
 * rename that never reaches [org.churchpresenter.app.churchpresenter.viewmodel.SceneViewModel.renameScene]
 * leaves the operator looking at the name they typed while the scene, the schedule and the saved
 * `scenes.json` all still hold the old one. That divergence is what these tests are for.
 *
 * **Locating the tick.** Its icon is declared with `contentDescription = null`, so it cannot be
 * addressed by label. While the editor is open it is the only button in the tab carrying neither text
 * nor a content description — every other one has a label ("Add source", "Go Live") or is a glyph
 * button whose symbol is its text. [confirmRenameButton] asserts that uniqueness rather than assuming
 * it, so if another unlabelled button ever appears the test says so instead of clicking the wrong
 * thing.
 */
class CanvasTabSceneRenameTest {

    private companion object {
        /** The rename pencil's tooltip and content description — `canvas_rename_scene`. */
        const val RENAME = "Rename"
    }

    /** The unlabelled tick that commits the rename. Fails loudly if it is no longer the only one. */
    private fun ComposeUiTest.confirmRenameButton(): SemanticsNodeInteraction {
        val unlabelled = onAllNodes(
            hasClickAction() and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.Text) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription) and
                SemanticsMatcher.keyNotDefined(SemanticsProperties.EditableText)
        )
        assertEquals(
            1,
            unlabelled.fetchSemanticsNodes().size,
            "the confirm tick is identified by being the only unlabelled button while renaming",
        )
        return unlabelled[0]
    }

    private fun ComposeUiTest.editorText(): String =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .fetchSemanticsNodes()
            .first()
            .config[SemanticsProperties.EditableText]
            .text

    private fun ComposeUiTest.isEditorOpen(): Boolean =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    @Test
    fun `the rename control opens an editor seeded with the name already on the scene`() =
        canvasTab(seed = { addScene("Opening Hymn") }) { _, _ ->
            waitForIdle()

            onNodeWithContentDescription(RENAME).performClick()
            waitForIdle()

            // Seeded, not blank: renaming is usually a small correction, and an empty field would
            // make the operator retype a name they only wanted to adjust.
            assertEquals("Opening Hymn", editorText())
        }

    @Test
    fun `confirming commits the typed name to the scene`() =
        canvasTab(seed = { addScene("Old Name") }) { vm, _ ->
            waitForIdle()
            onNodeWithContentDescription(RENAME).performClick()
            waitForIdle()

            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))[0]
                .performTextClearance()
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))[0]
                .performTextInput("New Name")
            confirmRenameButton().performClick()
            waitForIdle()

            assertEquals("New Name", vm.scenes.single().name, "the scene itself, not just the field")
        }

    @Test
    fun `confirming closes the editor and gives the rename control back`() =
        canvasTab(seed = { addScene("Old Name") }) { _, _ ->
            waitForIdle()
            onNodeWithContentDescription(RENAME).performClick()
            waitForIdle()

            confirmRenameButton().performClick()
            waitForIdle()

            // A row left in edit mode would swallow the next rename: the pencil is gone while it is
            // open, so the operator has no way back to it.
            assertEquals(false, isEditorOpen(), "the editor closed")
            onNodeWithContentDescription(RENAME).assertExists()
        }

    @Test
    fun `confirming without typing anything keeps the existing name`() =
        canvasTab(seed = { addScene("Unchanged") }) { vm, _ ->
            waitForIdle()
            onNodeWithContentDescription(RENAME).performClick()
            waitForIdle()

            confirmRenameButton().performClick()
            waitForIdle()

            // The commit is unconditional, so this only holds because the field was seeded. If the
            // seeding above ever regresses, this is what turns a blank field into a blank scene name.
            assertEquals("Unchanged", vm.scenes.single().name)
        }

    @Test
    fun `renaming one scene leaves the others alone`() =
        canvasTab(seed = { addScene("First"); addScene("Second") }) { vm, _ ->
            waitForIdle()

            // Two rows, so two pencils; the second belongs to "Second".
            val pencils = onAllNodesWithContentDescription(RENAME)
            assertEquals(2, pencils.fetchSemanticsNodes().size, "one rename control per scene")
            pencils[1].performClick()
            waitForIdle()
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))[0]
                .performTextClearance()
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))[0]
                .performTextInput("Renamed")
            confirmRenameButton().performClick()
            waitForIdle()

            // The commit is keyed by scene id, and this is what proves it is not "whichever row is
            // selected" or "the first one".
            assertEquals(listOf("First", "Renamed"), vm.scenes.map { it.name })
        }
}
