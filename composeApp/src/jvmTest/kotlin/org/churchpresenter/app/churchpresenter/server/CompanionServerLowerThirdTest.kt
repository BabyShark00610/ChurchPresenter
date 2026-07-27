package org.churchpresenter.app.churchpresenter.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.churchpresenter.app.churchpresenter.data.settings.AtemSettings
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The lower-third and ATEM-key endpoints — the routes a Stream Deck presses.
 *
 * These are the part of the server that reaches hardware, so what is pinned here is everything the
 * handlers decide **before** any of that happens: which file a name resolves to, whether the Lottie
 * can be timed, and whether the requested key target exists on the switcher. Each of those returns
 * its own status and stops, so the tests never open a connection to an ATEM and never start a key
 * sequence — a wrong answer at this stage is a button that silently does nothing mid-service, or a
 * key cut on a bus that isn't there.
 *
 * Two paths are deliberately not driven, both because their cost is a wait rather than work. A
 * *successful* run/show hands off to [LowerThirdSequencer], which talks to a switcher and holds the
 * animation for its full duration ([LowerThirdSequencerTest] covers the sequencer itself). And the
 * case where the topology is undetected — key counts of 0, meaning the app has never connected — is
 * accepted by validation by design and so has to reach the switcher; with no ATEM on the network
 * that ends in a 5-second socket timeout, which is a duration, not a test.
 *
 * Same harness as the sibling classes — a real `CompanionServer` on a free port, driven over real
 * HTTP — because `start()` builds its own Netty server rather than exposing a separable Ktor module.
 */
class CompanionServerLowerThirdTest {

    private lateinit var client: HttpClient

    companion object {
        /** A Lottie the duration parser can read: 60 frames at 30fps = 2000ms. */
        const val TIMED_LOTTIE =
            """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":1920,"h":1080,"layers":[]}"""

        /** Looks like a Lottie (so it is listed) but carries no timing the sequencer could use. */
        const val UNTIMED_LOTTIE = """{"v":"5.7.4","w":1920,"h":1080,"layers":[]}"""

        private lateinit var server: CompanionServer
        private lateinit var lottieFolder: java.io.File
        private var port: Int = 0

        /**
         * One server and one lower-third folder for the whole class.
         *
         * Binding Netty per test costs about a second; the ATEM settings these tests vary are a
         * live setter on the server, so they are reset per test instead.
         */
        @JvmStatic
        @BeforeClass
        fun startServer() {
            lottieFolder = java.nio.file.Files.createTempDirectory("cp-lowerthirds").toFile()
            java.io.File(lottieFolder, "Welcome.json").writeText(TIMED_LOTTIE)
            java.io.File(lottieFolder, "Announcement.json").writeText(TIMED_LOTTIE)
            java.io.File(lottieFolder, "Untimed.json").writeText(UNTIMED_LOTTIE)
            // Neither of these may be listed: one is not a Lottie, one is not JSON at all.
            java.io.File(lottieFolder, "notlottie.json").writeText("""{"hello":"world"}""")
            java.io.File(lottieFolder, "readme.txt").writeText("not json")

            server = CompanionServer()
            server.start(port = 39_713)
            port = runBlocking {
                withTimeoutOrNull(10_000) {
                    while (!server.isRunning.value || server.serverUrl.value.isBlank()) {
                        kotlinx.coroutines.delay(25)
                    }
                    server.serverUrl.value.substringAfterLast(':').toInt()
                }
            } ?: error("server did not start")
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            runCatching { server.stop() }
            runCatching { lottieFolder.deleteRecursively() }
        }
    }

    /** Every test starts from "folder configured, no ATEM" — the common church setup. */
    @BeforeTest
    fun resetState() {
        client = HttpClient(CIO)
        server.updateAtemConfig(AtemSettings(), lottieFolder.absolutePath)
    }

    @AfterTest
    fun closeClient() {
        runCatching { client.close() }
    }

    private fun url(path: String) = "http://127.0.0.1:$port$path"

    private fun getting(path: String): HttpResponse = runBlocking { client.get(url(path)) }

    private fun posting(path: String): HttpResponse = runBlocking { client.post(url(path)) }

    private fun HttpResponse.text(): String = runBlocking { bodyAsText() }

    // ── Listing what is available ───────────────────────────────────────────────

    @Test
    fun `the list holds every Lottie in the folder, and nothing else`() {
        val body = getting("/api/lowerthirds").text()

        assertTrue(body.contains(""""name":"Welcome""""), "Welcome is listed: $body")
        assertTrue(body.contains(""""name":"Announcement""""), "Announcement is listed: $body")
        assertTrue(body.contains(""""name":"Untimed""""), "an untimed Lottie is still listed: $body")
        assertTrue(!body.contains("notlottie"), "JSON that is not a Lottie is not a preset: $body")
        assertTrue(!body.contains("readme"), "a non-JSON file is not a preset: $body")
    }

    @Test
    fun `the list carries each animation's duration so a client can time its own UI`() {
        val body = getting("/api/lowerthirds").text()

        // 60 frames at 30fps.
        assertTrue(
            body.contains(""""name":"Welcome","durationMs":2000"""),
            "Welcome is timed at 2000ms: $body",
        )
        assertTrue(
            body.contains(""""name":"Untimed","durationMs":0"""),
            "one that cannot be timed reports 0 rather than dropping out of the list: $body",
        )
    }

    @Test
    fun `the list is empty when no lower-third folder is configured`() {
        server.updateAtemConfig(AtemSettings(), lowerThirdFolder = "")
        assertEquals("[]", getting("/api/lowerthirds").text())
    }

    @Test
    fun `the list is empty when the configured folder does not exist`() {
        server.updateAtemConfig(AtemSettings(), lowerThirdFolder = "/no/such/folder/here")
        assertEquals("[]", getting("/api/lowerthirds").text())
    }

    // ── Fetching one animation ──────────────────────────────────────────────────

    @Test
    fun `a preset's JSON is served whole, so a follower plays the same animation`() {
        val response = getting("/api/lowerthirds/Welcome/json")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(TIMED_LOTTIE, response.text())
    }

    @Test
    fun `a preset is found whatever case the name is asked for in`() {
        // Stream Deck buttons are configured by hand, so the lookup is deliberately case-insensitive.
        assertEquals(HttpStatusCode.OK, getting("/api/lowerthirds/welcome/json").status)
        assertEquals(HttpStatusCode.OK, getting("/api/lowerthirds/WELCOME/json").status)
    }

    @Test
    fun `an unknown preset is a not-found, not an empty animation`() {
        val response = getting("/api/lowerthirds/Nonexistent/json")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.text().contains("lower third not found"), response.text())
    }

    // ── Triggering ──────────────────────────────────────────────────────────────

    @Test
    fun `running an unknown preset is refused rather than starting nothing`() {
        val response = posting("/api/lowerthirds/Nonexistent/run")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.text().contains("lower third not found"), response.text())
    }

    @Test
    fun `showing an unknown preset is refused too`() {
        assertEquals(HttpStatusCode.NotFound, posting("/api/lowerthirds/Nonexistent/show").status)
    }

    @Test
    fun `a Lottie with no timing cannot be run`() {
        // The sequencer needs a duration to schedule the key off-air; without one there is nothing
        // to run, and the client is told which of the two problems it has.
        val response = posting("/api/lowerthirds/Untimed/run")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.text().contains("no timing information"), response.text())
    }

    @Test
    fun `hiding always succeeds, so a panic button is never left stuck`() {
        val response = posting("/api/lowerthirds/hide")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.text().contains(""""status":"stopped""""), response.text())
    }

    @Test
    fun `running against a key that does not exist is refused before anything is shown`() {
        // A switcher with one M/E carrying two upstream keyers: key 3 is not a thing.
        server.updateAtemConfig(
            AtemSettings(
                host = "192.0.2.1",
                detectedMixEffects = 1,
                detectedKeyersPerMe = listOf(2),
            ),
            lottieFolder.absolutePath,
        )

        val response = posting("/api/lowerthirds/Welcome/run?key=3")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(
            response.text().contains("Key 3 does not exist on M/E 1 (available: 1-2)"),
            "the error names the real topology so the button can be fixed: ${response.text()}",
        )
    }

    @Test
    fun `running against an M-E that does not exist is refused`() {
        server.updateAtemConfig(
            AtemSettings(host = "192.0.2.1", detectedMixEffects = 1),
            lottieFolder.absolutePath,
        )

        val response = posting("/api/lowerthirds/Welcome/run?me=2")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(
            response.text().contains("M/E 2 does not exist (available: 1-1)"),
            response.text(),
        )
    }

    @Test
    fun `running against a downstream keyer that does not exist is refused`() {
        server.updateAtemConfig(
            AtemSettings(host = "192.0.2.1", detectedDownstreamKeyers = 1),
            lottieFolder.absolutePath,
        )

        val response = posting("/api/lowerthirds/Welcome/run?keytype=dsk&key=2")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.text().contains("DSK 2 does not exist (available: 1-1)"), response.text())
    }

    // ── The standalone key cut ──────────────────────────────────────────────────

    @Test
    fun `a key cut with no ATEM configured reports the service as unavailable`() {
        // 503 rather than 500: nothing is broken, the feature simply isn't set up.
        val on = posting("/api/atem/key/on")
        assertEquals(HttpStatusCode.ServiceUnavailable, on.status)
        assertTrue(on.text().contains("ATEM not configured"), on.text())

        assertEquals(HttpStatusCode.ServiceUnavailable, posting("/api/atem/key/off").status)
    }

    @Test
    fun `a key cut at a target that does not exist is refused before connecting`() {
        server.updateAtemConfig(
            AtemSettings(
                host = "192.0.2.1",
                detectedMixEffects = 2,
                detectedKeyersPerMe = listOf(4, 4),
            ),
            lottieFolder.absolutePath,
        )

        val response = posting("/api/atem/key/on?me=3")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.text().contains("M/E 3 does not exist (available: 1-2)"), response.text())
    }
}
