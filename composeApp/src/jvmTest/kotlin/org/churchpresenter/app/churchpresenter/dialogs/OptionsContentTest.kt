@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.data.RemoteClientManager
import org.churchpresenter.app.churchpresenter.data.SettingsManager
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.server.CompanionServer
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import org.churchpresenter.app.churchpresenter.viewmodel.OBSWebSocketManager
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OptionsContentTest {

    private lateinit var home: File
    private var realHome: String? = null

    @BeforeTest
    fun isolateHome() {
        // Pin the JVM-wide log path to the real test home before swapping user.home below: this test
        // builds a PresenterManager and a CompanionServer, whose Instance Link paths log, and
        // InstanceLinkLogger keeps whatever user.home pointed at the first time anything logged.
        TestSingletons.latchToTestHome()
        realHome = System.getProperty("user.home")
        home = Files.createTempDirectory("cp-options-test").toFile()
        System.setProperty("user.home", home.absolutePath)
    }

    @AfterTest
    fun restoreHome() {
        realHome?.let { System.setProperty("user.home", it) }
        home.deleteRecursively()
    }

    private class Result {
        var dismissed = 0
        var saved: AppSettings? = null
    }

    private fun dialog(
        initialTab: Int = 0,
        obsManager: OBSWebSocketManager? = null,
        block: ComposeUiTest.(Result) -> Unit,
    ) {
        val result = Result()
        runComposeUiTest {
            setContent {
                OptionsDialogContent(
                    theme = ThemeMode.LIGHT,
                    settingsManager = SettingsManager(),
                    companionServer = CompanionServer(),
                    remoteClientManager = RemoteClientManager(),
                    presenterManager = PresenterManager(),
                    onDismiss = { result.dismissed++ },
                    onSave = { result.saved = it },
                    obsManager = obsManager,
                    initialTab = initialTab,
                )
            }
            block(result)
        }
    }

    @Test
    fun `every settings tab is shown without an OBS connection`() = dialog {
        listOf(
            "System", "Bible", "Song", "Background", "Projection", "Lower Third",
            "Server", "Stage Monitor", "ATEM", "Dictionary", "Companion Satellite",
        ).forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("OBS").assertDoesNotExist()
    }

    @Test
    fun `the System tab is selected by default`() = dialog {
        onNodeWithText("System").assertIsSelected()
    }

    @Test
    fun `clicking a different tab switches the selection`() = dialog {
        onNodeWithText("Bible").performClick()
        onNodeWithText("Bible").assertIsSelected()
        onNodeWithText("System").assertIsNotSelected()
    }

    @Test
    fun `initialTab opens directly on that tab`() = dialog(initialTab = 3) {
        onNodeWithText("Background").assertIsSelected()
    }

    @Test
    fun `an out-of-range initialTab is coerced onto the last real tab`() = dialog(initialTab = 999) {
        onNodeWithText("Companion Satellite").assertIsSelected()
    }

    @Test
    fun `Cancel dismisses without saving`() = dialog { result ->
        onNodeWithText("Cancel", substring = true).performClick()

        assertEquals(1, result.dismissed)
        assertNull(result.saved)
    }

    @Test
    fun `Apply saves without dismissing`() = dialog { result ->
        onNodeWithText("Apply").performClick()

        assertEquals(0, result.dismissed)
        assertEquals(ThemeMode.SYSTEM.name, result.saved?.theme)
        assertEquals(ThemeMode.SYSTEM.name, SettingsManager().loadSettings().theme)
    }

    @Test
    fun `OK saves and dismisses`() = dialog { result ->
        onNodeWithText("OK", substring = true).performClick()

        assertEquals(1, result.dismissed)
        assertEquals(ThemeMode.SYSTEM.name, result.saved?.theme)
        assertEquals(ThemeMode.SYSTEM.name, SettingsManager().loadSettings().theme)
    }

    @Test
    fun `every tab renders its own settings content when selected`() = dialog {
        listOf(
            "Song", "Lower Third", "Server",
            "Stage Monitor", "ATEM", "Dictionary",
        ).forEach { label ->
            onNode(hasText(label) and hasClickAction()).performClick()
            onNode(hasText(label) and hasClickAction()).assertIsSelected()
        }
    }

    @Test
    fun `an OBS connection adds an OBS tab ahead of Companion Satellite`() = dialog(
        obsManager = OBSWebSocketManager(),
    ) {
        onNodeWithText("OBS").assertExists()

        onNodeWithText("OBS").performClick()
        onNodeWithText("OBS").assertIsSelected()

        onNodeWithText("Companion Satellite").performClick()
        onNodeWithText("Companion Satellite").assertIsSelected()
    }
}
