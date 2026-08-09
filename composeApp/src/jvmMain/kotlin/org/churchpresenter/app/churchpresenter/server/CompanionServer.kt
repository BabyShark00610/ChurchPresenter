package org.churchpresenter.app.churchpresenter.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveStream
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.io.File
import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.churchpresenter.app.churchpresenter.BuildConfig
import org.churchpresenter.app.churchpresenter.data.Bible
import org.churchpresenter.app.churchpresenter.data.SongItem
import org.churchpresenter.app.churchpresenter.data.Songs
import org.churchpresenter.app.churchpresenter.data.StrongsEntry
import org.churchpresenter.app.churchpresenter.data.settings.AtemSettings
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundSettings
import org.churchpresenter.app.churchpresenter.data.settings.PresentationRemoteSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.models.LyricSection
import org.churchpresenter.app.churchpresenter.models.Question
import org.churchpresenter.app.churchpresenter.models.QuestionDto
import org.churchpresenter.app.churchpresenter.models.QuestionStatus
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.models.SubmitQuestionRequest
import org.churchpresenter.app.churchpresenter.models.VoteRequest
import org.churchpresenter.app.churchpresenter.models.toDto
import org.churchpresenter.app.churchpresenter.presenter.BrowserSourceFrame
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.CrashReporter
import org.churchpresenter.app.churchpresenter.utils.HeicDecoder
import org.churchpresenter.app.churchpresenter.utils.InstanceLinkLogSide
import org.churchpresenter.app.churchpresenter.utils.InstanceLinkLogger
import org.churchpresenter.app.churchpresenter.utils.UsageEvent
import org.churchpresenter.app.churchpresenter.utils.UsageEvents
import org.churchpresenter.app.churchpresenter.utils.isChorusHeader
import org.churchpresenter.app.churchpresenter.utils.isHeaderLine
import org.churchpresenter.app.churchpresenter.viewmodel.QAManager
import org.churchpresenter.app.churchpresenter.viewmodel.isLottieFile
import presentation.engine.DeckRasterizer
import presentation.engine.LoadResult
import presentation.engine.PresentationLoader
import presentation.engine.cache.SlideDiskCache

// ── CompanionServer ───────────────────────────────────────────────────────────

/**
 * Ktor-based HTTP + WebSocket server that exposes song/schedule data
 * to the KMP mobile companion app.
 *
 * Lifecycle: call [start] once, [stop] to clean up.
 * Data is pushed in via [updateSongs] / [updateSchedule].
 * Song-selection events from mobile arrive via [onSongSelected].
 */
class CompanionServer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Serializes background presentation renders for the companion API. A schedule with several
     * presentations would otherwise render every deck concurrently (one IO coroutine each), and
     * each deck holds a full POI [SlideShow] plus a 1920px frame buffer — a few heavy decks at
     * once exhaust the heap (OutOfMemoryError). Rendering one deck at a time caps peak memory to a
     * single deck's footprint; the renders just queue.
     */
    private val presentationRenderMutex = Mutex()

    private var _qaEventJob: kotlinx.coroutines.Job? = null
    var qaManager: QAManager? = null
        set(value) {
            _qaEventJob?.cancel()
            _qaEventJob = null
            field = value
            if (value != null) {
                _qaEventJob = scope.launch {
                    value.events.collect { _ ->
                        broadcast(WebSocketMessage(
                            type = Constants.WS_EVENT_QUESTIONS_UPDATED,
                            payload = ""
                        ))
                    }
                }
            }
        }
    @Volatile var qaAdminPassword: String = ""
    @Volatile var qaCooldownSeconds: Int = 30
    @Volatile var qaVotingEnabled: Boolean = false

    // Presentation remote control settings
    @Volatile var presentationRemoteEnabled: Boolean = false
    @Volatile var presentationRemotePassword: String = ""

    // Current presentation state (updated from desktop, read by remote clients)
    // `internal` rather than private: these are mutable and must be read (and some written)
    // per request by the extracted route groups, so unlike the immutable state they cannot be
    // passed in as parameters.
    @Volatile internal var _currentPresentationId: String = ""
    @Volatile internal var _currentSlideIndex: Int = 0
    @Volatile internal var _currentSlideTotalCount: Int = 0
    @Volatile internal var _presentationFrozen: Boolean = false
    @Volatile internal var _presentationIsPlaying: Boolean = false
    @Volatile internal var _presentationIsLive: Boolean = false
    @Volatile internal var _autoScrollInterval: Int = 5
    @Volatile internal var _presentationIsLooping: Boolean = true

    fun updatePresentationRemoteSettings(settings: PresentationRemoteSettings, apiKey: String) {
        val wasEnabled = presentationRemoteEnabled
        presentationRemoteEnabled = settings.remoteControlEnabled
        presentationRemotePassword = apiKey
        if (wasEnabled && !presentationRemoteEnabled) clearPresentationState()
        InstanceLinkLogger.log(
            InstanceLinkLogSide.PRIMARY, "state_updated",
            mapOf("type" to "presentation_remote_settings", "remoteControlEnabled" to settings.remoteControlEnabled)
        )
    }

    fun updateAutoScrollInterval(secs: Int) {
        if (_autoScrollInterval == secs) return
        _autoScrollInterval = secs
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_AUTO_SCROLL_CHANGED,
            payload = """{"autoScrollInterval":$secs}"""
        ))
    }

    fun updateLoopingState(looping: Boolean) {
        if (_presentationIsLooping == looping) return
        _presentationIsLooping = looping
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_LOOP_CHANGED,
            payload = """{"looping":$looping}"""
        ))
    }

    fun clearPresentationState() {
        _currentPresentationId = ""
        _currentSlideIndex = 0
        _currentSlideTotalCount = 0
        _presentationIsPlaying = false
        _presentationIsLive = false
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_SLIDE_CHANGED,
            payload = """{"id":"","index":0,"total":0,"isPlaying":false,"isLive":false}"""
        ))
    }

    fun updatePresentationLiveStatus(isLive: Boolean) {
        if (_presentationIsLive == isLive) return
        _presentationIsLive = isLive
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_LIVE_CHANGED,
            payload = """{"isLive":$isLive}"""
        ))
    }

    fun broadcastSlideChange(id: String, index: Int, total: Int, isPlaying: Boolean) {
        _currentPresentationId = id
        _currentSlideIndex = index
        _currentSlideTotalCount = total
        _presentationIsPlaying = isPlaying
        val note = _presentationNotes[id]?.getOrNull(index) ?: ""
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_SLIDE_CHANGED,
            payload = """{"id":"$id","index":$index,"total":$total,"isPlaying":$isPlaying,"isLive":$_presentationIsLive,"notes":"${jsonEscape(note)}"}"""
        ))
    }

    fun broadcastFreezeChange(frozen: Boolean) {
        _presentationFrozen = frozen
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PRESENTATION_FREEZE_CHANGED,
            payload = """{"frozen":$frozen}"""
        ))
    }

    /**
     * Broadcasts the desktop media player's playback state to companions (mobile Media tab).
     * Position ticks continuously, so callers poll this on a fixed cadence.
     */
    fun broadcastMediaState(
        isLive: Boolean,
        isLoaded: Boolean,
        isPlaying: Boolean,
        title: String,
        positionMs: Long,
        durationMs: Long,
        volume: Float,
        muted: Boolean,
        mediaType: String,
        source: String,
    ) {
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_MEDIA_STATE_CHANGED,
            payload = """{"isLive":$isLive,"isLoaded":$isLoaded,"isPlaying":$isPlaying,""" +
                """"title":"${jsonEscape(title)}","positionMs":$positionMs,"durationMs":$durationMs,""" +
                """"volume":$volume,"muted":$muted,"mediaType":"${jsonEscape(mediaType)}",""" +
                """"source":"${jsonEscape(source)}"}"""
        ))
    }

    /** Emitted when remote taps Go Live. */
    val onPresentationGoLive = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when remote presses freeze/blank. */
    val onPresentationFreezeToggle = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when remote presses play/pause. */
    val onPresentationPlayPause = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when remote presses the loop toggle. */
    val onPresentationLoopToggle = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when remote jumps to a specific slide index. */
    val onPresentationGoto = MutableSharedFlow<Int>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // ATEM + lower-third folder config for the Companion lower-third sequencer
    @Volatile internal var _atemSettings: AtemSettings? = null
    @Volatile private var _lowerThirdFolder: String = ""

    fun updateAtemConfig(atem: AtemSettings, lowerThirdFolder: String) {
        val prev = _atemSettings
        if (prev == null || prev.host != atem.host || prev.port != atem.port) {
            AtemConnectionManager.invalidate()
        }
        _atemSettings = atem
        _lowerThirdFolder = lowerThirdFolder
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "state_updated", mapOf("type" to "atem_config"))
    }

    // ── Browser Source outputs (OBS/vMix overlay) ─────────────────────────────
    // Content is rendered off-screen in main.kt (BrowserSourceVideoRenderer, the same
    // BiblePresenter/SongPresenter/etc composables used everywhere else) and streamed here over
    // a WebSocket as binary-framed PNG deltas — this class only owns serving, never
    // PresenterManager/content state. (Previously HTTP multipart/x-mixed-replace; switched to
    // WebSocket because that legacy MIME type turned out to be unreliable in both directions —
    // Chrome's <img> support for it is inconsistent, and Safari's fetch()/ReadableStream failed
    // outright with "Load failed" for this exact indefinitely-long streaming response pattern,
    // even on localhost. WebSocket is what the rest of this server already uses for real-time
    // push, and has none of that legacy baggage.)
    @Volatile private var _browserSourceOutputs: List<ScreenAssignment> = emptyList()
    private val _browserSourceFrameFlows = ConcurrentHashMap<Int, SharedFlow<BrowserSourceFrame>>()

    // Live WebSocket sessions per output index, so a renderer replacement can close them —
    // a session holds the flow it captured at connect time, and after re-registration that
    // old flow never emits again while the heartbeat keeps re-sending its stale last frame.
    // Closing forces the overlay page to reconnect (2s backoff) and reseed at the new stream.
    private val _browserSourceSessions = ConcurrentHashMap<Int, MutableSet<DefaultWebSocketServerSession>>()

    fun updateBrowserSourceOutputs(outputs: List<ScreenAssignment>) {
        _browserSourceOutputs = outputs
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "state_updated", mapOf("type" to "browser_source_outputs", "count" to outputs.size))
    }

    fun browserSourceOutput(index: Int): ScreenAssignment? = _browserSourceOutputs.getOrNull(index)

    /**
     * Registers (or replaces) the frame delta flow a given output's renderer produces.
     * Replacing an existing flow (renderer restarted, e.g. after a resolution/fps change)
     * closes that output's connected sessions so clients reconnect to the new stream.
     */
    fun registerBrowserSourceFrames(index: Int, frames: SharedFlow<BrowserSourceFrame>) {
        val previous = _browserSourceFrameFlows.put(index, frames)
        if (previous != null && previous !== frames) {
            val stranded = _browserSourceSessions.remove(index) ?: return
            scope.launch {
                stranded.forEach { session ->
                    try {
                        session.close(CloseReason(CloseReason.Codes.SERVICE_RESTART, "Renderer restarted"))
                    } catch (_: Exception) {
                        // already gone
                    }
                }
            }
        }
    }

    /** Pure check for the given output's independent Browser Source API-key requirement (separate from [_apiKeyEnabled]) — no response side effects, usable from both HTTP and WebSocket routes. */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun browserSourceApiKeyValid(call: ApplicationCall, output: ScreenAssignment): Boolean {
        if (!output.browserSourceApiKeyRequired || _apiKey.value.isEmpty()) return true
        val provided = call.request.headers[Constants.HEADER_API_KEY]
            ?: call.request.queryParameters[Constants.QUERY_PARAM_API_KEY]
            ?: ""
        return MessageDigest.isEqual(provided.toByteArray(), _apiKey.value.toByteArray())
    }

    /** Same check as [browserSourceApiKeyValid], but responds 401 on an HTTP route when invalid. */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun checkBrowserSourceApiKey(call: ApplicationCall, output: ScreenAssignment): Boolean {
        if (browserSourceApiKeyValid(call, output)) return true
        call.respond(HttpStatusCode.Unauthorized, "Invalid API key")
        return false
    }

    /**
     * Packs one [BrowserSourceFrame] into a single WebSocket binary message: a fixed 24-byte
     * big-endian header (x, y, rectWidth, rectHeight, fullWidth, fullHeight — six Int32s) followed
     * by the raw image bytes — PNG when the frame carries transparency, JPEG when fully opaque
     * (the client sniffs the first payload byte: 0x89 = PNG, 0xFF = JPEG). The client reads this
     * with a matching `DataView`. Using WebSocket
     * binary messages instead of HTTP multipart/x-mixed-replace means each frame's boundary is
     * handled natively by the WebSocket protocol — no manual buffer/boundary parsing needed on
     * either side, and no dependence on a legacy MIME type with inconsistent engine support.
     */
    /**
     * The Browser Source overlay page for [index]. The page itself is
     * [browserSourceOverlayPage] in BrowserSourcePage.kt; this reads the API-key state it
     * needs so callers do not have to.
     */
    internal fun browserSourceOverlayPageHtml(
        index: Int,
        output: ScreenAssignment,
        bgOverride: String? = null,
    ): String = browserSourceOverlayPage(
        index, output, _apiKeyEnabled.value, _apiKey.value, bgOverride
    )

    internal fun encodeBrowserSourceFrameMessage(frame: BrowserSourceFrame): ByteArray {
        val buf = java.nio.ByteBuffer.allocate(24 + frame.png.size)
        buf.putInt(frame.x)
        buf.putInt(frame.y)
        buf.putInt(frame.rectWidth)
        buf.putInt(frame.rectHeight)
        buf.putInt(frame.fullWidth)
        buf.putInt(frame.fullHeight)
        buf.put(frame.png)
        return buf.array()
    }

    /** Lottie files in the configured lower-third folder. */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun lowerThirdFiles(): List<File> =
        File(_lowerThirdFolder).takeIf { _lowerThirdFolder.isNotEmpty() && it.isDirectory }
            ?.listFiles { f -> f.extension.lowercase() == "json" && isLottieFile(f) }
            ?.sortedBy { it.nameWithoutExtension.lowercase() } ?: emptyList()

    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun jsonStr(s: String): String =
        json.encodeToString(kotlinx.serialization.serializer<String>(), s)

    /** Shared body of the run/show endpoints. */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun handleLowerThirdTrigger(
        call: ApplicationCall,
        autoEnd: Boolean
    ) {
        val rawName = call.parameters["name"] ?: ""
        val file = lowerThirdFiles().firstOrNull { it.nameWithoutExtension.equals(rawName, ignoreCase = true) }
        if (file == null) {
            call.respond(HttpStatusCode.NotFound, """{"error":"lower third not found"}""")
            return
        }
        val ltJson = try { file.readText() } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError, """{"error":"could not read lottie file"}""")
            return
        }
        val durationMs = LottieRenderCache.lottieDurationMs(ltJson)
        if (durationMs == null) {
            call.respond(HttpStatusCode.UnprocessableEntity, """{"error":"lottie has no timing information"}""")
            return
        }
        val atem = _atemSettings ?: AtemSettings()

        // Key target: USK (M/E + keyer) or DSK (?keytype / setting) from settings;
        // ?me=N&key=M (1-based) override; ?key=0 skips. For DSK ?key overrides the DSK index.
        val useDsk = resolveUseDsk(call, atem)
        val meParam = call.request.queryParameters["me"]?.toIntOrNull()
        val keyParam = call.request.queryParameters["key"]?.toIntOrNull()
        val mixEffect: Int?
        val keyer: Int?
        if (keyParam == 0) {
            mixEffect = null; keyer = null
        } else {
            mixEffect = if (useDsk) 0 else (if (meParam != null) meParam - 1 else atem.keyMixEffect)
            keyer = if (keyParam != null) keyParam - 1
                else if (useDsk) atem.dskIndex else atem.keyIndex
            validateKeyTarget(atem, useDsk, mixEffect, keyer)?.let {
                call.respond(HttpStatusCode.BadRequest, """{"error":${jsonStr(it)}}""")
                return
            }
        }

        val pause = call.request.queryParameters["pause"]?.toBooleanStrictOrNull() ?: false
        val pauseDurationMs = call.request.queryParameters["pauseDurationMs"]?.toLongOrNull() ?: 2000L

        val keyError = LowerThirdSequencer.run(
            name = file.nameWithoutExtension,
            json = ltJson,
            durationMs = durationMs,
            pauseAtFrame = pause,
            pauseDurationMs = pauseDurationMs,
            mixEffect = mixEffect,
            keyer = keyer,
            atem = atem,
            useDownstreamKey = useDsk,
            autoEnd = autoEnd
        )
        val totalMs = atem.keyPreRollMs + durationMs +
            (if (pause) pauseDurationMs else 0L) + atem.keyPostRollMs
        call.respondText(
            """{"status":"started","name":${jsonStr(file.nameWithoutExtension)},"durationMs":$durationMs,""" +
                """"totalMs":${if (autoEnd) totalMs else -1},"keyError":${keyError?.let { jsonStr(it) } ?: "null"}}""",
            ContentType.Application.Json
        )
    }

    /**
     * Standalone upstream-key on/off (POST /api/atem/key/on|off). Reuses the shared
     * keepalive connection when free; falls back to a short-lived connection when an
     * upload holds it, so a key cut never waits behind an upload. Synchronous 200/502.
     */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun handleKeyToggle(call: ApplicationCall, onAir: Boolean) {
        val atem = _atemSettings
        if (atem == null || atem.host.isBlank()) {
            call.respond(HttpStatusCode.ServiceUnavailable, """{"error":"ATEM not configured"}""")
            return
        }
        val useDsk = resolveUseDsk(call, atem)
        val meParam = call.request.queryParameters["me"]?.toIntOrNull()
        val keyParam = call.request.queryParameters["key"]?.toIntOrNull()
        val mixEffect = if (useDsk) 0 else (if (meParam != null) meParam - 1 else atem.keyMixEffect)
        val keyer = if (keyParam != null) keyParam - 1
            else if (useDsk) atem.dskIndex else atem.keyIndex
        validateKeyTarget(atem, useDsk, mixEffect, keyer)?.let {
            call.respond(HttpStatusCode.BadRequest, """{"error":${jsonStr(it)}}""")
            return
        }
        try {
            val ran = AtemConnectionManager.tryRun(atem.host, atem.port) { client ->
                client.setKeyOnAir(useDsk, mixEffect, keyer, onAir)
            }
            if (!ran) AtemClient.cutKey(atem.host, atem.port, useDsk, mixEffect, keyer, onAir)
            val target = if (useDsk) """"dsk":${keyer + 1}""" else """"me":${mixEffect + 1},"key":${keyer + 1}"""
            call.respondText(
                """{"status":"${if (onAir) "on" else "off"}",$target}""",
                ContentType.Application.Json
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadGateway,
                """{"error":${jsonStr(e.message ?: "ATEM command failed")}}"""
            )
        }
    }

    /**
     * Validate a 0-based key target against the detected topology. Null = OK.
     * For a downstream key [keyer] is the DSK index and [mixEffect] is ignored.
     */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun validateKeyTarget(atem: AtemSettings, useDsk: Boolean, mixEffect: Int, keyer: Int): String? {
        if (useDsk) {
            if (atem.detectedDownstreamKeyers > 0 && keyer !in 0 until atem.detectedDownstreamKeyers)
                return "DSK ${keyer + 1} does not exist (available: 1-${atem.detectedDownstreamKeyers})"
            return null
        }
        if (atem.detectedMixEffects > 0 && mixEffect !in 0 until atem.detectedMixEffects)
            return "M/E ${mixEffect + 1} does not exist (available: 1-${atem.detectedMixEffects})"
        val keyers = atem.detectedKeyersPerMe.getOrNull(mixEffect)
        if (keyers != null && keyers > 0 && keyer !in 0 until keyers)
            return "Key ${keyer + 1} does not exist on M/E ${mixEffect + 1} (available: 1-$keyers)"
        return null
    }

    /**
     * Resolves whether a request should drive a downstream key: `?keytype=dsk|usk` (or
     * `downstream|upstream`) overrides; otherwise the persisted [AtemSettings.useDownstreamKey].
     */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun resolveUseDsk(call: ApplicationCall, atem: AtemSettings): Boolean =
        when (call.request.queryParameters["keytype"]?.lowercase()) {
            "dsk", "downstream" -> true
            "usk", "upstream" -> false
            else -> atem.useDownstreamKey
        }

    // Current data — thread-safe StateFlows
    // All songs flat list
    // Current catalog — rebuilt whenever songs are updated
    private val _catalog = MutableStateFlow(SongCatalogResponse(emptyList(), 0, 0))
    /** Raw song list kept in sync with _catalog for per-number detail lookups */
    @Volatile internal var _songs: List<SongItem> = emptyList()
    private val _bibleCatalog = MutableStateFlow<BibleCatalogResponse?>(null)
    private val _bible = MutableStateFlow<Bible?>(null)
    /** Absolute path to the primary bible's .spb file — serves GET /api/bible/file for InstanceLink followers. */
    @Volatile internal var _bibleFilePath: String = ""
    /** Same as [_bibleFilePath] but for the secondary bible — serves GET /api/bible/file/secondary,
     *  only used when a follower opts in to mirroring the secondary too (most don't). */
    @Volatile internal var _secondaryBibleFilePath: String = ""
    @Volatile internal var _bibleFilePaths: List<String> = emptyList()
    /** Current background settings — serves GET /api/backgrounds for a follower that opted in to
     *  mirroring backgrounds. The image/video fields are still local file paths on this machine;
     *  GET /api/backgrounds/asset/{slot} resolves the current path for a given slot on demand. */
    private val _backgroundSettings = MutableStateFlow(BackgroundSettings())
    private val _schedule = MutableStateFlow<List<ScheduleItemDto>>(emptyList())
    /** Snapshot of whatever is currently live — see [LiveStateDto]. */
    private val _liveState = MutableStateFlow<LiveStateDto?>(null)
    /** Device IDs of currently-connected WS clients that identified as an Instance Link follower
     *  (as opposed to a regular mobile/browser companion client) — see [Constants.HEADER_CLIENT_ROLE]. */
    private val _connectedInstanceLinkFollowers = MutableStateFlow<Set<String>>(emptySet())
    val connectedInstanceLinkFollowers: StateFlow<Set<String>> = _connectedInstanceLinkFollowers.asStateFlow()

    /**
     * Device ids the operator has permanently blocked (mirrored from `RemoteClientManager`, which
     * owns the list and its persistence).
     *
     * Blocking used to stop only the requests that ask for approval — adding to the schedule,
     * projecting, removing. Everything instant went straight through: a blocked phone or linked
     * instance could still put a verse, a picture or a slide on the main screen, and still received
     * the whole live feed. Enforced here rather than at each of the ~15 command branches so a new
     * command cannot be added without it.
     */
    @Volatile
    var blockedClientIds: Set<String> = emptySet()

    /** Blank ids are anonymous clients, which cannot be blocked (there is nothing to block). */
    internal fun isClientBlocked(clientId: String): Boolean =
        clientId.isNotBlank() && clientId in blockedClientIds
    /** schedule item UUID → absolute local media file path — populated by updateSchedule, serves /api/media/stream */
    private val _scheduleItemToMediaPath = ConcurrentHashMap<String, String>()

    // Presentation catalog — metadata only; raw JPEG bytes stored per-slide in _slideBytes
    private val _presentationCatalog = MutableStateFlow(PresentationCatalogResponse(emptyList(), 0))
    /** presentationId → list of JPEG-encoded slide bytes (index = slide number). Max 5 cached. */
    private val _slideBytes = ConcurrentHashMap<String, List<ByteArray>>()
    private val _slideBytesOrder = java.util.concurrent.ConcurrentLinkedDeque<String>()
    private val MAX_CACHED_PRESENTATIONS = 5
    /** presentationId (file hash) → PresentationDto — covers tab-loaded and background-rendered items */
    private val _presentationCatalogs = ConcurrentHashMap<String, PresentationDto>()
    /** presentationId (file hash) → absolute file path — populated by updatePresentation and updateSchedule */
    private val _presentationFilePaths = ConcurrentHashMap<String, String>()
    /** presentationId (file hash) → per-slide presenter notes (index = slide number) */
    private val _presentationNotes = ConcurrentHashMap<String, List<String>>()
    /** schedule item UUID → presentation file hash — populated when schedule is updated */
    private val _scheduleItemToPresentationId = ConcurrentHashMap<String, String>()
    /** Set of presentation IDs currently being background-rendered (avoids duplicate renders) */
    private val _renderingPresentations = ConcurrentHashMap<String, Unit>()
    /** Cancels previous updatePresentation encode job when a new presentation is loaded */
    private var _activeUpdateJob: Job? = null
    /** Shared slide disk cache — same directory PresentationViewModel renders into (one render, both consumers). */
    private val slideDiskCache = SlideDiskCache()

    private fun cacheSlideBytes(id: String, slides: List<ByteArray>) {
        _slideBytes[id] = slides
        _slideBytesOrder.remove(id)
        _slideBytesOrder.addFirst(id)
        while (_slideBytesOrder.size > MAX_CACHED_PRESENTATIONS) {
            val evicted = _slideBytesOrder.pollLast()
            if (evicted != null) {
                _slideBytes.remove(evicted)
                _presentationNotes.remove(evicted)
            }
        }
    }

    /** Stable folder ID used for all device-uploaded photos (accumulates across sessions). */
    private val DEVICE_UPLOADS_FOLDER_ID = "device_uploads"

    /**
     * ID of the most recently device-uploaded presentation file.
     * Cleared from [_presentationCatalogs], [_slideBytes], and [_presentationFilePaths] when a new
     * upload replaces it, so the mobile's presentation list never accumulates stale entries.
     */
    @Volatile internal var _lastDeviceUploadedPresentationId: String? = null

    // Picture catalog — metadata + file references stored per folder
    private val _pictureCatalog = MutableStateFlow<PictureFolderResponse?>(null)
    /** folderId → ordered list of image Files (index = image order) */
    private val _pictureFiles = ConcurrentHashMap<String, List<File>>()
    /** folderId → catalog metadata (covers both the active folder and all schedule picture items) */
    private val _pictureCatalogs = ConcurrentHashMap<String, PictureFolderResponse>()
    /** Recognised image extensions — matches PicturesViewModel */
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

    // API key config (updated from settings without restart)
    private val _apiKeyEnabled = MutableStateFlow(false)
    private val _apiKey = MutableStateFlow("")

    // File upload permission (updated from settings without restart)
    private val _fileUploadEnabled = MutableStateFlow(true)
    // Max media-upload size in MB (updated from settings without restart)
    private val _maxMediaUploadMb = MutableStateFlow(Constants.DEFAULT_MAX_MEDIA_UPLOAD_MB)

    // Outgoing WebSocket broadcast channel. Buffer sized generously: it is shared by every
    // connected client's collector, and DROP_OLDEST means an overflow silently loses a message
    // for slow consumers with no redelivery until their next reconnect snapshot.
    // `internal` so a test can subscribe to exactly what a connected phone would receive, without
    // standing up a WebSocket client to read it back.
    internal val broadcastChannel = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Incoming song-selection requests from mobile clients
    val onSongSelected = MutableSharedFlow<ScheduleSongDto>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client requests an item to be added to the schedule. */
    val onAddToSchedule = MutableSharedFlow<PendingRemoteRequest>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client requests multiple items to be added to the schedule in one call. */
    val onAddBatchToSchedule = MutableSharedFlow<PendingBatchRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client requests an item to be removed from the schedule — same
     *  approval flow as [onAddToSchedule], just the reverse operation. */
    val onRemoveFromSchedule = MutableSharedFlow<PendingRemoveRequest>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client selects a picture image (POST /api/pictures/select or WS select_picture). */
    val onSelectPicture = MutableSharedFlow<SelectPictureRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emitted when a remote client navigates to a specific song section while live
     * (POST /api/songs/{number}/select or WS "select_song_section").
     */
    val onSelectSongSection = MutableSharedFlow<SelectSongSectionRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emitted when a remote client selects a specific slide to display
     * (POST /api/presentations/{id}/select or WS "select_slide").
     * No approval required — applied instantly.
     */
    val onSelectSlide = MutableSharedFlow<SelectSlideRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emitted when a mobile client uploads a presentation file via POST /api/presentations/upload.
     * The emitted [File] has already been saved to disk and is ready to be loaded by
     * [PresentationViewModel.addPresentation].
     */
    val onPresentationUploaded = MutableSharedFlow<File>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emitted when a remote client selects a Bible verse to display instantly
     * (POST /api/bible/select or WS "select_bible_verse").
     * No approval required — applied instantly.
     */
    val onSelectBibleVerse = MutableSharedFlow<SelectBibleVerseRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client requests an item to be sent directly to projection. */
    val onProject = MutableSharedFlow<PendingRemoteRequest>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a device authenticates against the presentation remote for the first time this session. */
    val onPresentationRemoteConnect = MutableSharedFlow<PendingConnectionRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client calls POST /api/clear or sends WS "clear". Clears the display instantly. */
    /** Emitted when a remote client requests a QA admin operation (add/edit/delete). */
    data class PendingQAAdminRequest(
        val action: String,
        val questionId: String = "",
        val text: String = "",
        val clientId: String = "",
        val decision: kotlinx.coroutines.CompletableDeferred<Boolean> = kotlinx.coroutines.CompletableDeferred()
    )

    val onQAAdminRequest = MutableSharedFlow<PendingQAAdminRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a device authenticates against the Q&A admin panel for the first time this session. */
    val onQaAdminConnect = MutableSharedFlow<PendingConnectionRequest>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when the web admin triggers Go Live or clear display for Q&A. Payload: Question or null. */
    val onQADisplay = MutableSharedFlow<Question?>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val onClear = MutableSharedFlow<Unit>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Emitted when a remote client sends WS "bible_hold". Payload: {"hold": true/false}. */
    val onBibleHold = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** ID-less navigation commands (Instance Link Controller mode) — advance/retreat whatever the
     *  primary currently has live, since a Controller has no way to learn the primary's internally-
     *  assigned folderId/presentationId. See Constants.WS_CMD_NEXT_PICTURE and siblings. */
    val onNextPicture = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onPreviousPicture = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onNextSlide = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onPreviousSlide = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // Media transport controls from a companion remote
    val onMediaPlayPause = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaStop = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaSeekForward = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaSeekBackward = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaSeekTo = MutableSharedFlow<Long>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaSetVolume = MutableSharedFlow<Float>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMediaMuteToggle = MutableSharedFlow<Unit>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Emitted for every instant (no-approval) action so the UI can show an activity toast.
     * Carries enough info to build a [RemoteActivityNotification] without approval logic.
     */
    data class RemoteInstantAction(
        /** One of: "present", "upload", "clear" — maps to RemoteEventType in the UI layer. */
        val actionType: String,
        val title: String,
        val detail: String = "",
        val clientId: String = ""
    )

    val onInstantAction = MutableSharedFlow<RemoteInstantAction>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    val tunnelManager = TunnelManager()


    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    internal var currentPort: Int = Constants.SERVER_DEFAULT_PORT

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        // Without this, fields still at their Kotlin default value (e.g. an unmodified
        // BibleSettings/SongSettings/StageMonitorSettings) are omitted from the JSON
        // entirely, which the Browser Source overlay page's JS can't distinguish from
        // "field doesn't exist" — it needs every field present to apply real styling.
        encodeDefaults = true
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Update API key settings without restarting the server. */
    fun  updateApiKey(enabled: Boolean, key: String) {
        _apiKeyEnabled.value = enabled
        _apiKey.value = key
    }

    /** Allow or disallow file uploads from mobile devices without restarting the server. */
    fun updateFileUploadEnabled(enabled: Boolean) {
        _fileUploadEnabled.value = enabled
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "state_updated", mapOf("type" to "file_upload_enabled", "enabled" to enabled))
    }

    /** Update the max media-upload size (MB) without restarting the server. */
    fun updateMaxMediaUploadMb(mb: Int) {
        _maxMediaUploadMb.value = mb.coerceAtLeast(1)
    }

    /**
     * Preloads songs and bible from disk on the server's IO scope.
     * Safe to call at any time; re-call whenever settings change.
     */
    fun preloadData(
        songStorageDir: String,
        bibleStorageDir: String,
        primaryBibleFileName: String
    ) {
        scope.launch {
            // ── Songs ──────────────────────────────────────────────────────────
            if (songStorageDir.isNotEmpty()) {
                try {
                    val dir = File(songStorageDir)
                    if (dir.exists() && dir.isDirectory) {
                        val songs = Songs()
                        dir.listFiles { f -> f.extension.lowercase() == Constants.EXTENSION_SPS }
                            ?.sortedBy { it.name }
                            ?.forEach { file ->
                                try { songs.loadFromSpsAppend(file.absolutePath) } catch (e: Exception) {
                                    System.err.println("[CompanionServer] Failed to load song ${file.name}: ${e.message}")
                                    CrashReporter.reportWarning(
                                        "Server: Failed to load song ${file.name}",
                                        throwable = e,
                                        tags = mapOf("subsystem" to "server")
                                    )
                                }
                            }
                        if (songs.getSongCount() > 0) {
                            updateSongs(songs.getSongs())
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("[CompanionServer] Failed to load songs from $songStorageDir: ${e.message}")
                    CrashReporter.reportWarning(
                        "Server: Failed to load songs from storage",
                        throwable = e,
                        tags = mapOf("subsystem" to "server")
                    )
                }
            }

            // ── Bible ──────────────────────────────────────────────────────────
            if (bibleStorageDir.isNotEmpty() && primaryBibleFileName.isNotEmpty()) {
                try {
                    val file = File(bibleStorageDir, primaryBibleFileName)
                    if (file.exists()) {
                        val bible = Bible()
                        bible.loadFromSpb(file.absolutePath)
                        updateBible(bible, primaryBibleFileName)
                    }
                } catch (e: Exception) {
                    System.err.println("[CompanionServer] Failed to load bible $primaryBibleFileName: ${e.message}")
                    CrashReporter.reportWarning(
                        "Server: Failed to load bible $primaryBibleFileName",
                        throwable = e,
                        tags = mapOf("subsystem" to "server")
                    )
                }
            }
        }
    }

    /** Feed the full song list — builds grouped catalog and broadcasts to WS clients. */
    fun updateSongs(songs: List<SongItem>) {
        _songs = songs
        val catalog = buildCatalog(songs)
        _catalog.value = catalog
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_SONGS_UPDATED,
            payload = json.encodeToString(SongCatalogResponse.serializer(), catalog)
        ))
    }

    /** Feed the primary Bible — builds full nested catalog and broadcasts to WS clients. */
    fun updateBible(bible: Bible, translation: String, filePath: String = "") {
        _bible.value = bible
        _bibleFilePath = filePath
        val catalog = buildBibleCatalog(bible, translation)
        _bibleCatalog.value = catalog
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_BIBLE_UPDATED,
            payload = json.encodeToString(BibleCatalogResponse.serializer(), catalog)
        ))
    }

    /** Records the secondary bible's file path for GET /api/bible/file/secondary — no mobile
     *  companion catalog/broadcast exists for the secondary bible, only InstanceLink uses this. */
    fun updateSecondaryBibleFilePath(filePath: String) {
        if (_secondaryBibleFilePath == filePath) return
        _secondaryBibleFilePath = filePath
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "state_updated", mapOf("type" to "secondary_bible_file_path", "filePath" to filePath))
        // Invalidation signal for followers mirroring the secondary bible — they re-download
        // the .spb on this event instead of trusting their local cache forever.
        broadcast(WebSocketMessage(type = Constants.WS_EVENT_SECONDARY_BIBLE_UPDATED, payload = ""))
    }

    /** Records every configured Bible module in presentation order for Instance Link replicas. */
    fun updateBibleFilePaths(filePaths: List<String>) {
        val existing = filePaths.filter { File(it).exists() }
        if (_bibleFilePaths == existing) return
        _bibleFilePaths = existing
        broadcast(WebSocketMessage(type = Constants.WS_EVENT_SECONDARY_BIBLE_UPDATED, payload = ""))
    }

    /** Records the current background settings for GET /api/backgrounds — only consumed by a
     *  follower that opted in to mirroring backgrounds (see InstanceLinkSettings.mirrorBackgrounds). */
    fun updateBackgroundSettings(settings: BackgroundSettings) {
        if (_backgroundSettings.value == settings) return
        _backgroundSettings.value = settings
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "state_updated", mapOf("type" to "background_settings"))
        // Invalidation signal for followers mirroring backgrounds — they clear their asset
        // cache and re-fetch on this event.
        broadcast(WebSocketMessage(type = Constants.WS_EVENT_BACKGROUNDS_UPDATED, payload = ""))
    }

    /**
     * Feed a loaded presentation (id, fileName, fileType and already-encoded JPEG slide files).
     * Reads bytes from disk on the IO thread; no re-encoding needed since files are already JPEG.
     */
    fun updatePresentation(
        id: String,
        filePath: String,
        fileName: String,
        fileType: String,
        slideFiles: List<File>,
        slideNotes: List<String> = emptyList()
    ) {
        if (filePath.isNotBlank()) {
            _presentationFilePaths[id] = filePath
        }
        _presentationNotes[id] = slideNotes
        _activeUpdateJob?.cancel()
        _activeUpdateJob = scope.launch {
            // slideFiles can be deleted out from under this coroutine (e.g. removePresentation()
            // invalidating the shared disk cache) while it's queued on Dispatchers.IO — treat a
            // vanished cache the same way renderPresentationForServer does: skip, don't crash.
            try {
                val jpegSlides = slideFiles.map { it.readBytes() }
                cacheSlideBytes(id, jpegSlides)

                val catalog = buildPresentationCatalog(id, fileName, fileType, jpegSlides.size)
                _presentationCatalogs[id] = catalog.presentations.first()
                _presentationCatalog.value = catalog
                broadcast(WebSocketMessage(
                    type = Constants.WS_EVENT_PRESENTATION_UPDATED,
                    payload = json.encodeToString(PresentationCatalogResponse.serializer(), catalog)
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Feed the current picture folder — stores file references and broadcasts
     * [Constants.WS_EVENT_PICTURES_UPDATED]. Image bytes are read from disk on-demand
     * when a remote client requests [Constants.ENDPOINT_PICTURES]/{id}/images/{index}.
     *
     * [folderId] is a stable ID derived from the folder path (e.g. hex hash).
     * [folderName] is the display name shown to remote clients.
     * [folderPath] is the absolute filesystem path.
     * [imageFiles] is the ordered list of image Files in the folder.
     */
    fun updatePictures(
        folderId: String,
        folderName: String,
        folderPath: String,
        imageFiles: List<File>
    ) {
        // Store file references — bytes are read on-demand when a client requests an image
        _pictureFiles[folderId] = imageFiles.toList()

        val catalog = PictureFolderResponse(
            folderId = folderId,
            folderName = folderName,
            folderPath = folderPath,
            imageTotal = imageFiles.size,
            images = imageFiles.mapIndexed { index, file ->
                PictureFileDto(
                    index = index,
                    fileName = file.name,
                    thumbnailUrl = "${Constants.ENDPOINT_PICTURES}/$folderId/images/$index"
                )
            }
        )
        _pictureCatalog.value = catalog
        _pictureCatalogs[folderId] = catalog
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_PICTURES_UPDATED,
            payload = json.encodeToString(PictureFolderResponse.serializer(), catalog)
        ))
    }

    /**
     * Returns the [File] for a specific image by folder ID and zero-based index, or null if not
     * found.  Used by the remote-select handler in MainDesktop so the correct file is presented
     * even when the requested folder differs from the currently loaded folder in the Pictures tab
     * (e.g. when the mobile sends a `device_uploads` selection).
     */
    fun getImageFile(folderId: String, index: Int): File? =
        _pictureFiles[folderId]?.getOrNull(index)

    /**
     * The folder-id of the currently active picture folder pushed to mobile companions via
     * GET /api/pictures.  Null until a folder has been loaded in the Pictures tab.
     */
    val activeFolderId: String? get() = _pictureCatalog.value?.folderId

    /**
     * Scans [folderPath] for image files, then caches them in [_pictureFiles] and [_pictureCatalogs]
     * under [id] (the schedule item's UUID).  Skipped if [id] is already registered.
     * Must be called on an IO thread.
     */
    private fun registerPictureItem(id: String, folderPath: String, folderName: String) {
        if (_pictureFiles.containsKey(id)) return          // already cached
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return
        val imageFiles = folder.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
            ?.sortedBy { it.name }
            ?: return
        if (imageFiles.isEmpty()) return
        _pictureFiles[id] = imageFiles
        _pictureCatalogs[id] = PictureFolderResponse(
            folderId   = id,
            folderName = folderName,
            folderPath = folderPath,
            imageTotal = imageFiles.size,
            images     = imageFiles.mapIndexed { index, file ->
                PictureFileDto(
                    index        = index,
                    fileName     = file.name,
                    thumbnailUrl = "${Constants.ENDPOINT_PICTURES}/$id/images/$index"
                )
            }
        )
    }

    /**
     * Clears the device_uploads directory tree on server startup so that device photos
     * are session-only — they disappear when the server is restarted.
     * Deletes all dated subdirectories (e.g. device_uploads/2026-04-13/) and resets
     * every in-memory catalog entry whose folder-id starts with [DEVICE_UPLOADS_FOLDER_ID].
     */
    private fun clearDeviceUploads() {
        val baseDir = File(System.getProperty("user.home"), ".churchpresenter/device_uploads")
        baseDir.deleteRecursively()   // removes dated subdirs and all files inside them
        // Purge every device_uploads_* entry (handles any date or legacy flat entries)
        _pictureFiles.keys
            .filter { it == DEVICE_UPLOADS_FOLDER_ID || it.startsWith("${DEVICE_UPLOADS_FOLDER_ID}_") }
            .forEach { id -> _pictureFiles.remove(id); _pictureCatalogs.remove(id) }
    }

    /**
     * Renders a schedule presentation for the mobile companion API using the shared presentation
     * engine. When the Presentation tab already rendered this file into the shared disk cache the
     * JPEGs are reused directly (any resolution); otherwise the deck is rendered here — into the
     * same shared cache, so a later tab open at the default width hits it too.
     */
    private fun renderPresentationForServer(presentationId: String, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        try {
            val jpegSlides: List<ByteArray>
            val notes: List<String>
            val cached = slideDiskCache.lookup(file, renderWidthPx = null)
            if (cached != null) {
                jpegSlides = cached.slideFiles.map { it.readBytes() }
                notes = cached.notes
            } else {
                val deck = when (val result = PresentationLoader.load(file)) {
                    is LoadResult.Failure -> {
                        CrashReporter.reportWarning(
                            "Presentation: No slides extracted from ${file.extension.lowercase()} file (server)",
                            tags = mapOf(
                                "subsystem" to "presentation",
                                "file.type" to file.extension.lowercase(),
                                "failure.reason" to result.error.name.lowercase()
                            )
                        )
                        return
                    }
                    is LoadResult.Success -> result.deck
                }
                notes = deck.slides.map { it.notes }
                val writer = slideDiskCache.beginWrite(file, deck.format, DeckRasterizer.DEFAULT_TARGET_WIDTH_PX)
                var committed = false
                try {
                    jpegSlides = CrashReporter.trace("server.render", "Server render presentation") {
                        DeckRasterizer(deck).use { rasterizer ->
                            deck.slides.map { slide ->
                                val slideFile = writer.putSlide(
                                    index = slide.index,
                                    image = rasterizer.renderFinalFrame(slide.index),
                                    note = slide.notes,
                                    fidelity = slide.fidelity,
                                    hasTimeline = slide.timeline != null
                                )
                                slideFile.readBytes()
                            }
                        }
                    }
                    writer.commit()
                    committed = true
                } finally {
                    if (!committed) writer.abort()
                }
            }
            if (jpegSlides.isEmpty()) return
            cacheSlideBytes(presentationId, jpegSlides)
            _presentationFilePaths[presentationId] = filePath
            _presentationNotes[presentationId] = notes
            val slideDtos = jpegSlides.indices.map { i ->
                SlideDto(slideIndex = i, thumbnailUrl = "${Constants.ENDPOINT_PRESENTATIONS}/$presentationId/slides/$i")
            }
            _presentationCatalogs[presentationId] = PresentationDto(
                id         = presentationId,
                fileName   = file.nameWithoutExtension,
                fileType   = file.extension.lowercase(),
                slideTotal = jpegSlides.size,
                slides     = slideDtos
            )
        } catch (oom: OutOfMemoryError) {
            // A background companion-API render must never take down the live app. OOM is an
            // Error, not an Exception, so the catch below wouldn't stop it escaping the coroutine
            // as an uncaught crash. Degrade to a warning and drop this presentation's render — the
            // client falls back to the 404-retry path (rendering still pending) just as it would
            // for any other render failure.
            CrashReporter.reportWarning(
                "Presentation: Out of memory rendering ${file.extension.lowercase()} for companion API (server)",
                tags = mapOf(
                    "subsystem" to "presentation",
                    "file.type" to file.extension.lowercase()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSchedule(items: List<ScheduleItem>) {
        items.forEach(::registerScheduleItemResources)
        val dtos = items.map { it.toDto() }
        _schedule.value = dtos
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_SCHEDULE_UPDATED,
            payload = json.encodeToString(ScheduleResponse.serializer(), ScheduleResponse(dtos, dtos.size))
        ))
    }

    /**
     * Server-side resources a schedule item needs before clients can ask for it: picture folders
     * are catalogued, presentations start rendering in the background, and local media paths are
     * recorded so the media endpoint can serve them. Item types with nothing to register fall
     * through.
     *
     * Split out of [updateSchedule]'s mapping loop so [toDto] stays a pure function of the item.
     * Runs for every item before any is mapped; the DTOs don't read anything this writes, so the
     * published schedule is identical either way.
     */
    private fun registerScheduleItemResources(item: ScheduleItem) {
        when (item) {
            is ScheduleItem.PictureItem -> scope.launch(Dispatchers.IO) {
                registerPictureItem(item.id, item.folderPath, item.folderName)
            }
            is ScheduleItem.PresentationItem -> {
                val presentationId = item.filePath.hashCode().toUInt().toString(16)
                _scheduleItemToPresentationId[item.id] = presentationId
                _presentationFilePaths[presentationId] = item.filePath
                if (!_slideBytes.containsKey(presentationId) &&
                    _renderingPresentations.putIfAbsent(presentationId, Unit) == null) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            // One render at a time — see presentationRenderMutex.
                            presentationRenderMutex.withLock {
                                renderPresentationForServer(presentationId, item.filePath)
                            }
                        } finally {
                            _renderingPresentations.remove(presentationId)
                        }
                    }
                }
            }
            is ScheduleItem.MediaItem ->
                if (item.mediaType == "local") _scheduleItemToMediaPath[item.id] = item.mediaUrl
            else -> Unit
        }
    }

    /**
     * Broadcasts a snapshot of whatever is currently live — fills the gap for content types with
     * no dedicated "now live" event (bible, songs, pictures, media, lower thirds, announcements,
     * websites, scenes, Q&A, dictionary). Presentations rely on the existing slide-changed events
     * instead — [mode] == "PRESENTATION" here is informational only.
     */
    fun updateLiveState(
        mode: String,
        bibleVerse: SelectedVerse?,
        lyricSection: LyricSection?,
        pictureImagePath: String?,
        mediaUrl: String?,
        mediaType: String?,
        announcementText: String?,
        websiteUrl: String?,
        websiteTitle: String?,
        sceneId: String?,
        sceneName: String?,
        questionId: String?,
        questionText: String?,
        dictionaryWord: String?,
        dictionaryEntry: StrongsEntry? = null,
        lowerThirdName: String? = null,
        // Canonical verse code (book, chapter, verse) computed from this instance's OWN loaded bible —
        // see LiveStateDto.verseCodeBook and BibleSyncMode.REFERENCE_ONLY. Null when not applicable.
        verseCode: Triple<Int, Int, Int>? = null,
        // Current line/section position within [lyricSection] — see LiveStateDto.songSectionIndex.
        songSectionIndex: Int? = null,
        songLineIndex: Int? = null
    ) {
        val (pictureFolderId, pictureIndex) = resolvePictureLocation(pictureImagePath)
        val mediaId = mediaUrl?.let { url -> _scheduleItemToMediaPath.entries.find { it.value == url }?.key }
        val dto = LiveStateDto(
            contentType = mode,
            bookName = bibleVerse?.bookName?.ifEmpty { null },
            chapter = bibleVerse?.chapter,
            verseNumber = bibleVerse?.verseNumber,
            verseRange = bibleVerse?.verseRange?.ifEmpty { null },
            verseText = bibleVerse?.verseText,
            verseCodeBook = verseCode?.first,
            verseCodeChapter = verseCode?.second,
            verseCodeVerse = verseCode?.third,
            songTitle = lyricSection?.title?.ifEmpty { null },
            songNumber = lyricSection?.songNumber,
            sectionType = lyricSection?.type?.ifEmpty { null },
            lines = lyricSection?.lines,
            songSectionIndex = songSectionIndex,
            songLineIndex = songLineIndex,
            pictureFolderId = pictureFolderId,
            pictureIndex = pictureIndex,
            mediaId = mediaId,
            mediaUrl = mediaUrl?.ifEmpty { null },
            mediaType = mediaType?.ifEmpty { null },
            announcementText = announcementText?.ifEmpty { null },
            websiteUrl = websiteUrl?.ifEmpty { null },
            websiteTitle = websiteTitle?.ifEmpty { null },
            sceneId = sceneId,
            sceneName = sceneName,
            questionId = questionId,
            questionText = questionText,
            dictionaryWord = dictionaryWord,
            dictionaryEntry = dictionaryEntry,
            lowerThirdName = lowerThirdName?.ifEmpty { null }
        )
        // Skip byte-identical re-broadcasts (content setters fire on every call, even when
        // nothing changed) — same early-return pattern the other update* functions use. Protects
        // the shared broadcast buffer from floods that could evict messages for slow clients.
        if (_liveState.value == dto) return
        _liveState.value = dto
        broadcast(WebSocketMessage(
            type = Constants.WS_EVENT_LIVE_STATE_CHANGED,
            payload = json.encodeToString(LiveStateDto.serializer(), dto)
        ))
    }

    /** Finds which registered picture folder (if any) contains [path], for [updateLiveState]. */
    private fun resolvePictureLocation(path: String?): Pair<String?, Int?> {
        if (path.isNullOrEmpty()) return null to null
        for ((folderId, files) in _pictureFiles) {
            val idx = files.indexOfFirst { it.absolutePath == path }
            if (idx >= 0) return folderId to idx
        }
        return null to null
    }

    /**
     * Starts the companion server on [port].
     *
     * @param hostOverride  When non-blank, this hostname/IP is used in the displayed
     *   Server URL instead of the auto-detected local address.  Set it to the
     *   machine's static IP or mDNS name (e.g. "192.168.1.50" or "church-mac.local")
     *   so the URL never changes between restarts.
     */
    fun start(port: Int = Constants.SERVER_DEFAULT_PORT, hostOverride: String = "") {
        if (_isRunning.value) return

        val actualPort = findFreePort(port)
        currentPort = actualPort

        val displayHost = hostOverride.trim().ifEmpty { localIpAddress() }

        // Always use plain HTTP — no SSL certificate required.
        // Mobile clients connect over the local network with ws:// and http://.
        startPlainHttp(actualPort, displayHost)
    }

    /** Fallback plain-HTTP start used only if SSL cert generation fails. */
    private fun startPlainHttp(port: Int, displayHost: String = localIpAddress()) {
        try {
            server = embeddedServer(Netty, configure = {
                connector {
                    host = "0.0.0.0"
                    this.port = port
                }
            }) { configurePipeline() }
            server?.start(wait = false)
            _isRunning.value = true
            _serverUrl.value = "http://$displayHost:$port"
            CrashReporter.breadcrumb("Server started on port $port", category = "server")
            scope.launch { clearDeviceUploads() }
        } catch (_: java.net.BindException) {
            server = null
        } catch (_: Exception) {
            server = null
        }
    }

    private fun findFreePort(startPort: Int): Int {
        for (candidate in startPort until startPort + 40) {
            if (isPortFree(candidate)) return candidate
        }
        return startPort
    }

    private fun isPortFree(port: Int): Boolean = try {
        ServerSocket(port).use { true }
    } catch (_: IOException) {
        false
    }

    private fun Application.configurePipeline() {
            install(ContentNegotiation) { json(json) }
            // Protocol-level pings on every /ws client (mobile companions answer them
            // automatically — invisible to application code). A client that stops ponging is
            // closed within ~timeoutMillis, which both frees its broadcast collector and clears
            // ghost entries from _connectedInstanceLinkFollowers instead of leaving half-open
            // TCP sessions "connected" indefinitely.
            install(WebSockets) {
                pingPeriodMillis = 10_000
                timeoutMillis = 20_000
            }
            install(PartialContent)
            install(CORS) {
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Delete)
                allowHeader(HttpHeaders.ContentType)
                allowHeader(Constants.HEADER_API_KEY)
                allowHeader(Constants.HEADER_DEVICE_ID)
                allowHeader(Constants.HEADER_APP_VERSION)
                allowHeader(Constants.HEADER_CLIENT_ROLE)
                allowHeader("X-QA-Password")
                allowHeader(Constants.HEADER_PRESENTATION_PASSWORD)
                exposeHeader(Constants.HEADER_SERVER_VERSION)
                anyHost()
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    cause.printStackTrace()
                    call.respondText("Internal server error")
                }
            }
            routing {
                // Outside the API key check — devices must be able to fetch the CA cert first.
                certificateRoutes()

                // ── API endpoints (require API key when enabled) ────────────────────────────
                infoAndSongRoutes(
                    this@CompanionServer, _bibleCatalog, _catalog, _fileUploadEnabled,
                    _maxMediaUploadMb, json, scope
                )
                scheduleRoutes(this@CompanionServer, _schedule, json, scope)
                bibleAndDictionaryRoutes(
                    this@CompanionServer, _bible, _bibleCatalog, _presentationCatalog, json, scope
                )
                presentationRoutes(
                    this@CompanionServer, _fileUploadEnabled, _maxMediaUploadMb, _presentationCatalog,
                    _presentationCatalogs, _presentationFilePaths, _scheduleItemToPresentationId,
                    _slideBytes, json, scope
                )
                presentationRemoteRoutes(this@CompanionServer, _presentationNotes, scope)
                mediaAndAssetRoutes(
                    this@CompanionServer, DEVICE_UPLOADS_FOLDER_ID, _backgroundSettings,
                    _fileUploadEnabled, _pictureCatalog, _pictureCatalogs, _pictureFiles,
                    _scheduleItemToMediaPath, json, scope
                )
                webSocketRoute(
                    this@CompanionServer, _apiKey, _apiKeyEnabled, _bibleCatalog, _catalog,
                    _connectedInstanceLinkFollowers, _liveState, _pictureCatalog, _pictureCatalogs,
                    _presentationCatalog, _presentationCatalogs, _schedule,
                    _scheduleItemToPresentationId, json, scope
                )
                lowerThirdAndAtemRoutes(this@CompanionServer, json, scope)
                browserSourceRoutes(
                    this@CompanionServer, _browserSourceFrameFlows, _browserSourceSessions, scope
                )
                qaRoutes(this@CompanionServer, json, scope)
            }
    }











    fun stop() {
        tunnelManager.stop()
        server?.stop(1_000, 2_000)
        server = null
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancelChildren()
        _isRunning.value = false
        _serverUrl.value = ""
        CrashReporter.breadcrumb("Server stopped", category = "server")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Try to parse a [ScheduleItem] from raw JSON using the flat [RemoteItemRequest] format first,
     * then fall back to the legacy sealed-class [AddToScheduleRequest] format.
     */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun parseRemoteItem(body: String): ScheduleItem? {
        // 1. Try flat format: {"item":{"songNumber":42,"title":"…","songbook":"…"}}
        try {
            val req = json.decodeFromString(RemoteItemRequest.serializer(), body)
            val dto = req.item
            // Handle picture identified by folder-id (companion app format).
            // folderPath is null in this case — resolve via the cached catalog.
            if (dto.folderId != null && dto.folderPath == null) {
                val catalog = _pictureCatalogs[dto.folderId]
                if (catalog != null) {
                    val safeId = dto.id.ifBlank { java.util.UUID.randomUUID().toString() }
                    return ScheduleItem.PictureItem(
                        id         = safeId,
                        folderPath = catalog.folderPath,
                        folderName = catalog.folderName,
                        imageCount = catalog.imageTotal
                    )
                }
            }
            // Handle presentation identified by id/fileHash (companion app format).
            // filePath is not sent — resolve via _presentationFilePaths (populated by
            // updatePresentation and updateSchedule) then fall back to _schedule scan.
            // NOTE: mobile may omit the "type" field when it equals the default ("presentation"),
            // so also accept type==null as long as the id resolves in _presentationFilePaths.
            if (dto.filePath == null && dto.folderId == null && dto.id.isNotBlank() &&
                (dto.type == "presentation" || dto.type == null)) {
                val filePath = _presentationFilePaths[dto.id]
                    ?: _schedule.value.firstOrNull { s ->
                        s.type == "presentation" && (
                            s.id == dto.id ||
                            s.filePath?.hashCode()?.toUInt()?.toString(16) == dto.id
                        )
                    }?.filePath
                if (filePath != null) {
                    val catalog = _presentationCatalogs[dto.id]
                    return ScheduleItem.PresentationItem(
                        id         = java.util.UUID.randomUUID().toString(),
                        filePath   = filePath,
                        fileName   = catalog?.fileName ?: dto.title ?: "",
                        slideCount = catalog?.slideTotal ?: 0,
                        fileType   = catalog?.fileType ?: ""
                    )
                }
            }
            val item = dto.toScheduleItem()
            if (item != null) return item
        } catch (_: Exception) {
            // ignore — try legacy format below
        }
        // 2. Fall back to legacy sealed-class format with discriminator
        try {
            return json.decodeFromString(AddToScheduleRequest.serializer(), body).item
        } catch (_: Exception) {
            // ignore
        }
        return null
    }

    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun checkApiKey(call: ApplicationCall): Boolean {
        if (!_apiKeyEnabled.value || _apiKey.value.isEmpty()) return true
        val provided = call.request.headers[Constants.HEADER_API_KEY]
            ?: call.request.queryParameters[Constants.QUERY_PARAM_API_KEY]
            ?: ""
        return if (MessageDigest.isEqual(provided.toByteArray(), _apiKey.value.toByteArray())) {
            true
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Invalid API key")
            false
        }
    }

    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun checkPresentationRemoteAuth(call: ApplicationCall): Boolean {
        if (!presentationRemoteEnabled) {
            call.respond(HttpStatusCode.Forbidden, """{"error":"remote control is disabled"}""")
            return false
        }
        val pw = presentationRemotePassword
        val provided = call.request.headers[Constants.HEADER_PRESENTATION_PASSWORD]
            ?: call.request.queryParameters["password"]
            ?: ""
        return if (pw.isEmpty() || MessageDigest.isEqual(provided.toByteArray(), pw.toByteArray())) {
            true
        } else {
            call.respond(HttpStatusCode.Unauthorized, """{"error":"Invalid password"}""")
            false
        }
    }

    /**
     * Asks the desktop operator to approve/deny this device connecting to the presentation
     * remote, exactly like any other remote action (add to schedule, QA moderation, etc.).
     * Only called from the initial /auth handshake — not on every subsequent action —
     * so an approved or session-approved device is never re-prompted mid-session.
     */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun checkPresentationRemoteConnect(call: ApplicationCall): Boolean {
        val clientId = call.request.headers[Constants.HEADER_DEVICE_ID] ?: ""
        val pending = PendingConnectionRequest(clientId)
        onPresentationRemoteConnect.emit(pending)
        val approved = pending.decision.await()
        if (!approved) {
            call.respond(HttpStatusCode.Forbidden, """{"error":"connection denied"}""")
        }
        return approved
    }

    /** `internal` rather than private so the extracted route groups can call it. */
    internal suspend fun handlePresentationFileUpload(call: ApplicationCall) {
        try {
            val contentLength = call.request.headers["Content-Length"]?.toLongOrNull() ?: 0L
            if (contentLength > 200 * 1024 * 1024) {
                call.respond(HttpStatusCode.PayloadTooLarge, """{"error":"file too large (max 200 MB)"}""")
                return
            }
            val body   = call.receiveText()
            val parsed = json.parseToJsonElement(body) as? JsonObject
            val name   = (parsed?.get("name") as? JsonPrimitive)?.content
            val data   = (parsed?.get("data") as? JsonPrimitive)?.content
            if (name.isNullOrBlank() || data.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, """{"error":"name and data are required"}""")
                return
            }
            val safeName = File(name).name.ifBlank { "upload.pdf" }
            val ext = safeName.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("pdf", "ppt", "pptx", "key")) {
                call.respond(HttpStatusCode.UnsupportedMediaType, """{"error":"unsupported file type: $ext"}""")
                return
            }
            val base64Match = Regex("^data:[^;]+;base64,(.+)$").find(data)
            if (base64Match == null) {
                call.respond(HttpStatusCode.BadRequest, """{"error":"data must be a base64 data URI"}""")
                return
            }
            val fileBytes = Base64.getDecoder().decode(base64Match.groupValues[1])
            val uploadDir = File(System.getProperty("user.home"), ".churchpresenter/device_presentations").also { it.mkdirs() }
            val uniqueName = if (File(uploadDir, safeName).exists()) {
                val ts   = System.currentTimeMillis()
                val base = safeName.substringBeforeLast('.', safeName)
                "${base}_$ts.$ext"
            } else safeName
            val file = File(uploadDir, uniqueName)
            file.writeBytes(fileBytes)
            val id = file.absolutePath.hashCode().toUInt().toString(16)
            _lastDeviceUploadedPresentationId?.let { oldId ->
                _presentationCatalogs.remove(oldId)
                _slideBytes.remove(oldId)
                _presentationFilePaths.remove(oldId)
            }
            _lastDeviceUploadedPresentationId = id
            val uploadClientId = call.request.headers[Constants.HEADER_DEVICE_ID] ?: ""
            scope.launch { onPresentationUploaded.emit(file) }
            scope.launch { onInstantAction.emit(RemoteInstantAction(
                actionType = "upload",
                title = file.name,
                detail = "${fileBytes.size / 1024} KB",
                clientId = uploadClientId
            )) }
            call.respondText(
                """{"ok":true,"id":"$id","name":"${file.nameWithoutExtension.replace("\"", "\\\"")}"}""",
                ContentType.Application.Json
            )
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, """{"error":"upload failed: ${e.message?.replace("\"", "\\\"")}"}""")
        }
    }

    /** `internal` rather than private so the extracted [qaRoutes] group can call it. */
    internal suspend fun checkQaAdmin(call: ApplicationCall): Boolean {
        val pw = qaAdminPassword
        if (pw.isEmpty()) return true
        val provided = call.request.headers["X-QA-Password"]
            ?: call.request.queryParameters["password"]
            ?: ""
        return if (MessageDigest.isEqual(provided.toByteArray(), pw.toByteArray())) {
            true
        } else {
            call.respond(HttpStatusCode.Unauthorized, """{"error":"Invalid admin password"}""")
            false
        }
    }

    /**
     * Asks the desktop operator to approve/deny this device connecting to the Q&A admin panel,
     * exactly like the presentation remote's initial connection handshake.
     * Only called from the initial /api/qa/auth handshake — not on every subsequent action —
     * so an approved or session-approved device is never re-prompted mid-session.
     */
    /** `internal` rather than private so the extracted [qaRoutes] group can call it. */
    internal suspend fun checkQaAdminConnect(call: ApplicationCall): Boolean {
        val clientId = call.request.headers[Constants.HEADER_DEVICE_ID] ?: ""
        val pending = PendingConnectionRequest(clientId)
        onQaAdminConnect.emit(pending)
        val approved = pending.decision.await()
        if (!approved) {
            call.respond(HttpStatusCode.Forbidden, """{"error":"connection denied"}""")
        }
        return approved
    }


    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun broadcast(msg: WebSocketMessage) {
        InstanceLinkLogger.log(InstanceLinkLogSide.PRIMARY, "broadcast", mapOf("type" to msg.type))
        scope.launch {
            broadcastChannel.emit(json.encodeToString(WebSocketMessage.serializer(), msg))
        }
    }

    /** Logs one REST hit on an Instance-Link-relevant endpoint — success and failure alike, so the
     *  primary's own log shows exactly what it served without needing to infer it from a follower's
     *  fetch_result. [status] is the HTTP status code actually sent. */
    /** `internal` rather than private so the extracted route groups can call it. */
    internal fun logRest(endpoint: String, status: Int, reason: String? = null) {
        InstanceLinkLogger.log(
            InstanceLinkLogSide.PRIMARY, "rest_request",
            mapOf("endpoint" to endpoint, "status" to status, "reason" to reason)
        )
    }

    /** Broadcasts a display_cleared event to all connected mobile clients. */
    fun broadcastDisplayCleared() {
        broadcast(WebSocketMessage(type = Constants.WS_EVENT_DISPLAY_CLEARED, payload = ""))
    }

    /** Broadcasts the currently active song section index to all connected mobile clients. */
    fun broadcastSongSectionSelected(sectionIndex: Int) {
        broadcast(WebSocketMessage(type = Constants.WS_EVENT_SONG_SECTION_SELECTED, payload = sectionIndex.toString()))
    }


}

