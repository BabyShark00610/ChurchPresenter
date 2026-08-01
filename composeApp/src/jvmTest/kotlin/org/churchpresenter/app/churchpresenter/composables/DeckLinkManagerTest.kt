package org.churchpresenter.app.churchpresenter.composables

import org.churchpresenter.app.churchpresenter.TestSingletons
import org.churchpresenter.app.churchpresenter.models.Scene
import org.churchpresenter.app.churchpresenter.models.SceneSource
import org.churchpresenter.app.churchpresenter.models.SourceTransform
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The BlackMagic DeckLink JNI wrapper. No DeckLink SDK/hardware exists in this test JVM, so
 * `System.loadLibrary("decklink_jni")` always throws `UnsatisfiedLinkError`, which [DeckLinkManager]
 * catches and memoizes as `isAvailable() == false` — permanently, for the rest of this JVM's life,
 * since the result is cached in a private var with no reset hook (by design: real hardware doesn't
 * appear mid-process). That makes every public function's "hardware unavailable" guard clause
 * deterministic and safe to exercise directly: every method below is expected to hit its early
 * `if (!isAvailable()) return ...` branch, never the native call inside. The native calls
 * themselves (`nativeXxx`) are therefore permanently unreachable in this suite, by construction —
 * consistent with this project's rule that hardware-only code paths are not force-tested.
 */
class DeckLinkManagerTest {

    private var realHome: String? = null
    private var tempHome: File? = null

    @BeforeTest
    fun setUp() {
        TestSingletons.latchToTestHome()
        realHome = System.getProperty("user.home")
        tempHome = Files.createTempDirectory("cp-decklink-test").toFile()
        System.setProperty("user.home", tempHome!!.absolutePath)
    }

    @AfterTest
    fun tearDown() {
        realHome?.let { System.setProperty("user.home", it) }
        tempHome?.deleteRecursively()
    }

    // ── Availability ───────────────────────────────────────────────────────────────────────────

    @Test
    fun `isAvailable is false with no native library on the test machine`() {
        assertFalse(DeckLinkManager.isAvailable())
    }

    @Test
    fun `listDevices is empty when unavailable`() {
        assertEquals(emptyList(), DeckLinkManager.listDevices())
    }

    // ── Output API guard clauses ──────────────────────────────────────────────────────────────

    @Test
    fun `output functions no-op or return their unavailable default`() {
        assertFalse(DeckLinkManager.open(0))
        assertFalse(DeckLinkManager.open(0, 1280, 720))
        assertNull(DeckLinkManager.getOutputInfo(0))
        DeckLinkManager.sendFrame(0, IntArray(0), 0, 0)
        assertFalse(DeckLinkManager.startScheduledPlayback(0))
        assertFalse(DeckLinkManager.startScheduledPlayback(0, 25.0))
        DeckLinkManager.scheduleFrame(0, IntArray(0), 0, 0)
        DeckLinkManager.stopPlayback(0)
        DeckLinkManager.close(0)
        DeckLinkManager.closeAllOutputs() // no devices ever opened — loop body never runs
    }

    @Test
    fun `isOutputActive is false for any device index, opened or not`() {
        assertFalse(DeckLinkManager.isOutputActive(0))
        assertFalse(DeckLinkManager.isOutputActive(99))
    }

    // ── Input capture API guard clauses ───────────────────────────────────────────────────────

    @Test
    fun `input functions no-op or return their unavailable default`() {
        assertEquals(emptyList(), DeckLinkManager.listInputModes(0))
        assertEquals(emptyList(), DeckLinkManager.listVideoConnections(0))
        assertFalse(DeckLinkManager.openInput(0))
        assertFalse(DeckLinkManager.openInput(0, "1080p60", 1))
        assertNull(DeckLinkManager.getInputFrame(0))
        DeckLinkManager.closeInput(0)
    }

    @Test
    fun `isInputActive is false for any device index, opened or not`() {
        assertFalse(DeckLinkManager.isInputActive(0))
        assertFalse(DeckLinkManager.isInputActive(99))
    }

    // ── Audio input/output API guard clauses ──────────────────────────────────────────────────

    @Test
    fun `audio functions no-op or return their unavailable default`() {
        assertFalse(DeckLinkManager.enableAudioInput(0))
        assertFalse(DeckLinkManager.enableAudioInput(0, 6))
        assertNull(DeckLinkManager.getInputAudio(0))
        assertFalse(DeckLinkManager.enableAudioOutput(0))
        assertFalse(DeckLinkManager.enableAudioOutput(0, 6))
        assertEquals(0, DeckLinkManager.writeAudioSamples(0, ShortArray(0), 0))
        DeckLinkManager.disableAudioOutput(0)
    }

    // ── Keyer API guard clauses ────────────────────────────────────────────────────────────────

    @Test
    fun `keyer functions no-op or return their unavailable default`() {
        assertFalse(DeckLinkManager.enableKeyer(0))
        assertFalse(DeckLinkManager.enableKeyer(0, isExternal = true))
        DeckLinkManager.setKeyerLevel(0, 128)
        DeckLinkManager.keyerRampUp(0)
        DeckLinkManager.keyerRampUp(0, 10)
        DeckLinkManager.keyerRampDown(0)
        DeckLinkManager.keyerRampDown(0, 10)
        DeckLinkManager.disableKeyer(0)
    }

    // ── Output connection + status API guard clauses ──────────────────────────────────────────

    @Test
    fun `output connection and status functions return their unavailable default`() {
        assertFalse(DeckLinkManager.setOutputConnection(0, 1))
        assertEquals(emptyList(), DeckLinkManager.listOutputConnections(0))
        assertNull(DeckLinkManager.getDeviceStatus(0))
    }

    // ── isInputConfigured — real logic, not hardware-gated ────────────────────────────────────

    private fun cameraSource(isDeckLink: Boolean, deckLinkIndex: Int) = SceneSource.CameraSource(
        id = "cam1", name = "Camera", transform = SourceTransform(),
        isDeckLink = isDeckLink, deckLinkIndex = deckLinkIndex,
    )

    @Test
    fun `isInputConfigured is true when a scene has a matching DeckLink camera source`() {
        val scene = Scene(sources = listOf(cameraSource(isDeckLink = true, deckLinkIndex = 2)))
        assertTrue(DeckLinkManager.isInputConfigured(2, listOf(scene)))
    }

    @Test
    fun `isInputConfigured is false when no scene's index matches`() {
        val scene = Scene(sources = listOf(cameraSource(isDeckLink = true, deckLinkIndex = 2)))
        assertFalse(DeckLinkManager.isInputConfigured(3, listOf(scene)))
    }

    @Test
    fun `isInputConfigured is false when the matching source is not a DeckLink camera`() {
        val scene = Scene(sources = listOf(cameraSource(isDeckLink = false, deckLinkIndex = 2)))
        assertFalse(DeckLinkManager.isInputConfigured(2, listOf(scene)))
    }

    @Test
    fun `isInputConfigured is false when no scenes list is given and no scenes file exists`() {
        assertFalse(DeckLinkManager.isInputConfigured(2))
    }

    @Test
    fun `isInputConfigured falls back to a persisted scenes file when the index matches, no-space form`() {
        val appDir = File(tempHome, ".churchpresenter").apply { mkdirs() }
        File(appDir, "scenes.json").writeText("""{"sources":[{"deckLinkIndex":4}]}""")
        assertTrue(DeckLinkManager.isInputConfigured(4))
    }

    @Test
    fun `isInputConfigured falls back to a persisted scenes file when the index matches, spaced form`() {
        val appDir = File(tempHome, ".churchpresenter").apply { mkdirs() }
        File(appDir, "scenes.json").writeText("""{"sources":[{"deckLinkIndex": 5}]}""")
        assertTrue(DeckLinkManager.isInputConfigured(5))
    }

    @Test
    fun `isInputConfigured returns false when the persisted scenes file exists but does not match`() {
        val appDir = File(tempHome, ".churchpresenter").apply { mkdirs() }
        File(appDir, "scenes.json").writeText("""{"sources":[{"deckLinkIndex":4}]}""")
        assertFalse(DeckLinkManager.isInputConfigured(9))
    }

    // ── Data classes ───────────────────────────────────────────────────────────────────────────

    @Test
    fun `OutputInfo computes fps from numerator over denominator`() {
        val info = DeckLinkManager.OutputInfo(1920, 1080, 60000, 1001)
        assertEquals(60000.0 / 1001, info.fps)
    }

    @Test
    fun `OutputInfo falls back to 30 fps when the denominator is zero`() {
        val info = DeckLinkManager.OutputInfo(1920, 1080, 60, 0)
        assertEquals(30.0, info.fps)
    }

    @Test
    fun `the remaining data classes hold the fields they are constructed with`() {
        assertEquals(DeckLinkManager.DeckLinkDevice(0, "UltraStudio"), DeckLinkManager.DeckLinkDevice(0, "UltraStudio"))
        assertEquals(DeckLinkManager.InputMode("1080p60", "abcd"), DeckLinkManager.InputMode("1080p60", "abcd"))
        assertEquals(DeckLinkManager.VideoConnection("SDI", 1), DeckLinkManager.VideoConnection("SDI", 1))
        val status = DeckLinkManager.DeviceStatus(signalLocked = true, busy = 0, detectedModeCode = 7)
        assertTrue(status.signalLocked)
        assertEquals(7, status.detectedModeCode)
        val audio = DeckLinkManager.AudioFrame(sampleFrames = 2, channels = 2, samples = shortArrayOf(1, 2, 3, 4))
        assertEquals(2, audio.sampleFrames)
        assertEquals(4, audio.samples.size)
    }
}
