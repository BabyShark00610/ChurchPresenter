package org.churchpresenter.app.churchpresenter

import org.churchpresenter.app.churchpresenter.data.Language
import org.churchpresenter.app.churchpresenter.data.settings.CompanionSatelliteSettings
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundSettings
import org.churchpresenter.app.churchpresenter.data.settings.InstanceLinkSettings
import org.churchpresenter.app.churchpresenter.data.settings.OBSSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.data.settings.ServerSettings
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.server.TunnelStatus
import org.churchpresenter.app.churchpresenter.utils.UpdateCheckResult
import org.churchpresenter.app.churchpresenter.utils.UpdateInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rules the entry point applies at startup, checked without starting one.
 */
class MainLogicTest {

    // ── Renderer ────────────────────────────────────────────────────────────────

    @Test
    fun `macOS is pinned to Metal`() {
        // Left to choose, skiko falls back to OpenGL there and crashes on some machines.
        assertTrue(shouldForceMetalRenderer("Mac OS X"))
        assertTrue(shouldForceMetalRenderer("macOS"))
    }

    @Test
    fun `every other platform chooses for itself`() {
        assertFalse(shouldForceMetalRenderer("Windows 11"))
        assertFalse(shouldForceMetalRenderer("Linux"))
        assertFalse(shouldForceMetalRenderer(""))
    }

    // ── The single-instance lock ────────────────────────────────────────────────

    @Test
    fun `the lock uses its own port when nothing overrides it`() {
        assertEquals(47632, singleInstanceLockPort(override = null, default = 47632))
    }

    @Test
    fun `a second dev instance can be given its own port`() {
        assertEquals(47633, singleInstanceLockPort(override = "47633", default = 47632))
    }

    @Test
    fun `an unreadable override falls back rather than failing to start`() {
        assertEquals(47632, singleInstanceLockPort(override = "not-a-port", default = 47632))
        assertEquals(47632, singleInstanceLockPort(override = "", default = 47632))
    }

    // ── Language ────────────────────────────────────────────────────────────────

    @Test
    fun `the saved language is the one started in`() {
        assertEquals(Language.ENGLISH, resolveStartupLanguage("en"))
        Language.entries.forEach { assertEquals(it, resolveStartupLanguage(it.code)) }
    }

    @Test
    fun `a language this build no longer has falls back to english`() {
        // A settings file naming a removed language must not stop the app starting.
        assertEquals(Language.ENGLISH, resolveStartupLanguage("xx"))
        assertEquals(Language.ENGLISH, resolveStartupLanguage(""))
    }

    // ── DeckLink ────────────────────────────────────────────────────────────────

    @Test
    fun `outputs are counted only when the driver is there`() {
        assertEquals(3, deckLinkOutputCount(available = true) { 3 })
    }

    @Test
    fun `no driver means no outputs, and the count is never asked for`() {
        var asked = false
        assertEquals(0, deckLinkOutputCount(available = false) { asked = true; 3 })
        assertFalse(asked, "listing devices without the driver is what crashes")
    }

    // ── Companion connections ───────────────────────────────────────────────────

    private fun connection(id: String = "c1", autoConnect: Boolean = false, deviceId: String = "d1") =
        CompanionSatelliteSettings(id = id, host = "10.0.0.2", autoConnect = autoConnect, deviceId = deviceId)

    @Test
    fun `a connection with something already live is brought up`() {
        val c = connection()
        assertTrue(shouldConnectCompanion(hasLiveSlot = true, autoConnect = false, lastSeen = c, current = c))
    }

    @Test
    fun `a connection set to auto-connect is brought up`() {
        val c = connection(autoConnect = true)
        assertTrue(shouldConnectCompanion(hasLiveSlot = false, autoConnect = true, lastSeen = null, current = c))
    }

    @Test
    fun `a connection just edited is brought up even with auto-connect off`() {
        // An edit is an explicit action, so it connects; that is what separates it from startup.
        val before = connection(deviceId = "d1")
        val after = before.copy(deviceId = "d2")
        assertTrue(shouldConnectCompanion(hasLiveSlot = false, autoConnect = false, lastSeen = before, current = after))
    }

    @Test
    fun `a connection seen for the first time at startup is left alone`() {
        // Keeps startup opt-in: never seen before, nothing live, auto-connect off.
        val c = connection()
        assertFalse(shouldConnectCompanion(hasLiveSlot = false, autoConnect = false, lastSeen = null, current = c))
    }

    @Test
    fun `a connection seen before and unchanged is left alone`() {
        val c = connection()
        assertFalse(shouldConnectCompanion(hasLiveSlot = false, autoConnect = false, lastSeen = c, current = c))
    }

    @Test
    fun `a blank device id has to be minted before use`() {
        assertTrue(needsGeneratedDeviceId(connection(deviceId = "")))
        assertTrue(needsGeneratedDeviceId(connection(deviceId = "   ")))
        assertFalse(needsGeneratedDeviceId(connection(deviceId = "d1")))
    }

    @Test
    fun `minting a device id leaves every other connection alone`() {
        val all = listOf(connection(id = "a", deviceId = ""), connection(id = "b", deviceId = "keep"))

        val updated = withGeneratedDeviceId(all, id = "a", deviceId = "minted")

        assertEquals("minted", updated.first { it.id == "a" }.deviceId)
        assertEquals("keep", updated.first { it.id == "b" }.deviceId)
    }

    @Test
    fun `minting against an id that is not there changes nothing`() {
        val all = listOf(connection(id = "a", deviceId = "keep"))
        assertEquals(all, withGeneratedDeviceId(all, id = "missing", deviceId = "minted"))
    }

    // ── The instance link ───────────────────────────────────────────────────────

    private fun link(
        enabled: Boolean = true,
        autoConnect: Boolean = true,
        host: String = "10.0.0.9",
        port: Int = 8080,
    ) = InstanceLinkSettings(enabled = enabled, autoConnect = autoConnect, primaryHost = host, primaryPort = port)

    @Test
    fun `a link that is on, set to, and addressed dials out`() {
        assertTrue(shouldAutoConnectInstanceLink(link()))
    }

    @Test
    fun `a link missing any one of those does not`() {
        assertFalse(shouldAutoConnectInstanceLink(link(enabled = false)))
        assertFalse(shouldAutoConnectInstanceLink(link(autoConnect = false)))
        assertFalse(shouldAutoConnectInstanceLink(link(host = "")))
        assertFalse(shouldAutoConnectInstanceLink(link(port = 0)))
    }

    @Test
    fun `switching the link off drops it now rather than at the next launch`() {
        assertTrue(shouldDisconnectInstanceLink(link(enabled = false)))
        assertFalse(shouldDisconnectInstanceLink(link(enabled = true)))
    }

    @Test
    fun `only a real change of intent is persisted`() {
        assertTrue(instanceLinkEnabledChanged(link(enabled = false), enabled = true))
        assertFalse(instanceLinkEnabledChanged(link(enabled = true), enabled = true))
    }

    // ── The key remote callers must present ─────────────────────────────────────

    @Test
    fun `a key is required only once key checking is switched on`() {
        assertEquals("s3cret", activeApiKey(ServerSettings(apiKeyEnabled = true, apiKey = "s3cret")))
    }

    @Test
    fun `with key checking off no key is required, whatever is stored`() {
        // The stored key survives being switched off, so the flag has to be what decides.
        assertEquals("", activeApiKey(ServerSettings(apiKeyEnabled = false, apiKey = "s3cret")))
    }

    // ── The tunnel ──────────────────────────────────────────────────────────────

    @Test
    fun `only a connected tunnel counts as up`() {
        assertTrue(isTunnelConnected(TunnelStatus.Connected("https://x.trycloudflare.com")))
        assertFalse(isTunnelConnected(TunnelStatus.Idle))
        assertFalse(isTunnelConnected(TunnelStatus.Starting))
        assertFalse(isTunnelConnected(TunnelStatus.Downloading))
        assertFalse(isTunnelConnected(TunnelStatus.Error("gone")))
    }

    @Test
    fun `the drop is the moment it goes down, not every moment after`() {
        assertTrue(tunnelJustDropped(previouslyConnected = true, isConnected = false))
        assertFalse(tunnelJustDropped(previouslyConnected = false, isConnected = false))
        assertFalse(tunnelJustDropped(previouslyConnected = true, isConnected = true))
        assertFalse(tunnelJustDropped(previouslyConnected = false, isConnected = true))
    }

    // ── Browser source outputs ──────────────────────────────────────────────────

    @Test
    fun `a configured browser source output is used`() {
        val configured = ScreenAssignment(displayMode = "browser_source")
        assertEquals(configured, browserSourceOutputAt(listOf(configured), 0))
    }

    @Test
    fun `an output that was never configured falls back rather than failing`() {
        assertEquals(ScreenAssignment(), browserSourceOutputAt(emptyList(), 0))
        assertEquals(ScreenAssignment(), browserSourceOutputAt(listOf(ScreenAssignment()), 3))
    }

    // ── What a follower is told is live ─────────────────────────────────────────

    @Test
    fun `media is reported live only while media is what is on the output`() {
        assertTrue(isMediaLive(Presenting.MEDIA))
        assertFalse(isMediaLive(Presenting.LYRICS))
        assertFalse(isMediaLive(Presenting.NONE))
    }

    @Test
    fun `a position is announced only for the content it belongs to`() {
        assertEquals(4, livePositionOrNull(Presenting.LYRICS, Presenting.LYRICS, 4))
        assertEquals(null, livePositionOrNull(Presenting.BIBLE, Presenting.LYRICS, 4))
    }

    // ── What goes out on the wire ───────────────────────────────────────────────

    @Test
    fun `an empty field is sent as nothing rather than as blank`() {
        assertEquals(null, nullIfEmpty(""))
        assertEquals("a value", nullIfEmpty("a value"))
    }

    @Test
    fun `a verse reference is resolved through the loaded bible`() {
        val code = liveVerseCode(
            source = Presenting.BIBLE,
            bookName = "John", chapter = 3, verseNumber = 16,
            bookIdByName = { 43 },
            codeReference = { bookId, chapter, verse -> Triple(bookId, chapter, verse) },
        )
        assertEquals(Triple(43, 3, 16), code)
    }

    @Test
    fun `no reference is announced when scripture is not what is live`() {
        val code = liveVerseCode(
            source = Presenting.LYRICS,
            bookName = "John", chapter = 3, verseNumber = 16,
            bookIdByName = { error("must not be consulted when scripture is not live") },
            codeReference = { _, _, _ -> error("must not be consulted") },
        )
        assertEquals(null, code)
    }

    @Test
    fun `a half-filled verse has no reference rather than a wrong one`() {
        // Mid-selection the book can still be empty; resolving that would name the wrong passage.
        val code = liveVerseCode(
            source = Presenting.BIBLE,
            bookName = "", chapter = 3, verseNumber = 16,
            bookIdByName = { error("must not be consulted without a book") },
            codeReference = { _, _, _ -> error("must not be consulted") },
        )
        assertEquals(null, code)
    }

    @Test
    fun `a book the loaded bible does not know has no reference`() {
        val code = liveVerseCode(
            source = Presenting.BIBLE,
            bookName = "Some Other Book", chapter = 1, verseNumber = 1,
            bookIdByName = { null },
            codeReference = { _, _, _ -> error("must not be consulted for an unknown book") },
        )
        assertEquals(null, code)
    }

    // ── OBS ─────────────────────────────────────────────────────────────────────

    @Test
    fun `obs is connected only while the integration is switched on`() {
        assertTrue(shouldConnectObs(OBSSettings(enabled = true)))
        assertFalse(shouldConnectObs(OBSSettings(enabled = false)))
    }

    // ── Mirroring the primary ───────────────────────────────────────────────────

    @Test
    fun `a slide the primary actually has is fetched`() {
        assertTrue(hasFetchableSlide("deck-1"))
    }

    @Test
    fun `the empty slide in a connect snapshot is not fetched`() {
        // The snapshot always carries this event; an empty id means no deck is open, and asking
        // for it would 404 and be logged as a failed mirror.
        assertFalse(hasFetchableSlide(""))
        assertFalse(hasFetchableSlide("   "))
    }

    @Test
    fun `an announced background change empties the cache first`() {
        // The per-file check is "does this exist locally", so a replaced background under the same
        // name would otherwise satisfy it forever.
        assertTrue(shouldInvalidateBackgroundCache(1))
        assertFalse(shouldInvalidateBackgroundCache(0))
    }

    @Test
    fun `mirrored backgrounds are used for rendering`() {
        val mirrored = BackgroundSettings(defaultBackgroundColor = "#123456")
        val effective = withMirroredBackgrounds(AppSettings(), mirrored)

        assertEquals(mirrored, effective.backgroundSettings)
    }

    @Test
    fun `without a mirror the instance keeps its own backgrounds`() {
        // The follower must never persist the primary's backgrounds over its own configuration.
        val own = AppSettings()
        assertEquals(own, withMirroredBackgrounds(own, null))
    }

    // ── Update checks ───────────────────────────────────────────────────────────

    private fun available() = UpdateCheckResult.Available(
        UpdateInfo(latestVersion = "1.2.3", releaseUrl = "https://example.invalid", releaseNotes = ""),
    )

    @Test
    fun `an install that has never checked is recognised`() {
        assertTrue(isFirstEverUpdateCheck(0L))
        assertFalse(isFirstEverUpdateCheck(1L))
    }

    @Test
    fun `the very first check is shown whatever it found`() {
        // That is the one chance to ask how often the operator wants checking done.
        assertTrue(shouldShowUpdateResult(firstEverCheck = true, result = UpdateCheckResult.UpToDate))
        assertTrue(shouldShowUpdateResult(firstEverCheck = true, result = available()))
    }

    @Test
    fun `later checks interrupt only when there is something to install`() {
        assertTrue(shouldShowUpdateResult(firstEverCheck = false, result = available()))
        assertFalse(
            shouldShowUpdateResult(firstEverCheck = false, result = UpdateCheckResult.UpToDate),
            "a routine up-to-date result must not appear unasked",
        )
    }

    // ── Window placement ────────────────────────────────────────────────────────

    @Test
    fun `a floating window is restored where it was left`() {
        assertTrue(shouldRestoreWindowGeometry(isFloating = true, savedX = 0))
        assertTrue(shouldRestoreWindowGeometry(isFloating = true, savedX = 640))
    }

    @Test
    fun `a maximized window is not restored to a floating geometry`() {
        assertFalse(shouldRestoreWindowGeometry(isFloating = false, savedX = 640))
    }

    @Test
    fun `geometry that was never saved is not restored`() {
        // A negative coordinate is the never-saved value; honouring it puts the window off-screen.
        assertFalse(shouldRestoreWindowGeometry(isFloating = true, savedX = -1))
    }

    // ── What the presentation-live flag reports ─────────────────────────────────

    @Test
    fun `a presentation is reported live only while it is on the output`() {
        assertTrue(isPresentationLive(Presenting.PRESENTATION))
        assertFalse(isPresentationLive(Presenting.LYRICS))
        assertFalse(isPresentationLive(Presenting.NONE))
    }
}