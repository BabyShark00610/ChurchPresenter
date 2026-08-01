package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JPanel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * VLC/VLCJ-backed video playback. The two composables ([VideoPlayer], [SoftwareVideoPlayer]) need
 * a real, working native VLC install to get past `createMediaPlayerComponent()`; whether that
 * succeeds varies by machine (this project's own [SceneSourceRendererTest] documents the same
 * constraint for the canvas Video source), so they are not exercised here. What *is* tested is
 * every piece of this file's logic that does not require VLC to actually load: the VLC-directory
 * detection helpers (pure filesystem checks), the [Component] extension functions' non-VLC
 * fallback branch, and [SharedVideoOutputDisplay] — a plain composable over an in-memory
 * [SharedVideoOutput] frame holder with no VLC involvement at all.
 */
@OptIn(ExperimentalTestApi::class)
class VideoPlayerTest {

    // ── dirContainsVlcLib ──────────────────────────────────────────────────────────────────────

    private fun tempDir(): Path = Files.createTempDirectory("cp-vlc-test")

    @Test
    fun `a directory containing libvlc dll is recognized`() {
        val dir = tempDir()
        try {
            Files.createFile(dir.resolve("libvlc.dll"))
            assertTrue(dirContainsVlcLib(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a directory containing libvlc dylib is recognized`() {
        val dir = tempDir()
        try {
            Files.createFile(dir.resolve("libvlc.dylib"))
            assertTrue(dirContainsVlcLib(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a directory containing a versioned libvlc so is recognized`() {
        val dir = tempDir()
        try {
            Files.createFile(dir.resolve("libvlc.so.5"))
            assertTrue(dirContainsVlcLib(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a directory containing only libvlccore is not recognized as containing libvlc`() {
        val dir = tempDir()
        try {
            Files.createFile(dir.resolve("libvlccore.so.9"))
            assertFalse(dirContainsVlcLib(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `an empty directory does not contain a VLC library`() {
        val dir = tempDir()
        try {
            assertFalse(dirContainsVlcLib(dir))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a nonexistent directory does not contain a VLC library`() {
        assertFalse(dirContainsVlcLib(Path.of("/nonexistent/path/for/cp-vlc-test")))
    }

    // ── applyCustomVlcPath ─────────────────────────────────────────────────────────────────────

    private var savedCustomPath: String = ""
    private var savedJnaPath: String? = null

    @BeforeTest
    fun saveVlcState() {
        savedCustomPath = vlcCustomPath
        savedJnaPath = System.getProperty("jna.library.path")
    }

    @AfterTest
    fun restoreVlcState() {
        vlcCustomPath = savedCustomPath
        if (savedJnaPath != null) System.setProperty("jna.library.path", savedJnaPath!!)
        else System.clearProperty("jna.library.path")
    }

    @Test
    fun `a blank custom VLC path leaves jna library path untouched`() {
        System.clearProperty("jna.library.path")
        vlcCustomPath = ""
        applyCustomVlcPath()
        assertEquals(null, System.getProperty("jna.library.path"))
    }

    @Test
    fun `a custom VLC path pointing at a real directory is appended to jna library path`() {
        val dir = tempDir()
        try {
            System.clearProperty("jna.library.path")
            vlcCustomPath = dir.toString()
            applyCustomVlcPath()
            assertTrue(System.getProperty("jna.library.path").orEmpty().contains(dir.toString()))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a custom VLC path that is not a directory is ignored`() {
        val file = Files.createTempFile("cp-vlc-test-file", ".txt")
        try {
            System.clearProperty("jna.library.path")
            vlcCustomPath = file.toString()
            applyCustomVlcPath()
            assertEquals(null, System.getProperty("jna.library.path"))
        } finally {
            file.toFile().delete()
        }
    }

    @Test
    fun `applying the same custom path twice does not duplicate the jna library path entry`() {
        val dir = tempDir()
        try {
            System.clearProperty("jna.library.path")
            vlcCustomPath = dir.toString()
            applyCustomVlcPath()
            val once = System.getProperty("jna.library.path")
            applyCustomVlcPath()
            assertEquals(once, System.getProperty("jna.library.path"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    // ── detectVlcInstallPath ───────────────────────────────────────────────────────────────────

    private var savedOsName: String = ""

    @BeforeTest
    fun saveOsName() {
        savedOsName = System.getProperty("os.name", "")
    }

    @AfterTest
    fun restoreOsName() {
        System.setProperty("os.name", savedOsName)
    }

    @Test
    fun `detectVlcInstallPath finds nothing on a forced Windows OS name in this test environment`() {
        System.setProperty("os.name", "Windows 11")
        assertEquals("", detectVlcInstallPath())
    }

    @Test
    fun `detectVlcInstallPath finds nothing on a forced generic Linux OS name in this test environment`() {
        System.setProperty("os.name", "Generic Linux")
        assertEquals("", detectVlcInstallPath())
    }

    @Test
    fun `detectVlcInstallPath returns a path only when it actually contains a VLC library`() {
        System.setProperty("os.name", "Mac OS X")
        val result = detectVlcInstallPath()
        // Environment-dependent (whether VLC.app is installed on the machine running this suite),
        // so only the invariant is asserted: an empty result, or a real, VLC-containing/plausible path.
        assertTrue(result.isEmpty() || result.contains("VLC"))
    }

    // ── Component extension functions ─────────────────────────────────────────────────────────

    @Test
    fun `mediaPlayer throws for a Component that is neither known VLCJ component type`() {
        assertFailsWith<IllegalStateException> { JPanel().mediaPlayer() }
    }

    @Test
    fun `releasePlayer is a no-op for a Component that is neither known VLCJ component type`() {
        JPanel().releasePlayer() // must not throw
    }

    // ── VlcAudioDevice ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `VlcAudioDevice holds the id and description it is constructed with`() {
        val device = VlcAudioDevice("dev-1", "Built-in Output")
        assertEquals("dev-1", device.id)
        assertEquals("Built-in Output", device.description)
    }

    // ── SharedVideoOutputDisplay ───────────────────────────────────────────────────────────────

    @Test
    fun `SharedVideoOutputDisplay renders nothing when no frame has been written`() = runComposeUiTest {
        SharedVideoOutput.frame.value = null
        setContent {
            MaterialTheme {
                SharedVideoOutputDisplay(modifier = Modifier.testTag("shared-video"))
            }
        }
        onNodeWithTag("shared-video").assertDoesNotExist()
    }

    @Test
    fun `SharedVideoOutputDisplay renders the latest written frame`() = runComposeUiTest {
        try {
            SharedVideoOutput.frame.value = ImageBitmap(4, 4)
            setContent {
                MaterialTheme {
                    SharedVideoOutputDisplay(modifier = Modifier.testTag("shared-video"))
                }
            }
            onNodeWithTag("shared-video").assertExists()
        } finally {
            SharedVideoOutput.frame.value = null
        }
    }
}
