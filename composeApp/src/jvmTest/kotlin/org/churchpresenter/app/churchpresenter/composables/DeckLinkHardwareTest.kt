package org.churchpresenter.app.churchpresenter.composables

import org.churchpresenter.app.churchpresenter.TestSingletons
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that exercise DeckLinkManager against a real Blackmagic DeckLink card.
 *
 * These tests only run meaningfully on a machine with the Blackmagic Desktop Video drivers
 * installed and a DeckLink card present. On any other machine, [DeckLinkManager.isAvailable]
 * returns false and every test is a no-op (asserts skip via an `assumeAvailable()` guard).
 *
 * The [available] field is a private memoizing `Boolean?` — once the guard-clause tests in
 * [DeckLinkManagerTest] latch it to `false`, it stays false for the rest of the JVM. This class
 * resets it to `null` before each test and sets `compose.application.resources.dir` to the
 * directory containing `decklink_jni.dll`, giving [isAvailable] a fresh chance to load the
 * native library. After each test, the field and system property are restored so
 * [DeckLinkManagerTest]'s own guard-clause assumptions hold if it runs later in the same JVM.
 */
class DeckLinkHardwareTest {

    private var realHome: String? = null
    private var tempHome: File? = null
    private var savedAvailable: Any? = SENTINEL
    private var savedResDir: String? = null

    private val availableField = DeckLinkManager::class.java.getDeclaredField("available").apply {
        isAccessible = true
    }

    @BeforeTest
    fun setUp() {
        TestSingletons.latchToTestHome()
        realHome = System.getProperty("user.home")
        tempHome = Files.createTempDirectory("cp-decklink-hw").toFile()
        System.setProperty("user.home", tempHome!!.absolutePath)

        savedAvailable = availableField.get(DeckLinkManager)
        savedResDir = System.getProperty("compose.application.resources.dir")

        // Reset the memoized availability and point at the DLL's directory.
        // Gradle's test working dir may differ from the project root, so walk up to find it.
        availableField.set(DeckLinkManager, null)
        val candidates = listOf(
            File("composeApp/src/jvmMain/appResources/windows"),
            File("src/jvmMain/appResources/windows"),
            File("../composeApp/src/jvmMain/appResources/windows"),
        )
        val dllDir = candidates.firstOrNull { File(it, "decklink_jni.dll").exists() }
        if (dllDir != null) {
            System.setProperty("compose.application.resources.dir", dllDir.absolutePath)
        } else {
            println("[DeckLinkHardwareTest] Could not find decklink_jni.dll; cwd=${File(".").absolutePath}")
        }
    }

    @AfterTest
    fun tearDown() {
        // Restore the memoized field so guard-clause tests in DeckLinkManagerTest still work
        if (savedAvailable !== SENTINEL) {
            availableField.set(DeckLinkManager, savedAvailable)
        }
        if (savedResDir != null) {
            System.setProperty("compose.application.resources.dir", savedResDir!!)
        } else {
            System.clearProperty("compose.application.resources.dir")
        }
        realHome?.let { System.setProperty("user.home", it) }
        tempHome?.deleteRecursively()
    }

    private fun assumeAvailable(): Boolean {
        if (!DeckLinkManager.isAvailable()) {
            println("[DeckLinkHardwareTest] Native library not loadable — skipping hardware test")
            return false
        }
        return true
    }

    // ── Device enumeration ────────────────────────────────────────────────────────

    @Test
    fun `listDevices returns at least one device when hardware is present`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        assertTrue(devices.isNotEmpty(), "DeckLink card is installed but listDevices() returned empty")
        println("[DeckLinkHardwareTest] Found ${devices.size} device(s): ${devices.map { "${it.index}=${it.name}" }}")
    }

    @Test
    fun `listDevices returns devices with sequential indices starting from 0`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        assertEquals(0, devices.first().index)
        devices.forEachIndexed { i, dev -> assertEquals(i, dev.index) }
    }

    @Test
    fun `device names are non-blank`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        devices.forEach { assertTrue(it.name.isNotBlank(), "Device ${it.index} has a blank name") }
    }

    // ── Output info ───────────────────────────────────────────────────────────────

    @Test
    fun `getOutputInfo returns dimensions for the first device before opening`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        // Some cards report output info even before open(); others return null — both are valid.
        val info = DeckLinkManager.getOutputInfo(devices.first().index)
        if (info != null) {
            assertTrue(info.width > 0, "Output width should be positive")
            assertTrue(info.height > 0, "Output height should be positive")
            assertTrue(info.fps > 0, "FPS should be positive")
            println("[DeckLinkHardwareTest] Output info: ${info.width}x${info.height} @ ${info.fps} fps")
        } else {
            println("[DeckLinkHardwareTest] getOutputInfo returned null before open (expected for some cards)")
        }
    }

    // ── Input modes ───────────────────────────────────────────────────────────────

    @Test
    fun `listInputModes returns at least one mode for the first device`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        val modes = DeckLinkManager.listInputModes(devices.first().index)
        assertTrue(modes.isNotEmpty(), "DeckLink card should report at least one input mode")
        modes.forEach {
            assertTrue(it.name.isNotBlank(), "Input mode name should not be blank")
        }
        println("[DeckLinkHardwareTest] Input modes: ${modes.map { it.name }}")
    }

    // ── Video connections ─────────────────────────────────────────────────────────

    @Test
    fun `listVideoConnections returns at least one connection for the first device`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        val conns = DeckLinkManager.listVideoConnections(devices.first().index)
        assertTrue(conns.isNotEmpty(), "DeckLink card should report at least one video connection")
        conns.forEach {
            assertTrue(it.name.isNotBlank(), "Connection name should not be blank")
        }
        println("[DeckLinkHardwareTest] Video connections: ${conns.map { "${it.name}(${it.value})" }}")
    }

    // ── Output connections ────────────────────────────────────────────────────────

    @Test
    fun `listOutputConnections returns connections or handles missing native symbol`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        try {
            val conns = DeckLinkManager.listOutputConnections(devices.first().index)
            println("[DeckLinkHardwareTest] Output connections: ${conns.map { "${it.name}(${it.value})" }}")
        } catch (_: UnsatisfiedLinkError) {
            println("[DeckLinkHardwareTest] nativeListOutputConnections not in DLL — symbol not compiled")
        }
    }

    // ── Device status ─────────────────────────────────────────────────────────────

    @Test
    fun `getDeviceStatus returns a status or handles missing native symbol`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        try {
            val status = DeckLinkManager.getDeviceStatus(devices.first().index)
            if (status != null) {
                println("[DeckLinkHardwareTest] Status: signalLocked=${status.signalLocked}, busy=${status.busy}, modeCode=${status.detectedModeCode}")
            } else {
                println("[DeckLinkHardwareTest] getDeviceStatus returned null")
            }
        } catch (_: UnsatisfiedLinkError) {
            println("[DeckLinkHardwareTest] nativeGetDeviceStatus not in DLL — symbol not compiled")
        }
    }

    // ── Open / send / close cycle ─────────────────────────────────────────────────

    @Test
    fun `open send black frame and close completes without error`() {
        if (!assumeAvailable()) return
        val devices = DeckLinkManager.listDevices()
        if (devices.isEmpty()) return
        val idx = devices.first().index
        val opened = DeckLinkManager.open(idx)
        if (!opened) {
            println("[DeckLinkHardwareTest] Could not open device $idx for output (may be busy)")
            return
        }
        try {
            assertTrue(DeckLinkManager.isOutputActive(idx))
            val info = DeckLinkManager.getOutputInfo(idx)
            val w = info?.width ?: 1920
            val h = info?.height ?: 1080
            DeckLinkManager.sendFrame(idx, IntArray(w * h), w, h)
            println("[DeckLinkHardwareTest] Sent one black frame to device $idx at ${w}x${h}")
        } finally {
            DeckLinkManager.close(idx)
        }
    }

    companion object {
        private val SENTINEL = Any()
    }
}
