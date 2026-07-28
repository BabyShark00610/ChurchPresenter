@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import org.churchpresenter.app.churchpresenter.data.BibleCatalogOutcome
import org.churchpresenter.app.churchpresenter.data.BibleInstallOutcome
import org.churchpresenter.app.churchpresenter.data.BibleModule
import org.churchpresenter.app.churchpresenter.data.BibleSource
import org.churchpresenter.app.churchpresenter.data.BibleSourceId
import org.churchpresenter.app.churchpresenter.data.InstallProgress
import org.churchpresenter.app.churchpresenter.viewmodel.BibleCatalogViewModel
import java.io.File
import java.nio.file.Files
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BibleCatalogBrowserContentTest {

    private lateinit var dir: File
    private val created = mutableListOf<BibleCatalogViewModel>()

    @BeforeTest
    fun createDir() {
        dir = Files.createTempDirectory("cp-bible-catalog-dialog-test").toFile()
    }

    @AfterTest
    fun cleanUp() {
        created.forEach { runCatching { it.dispose() } }
        created.clear()
        dir.deleteRecursively()
    }

    private class FakeSource(
        var catalogOutcome: BibleCatalogOutcome = BibleCatalogOutcome.Success(emptyList()),
        var installOutcome: BibleInstallOutcome = BibleInstallOutcome.Success(File("x.spb"), "Installed Title", 66, "public domain"),
    ) : BibleSource {
        override val sourceId = BibleSourceId.EBIBLE
        override suspend fun catalog(nowMillis: Long) = catalogOutcome
        override suspend fun install(module: BibleModule, targetDir: File, onProgress: (InstallProgress) -> Unit) = installOutcome
    }

    private fun module(
        identifier: String = "ACV",
        displayName: String = "A Conservative Version",
        language: String = "ENG",
        fileStem: String = "ENG_ACV",
    ) = BibleModule(
        sourceId = BibleSourceId.EBIBLE,
        downloadKey = identifier,
        language = language,
        identifier = identifier,
        displayName = displayName,
        fileStem = fileStem,
    )

    private fun settle() = repeat(2) { SwingUtilities.invokeAndWait { } }

    private fun dialog(
        catalogOutcome: BibleCatalogOutcome = BibleCatalogOutcome.Success(emptyList()),
        installOutcome: BibleInstallOutcome = BibleInstallOutcome.Success(File("x.spb"), "Installed Title", 66, "public domain"),
        block: ComposeUiTest.(dismissed: () -> Int, installed: () -> String?) -> Unit,
    ) {
        val source = FakeSource(catalogOutcome, installOutcome)
        val vm = BibleCatalogViewModel(source, dir.absolutePath, dispatcher = Dispatchers.Unconfined).also { created.add(it) }
        var dismissed = 0
        var installedFileName: String? = null
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    BibleCatalogBrowserDialogContent(
                        viewModels = listOf(vm),
                        tabLabels = listOf("eBible.org"),
                        onDismiss = { dismissed++ },
                        onBibleInstalled = { installedFileName = it },
                    )
                }
            }
            settle()
            waitForIdle()
            block({ dismissed }, { installedFileName })
        }
    }

    @Test
    fun `an empty catalogue shows the empty-state message`() = dialog { _, _ ->
        onNodeWithText("No Bibles match this filter").assertExists()
    }

    @Test
    fun `a network error shows its own message and a retry button`() = dialog(catalogOutcome = BibleCatalogOutcome.NetworkError) { _, _ ->
        onNodeWithText("Couldn't reach the Bible archive — check your connection").assertExists()
        onNodeWithText("Retry").assertExists()
    }

    @Test
    fun `a listed module shows its name and a Download button`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module())),
    ) { _, _ ->
        onNodeWithText("A Conservative Version").assertExists()
        onNodeWithText("Download").assertExists()
    }

    @Test
    fun `searching filters the module list by name`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module(), module(identifier = "KJV", displayName = "King James Version", fileStem = "ENG_KJV"))),
    ) { _, _ ->
        onNodeWithText("Filter by name or language…").performTextInput("King James")
        waitForIdle()

        onNodeWithText("King James Version").assertExists()
        onNodeWithText("A Conservative Version").assertDoesNotExist()
    }

    @Test
    fun `clicking Download opens the licence confirmation`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module())),
    ) { _, _ ->
        onNodeWithText("Download").performClick()
        onNodeWithText("Copyright and licensing").assertExists()
    }

    @Test
    fun `cancelling the licence confirmation installs nothing`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module())),
    ) { _, installed ->
        onNodeWithText("Download").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("Copyright and licensing").assertDoesNotExist()
        assertEquals(null, installed())
    }

    @Test
    fun `accepting the licence installs the module and reports success`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module())),
    ) { _, installed ->
        onNodeWithText("Download").performClick()
        onNodeWithText("I understand — Download").performClick()
        settle()
        waitForIdle()

        assertEquals("ENG_ACV.spb", installed())
        onNodeWithText("Installed \"Installed Title\" — 66 books.").assertExists()
    }

    @Test
    fun `dismissing the installed notice closes it`() = dialog(
        catalogOutcome = BibleCatalogOutcome.Success(listOf(module())),
    ) { _, _ ->
        onNodeWithText("Download").performClick()
        onNodeWithText("I understand — Download").performClick()
        settle()
        waitForIdle()

        onAllNodes(hasText("OK")).onLast().performClick()
        waitForIdle()

        onNodeWithText("Installed \"Installed Title\" — 66 books.").assertDoesNotExist()
    }

    @Test
    fun `an already-installed module shows the Installed badge and a re-download option`() {
        val installedFile = File(dir, "ENG_ACV.spb").apply { writeText("##spDataVersion:\t1\n") }
        dialog(catalogOutcome = BibleCatalogOutcome.Success(listOf(module()))) { _, _ ->
            onNodeWithText("Installed").assertExists()
            onNodeWithText("Re-download").assertExists()
        }
        installedFile.delete()
    }

    @Test
    fun `the OK button dismisses the dialog`() = dialog { dismissed, _ ->
        onAllNodes(hasText("OK")).onFirst().performClick()
        assertEquals(1, dismissed())
    }
}
