package org.churchpresenter.app.churchpresenter

import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key as composeKey
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import org.churchpresenter.app.churchpresenter.BuildConfig
import org.churchpresenter.app.churchpresenter.composables.DeckLinkManager
import org.churchpresenter.app.churchpresenter.utils.DevFlags
import org.churchpresenter.app.churchpresenter.utils.LottieFonts
import org.churchpresenter.app.churchpresenter.utils.findScreenIndexByBounds
import org.churchpresenter.app.churchpresenter.utils.rememberScreenDevices
import presentation.engine.fonts.SlideFontRegistry
import androidx.compose.ui.window.rememberWindowState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.app_name
import churchpresenter.composeapp.generated.resources.ic_app_icon
import churchpresenter.composeapp.generated.resources.loading
import churchpresenter.composeapp.generated.resources.presenter_view_title
import churchpresenter.composeapp.generated.resources.key_output_title
import churchpresenter.composeapp.generated.resources.screen_number
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleSyncMode
import org.churchpresenter.app.churchpresenter.data.settings.CompanionSatelliteSettings
import org.churchpresenter.app.churchpresenter.data.settings.InstanceLinkRole
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.data.settings.ResolvedDisplay
import org.churchpresenter.app.churchpresenter.data.settings.obsSceneFor
import org.churchpresenter.app.churchpresenter.data.settings.reconcileScreenAssignments
import org.churchpresenter.app.churchpresenter.data.settings.withBundledBible
import org.churchpresenter.app.churchpresenter.data.Language
import org.churchpresenter.app.churchpresenter.data.RemoteClientManager
import org.churchpresenter.app.churchpresenter.data.SettingsManager
import org.churchpresenter.app.churchpresenter.data.StatisticsManager
import org.churchpresenter.app.churchpresenter.dialogs.AboutDialog
import org.churchpresenter.app.churchpresenter.dialogs.InstanceLinkToastHost
import org.churchpresenter.app.churchpresenter.dialogs.ContactUsDialog
import org.churchpresenter.app.churchpresenter.dialogs.ConverterWindow
import org.churchpresenter.app.churchpresenter.dialogs.LottieGenWindow
import org.churchpresenter.app.churchpresenter.dialogs.StyleEditorWindow
import org.churchpresenter.app.churchpresenter.dialogs.MemoryMonitorWindow
import org.churchpresenter.app.churchpresenter.dialogs.KeyboardShortcutsDialog
import org.churchpresenter.app.churchpresenter.dialogs.LicenseDialog
import org.churchpresenter.app.churchpresenter.dialogs.SetupWizardDialog
import org.churchpresenter.app.churchpresenter.dialogs.RemoteActivityNotification
import org.churchpresenter.app.churchpresenter.dialogs.RemoteActivityToastHost
import org.churchpresenter.app.churchpresenter.dialogs.RemoteEvent
import org.churchpresenter.app.churchpresenter.dialogs.RemoteEventDialog
import org.churchpresenter.app.churchpresenter.dialogs.RemoteEventType
import org.churchpresenter.app.churchpresenter.dialogs.OptionsDialog
import org.churchpresenter.app.churchpresenter.presenter.DeckLinkComposeOutput
import org.churchpresenter.app.churchpresenter.presenter.BrowserSourceVideoRenderer
import org.churchpresenter.app.churchpresenter.presenter.AnnouncementsPresenter
import org.churchpresenter.app.churchpresenter.presenter.QAPresenter
import org.churchpresenter.app.churchpresenter.presenter.STTPresenter
import org.churchpresenter.app.churchpresenter.presenter.DictionaryPresenter
import org.churchpresenter.app.churchpresenter.presenter.QAQRCodePresenter
import org.churchpresenter.app.churchpresenter.presenter.CefManager
import org.churchpresenter.app.churchpresenter.presenter.WebsitePresenter
import org.churchpresenter.app.churchpresenter.presenter.BiblePresenter
import org.churchpresenter.app.churchpresenter.presenter.LowerThirdPresenter
import org.churchpresenter.app.churchpresenter.presenter.MediaPresenter
import org.churchpresenter.app.churchpresenter.presenter.PicturePresenter
import org.churchpresenter.app.churchpresenter.presenter.PresentationPresenter
import org.churchpresenter.app.churchpresenter.presenter.SlidePresenter
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.presenter.ScenePresenter
import org.churchpresenter.app.churchpresenter.presenter.SongPresenter
import org.churchpresenter.app.churchpresenter.models.AnimationType
import org.churchpresenter.app.churchpresenter.models.LyricSection
import org.churchpresenter.app.churchpresenter.models.ScheduleItem
import org.churchpresenter.app.churchpresenter.models.Scene
import org.churchpresenter.app.churchpresenter.models.Question
import org.churchpresenter.app.churchpresenter.models.QuestionStatus
import org.churchpresenter.app.churchpresenter.data.StrongsEntry
import org.churchpresenter.app.churchpresenter.ui.theme.LanguageProvider
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import org.churchpresenter.app.churchpresenter.ui.theme.themeFromSettings
import org.churchpresenter.app.churchpresenter.viewmodel.LocalMediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.InstanceLinkCommandFailure
import org.churchpresenter.app.churchpresenter.viewmodel.MediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import org.churchpresenter.app.churchpresenter.composables.preWarmJavaFX
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.churchpresenter.app.churchpresenter.composables.vlcCustomPath
import org.churchpresenter.app.churchpresenter.data.Bible
import org.churchpresenter.app.churchpresenter.server.LottieRenderCache
import org.churchpresenter.app.churchpresenter.server.CompanionServer
import org.churchpresenter.app.churchpresenter.server.LowerThirdSequencer
import org.churchpresenter.app.churchpresenter.server.TunnelStatus
import org.churchpresenter.app.churchpresenter.server.InstanceLinkStatus
import org.churchpresenter.app.churchpresenter.server.LiveStateDto
import org.churchpresenter.app.churchpresenter.dialogs.InstanceLinkDialog
import org.churchpresenter.app.churchpresenter.viewmodel.QAManager
import org.churchpresenter.app.churchpresenter.viewmodel.OBSWebSocketManager
import org.churchpresenter.app.churchpresenter.viewmodel.CompanionSatelliteViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.InstanceLinkViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.STTManager
import org.churchpresenter.app.churchpresenter.ui.theme.AppThemeWrapper
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.presenterScreenBounds

import org.churchpresenter.app.churchpresenter.utils.AutoStartManager
import org.churchpresenter.app.churchpresenter.utils.CrashReporter
import org.churchpresenter.app.churchpresenter.utils.InstanceLinkLogSide
import org.churchpresenter.app.churchpresenter.utils.InstanceLinkLogger
import org.churchpresenter.app.churchpresenter.utils.LiveMapReporter
import org.churchpresenter.app.churchpresenter.utils.MacMenuBarActivationFix
import org.churchpresenter.app.churchpresenter.utils.UpdateCheckResult
import org.churchpresenter.app.churchpresenter.utils.UpdateChecker
import org.churchpresenter.app.churchpresenter.dialogs.StatisticsDialog
import org.churchpresenter.app.churchpresenter.dialogs.UpdateAvailableDialog
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.io.File
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.CoroutineExceptionHandler
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.server.applyRemoteLiveState
import org.churchpresenter.app.churchpresenter.server.downloadMirroredBackgroundSettings
import org.churchpresenter.app.churchpresenter.server.RemoteAccess
import org.churchpresenter.app.churchpresenter.server.remoteAccessDecision
import org.churchpresenter.app.churchpresenter.server.addScheduleItem
import org.churchpresenter.app.churchpresenter.server.batchEventSummary
import org.churchpresenter.app.churchpresenter.server.emitRemoteTabSelection
import org.churchpresenter.app.churchpresenter.server.RemoteApproval
import org.churchpresenter.app.churchpresenter.server.remoteApproval
import org.churchpresenter.app.churchpresenter.server.executeProjectItem
import org.churchpresenter.app.churchpresenter.server.instanceLinkBackgroundCacheDir
import org.churchpresenter.app.churchpresenter.server.instanceLinkPictureCacheDir
import org.churchpresenter.app.churchpresenter.server.qaActionType
import org.churchpresenter.app.churchpresenter.server.remoteEventLabel
import org.churchpresenter.app.churchpresenter.server.shouldMirrorRemoteBackgrounds
import org.churchpresenter.app.churchpresenter.server.shouldMirrorRemoteOutput
import org.churchpresenter.app.churchpresenter.server.shouldUseRemoteContent
import org.churchpresenter.app.churchpresenter.server.withAnnouncement


private const val CURRENT_EULA_VERSION = 1

private var singleInstanceSocket: java.net.ServerSocket? = null

/**
 * Attempt to bind a local port to enforce single-instance.
 * Returns true if this is the first instance, false if another is already running.
 */
private fun acquireSingleInstanceLock(): Boolean {
    return try {
        // Bind to a fixed localhost port — if it's already taken, another instance is running.
        // The system property override exists for development/testing (e.g. running an
        // InstanceLink follower side by side with a primary on one machine, combined with
        // -Duser.home to isolate its settings and caches).
        val lockPort = System.getProperty("churchpresenter.singleInstancePort")?.toIntOrNull()
            ?: Constants.SINGLE_INSTANCE_PORT
        singleInstanceSocket = java.net.ServerSocket(lockPort, 1, java.net.InetAddress.getLoopbackAddress())
        true
    } catch (_: Exception) {
        false
    }
}

fun main() {
    // Ensure Skiko uses Metal on macOS — prevents OPENGL fallback crash
    if (System.getProperty("os.name", "").lowercase().contains("mac")) {
        System.setProperty("skiko.renderApi", "METAL")
    }
    // Enforce single instance — exit immediately if another is already running
    if (!acquireSingleInstanceLock()) {
        System.err.println("ChurchPresenter is already running.")
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "ChurchPresenter is already running.",
            "ChurchPresenter",
            javax.swing.JOptionPane.WARNING_MESSAGE
        )
        System.exit(0)
        return
    }

    // Install crash reporting before anything else
    val startupSettings = SettingsManager().loadSettings()
    CrashReporter.initialize(startupSettings.analyticsReportingEnabled)
    CrashReporter.breadcrumb("Application started", category = "lifecycle")

    // First run only: if no Bible has been configured yet, bundle the KJV 1769
    // into a default folder and set it as the primary Bible so the app works
    // out of the box without requiring the user to pick a Bible folder first.
    if (startupSettings.bibleSettings.storageDirectory.isEmpty() && startupSettings.bibleSettings.primaryBible.isEmpty()) {
        try {
            val defaultBibleDir = File(System.getProperty("user.home"), Constants.DEFAULT_BIBLES_DIR)
            defaultBibleDir.mkdirs()
            val targetFile = File(defaultBibleDir, "kjv1769.spb")
            if (!targetFile.exists()) {
                targetFile.writeBytes(runBlocking { Res.readBytes("files/bible_samples/kjv1769.spb") })
            }
            SettingsManager().saveSettings(
                startupSettings.withBundledBible(defaultBibleDir.absolutePath, "kjv1769.spb")
            )
        } catch (e: Exception) {
            CrashReporter.reportException(e, "Bundling default KJV Bible")
        }
    }

    // Pass the install id only when analytics is enabled, so opted-out users
    // still send an anonymous geo ping but no persistent identifier.
    LiveMapReporter.pingOnOpen(
        installId = if (startupSettings.analyticsReportingEnabled) CrashReporter.installId() else null,
        updateCheckInterval = startupSettings.updateCheckInterval
    )

    // Catch exceptions thrown inside coroutines / Compose lambdas —
    // these never reach Thread.setDefaultUncaughtExceptionHandler on their own.
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        CrashReporter.reportException(throwable, context = "CoroutineExceptionHandler")
    }

    // Pre-warm JavaFX on a background thread before UI starts
    preWarmJavaFX()

    // Initialize JCEF (Chromium) for embedded web browsing
    CefManager.init()
    // Startup config tag: whether the embedded browser engine loaded (aids triage of web errors).
    CrashReporter.setTag("jcef.available", CefManager.initialized.toString())
    if (CefManager.macOsUnsupported) CrashReporter.setTag("jcef.macos_unsupported", "true")

    // Initialize FileKit so native file dialogs can resolve app directories
    io.github.vinceglb.filekit.FileKit.init(appId = "ChurchPresenter")

    // Repair a stale login-launch registration if the install path changed (e.g. after an update)
    Thread { AutoStartManager.syncRegistration() }.apply { isDaemon = true }.start()

    // Register bundled fonts with AWT and scan platform font dirs so slide rendering (POI/PDF)
    // resolves real typefaces instead of silently substituting the JVM default. Runs in the
    // background — the registry substitutes safely for any slide rendered before it finishes.
    Thread {
        LottieFonts.bundledFontResources().forEach { resource ->
            LottieFonts::class.java.getResourceAsStream(resource)?.let {
                SlideFontRegistry.registerFontStream(it)
            }
        }
        SlideFontRegistry.initialize()
    }.apply { isDaemon = true }.start()

    // Set custom VLC path from saved settings before any composable checks isVlcAvailable
    vlcCustomPath = startupSettings.projectionSettings.vlcPath

    // Pre-render lower-third frame caches in the background so playback starts on raw
    // frames and "Send to ATEM" is instant even before the Lower Third tab is opened
    LottieRenderCache.ensureForFolder(
        startupSettings.streamingSettings.lowerThirdFolder,
        startupSettings.atemSettings
    )

    application(exitProcessOnExit = true) {
        var appReady by remember { mutableStateOf(false) }
        // Business logic layer
        val settingsManager = remember { SettingsManager() }
        val statisticsManager = remember { StatisticsManager() }
        var appSettings by remember {
            mutableStateOf(settingsManager.loadSettings().let {
                it.copy(presentationRemoteSettings = it.presentationRemoteSettings.copy(remoteControlEnabled = false))
            })
        }

        // Resolve any unassigned (-1 auto) screen assignments at startup so that
        // DeckLink-only slots are set to None before the UI renders.
        remember {
            val screenDevicesAll = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            val primaryDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
            val nonPrimaryDisplays = screenDevicesAll.filter { it != primaryDevice }.map { device ->
                val bounds = device.defaultConfiguration.bounds
                ResolvedDisplay(
                    deviceIndex = screenDevicesAll.indexOf(device),
                    x = bounds.x, y = bounds.y, width = bounds.width, height = bounds.height,
                )
            }
            val deckLinkCount = if (DeckLinkManager.isAvailable()) DeckLinkManager.listDevices().size else 0

            val proj = appSettings.projectionSettings
            val assignments = reconcileScreenAssignments(proj.screenAssignments, nonPrimaryDisplays, deckLinkCount)
            if (assignments != null) {
                appSettings = appSettings.copy(
                    projectionSettings = proj.copy(screenAssignments = assignments)
                )
                settingsManager.saveSettings(appSettings)
            }
        }

        val presenterManager = remember { PresenterManager() }
        // Keep the manager's copy of the ATEM settings current — lower-third pre-renders use
        // them to pick the shared render size so playback and ATEM uploads hit one cache entry.
        LaunchedEffect(appSettings.atemSettings) {
            presenterManager.setAtemRenderSettings(appSettings.atemSettings)
        }

        var eulaAccepted by remember { mutableStateOf(appSettings.eulaAcceptedVersion >= CURRENT_EULA_VERSION) }
        var showSetupWizard by remember {
            val bibleReady = appSettings.bibleSettings.primaryBible.isNotEmpty()
            val songsReady = appSettings.songSettings.storageDirectory.isNotEmpty()
            mutableStateOf(!appSettings.setupWizardShown && !(bibleReady && songsReady))
        }

        var currentLanguage by remember {
            val savedLanguageCode = appSettings.language
            val language = Language.entries.find { it.code == savedLanguageCode } ?: Language.ENGLISH
            Locale.setDefault(Locale.forLanguageTag(language.code))
            mutableStateOf(language)
        }

        var scheduleActions by remember { mutableStateOf(ScheduleActions()) }
        val currentScheduleActions by rememberUpdatedState(scheduleActions)

        val mediaViewModel = remember { MediaViewModel() }

        var identifyingScreen by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope { coroutineExceptionHandler }

        var theme by remember { mutableStateOf(themeFromSettings(appSettings.theme)) }
        val companionServer = remember { CompanionServer() }
        val qaManager = remember { QAManager() }
        val sttManager = remember { STTManager() }
        LaunchedEffect(appSettings.bibleEngineSettings.helpDevMode) {
            sttManager.helpDevModeEnabled = appSettings.bibleEngineSettings.helpDevMode
        }
        val obsManager = remember { OBSWebSocketManager() }
        val companionSatelliteViewModel = remember { CompanionSatelliteViewModel() }
        DisposableEffect(Unit) { onDispose { companionSatelliteViewModel.dispose() } }
        // Auto-connect newly-added connections once; re-keying on id list (not full settings) avoids
        // reconnecting everything whenever an unrelated field on an existing connection is edited.
        val autoConnectedIds = remember { mutableSetOf<String>() }
        LaunchedEffect(appSettings.companionSatelliteConnections.map { it.id }) {
            for (connection in appSettings.companionSatelliteConnections) {
                if (connection.autoConnect && autoConnectedIds.add(connection.id)) {
                    // Companion requires a non-empty DEVICEID — generate + persist one if it was
                    // cleared, same guard as the manual Connect button in settings.
                    val effective = if (connection.deviceId.isBlank()) {
                        val generated = java.util.UUID.randomUUID().toString()
                        appSettings = appSettings.copy(
                            companionSatelliteConnections = appSettings.companionSatelliteConnections.map {
                                if (it.id == connection.id) it.copy(deviceId = generated) else it
                            }
                        )
                        settingsManager.saveSettings(appSettings)
                        connection.copy(deviceId = generated)
                    } else connection
                    companionSatelliteViewModel.connectAll(effective)
                }
            }
        }
        // Reconcile any live settings edit for already-active connections — otherwise unchecking
        // e.g. "Left Sidebar", or editing rows/columns/host/port/etc. on a connection that's
        // already live, would leave that slot's client running with stale registration until the
        // user manually hits Disconnect/Connect again. connectAll() is diff-based (see
        // CompanionSatelliteViewModel), so this is a no-op for connections that aren't changing.
        // Keyed on the full connection list (not hand-picked fields) so this never needs updating
        // when a new registration-affecting setting is added.
        val lastReconciled = remember { mutableMapOf<String, CompanionSatelliteSettings>() }
        LaunchedEffect(appSettings.companionSatelliteConnections) {
            for (connection in appSettings.companionSatelliteConnections) {
                val hasLiveSlot = companionSatelliteViewModel.connectionStates.keys.any { it.connectionId == connection.id }
                // A connection seen before by this effect with different settings than last time
                // was actively edited by the user just now (not merely observed for the first time
                // at startup) — treat that the same as toggling the placement checkbox itself: an
                // explicit action, so it should connect even if autoConnect is off and nothing was
                // live yet. A brand-new/never-before-seen connection still only auto-connects when
                // autoConnect is set, preserving startup's opt-in-only behavior (handled primarily
                // by the auto-connect-once effect above).
                val isLiveEdit = lastReconciled[connection.id]?.let { it != connection } ?: false
                if (hasLiveSlot || connection.autoConnect || isLiveEdit) {
                    companionSatelliteViewModel.connectAll(connection)
                }
                lastReconciled[connection.id] = connection
            }
        }

        val instanceLinkViewModel = remember { InstanceLinkViewModel() }
        DisposableEffect(Unit) { onDispose { instanceLinkViewModel.dispose() } }
        // Captured from the existing onBibleLoaded callback below (BibleViewModel itself stays owned
        // by MainDesktop — only the plain Bible object it already hands out crosses here, same as
        // onBibleLoaded already does for companionServer.updateBible). Used to compute a canonical
        // verse code when broadcasting a live Bible verse, for followers in BibleSyncMode.REFERENCE_ONLY.
        var primaryBibleForInstanceLink by remember { mutableStateOf<Bible?>(null) }
        // Same hoist pattern for the Canvas scene list (SceneViewModel stays owned by MainDesktop;
        // only the plain Scene list crosses here) — used to resolve a mirrored CANVAS live state
        // by scene id.
        var scenesForInstanceLink by remember { mutableStateOf<List<Scene>>(emptyList()) }
        // Records the operator's connect/disconnect intent so it survives a restart: without this,
        // `enabled` was only ever written as true and Disconnect was silently undone by the next
        // launch's auto-connect, with no way to turn the link off short of editing settings.json.
        fun setInstanceLinkEnabled(enabled: Boolean) {
            if (appSettings.instanceLink.enabled == enabled) return
            appSettings = appSettings.copy(instanceLink = appSettings.instanceLink.copy(enabled = enabled))
            settingsManager.saveSettings(appSettings)
        }
        // Auto-connect on launch (and reconnect if the saved connection details change while
        // enabled). Keys are the CONNECTION parameters only — role/bibleSyncMode/mirrorBackgrounds
        // are consumed reactively elsewhere and must not force a spurious reconnect when toggled.
        LaunchedEffect(
            appSettings.instanceLink.enabled,
            appSettings.instanceLink.autoConnect,
            appSettings.instanceLink.primaryHost,
            appSettings.instanceLink.primaryPort,
            appSettings.instanceLink.apiKey,
            appSettings.instanceLink.reconnectDelayMs
        ) {
            val link = appSettings.instanceLink
            if (link.enabled && link.autoConnect && link.primaryHost.isNotBlank() && link.primaryPort > 0) {
                instanceLinkViewModel.connect(
                    link.primaryHost, link.primaryPort, link.apiKey, link.deviceId,
                    link.reconnectDelayMs.toLong()
                )
            } else if (!link.enabled) {
                // Toggling the link off should actually drop the connection, not leave it
                // running until the next app restart.
                instanceLinkViewModel.disconnect()
            }
        }
        // Mirrors the primary's live content locally — the counterpart to onLiveStateChanged below.
        // collectLatest: applying a state does suspend network fetches (pictures, lottie JSON), so
        // a newer state must cancel an in-flight apply instead of queueing behind it — otherwise a
        // slow fetch can finish late and clobber content the operator has already moved past.
        // Controlled mode only — see shouldMirrorRemoteOutput for why a Controller must not mirror.
        LaunchedEffect(instanceLinkViewModel, appSettings.instanceLink.role) {
            if (!shouldMirrorRemoteOutput(appSettings.instanceLink.role)) return@LaunchedEffect
            instanceLinkViewModel.remoteLiveState.collectLatest { state ->
                if (state == null) return@collectLatest
                applyRemoteLiveState(
                    state, presenterManager, instanceLinkViewModel,
                    bibleSyncMode = appSettings.instanceLink.bibleSyncMode,
                    localPrimaryBible = primaryBibleForInstanceLink,
                    localScenes = scenesForInstanceLink,
                    onPlayRemoteMedia = { url, type ->
                        mediaViewModel.loadMedia(url, type)
                        mediaViewModel.play()
                    }
                )
            }
        }
        // Presentations have their own dedicated broadcast (richer than LiveStateDto's mode-only
        // PRESENTATION entry) — fetch and mirror whichever slide the primary is currently showing.
        // Controlled mode only, same reasoning as the live-state mirror above.
        LaunchedEffect(instanceLinkViewModel, appSettings.instanceLink.role) {
            if (!shouldMirrorRemoteOutput(appSettings.instanceLink.role)) return@LaunchedEffect
            instanceLinkViewModel.remotePresentationSlide.collectLatest { slide ->
                if (slide == null) return@collectLatest
                // The connect snapshot always sends this event, with an empty id when the primary
                // has no presentation loaded — nothing to fetch, and the request would 404.
                if (slide.id.isBlank()) return@collectLatest
                val bytes = instanceLinkViewModel.fetchPresentationSlideBytes(slide.id, slide.index)
                if (bytes == null) {
                    InstanceLinkLogger.log(
                        InstanceLinkLogSide.FOLLOWER, "apply_live_state",
                        mapOf("contentType" to "PRESENTATION", "resolved" to false, "reason" to "fetch_failed")
                    )
                    return@collectLatest
                }
                val bitmap = runCatching {
                    Image.makeFromEncoded(bytes).toComposeImageBitmap()
                }.getOrNull()
                if (bitmap == null) {
                    InstanceLinkLogger.log(
                        InstanceLinkLogSide.FOLLOWER, "apply_live_state",
                        mapOf("contentType" to "PRESENTATION", "resolved" to false, "reason" to "decode_failed")
                    )
                    return@collectLatest
                }
                presenterManager.setSelectedSlide(bitmap)
                if (slide.isLive) {
                    presenterManager.setPresentingMode(Presenting.PRESENTATION)
                    presenterManager.setShowPresenterWindow(true)
                }
                InstanceLinkLogger.log(
                    InstanceLinkLogSide.FOLLOWER, "apply_live_state",
                    mapOf("contentType" to "PRESENTATION", "resolved" to true, "isLive" to slide.isLive)
                )
            }
        }
        // Controlled mode only — a Controller sending Clear must not have its own display cleared by
        // the echo of the primary acting on that command.
        LaunchedEffect(instanceLinkViewModel, appSettings.instanceLink.role) {
            if (!shouldMirrorRemoteOutput(appSettings.instanceLink.role)) return@LaunchedEffect
            // Skip the value the StateFlow replays on subscribe (and on any re-subscribe when the
            // role changes) — only an actual increment past the count seen here is a fresh clear.
            var lastSeen = instanceLinkViewModel.displayClearedSignal.value
            instanceLinkViewModel.displayClearedSignal.collect { signal ->
                if (signal == lastSeen) return@collect
                lastSeen = signal
                presenterManager.requestClearDisplay()
            }
        }
        // Controller-mode command failures → toast (see InstanceLinkToastHost below).
        val instanceLinkCommandFailures = remember { mutableStateListOf<InstanceLinkCommandFailure>() }
        LaunchedEffect(instanceLinkViewModel) {
            instanceLinkViewModel.commandFailures.collect { failure ->
                instanceLinkCommandFailures.add(failure)
            }
        }
        // The primary's picture folders changed — cached picture files are keyed by folderId+index
        // only, so a replaced image at the same position would otherwise be served stale forever.
        // Clearing the whole cache is cheap: live pictures re-fetch lazily on next display.
        LaunchedEffect(instanceLinkViewModel) {
            instanceLinkViewModel.picturesUpdatedSignal.collect { signal ->
                if (signal == 0) return@collect
                withContext(Dispatchers.IO) {
                    instanceLinkPictureCacheDir.listFiles()?.forEach { it.delete() }
                }
                InstanceLinkLogger.log(
                    InstanceLinkLogSide.FOLLOWER, "cache_invalidated",
                    mapOf("kind" to "pictures", "trigger" to "pictures_updated")
                )
            }
        }
        // Backgrounds change rarely (initial setup, not per-service) so this fetches once per
        // connection rather than needing a live WS push, same as the Bible file fetch — only when
        // the follower opted in via InstanceLinkSettings.mirrorBackgrounds (off by default, since
        // backgrounds are often venue-specific).
        var mirroredBackgroundSettings by remember { mutableStateOf<BackgroundSettings?>(null) }
        val instanceLinkConnectionStatusForBackgrounds by instanceLinkViewModel.connectionStatus.collectAsState()
        // backgroundsUpdatedSignal re-runs this when the primary announces a background change —
        // the asset cache is cleared first so the per-file exists() gate re-downloads fresh bytes.
        val instanceLinkBackgroundsSignal by instanceLinkViewModel.backgroundsUpdatedSignal.collectAsState()
        LaunchedEffect(instanceLinkConnectionStatusForBackgrounds, appSettings.instanceLink.mirrorBackgrounds, appSettings.instanceLink.role, instanceLinkBackgroundsSignal) {
            if (!shouldMirrorRemoteBackgrounds(
                    status = instanceLinkConnectionStatusForBackgrounds,
                    role = appSettings.instanceLink.role,
                    mirrorBackgrounds = appSettings.instanceLink.mirrorBackgrounds
                )
            ) {
                mirroredBackgroundSettings = null
                return@LaunchedEffect
            }
            if (instanceLinkBackgroundsSignal > 0) {
                withContext(Dispatchers.IO) {
                    instanceLinkBackgroundCacheDir.listFiles()?.forEach { it.delete() }
                }
                InstanceLinkLogger.log(
                    InstanceLinkLogSide.FOLLOWER, "cache_invalidated",
                    mapOf("kind" to "backgrounds", "trigger" to "backgrounds_updated")
                )
            }
            val remote = instanceLinkViewModel.fetchBackgroundSettings() ?: return@LaunchedEffect
            mirroredBackgroundSettings = downloadMirroredBackgroundSettings(remote, instanceLinkViewModel)
        }
        // The single override point for rendering paths (PresenterWindows, BrowserSourceVideoRenderer,
        // and MainDesktop's live preview) — everywhere else appSettings is used as-is (editing,
        // persistence, general settings), since those should never show the mirrored backgrounds as
        // if they were this instance's own configuration.
        val effectiveAppSettings = remember(appSettings, mirroredBackgroundSettings) {
            mirroredBackgroundSettings?.let { appSettings.copy(backgroundSettings = it) } ?: appSettings
        }
        // Broadcasts this instance's live content to any connected InstanceLink follower — the
        // counterpart to the remoteLiveState collector above.
        LaunchedEffect(Unit) {
            presenterManager.onLiveStateChanged = { pm, source ->
                val verseCode = if (source == Presenting.BIBLE) {
                    pm.selectedVerse.value.takeIf { it.bookName.isNotEmpty() }?.let { v ->
                        primaryBibleForInstanceLink?.getBookIdByName(v.bookName)?.let { bookId ->
                            primaryBibleForInstanceLink?.getCodeReference(bookId, v.chapter, v.verseNumber)
                        }
                    }
                } else null
                companionServer.updateLiveState(
                    mode = source.name,
                    bibleVerse = pm.selectedVerse.value,
                    lyricSection = pm.lyricSection.value,
                    pictureImagePath = pm.selectedImagePath.value,
                    mediaUrl = pm.currentMediaUrl.value.ifEmpty { null },
                    mediaType = pm.currentMediaType.value.ifEmpty { null },
                    announcementText = pm.announcementText.value.ifEmpty { null },
                    websiteUrl = pm.websiteUrl.value.ifEmpty { null },
                    websiteTitle = pm.webPageTitle.value.ifEmpty { null },
                    sceneId = pm.activeScene.value?.id,
                    sceneName = pm.activeScene.value?.name,
                    questionId = pm.displayedQuestion.value?.id,
                    questionText = pm.displayedQuestion.value?.text,
                    dictionaryWord = pm.displayedDictionaryEntry.value?.word,
                    dictionaryEntry = pm.displayedDictionaryEntry.value,
                    lowerThirdName = pm.currentLowerThirdName.value.ifEmpty { null },
                    verseCode = verseCode,
                    songSectionIndex = if (source == Presenting.LYRICS) pm.songDisplaySectionIndex.value else null,
                    songLineIndex = if (source == Presenting.LYRICS) pm.songDisplayLineIndex.value else null
                )
            }
        }
        remember(qaManager) { companionServer.qaManager = qaManager; true }
        // Auto-connect OBS when settings change (or on first load if enabled)
        LaunchedEffect(
            appSettings.obsSettings.enabled,
            appSettings.obsSettings.host,
            appSettings.obsSettings.port,
            appSettings.obsSettings.password
        ) {
            if (appSettings.obsSettings.enabled) {
                obsManager.connect(
                    appSettings.obsSettings.host,
                    appSettings.obsSettings.port,
                    appSettings.obsSettings.password
                )
            } else {
                obsManager.disconnect()
            }
        }
        // Switch OBS scene when presenting mode changes
        LaunchedEffect(Unit) {
            snapshotFlow { presenterManager.presentingMode.value }
                .collect { mode ->
                    val sceneName = obsSceneFor(mode, appSettings.obsSettings) ?: return@collect
                    obsManager.setScene(sceneName)
                }
        }
        // Sync QA settings to server — admin auth reuses the server API key, just like the presentation remote
        LaunchedEffect(appSettings.serverSettings.apiKeyEnabled, appSettings.serverSettings.apiKey, appSettings.qaSettings.rateLimitCooldownSeconds, appSettings.qaSettings.votingEnabled) {
            companionServer.qaAdminPassword = if (appSettings.serverSettings.apiKeyEnabled) appSettings.serverSettings.apiKey else ""
            companionServer.qaCooldownSeconds = appSettings.qaSettings.rateLimitCooldownSeconds
            companionServer.qaVotingEnabled = appSettings.qaSettings.votingEnabled
        }
        val tunnelStatus by companionServer.tunnelManager.status.collectAsState()
        val tunnelUrl by companionServer.tunnelManager.tunnelUrl.collectAsState()
        val prevTunnelWasConnected = remember { mutableStateOf(false) }
        var qaDisplayUrl by remember { mutableStateOf("") }
        var presentationDisplayUrl by remember { mutableStateOf("") }
        LaunchedEffect(tunnelStatus) {
            val isConnected = tunnelStatus is TunnelStatus.Connected
            if (prevTunnelWasConnected.value && !isConnected) {
                companionServer.clearPresentationState()
                qaDisplayUrl = ""
                presentationDisplayUrl = ""
            }
            prevTunnelWasConnected.value = isConnected
        }
        var presentationFrozen by remember { mutableStateOf(false) }
        LaunchedEffect(appSettings.presentationRemoteSettings.remoteControlEnabled, appSettings.serverSettings.apiKeyEnabled, appSettings.serverSettings.apiKey) {
            val activeApiKey = if (appSettings.serverSettings.apiKeyEnabled) appSettings.serverSettings.apiKey else ""
            companionServer.updatePresentationRemoteSettings(appSettings.presentationRemoteSettings, activeApiKey)
        }
        LaunchedEffect(appSettings.presentationSettings.autoScrollInterval) {
            companionServer.updateAutoScrollInterval(appSettings.presentationSettings.autoScrollInterval.toInt())
        }
        LaunchedEffect(appSettings.presentationSettings.isLooping) {
            companionServer.updateLoopingState(appSettings.presentationSettings.isLooping)
        }
        LaunchedEffect(Unit) {
            companionServer.onPresentationFreezeToggle.collect {
                presentationFrozen = !presentationFrozen
                companionServer.broadcastFreezeChange(presentationFrozen)
                presenterManager.setSlideFrozen(presentationFrozen)
            }
        }
        // Broadcast media playback state to companions (mobile Media tab). Position ticks
        // continuously, so poll on a fixed cadence rather than per-frame.
        LaunchedEffect(Unit) {
            var wasLoaded = false
            while (true) {
                val loaded = mediaViewModel.isLoaded
                if (loaded) {
                    companionServer.broadcastMediaState(
                        isLive = presenterManager.presentingMode.value == Presenting.MEDIA,
                        isLoaded = true,
                        isPlaying = mediaViewModel.isPlaying,
                        title = mediaViewModel.mediaTitle,
                        positionMs = mediaViewModel.currentPosition,
                        durationMs = mediaViewModel.duration,
                        volume = mediaViewModel.volume,
                        muted = mediaViewModel.isMuted,
                        mediaType = mediaViewModel.mediaType,
                        source = mediaViewModel.mediaUrl,
                    )
                    wasLoaded = true
                } else if (wasLoaded) {
                    // One final "not loaded" so the mobile clears its now-playing view.
                    companionServer.broadcastMediaState(
                        isLive = false, isLoaded = false, isPlaying = false,
                        title = "", positionMs = 0L, durationMs = 0L,
                        volume = mediaViewModel.volume, muted = mediaViewModel.isMuted,
                        mediaType = mediaViewModel.mediaType, source = "",
                    )
                    wasLoaded = false
                }
                delay(500)
            }
        }
        // Media transport controls from a companion remote (mobile Media tab).
        LaunchedEffect(Unit) { companionServer.onMediaPlayPause.collect { mediaViewModel.togglePlayPause() } }
        LaunchedEffect(Unit) { companionServer.onMediaStop.collect { mediaViewModel.stop() } }
        LaunchedEffect(Unit) { companionServer.onMediaSeekForward.collect { mediaViewModel.seekForward() } }
        LaunchedEffect(Unit) { companionServer.onMediaSeekBackward.collect { mediaViewModel.seekBackward() } }
        LaunchedEffect(Unit) { companionServer.onMediaSeekTo.collect { mediaViewModel.seekTo(it) } }
        LaunchedEffect(Unit) { companionServer.onMediaSetVolume.collect { mediaViewModel.setVolume(it) } }
        LaunchedEffect(Unit) { companionServer.onMediaMuteToggle.collect { mediaViewModel.toggleMute() } }
        val presentingModeValue = presenterManager.presentingMode.value
        LaunchedEffect(presentingModeValue) {
            companionServer.updatePresentationLiveStatus(presentingModeValue == Presenting.PRESENTATION)
        }
        // ── Browser Source outputs (OBS/vMix overlay) ─────────────────────────────
        // Each output gets its own off-screen renderer (BrowserSourceVideoRenderer) that
        // renders the same BiblePresenter/SongPresenter/AnnouncementsPresenter/PicturePresenter/
        // StageMonitorScreen composables used everywhere else, streamed to CompanionServer as
        // PNG frames — pixel-identical to the native output, no separate styling logic to
        // maintain. PresenterManager itself never leaves this scope; only each renderer's
        // frame flow crosses into CompanionServer.
        LaunchedEffect(appSettings.projectionSettings.browserSourceOutputs) {
            companionServer.updateBrowserSourceOutputs(appSettings.projectionSettings.browserSourceOutputs)
        }
        LaunchedEffect(appSettings.backgroundSettings) {
            companionServer.updateBackgroundSettings(appSettings.backgroundSettings)
        }
        val browserSourceServerUrlState = companionServer.serverUrl.collectAsState()
        appSettings.projectionSettings.browserSourceOutputs.indices.forEach { i ->
            composeKey(i) {
                // rememberUpdatedState, not remember { derivedStateOf { ... } } — appSettings and
                // qaDisplayUrl are plain composable parameters, not Compose State reads, so a
                // keyless derivedStateOf wrapping them would only ever capture their value from
                // this composeKey block's first-ever composition and never update again (no
                // tracked State read inside the calculation to invalidate on). That silently
                // froze every appSettings-driven Browser Source setting — background
                // image/type, fonts, colors, etc. — at whatever it was when the app started,
                // which is why a background image change only took effect after restarting the
                // app. effectiveModeState is unaffected since it genuinely reads presenterManager
                // State objects (.value), which derivedStateOf tracks correctly.
                val appSettingsState = rememberUpdatedState(effectiveAppSettings)
                val screenAssignmentState = rememberUpdatedState(
                    appSettings.projectionSettings.browserSourceOutputs.getOrNull(i) ?: ScreenAssignment()
                )
                val effectiveModeState = remember {
                    derivedStateOf { presenterManager.browserSourceLocks.value[i] ?: presenterManager.presentingMode.value }
                }
                val qaDisplayUrlState = rememberUpdatedState(qaDisplayUrl)
                // Keyed on geometry/fps so a settings change tears the renderer down and builds a
                // fresh one; registerBrowserSourceFrames then closes connected clients, which
                // reconnect and reseed with a full frame at the new size.
                val bsOutput = appSettings.projectionSettings.browserSourceOutputs.getOrNull(i) ?: ScreenAssignment()
                val renderer = remember(i, bsOutput.browserSourceWidth, bsOutput.browserSourceHeight, bsOutput.browserSourceFps) {
                    BrowserSourceVideoRenderer(
                        presenterManager, appSettingsState, screenAssignmentState, effectiveModeState,
                        outputIndex = i,
                        sttManager = sttManager,
                        mediaViewModel = mediaViewModel,
                        qaDisplayUrlState = qaDisplayUrlState,
                        serverUrlState = browserSourceServerUrlState,
                        width = bsOutput.browserSourceWidth,
                        height = bsOutput.browserSourceHeight,
                        fps = bsOutput.browserSourceFps,
                    )
                }
                LaunchedEffect(renderer) {
                    renderer.start(this)
                    companionServer.registerBrowserSourceFrames(i, renderer.frames)
                }
                DisposableEffect(renderer) {
                    onDispose { renderer.stop() }
                }
            }
        }
        LaunchedEffect(Unit) {
            companionServer.onPresentationGoLive.collect {
                presenterManager.setPresentingMode(Presenting.PRESENTATION)
                presenterManager.setShowPresenterWindow(true)
            }
        }
        val remoteSelectSongFlow =
            remember { kotlinx.coroutines.flow.MutableSharedFlow<ScheduleItem.SongItem>(extraBufferCapacity = 8) }
        // Same backfill mechanism as remoteSelectSongFlow — a remote PROJECT go-live only adds the
        // item to the schedule and flips presentingMode; these flows drive MainDesktop to actually
        // load and push real content (see executeProjectItem below, which deliberately does NOT push
        // picture/slide content itself).
        val remoteSelectPictureFlow =
            remember { kotlinx.coroutines.flow.MutableSharedFlow<ScheduleItem.PictureItem>(extraBufferCapacity = 8) }
        val remoteSelectPresentationFlow =
            remember { kotlinx.coroutines.flow.MutableSharedFlow<ScheduleItem.PresentationItem>(extraBufferCapacity = 8) }
        var dialogDismissSignal by remember { mutableStateOf(0) }
        var showOptionsDialog by remember { mutableStateOf(false) }
        var optionsDialogInitialTab by remember { mutableStateOf(0) }
        // Single entry point so every open site picks its tab explicitly
        val openOptionsDialog: (Int) -> Unit = { tab ->
            optionsDialogInitialTab = tab
            showOptionsDialog = true
        }
        var showStatisticsDialog by remember { mutableStateOf(false) }
        var showInstanceLinkDialog by remember { mutableStateOf(false) }
        var showKeyboardShortcutsDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var showContactDialog by remember { mutableStateOf(false) }
        var showConverterWindow by remember { mutableStateOf(false) }
        var showLottieGenWindow by remember { mutableStateOf(false) }
        var showStyleEditorWindow by remember { mutableStateOf(false) }
        var showMemoryMonitorWindow by remember { mutableStateOf(false) }
        // Secret keypress unlock (press D seven times) — reveals the Developer menu in a
        // packaged build for this session only. See MainDesktop's onPreviewKeyEvent.
        var developerMenuUnlocked by remember { mutableStateOf(false) }
        var lottieGenOutputDir by remember { mutableStateOf<File?>(null) }
        var lottieGenOnFileSaved by remember { mutableStateOf<(() -> Unit)?>(null) }
        var pendingUpdateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
        var pendingUpdateCheckWasManual by remember { mutableStateOf(false) }
        var selectedScheduleItemId by remember { mutableStateOf<String?>(null) }

        // Preload songs and bible at startup, then signal ready
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                companionServer.preloadData(
                    songStorageDir = appSettings.songSettings.storageDirectory,
                    bibleStorageDir = appSettings.bibleSettings.storageDirectory,
                    primaryBibleFileName = appSettings.bibleSettings.primaryBible
                )
                // Seed API key from saved settings before starting, so the first
                // request is already checked against the correct key.
                companionServer.updateApiKey(
                    enabled = appSettings.serverSettings.apiKeyEnabled,
                    key = appSettings.serverSettings.apiKey
                )
                companionServer.updateFileUploadEnabled(appSettings.serverSettings.fileUploadEnabled)
                companionServer.updateMaxMediaUploadMb(appSettings.serverSettings.maxMediaUploadMb)
                companionServer.updateAtemConfig(
                    appSettings.atemSettings,
                    appSettings.streamingSettings.lowerThirdFolder
                )
                // Auto-start server if user previously enabled it
                if (appSettings.serverSettings.enabled) {
                    companionServer.start(
                        port = appSettings.serverSettings.port,
                        hostOverride = appSettings.serverSettings.serverHost
                    )
                }
            }
            appReady = true
            // Check for updates in background after startup, respecting the configured interval
            val isFirstEverUpdateCheck = appSettings.lastUpdateCheckTimestamp == 0L
            if (appSettings.updateCheckInterval.isDueSince(appSettings.lastUpdateCheckTimestamp)) {
                val result = UpdateChecker.checkForUpdate(includePrereleases = appSettings.participateInPrereleases)
                appSettings = appSettings.copy(lastUpdateCheckTimestamp = System.currentTimeMillis())
                settingsManager.saveSettings(appSettings)
                if (isFirstEverUpdateCheck) {
                    // Let the user pick their update-check frequency the first time it ever runs.
                    pendingUpdateResult = result
                    pendingUpdateCheckWasManual = true
                } else if (result is UpdateCheckResult.Available) {
                    pendingUpdateResult = result
                    pendingUpdateCheckWasManual = false
                }
            }
        }


        val screens = rememberScreenDevices()
        val savedPlacement = when (appSettings.windowPlacement) {
            "floating" -> WindowPlacement.Floating
            "fullscreen" -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Maximized
        }
        // Use OS primary monitor bounds so maximized/fullscreen stays on one screen
        val primaryBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration.bounds
        val state = rememberWindowState(
            placement = savedPlacement,
            position = if (savedPlacement == WindowPlacement.Floating && appSettings.windowX >= 0)
                WindowPosition(appSettings.windowX.dp, appSettings.windowY.dp)
            else WindowPosition(primaryBounds.x.dp, primaryBounds.y.dp),
            size = if (savedPlacement == WindowPlacement.Floating)
                DpSize(appSettings.windowWidth.dp, appSettings.windowHeight.dp)
            else DpSize(primaryBounds.width.dp, primaryBounds.height.dp)
        )

        // Splash screen while app is loading
        if (!appReady) {
            SplashWindow(theme = theme)
        }

        if (appReady && eulaAccepted) {
            Window(
                onCloseRequest = {
                    val placementStr = when (state.placement) {
                        WindowPlacement.Floating -> "floating"
                        WindowPlacement.Fullscreen -> "fullscreen"
                        WindowPlacement.Maximized -> "maximized"
                    }
                    val isFloating = state.placement == WindowPlacement.Floating
                    appSettings = appSettings.copy(
                        windowPlacement = placementStr,
                        windowWidth = if (isFloating) state.size.width.value.toInt() else appSettings.windowWidth,
                        windowHeight = if (isFloating) state.size.height.value.toInt() else appSettings.windowHeight,
                        windowX = if (isFloating) state.position.x.value.toInt() else -1,
                        windowY = if (isFloating) state.position.y.value.toInt() else -1
                    )
                    settingsManager.saveSettings(appSettings)
                    if (qaManager.sessionActive) qaManager.toggleSession()
                    companionServer.clearPresentationState()
                    companionServer.tunnelManager.shutdown()
                    exitApplication()
                },
                title = stringResource(Res.string.app_name),
                icon = painterResource(Res.drawable.ic_app_icon),
                state = state
            ) {
                MacMenuBarActivationFix()
                LanguageProvider(language = currentLanguage) {
                    AppThemeWrapper(theme = theme) {
                        CompositionLocalProvider(
                            LocalMediaViewModel provides mediaViewModel,
                            LocalMainWindowState provides state
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {

                                // ── Remote API permission state (inside Window so schedule actions are live) ──
                                // Each entry: Triple(RemoteEvent, allowAction, denyAction)
                                val remoteEventQueue =
                                    remember { mutableStateListOf<Triple<RemoteEvent, () -> Unit, () -> Unit>>() }

                                // Persistent allow/block lists (survive app restarts)
                                val remoteClientManager = remember { RemoteClientManager() }
                                // Session-only sets (cleared on app restart)
                                val sessionAllowedClients =
                                    remember { mutableStateListOf<String>() }
                                val sessionBlockedClients =
                                    remember { mutableStateListOf<String>() }
                                // Activity toasts for already-allowed clients (auto-approved actions)
                                val remoteActivityNotifications =
                                    remember { mutableStateListOf<RemoteActivityNotification>() }

                                // Both block lists reach the server itself, so a blocked device is
                                // refused at the socket instead of only at the approval-gated
                                // commands — see CompanionServer.blockedClientIds for what that
                                // used to leave open.
                                LaunchedEffect(remoteClientManager.blockedClients, sessionBlockedClients.toList()) {
                                    companionServer.blockedClientIds =
                                        remoteClientManager.blockedClients + sessionBlockedClients
                                }

                                // ── Remote add-to-schedule requests ──────────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onAddToSchedule.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        val item = pending.item
                                        val add: () -> Unit = {
                                            addScheduleItem(item, currentScheduleActions) { song ->
                                                coroutineScope.launch { remoteSelectSongFlow.emit(song) }
                                            }
                                            pending.decision.complete(true)
                                        }
                                        val (eTitle, eDetail) = remoteEventLabel(item)
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.ADD_TO_SCHEDULE,
                                            title = eTitle,
                                            detail = eDetail,
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                add()
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event, add, { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Remote remove-from-schedule requests ──────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onRemoveFromSchedule.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        val remove: () -> Unit = {
                                            currentScheduleActions.removeById(pending.id)
                                            pending.decision.complete(true)
                                        }
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.REMOVE_FROM_SCHEDULE,
                                            title = pending.label,
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                remove()
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event, remove, { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Remote add-batch-to-schedule requests ─────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onAddBatchToSchedule.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        val addAll: () -> Unit = {
                                            for (item in pending.items) {
                                                addScheduleItem(item, currentScheduleActions) { song ->
                                                    coroutineScope.launch { remoteSelectSongFlow.emit(song) }
                                                }
                                            }
                                            pending.decision.complete(true)
                                        }
                                        // First 3 items joined, then "…" if more
                                        val (batchTitle, batchDetail) = batchEventSummary(pending.items)
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.ADD_TO_SCHEDULE,
                                            title = batchTitle,
                                            detail = batchDetail,
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                addAll()
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event, addAll, { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Remote project requests ──────────────────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onProject.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        val item = pending.item
                                        val project: () -> Unit = {
                                            if (item is ScheduleItem.AnnouncementItem) {
                                                appSettings = appSettings.withAnnouncement(item)
                                            }
                                            executeProjectItem(
                                                item,
                                                currentScheduleActions,
                                                presenterManager,
                                                statisticsManager
                                            )
                                            // Also drive the owning tab so it loads the real content
                                            coroutineScope.launch {
                                                emitRemoteTabSelection(
                                                    item, remoteSelectSongFlow,
                                                    remoteSelectPictureFlow, remoteSelectPresentationFlow,
                                                )
                                            }
                                            pending.decision.complete(true)
                                        }
                                        val (pTitle, pDetail) = remoteEventLabel(item)
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.PROJECT,
                                            title = pTitle,
                                            detail = pDetail,
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                project()
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event, project, { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Remote song-section navigation ───────────────────────────────────────────
                                // Fires when a mobile client calls POST /api/songs/{n}/select or sends
                                // WS "select_song_section".  No approval required — applied instantly.
                                LaunchedEffect(Unit) {
                                    companionServer.onSelectSongSection.collect { req ->
                                        val sections = presenterManager.allLyricSections.value
                                        val section = sections.getOrNull(req.section) ?: return@collect
                                        presenterManager.setLyricSection(section)
                                        presenterManager.setSongDisplaySectionIndex(req.section)
                                        presenterManager.setSongDisplayLineIndex(if (req.lineIndex >= 0) req.lineIndex else -1)
                                        // Make sure the presenter is showing lyrics
                                        if (presenterManager.presentingMode.value != Presenting.LYRICS) {
                                            presenterManager.setPresentingMode(Presenting.LYRICS)
                                            presenterManager.setShowPresenterWindow(true)
                                        }
                                    }
                                }

                                // ── Remote clear / display-off ────────────────────────────────────────────────
                                // Fires when a mobile client calls POST /api/clear or sends WS "clear".
                                LaunchedEffect(Unit) {
                                    companionServer.onClear.collect {
                                        mediaViewModel.pause()
                                        presenterManager.requestClearDisplay()
                                    }
                                }

                                // ── Companion lower-third sequence (POST /api/lowerthirds/{name}/run) ───────
                                // The sequencer handles the ATEM DSK and timing; these collectors do the
                                // same go-live / off-air the Lower Third tab does.
                                LaunchedEffect(Unit) {
                                    LowerThirdSequencer.onShow.collect { req ->
                                        presenterManager.setLottieContent(
                                            req.json, req.pauseAtFrame, req.pauseFrame, req.pauseDurationMs, req.name
                                        )
                                        presenterManager.setPresentingMode(Presenting.LOWER_THIRD)
                                        presenterManager.setShowPresenterWindow(true)
                                    }
                                }
                                LaunchedEffect(Unit) {
                                    LowerThirdSequencer.onClear.collect {
                                        if (presenterManager.presentingMode.value == Presenting.LOWER_THIRD) {
                                            presenterManager.requestClearDisplay()
                                        }
                                    }
                                }
                                LaunchedEffect(Unit) {
                                    companionServer.onQADisplay.collect { question ->
                                        if (question != null) {
                                            presenterManager.setDisplayedQuestion(question)
                                            presenterManager.setShowQRCodeOnDisplay(false)
                                            presenterManager.setPresentingMode(Presenting.QA)
                                        } else {
                                            presenterManager.setDisplayedQuestion(null)
                                            presenterManager.setPresentingMode(Presenting.NONE)
                                        }
                                    }
                                }

                                // ── Remote QA admin requests (add / edit / delete) ───────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onQAAdminRequest.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = qaActionType(pending.action),
                                            title = pending.text.take(80),
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                pending.decision.complete(true)
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event,
                                                { pending.decision.complete(true) },
                                                { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Presentation remote connection requests ───────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onPresentationRemoteConnect.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.PRESENTATION_CONNECT,
                                            title = "",
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                pending.decision.complete(true)
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event,
                                                { pending.decision.complete(true) },
                                                { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Q&A admin connection requests ─────────────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onQaAdminConnect.collect { pending ->
                                        val clientId = pending.clientId
                                        val access = remoteAccessDecision(
                                            clientId,
                                            remoteClientManager.allowedClients, remoteClientManager.blockedClients,
                                            sessionAllowedClients, sessionBlockedClients,
                                        )
                                        when (val outcome = remoteApproval(
                                            access,
                                            type = RemoteEventType.QA_ADMIN_CONNECT,
                                            title = "",
                                            clientId = clientId,
                                            clientLabel = remoteClientManager.getLabel(clientId),
                                        )) {
                                            RemoteApproval.Reject -> pending.decision.complete(false)
                                            is RemoteApproval.Approve -> {
                                                pending.decision.complete(true)
                                                remoteActivityNotifications.add(outcome.notification)
                                            }
                                            is RemoteApproval.Ask -> remoteEventQueue.add(Triple(
                                                outcome.event,
                                                { pending.decision.complete(true) },
                                                { pending.decision.complete(false) },
                                            ))
                                        }
                                    }
                                }

                                // ── Remote Bible hold toggle ──────────────────────────────────────────────────
                                LaunchedEffect(Unit) {
                                    companionServer.onBibleHold.collect { hold ->
                                        presenterManager.setBibleHold(hold)
                                    }
                                }

                                // ── Notify mobile clients when display is cleared ─────────────────────────────
                                LaunchedEffect(Unit) {
                                    snapshotFlow { presenterManager.presentingMode.value }
                                        .collect { mode ->
                                            if (mode == Presenting.NONE) {
                                                companionServer.broadcastDisplayCleared()
                                            }
                                        }
                                }

                                // ── Notify mobile clients when song section changes ──────────────────────────
                                LaunchedEffect(Unit) {
                                    snapshotFlow { presenterManager.songDisplaySectionIndex.value }
                                        .collect { index ->
                                            if (presenterManager.presentingMode.value == Presenting.LYRICS) {
                                                companionServer.broadcastSongSectionSelected(index)
                                            }
                                        }
                                }

                                // ── Instant-action activity toasts ────────────────────────────────────────────
                                // For every no-approval action (present, upload, clear) show a toast so the
                                // operator can see what a remote client just did and optionally block them.
                                LaunchedEffect(Unit) {
                                    companionServer.onInstantAction.collect { action ->
                                        val type = when (action.actionType) {
                                            "present" -> RemoteEventType.PRESENT
                                            "upload"  -> RemoteEventType.UPLOAD
                                            "clear"   -> RemoteEventType.CLEAR
                                            else      -> RemoteEventType.PRESENT
                                        }
                                        remoteActivityNotifications.add(
                                            RemoteActivityNotification(
                                                type = type,
                                                title = action.title,
                                                detail = action.detail,
                                                clientId = action.clientId,
                                                clientLabel = remoteClientManager.getLabel(action.clientId)
                                            )
                                        )
                                    }
                                }

                                NavigationTopBar(
                                    currentTheme = theme,
                                    onAbout = { showAboutDialog = true },
                                    onContactUs = { showContactDialog = true },
                                    onGettingStarted = { showSetupWizard = true },
                                    onStatistics = { showStatisticsDialog = true },
                                    onConnectToInstance = { showInstanceLinkDialog = true },
                                    onDisconnectInstance = { instanceLinkViewModel.disconnect() },
                                    isInstanceLinkConnected = instanceLinkViewModel.connectionStatus.collectAsState().value != InstanceLinkStatus.DISCONNECTED,
                                    onConverter = { showConverterWindow = true },
                                    onHelp = {
                                        Desktop.getDesktop()
                                            .browse(URI("https://churchpresenter.org/wiki"))
                                    },
                                    onHowToBlog = {
                                        Desktop.getDesktop()
                                            .browse(URI("https://churchpresenter.org/blog"))
                                    },
                                    onCheckForUpdates = {
                                        coroutineScope.launch {
                                            pendingUpdateResult = UpdateChecker.checkForUpdate(
                                                includePrereleases = appSettings.participateInPrereleases
                                            )
                                            pendingUpdateCheckWasManual = true
                                            appSettings = appSettings.copy(lastUpdateCheckTimestamp = System.currentTimeMillis())
                                            settingsManager.saveSettings(appSettings)
                                        }
                                    },
                                    onKeyboardShortcuts = { showKeyboardShortcutsDialog = true },
                                    theme = {
                                        appSettings = appSettings.copy(theme = it.toString())
                                        theme = it
                                        settingsManager.saveSettings(appSettings)
                                    },
                                    onLanguageChange = { language ->
                                        currentLanguage = language
                                        appSettings = appSettings.copy(language = language.code)
                                        settingsManager.saveSettings(appSettings)
                                        Locale.setDefault(Locale.forLanguageTag(language.code))
                                    },
                                    onSettings = { openOptionsDialog(0) },
                                    onExit = { exitApplication() },
                                    onAddToSchedule = { },
                                    onNewSchedule = { currentScheduleActions.newSchedule() },
                                    onOpenSchedule = { currentScheduleActions.openSchedule() },
                                    onSaveSchedule = { currentScheduleActions.saveSchedule() },
                                    onSaveScheduleAs = { currentScheduleActions.saveScheduleAs() },
                                    onCloseSchedule = { currentScheduleActions.newSchedule() },
                                    onRemoveFromSchedule = {
                                        selectedScheduleItemId?.let {
                                            currentScheduleActions.removeSelected()
                                            selectedScheduleItemId = null
                                        }
                                    },
                                    onClearSchedule = {
                                        currentScheduleActions.clearSchedule()
                                        selectedScheduleItemId = null
                                    },
                                    // Mirrors the devWindowedFallback gate below (main.kt) and in
                                    // ProjectionSettingsTab.kt — a dev build or the forced flag
                                    // gets this menu, plus the secret D-x7 keypress unlock
                                    // (developerMenuUnlocked, session-only) for packaged builds.
                                    showDeveloperMenu = !BuildConfig.IS_RELEASE || DevFlags.forceDevWindow || developerMenuUnlocked,
                                    isPresenterWindowVisible = presenterManager.showPresenterWindow.value,
                                    onSetPresenterWindowVisible = { presenterManager.setShowPresenterWindow(it) },
                                    isDevWindowAlwaysOnTop = presenterManager.devWindowAlwaysOnTop.value,
                                    onSetDevWindowAlwaysOnTop = { presenterManager.setDevWindowAlwaysOnTop(it) },
                                    onOpenStyleEditor = { showStyleEditorWindow = true },
                                    onOpenMemoryMonitor = { showMemoryMonitorWindow = true },
                                )
                                // Crash recovery warning banner
                                if (CrashReporter.didCrashLastRun && CrashReporter.videoBackgroundsDisabled) {
                                    var showBanner by remember { mutableStateOf(true) }
                                    if (showBanner) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Video backgrounds disabled after ${CrashReporter.consecutiveCrashes} consecutive crashes.  [Re-enable]  [Dismiss]",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.onPreviewKeyEvent {
                                                    showBanner = false; true
                                                }
                                            )
                                        }
                                        // Auto-dismiss after 15 seconds
                                        LaunchedEffect(Unit) {
                                            delay(15_000)
                                            showBanner = false
                                        }
                                    }
                                }

                                val instanceLinkStatus = instanceLinkViewModel.connectionStatus.collectAsState().value
                                val instanceLinkIsControllerConnected =
                                    instanceLinkStatus == InstanceLinkStatus.CONNECTED &&
                                        appSettings.instanceLink.role == InstanceLinkRole.CONTROLLER
                                // See shouldUseRemoteContent — the remote-asset fallbacks belong to a
                                // mirrored schedule, so they are Controlled-only.
                                val instanceLinkUsesRemoteContent =
                                    shouldUseRemoteContent(instanceLinkStatus, appSettings.instanceLink.role)
                                MainDesktop(
                                    hostWindow = window,
                                    instanceLinkConnectionStatus = instanceLinkViewModel.connectionStatus.collectAsState().value,
                                    instanceLinkNextRetryAtMs = instanceLinkViewModel.nextRetryAtMs.collectAsState().value,
                                    instanceLinkBibleUpdatedSignal = instanceLinkViewModel.bibleUpdatedSignal.collectAsState().value,
                                    instanceLinkSecondaryBibleUpdatedSignal = instanceLinkViewModel.secondaryBibleUpdatedSignal.collectAsState().value,
                                    instanceLinkFollowingHost = appSettings.instanceLink.primaryHost,
                                    connectedInstanceLinkFollowerCount = companionServer.connectedInstanceLinkFollowers.collectAsState().value.size,
                                    onInstanceLinkConnect = {
                                        val link = appSettings.instanceLink
                                        setInstanceLinkEnabled(true)
                                        instanceLinkViewModel.connect(
                                            link.primaryHost, link.primaryPort, link.apiKey, link.deviceId,
                                            link.reconnectDelayMs.toLong()
                                        )
                                    },
                                    onInstanceLinkDisconnect = {
                                        setInstanceLinkEnabled(false)
                                        instanceLinkViewModel.disconnect()
                                    },
                                    instanceLinkRemoteSchedule = instanceLinkViewModel.remoteSchedule.collectAsState().value,
                                    instanceLinkRemoteSongCatalog = instanceLinkViewModel.remoteSongCatalog.collectAsState().value,
                                    instanceLinkFetchSongDetail = { number, songbook -> instanceLinkViewModel.fetchSongDetail(number, songbook) },
                                    instanceLinkFetchBibleFile = { instanceLinkViewModel.fetchBibleFile() },
                                    instanceLinkBibleSyncMode = appSettings.instanceLink.bibleSyncMode,
                                    instanceLinkFetchSecondaryBibleFile = { instanceLinkViewModel.fetchSecondaryBibleFile() },
                                    instanceLinkFetchBibleTranslations = { instanceLinkViewModel.fetchBibleTranslations() },
                                    instanceLinkOnSecondaryBibleFilePathChanged = { path -> companionServer.updateSecondaryBibleFilePath(path) },
                                    instanceLinkOnBibleFilePathsChanged = { paths -> companionServer.updateBibleFilePaths(paths) },
                                    instanceLinkSendAddToSchedule = if (appSettings.instanceLink.allowPushToSchedule) {
                                        { item -> instanceLinkViewModel.sendAddToSchedule(item) }
                                    } else null,
                                    instanceLinkSendRemoveFromSchedule = if (appSettings.instanceLink.allowPushToSchedule) {
                                        { id -> instanceLinkViewModel.sendRemoveFromSchedule(id) }
                                    } else null,
                                    instanceLinkRole = appSettings.instanceLink.role,
                                    instanceLinkSendProject = if (instanceLinkIsControllerConnected) {
                                        { item -> instanceLinkViewModel.sendProject(item) }
                                    } else null,
                                    instanceLinkSendVerse = if (instanceLinkIsControllerConnected) {
                                        { bookName, chapter, verseNumber, verseText, verseRange ->
                                            instanceLinkViewModel.sendSelectBibleVerse(bookName, chapter, verseNumber, verseText, verseRange)
                                        }
                                    } else null,
                                    instanceLinkSendPicture = if (instanceLinkIsControllerConnected) {
                                        { folderId, index, fileName -> instanceLinkViewModel.sendSelectPicture(folderId, index, fileName) }
                                    } else null,
                                    instanceLinkSendSongSection = if (instanceLinkIsControllerConnected) {
                                        { number, section, lineIndex -> instanceLinkViewModel.sendSelectSongSection(number, section, lineIndex) }
                                    } else null,
                                    instanceLinkSendSlide = if (instanceLinkIsControllerConnected) {
                                        { id, index -> instanceLinkViewModel.sendSelectSlide(id, index) }
                                    } else null,
                                    instanceLinkSendClear = if (instanceLinkIsControllerConnected) {
                                        { instanceLinkViewModel.sendClear() }
                                    } else null,
                                    instanceLinkSendBibleHold = if (instanceLinkIsControllerConnected) {
                                        { hold -> instanceLinkViewModel.sendBibleHold(hold) }
                                    } else null,
                                    instanceLinkSendNextPicture = if (instanceLinkIsControllerConnected) {
                                        { instanceLinkViewModel.sendNextPicture() }
                                    } else null,
                                    instanceLinkSendPreviousPicture = if (instanceLinkIsControllerConnected) {
                                        { instanceLinkViewModel.sendPreviousPicture() }
                                    } else null,
                                    instanceLinkSendNextSlide = if (instanceLinkIsControllerConnected) {
                                        { instanceLinkViewModel.sendNextSlide() }
                                    } else null,
                                    instanceLinkSendPreviousSlide = if (instanceLinkIsControllerConnected) {
                                        { instanceLinkViewModel.sendPreviousSlide() }
                                    } else null,
                                    instanceLinkFetchPictureImageBytes = if (instanceLinkUsesRemoteContent) {
                                        { folderId, index -> instanceLinkViewModel.fetchPictureImageBytes(folderId, index) }
                                    } else null,
                                    instanceLinkFetchPresentationSlideBytes = if (instanceLinkUsesRemoteContent) {
                                        { id, index -> instanceLinkViewModel.fetchPresentationSlideBytes(id, index) }
                                    } else null,
                                    instanceLinkMediaStreamUrl = run {
                                        val link = appSettings.instanceLink
                                        if (instanceLinkUsesRemoteContent) {
                                            ({ itemId: String ->
                                                val keyParam = if (link.apiKey.isNotEmpty()) "?${Constants.QUERY_PARAM_API_KEY}=${link.apiKey}" else ""
                                                "http://${link.primaryHost}:${link.primaryPort}${Constants.ENDPOINT_MEDIA_STREAM}/$itemId$keyParam"
                                            })
                                        } else null
                                    },
                                    onVerseSelected = { verses -> presenterManager.setSelectedVerses(verses) },
                                    onSongItemSelected = { section ->
                                        presenterManager.setLyricSection(section)
                                        // In line mode, sync displayedLyricSection immediately so it updates
                                        // in the same Compose snapshot as songDisplayLineIndex. Without this,
                                        // there is an intermediate recomposition where songDisplayLineIndex=0
                                        // but displayedLyricSection still points to the old verse, causing the
                                        // first line of the old verse to flash briefly on verse boundaries.
                                        val ss = appSettings.songSettings
                                        val inLineMode = ss.fullscreenDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                                            ss.lowerThirdDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                                            ss.lookAheadDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                                            ss.lowerThirdLookAheadDisplayMode == Constants.SONG_DISPLAY_MODE_LINE
                                        if (inLineMode) {
                                            presenterManager.setDisplayedLyricSection(section)
                                        }
                                    },
                                    onAllSectionsChanged = { presenterManager.setAllLyricSections(it) },
                                    onSectionIndexChanged = { presenterManager.setSongDisplaySectionIndex(it) },
                                    onLineIndexChanged = { presenterManager.setSongDisplayLineIndex(it) },
                                    appSettings = appSettings,
                                    livePreviewAppSettings = effectiveAppSettings,
                                    presenterManager = presenterManager,
                                    statisticsManager = statisticsManager,
                                    onScheduleActionsReady = { scheduleActions = it },
                                    presenting = { mode ->
                                        presenterManager.setPresentingMode(mode)
                                        if (mode != Presenting.NONE) presenterManager.setShowPresenterWindow(true)
                                    },
                                    onScheduleItemSelected = { itemId -> selectedScheduleItemId = itemId },
                                    onShowSettings = { openOptionsDialog(0) },
                                    onShowBackgroundSettings = { openOptionsDialog(3) },
                                    onSettingsChange = { updateFn ->
                                        appSettings = updateFn(appSettings)
                                        settingsManager.saveSettings(appSettings)
                                    },
                                    theme = theme,
                                    onSongsLoaded = { songs -> companionServer.updateSongs(songs) },
                                    onScenesChanged = { scenes -> scenesForInstanceLink = scenes },
                                    onBibleLoaded = { bible, translation ->
                                        primaryBibleForInstanceLink = bible
                                        companionServer.updateBible(
                                            bible,
                                            translation,
                                            filePath = File(appSettings.bibleSettings.storageDirectory, translation).absolutePath
                                        )
                                    },
                                    onScheduleChanged = { items -> companionServer.updateSchedule(items) },
                                    onPresentationSlidesLoaded = { id, filePath, fileName, fileType, slides, notes ->
                                        companionServer.updatePresentation(id, filePath, fileName, fileType, slides, notes)
                                    },
                                    onPicturesLoaded = { folderId, folderName, folderPath, imageFiles ->
                                        companionServer.updatePictures(folderId, folderName, folderPath, imageFiles)
                                    },
                                    selectPictureImageFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onSelectPicture.collect { req ->
                                            emit(req.folderId to req.index)
                                        }
                                    },
                                    resolveImageFile = { folderId, index ->
                                        companionServer.getImageFile(folderId, index)
                                    },
                                    selectSlideFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onSelectSlide.collect { req ->
                                            emit(req.id to req.index)
                                        }
                                    },
                                    selectBibleVerseFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onSelectBibleVerse.collect { req ->
                                            emit(req)
                                        }
                                    },
                                    remoteSelectSongFlow = remoteSelectSongFlow,
                                    remoteSelectPictureFlow = remoteSelectPictureFlow,
                                    remoteSelectPresentationFlow = remoteSelectPresentationFlow,
                                    nextPictureFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onNextPicture.collect { emit(Unit) }
                                    },
                                    previousPictureFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onPreviousPicture.collect { emit(Unit) }
                                    },
                                    nextSlideFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onNextSlide.collect { emit(Unit) }
                                    },
                                    previousSlideFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onPreviousSlide.collect { emit(Unit) }
                                    },
                                    uploadPresentationFlow = kotlinx.coroutines.flow.flow {
                                        companionServer.onPresentationUploaded.collect { file ->
                                            emit(file)
                                        }
                                    },
                                    serverUrl = companionServer.serverUrl.collectAsState().value,
                                    qaManager = qaManager,
                                    tunnelStatus = tunnelStatus,
                                    tunnelUrl = tunnelUrl ?: "",
                                    onStartTunnel = { companionServer.tunnelManager.start(appSettings.serverSettings.port) },
                                    onStopTunnel = { companionServer.tunnelManager.stop() },
                                    qaDisplayUrl = qaDisplayUrl,
                                    onQaDisplayUrlChanged = { qaDisplayUrl = it },
                                    presentationDisplayUrl = presentationDisplayUrl,
                                    onPresentationDisplayUrlChanged = { presentationDisplayUrl = it },
                                    onSlideChanged = { id, index, total, isPlaying ->
                                        companionServer.broadcastSlideChange(id, index, total, isPlaying)
                                    },
                                    remotePresentationPlayPauseFlow = companionServer.onPresentationPlayPause,
                                    remotePresentationLoopToggleFlow = companionServer.onPresentationLoopToggle,
                                    remotePresentationGotoFlow = companionServer.onPresentationGoto,
                                    presentationFrozen = presentationFrozen,
                                    onFreezeToggle = {
                                        presentationFrozen = !presentationFrozen
                                        companionServer.broadcastFreezeChange(presentationFrozen)
                                        presenterManager.setSlideFrozen(presentationFrozen)
                                    },
                                    onClearPresentation = {
                                        companionServer.clearPresentationState()
                                        presenterManager.requestClearDisplay()
                                    },
                                    onOpenLottieGen = { outputDir, onSaved ->
                                        if (outputDir.isNotEmpty() && File(outputDir).isDirectory) {
                                            lottieGenOutputDir = File(outputDir)
                                            lottieGenOnFileSaved = onSaved
                                            showLottieGenWindow = true
                                        } else {
                                            javax.swing.JOptionPane.showMessageDialog(
                                                null,
                                                "Please set a Lower Third folder in Settings first.",
                                                "No Folder Configured",
                                                javax.swing.JOptionPane.WARNING_MESSAGE
                                            )
                                        }
                                    },
                                    sttManager = sttManager,
                                    dialogDismissSignal = dialogDismissSignal,
                                    companionSatelliteViewModel = companionSatelliteViewModel,
                                    onRequestDeveloperMenuUnlock = { developerMenuUnlocked = true }
                                )
                                OptionsDialog(
                                    isVisible = showOptionsDialog,
                                    initialTab = optionsDialogInitialTab,
                                    initialSettings = appSettings,
                                    theme = theme,
                                    settingsManager = settingsManager,
                                    companionServer = companionServer,
                                    remoteClientManager = remoteClientManager,
                                    presenterManager = presenterManager,
                                    onDismiss = { showOptionsDialog = false; dialogDismissSignal++ },
                                    onSave = { updated ->
                                        appSettings = updated
                                        settingsManager.saveSettings(updated)
                                        // Re-preload in case bible/song directories changed
                                        companionServer.preloadData(
                                            songStorageDir = updated.songSettings.storageDirectory,
                                            bibleStorageDir = updated.bibleSettings.storageDirectory,
                                            primaryBibleFileName = updated.bibleSettings.primaryBible
                                        )
                                        // Keep API key enforcement in sync with saved settings
                                        companionServer.updateApiKey(
                                            enabled = updated.serverSettings.apiKeyEnabled,
                                            key = updated.serverSettings.apiKey
                                        )
                                        companionServer.updateFileUploadEnabled(updated.serverSettings.fileUploadEnabled)
                                        companionServer.updateMaxMediaUploadMb(updated.serverSettings.maxMediaUploadMb)
                                        companionServer.updateAtemConfig(
                                            updated.atemSettings,
                                            updated.streamingSettings.lowerThirdFolder
                                        )
                                    },
                                    onThemeChange = { newTheme ->
                                        appSettings = appSettings.copy(theme = newTheme.toString())
                                        theme = newTheme
                                        settingsManager.saveSettings(appSettings)
                                    },
                                    onIdentifyScreen = {
                                        identifyingScreen = true
                                        coroutineScope.launch {
                                            delay(5_000L)
                                            identifyingScreen = false
                                        }
                                    },
                                    onIdentifyBrowserSource = { index ->
                                        presenterManager.identifyBrowserSourceOutput(index)
                                    },
                                    onOpenLottieGen = { outputDir, onSaved ->
                                        if (outputDir.isNotEmpty() && File(outputDir).isDirectory) {
                                            lottieGenOutputDir = File(outputDir)
                                            lottieGenOnFileSaved = onSaved
                                            showLottieGenWindow = true
                                        } else {
                                            javax.swing.JOptionPane.showMessageDialog(
                                                null,
                                                "Please set a Lower Third folder in Settings first.",
                                                "No Folder Configured",
                                                javax.swing.JOptionPane.WARNING_MESSAGE
                                            )
                                        }
                                    },
                                    obsManager = obsManager,
                                    companionSatelliteViewModel = companionSatelliteViewModel
                                )
                                KeyboardShortcutsDialog(
                                    isVisible = showKeyboardShortcutsDialog,
                                    onDismiss = { showKeyboardShortcutsDialog = false; dialogDismissSignal++ }
                                )
                                StatisticsDialog(
                                    isVisible = showStatisticsDialog,
                                    theme = theme,
                                    statisticsManager = statisticsManager,
                                    onDismiss = { showStatisticsDialog = false; dialogDismissSignal++ }
                                )
                                InstanceLinkDialog(
                                    isVisible = showInstanceLinkDialog,
                                    settings = appSettings.instanceLink,
                                    connectionStatus = instanceLinkViewModel.connectionStatus.collectAsState().value,
                                    remoteLiveState = instanceLinkViewModel.remoteLiveState.collectAsState().value,
                                    remoteScheduleCount = instanceLinkViewModel.remoteSchedule.collectAsState().value.size,
                                    lastMessageAtMs = instanceLinkViewModel.lastMessageAtMs.collectAsState().value,
                                    onConnect = { edited ->
                                        val link = edited.copy(enabled = true)
                                        appSettings = appSettings.copy(instanceLink = link)
                                        settingsManager.saveSettings(appSettings)
                                        instanceLinkViewModel.connect(
                                            link.primaryHost, link.primaryPort, link.apiKey, link.deviceId,
                                            link.reconnectDelayMs.toLong()
                                        )
                                    },
                                    // Persist only — the connection is left exactly as it is, and the
                                    // reactive settings (role, Bible sync, backgrounds) take effect
                                    // from appSettings without one.
                                    onSave = { edited ->
                                        appSettings = appSettings.copy(instanceLink = edited)
                                        settingsManager.saveSettings(appSettings)
                                    },
                                    onDisconnect = {
                                        setInstanceLinkEnabled(false)
                                        instanceLinkViewModel.disconnect()
                                    },
                                    onDismiss = { showInstanceLinkDialog = false; dialogDismissSignal++ }
                                )
                                AboutDialog(
                                    isVisible = showAboutDialog,
                                    onDismiss = { showAboutDialog = false; dialogDismissSignal++ },
                                    appSettings = appSettings,
                                    theme = theme
                                )
                                ContactUsDialog(
                                    isVisible = showContactDialog,
                                    onDismiss = { showContactDialog = false; dialogDismissSignal++ }
                                )
                                if (showConverterWindow) {
                                    ConverterWindow(
                                        theme = theme,
                                        onClose = { showConverterWindow = false }
                                    )
                                }
                                if (showLottieGenWindow) {
                                    val screenBounds = presenterScreenBounds()
                                    LottieGenWindow(
                                        theme = theme,
                                        outputDir = lottieGenOutputDir,
                                        onClose = { showLottieGenWindow = false },
                                        onFileSaved = lottieGenOnFileSaved,
                                        canvasWidth = screenBounds.width,
                                        canvasHeight = screenBounds.height
                                    )
                                }
                                MemoryMonitorWindow(
                                    isVisible = showMemoryMonitorWindow,
                                    theme = theme,
                                    onClose = { showMemoryMonitorWindow = false }
                                )
                                if (showStyleEditorWindow) {
                                    StyleEditorWindow(
                                        theme = theme,
                                        onClose = { showStyleEditorWindow = false }
                                    )
                                }
                                UpdateAvailableDialog(
                                    result = pendingUpdateResult,
                                    isManualCheck = pendingUpdateCheckWasManual,
                                    participateInPrereleases = appSettings.participateInPrereleases,
                                    onParticipateInPrereleasesChange = { enabled ->
                                        appSettings = appSettings.copy(participateInPrereleases = enabled)
                                        settingsManager.saveSettings(appSettings)
                                        coroutineScope.launch {
                                            pendingUpdateResult = UpdateChecker.checkForUpdate(includePrereleases = enabled)
                                        }
                                    },
                                    updateCheckInterval = appSettings.updateCheckInterval,
                                    onUpdateCheckIntervalChange = { interval ->
                                        appSettings = appSettings.copy(updateCheckInterval = interval)
                                        settingsManager.saveSettings(appSettings)
                                    },
                                    onDismiss = { pendingUpdateResult = null }
                                )

                                // ── Remote API event dialog ───────────────────────
                                val currentRemote = remoteEventQueue.firstOrNull()
                                val currentClientId = currentRemote?.first?.clientId ?: ""
                                RemoteEventDialog(
                                    event = currentRemote?.first,
                                    queueSize = remoteEventQueue.size,
                                    isClientKnownAllowed = remoteClientManager.isAllowed(currentClientId),
                                    isClientKnownBlocked = remoteClientManager.isBlocked(currentClientId),
                                    isInstanceLinkFollower = currentClientId.isNotBlank() &&
                                        currentClientId in companionServer.connectedInstanceLinkFollowers.collectAsState().value,
                                    onAllow = {
                                        currentRemote?.second?.invoke()
                                        if (remoteEventQueue.isNotEmpty()) remoteEventQueue.removeAt(0)
                                    },
                                    onAllowForSession = {
                                        // Mark this client as session-allowed, then silently approve
                                        // the current item AND every other queued item from the same client
                                        if (currentClientId.isNotBlank() && !sessionAllowedClients.contains(
                                                currentClientId
                                            )
                                        ) {
                                            sessionAllowedClients.add(currentClientId)
                                        }
                                        val clientToAllow = currentClientId
                                        val toApprove =
                                            remoteEventQueue.filter { it.first.clientId == clientToAllow || clientToAllow.isBlank() }
                                        toApprove.forEach { it.second.invoke() }
                                        remoteEventQueue.removeAll(toApprove)
                                    },
                                    onAllowPermanently = {
                                        // Permanently allow and silently approve all queued items from this client
                                        remoteClientManager.allowPermanently(currentClientId)
                                        val clientToAllow = currentClientId
                                        val toApprove =
                                            remoteEventQueue.filter { it.first.clientId == clientToAllow || clientToAllow.isBlank() }
                                        toApprove.forEach { it.second.invoke() }
                                        remoteEventQueue.removeAll(toApprove)
                                    },
                                    onBlockForSession = {
                                        // Deny all queued items from this client; mark session-blocked
                                        if (currentClientId.isNotBlank() && !sessionBlockedClients.contains(
                                                currentClientId
                                            )
                                        ) {
                                            sessionBlockedClients.add(currentClientId)
                                        }
                                        val clientToBlock = currentClientId
                                        val toRemove =
                                            remoteEventQueue.filter { it.first.clientId == clientToBlock || clientToBlock.isBlank() }
                                        toRemove.forEach { it.third.invoke() }
                                        remoteEventQueue.removeAll(toRemove)
                                    },
                                    onBlockPermanently = {
                                        remoteClientManager.blockPermanently(currentClientId)
                                        // Deny all queued items from this client
                                        val clientToBlock = currentClientId
                                        val toRemove =
                                            remoteEventQueue.filter { it.first.clientId == clientToBlock || clientToBlock.isBlank() }
                                        toRemove.forEach { it.third.invoke() }
                                        remoteEventQueue.removeAll(toRemove)
                                    },
                                    onDeny = {
                                        currentRemote?.third?.invoke()
                                        if (remoteEventQueue.isNotEmpty()) remoteEventQueue.removeAt(0)
                                    }
                                )

                                // ── Activity toast for auto-approved clients ──────────────
                                InstanceLinkToastHost(
                                    failures = instanceLinkCommandFailures,
                                    onDismiss = { failure -> instanceLinkCommandFailures.remove(failure) }
                                )
                                RemoteActivityToastHost(
                                    notifications = remoteActivityNotifications,
                                    connectedInstanceLinkFollowers = companionServer.connectedInstanceLinkFollowers.collectAsState().value,
                                    onDismiss = { n -> remoteActivityNotifications.remove(n) },
                                    onDismissAll = { remoteActivityNotifications.clear() },
                                    onBlockForSession = { n ->
                                        val cid = n.clientId
                                        if (cid.isNotBlank() && !sessionBlockedClients.contains(cid)) {
                                            sessionBlockedClients.add(cid)
                                            // Also remove from session-allowed if present
                                            sessionAllowedClients.remove(cid)
                                        }
                                        remoteActivityNotifications.removeAll { it.clientId == cid }
                                    }
                                )
                            } // end Box (window content)
                        }
                    }
                }
            }

            // Auto-clear presenting mode when media finishes playing
            LaunchedEffect(mediaViewModel.mediaFinished) {
                if (mediaViewModel.mediaFinished) {
                    presenterManager.requestClearDisplay()
                    mediaViewModel.clearFinished()
                }
            }

            PresenterWindows(
                screens = screens,
                presenterManager = presenterManager,
                mediaViewModel = mediaViewModel,
                appSettings = effectiveAppSettings,
                identifyingScreen = identifyingScreen,
                serverUrl = companionServer.serverUrl.collectAsState().value,
                qaDisplayUrl = qaDisplayUrl,
                sttManager = sttManager,
            )
        }

        if (appReady && eulaAccepted && showSetupWizard) {
            SetupWizardDialog(
                theme = theme,
                selectedLanguage = currentLanguage,
                alwaysOnTop = !showOptionsDialog,
                onLanguageSelected = { language ->
                    currentLanguage = language
                    appSettings = appSettings.copy(language = language.code)
                    settingsManager.saveSettings(appSettings)
                    Locale.setDefault(Locale.forLanguageTag(language.code))
                },
                onThemeSelected = { newTheme ->
                    theme = newTheme
                    appSettings = appSettings.copy(theme = newTheme.toString())
                    settingsManager.saveSettings(appSettings)
                },
                onOpenSettings = { openOptionsDialog(0) },
                onDismiss = {
                    val updated = appSettings.copy(setupWizardShown = true)
                    settingsManager.saveSettings(updated)
                    appSettings = updated
                    showSetupWizard = false
                }
            )
        }

        LicenseDialog(
            isVisible = appReady && !eulaAccepted,
            onAccept = {
                val updated = appSettings.copy(eulaAcceptedVersion = CURRENT_EULA_VERSION)
                settingsManager.saveSettings(updated)
                appSettings = updated
                eulaAccepted = true
            },
            onDecline = { exitApplication() }
        )
    }
}


@Composable
private fun SplashWindow(theme: ThemeMode) {
    Window(
        onCloseRequest = {},
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.ic_app_icon),
        state = rememberWindowState(
            width = 400.dp,
            height = 300.dp,
            position = WindowPosition(Alignment.Center)
        ),
        undecorated = true,
        resizable = false,
        alwaysOnTop = true
    ) {
        AppThemeWrapper(theme = theme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenterWindows(
    screens: Array<GraphicsDevice>,
    presenterManager: PresenterManager,
    mediaViewModel: MediaViewModel,
    appSettings: AppSettings,
    identifyingScreen: Boolean,
    serverUrl: String = "",
    qaDisplayUrl: String = "",
    sttManager: STTManager,
) {
    val showPresenterWindow by presenterManager.showPresenterWindow
    val presentingMode by presenterManager.presentingMode
    val screenLocks by presenterManager.screenLocks
    val selectedVerses by presenterManager.selectedVerses
    val displayedVerses by presenterManager.displayedVerses
    val nextVerses by presenterManager.nextVerses
    val bibleTransitionAlpha by presenterManager.bibleTransitionAlpha
    val lyricSection by presenterManager.lyricSection
    val lyricSectionVersion by presenterManager.lyricSectionVersion
    val displayedLyricSection by presenterManager.displayedLyricSection
    val songTransitionAlpha by presenterManager.songTransitionAlpha
    val songDisplayLineIndex by presenterManager.songDisplayLineIndex
    val allLyricSections by presenterManager.allLyricSections
    val songDisplaySectionIndex by presenterManager.songDisplaySectionIndex
    val selectedImagePath by presenterManager.selectedImagePath
    val displayedImagePath by presenterManager.displayedImagePath
    val pictureTransitionAlpha by presenterManager.pictureTransitionAlpha
    val previousDisplayedImagePath by presenterManager.previousDisplayedImagePath
    val pictureSlideOffset by presenterManager.pictureSlideOffset
    val selectedSlide by presenterManager.selectedSlide
    val displayedSlide by presenterManager.displayedSlide
    val slideFrozen by presenterManager.slideFrozen
    val presentationFrame by presenterManager.presentationFrame
    val slideTransitionAlpha by presenterManager.slideTransitionAlpha
    val previousDisplayedSlide by presenterManager.previousDisplayedSlide
    val slideSlideOffset by presenterManager.slideSlideOffset
    val animationType by presenterManager.animationType
    val transitionDuration by presenterManager.transitionDuration
    val announcementText by presenterManager.announcementText
    val displayedAnnouncementText by presenterManager.displayedAnnouncementText
    val announcementTransitionAlpha by presenterManager.announcementTransitionAlpha
    val clearAnnouncementOnFinish = {
        presenterManager.setAnnouncementText("")
        presenterManager.setDisplayedAnnouncementText("")
        presenterManager.requestClearDisplay()
    }
    val lottieJsonContent by presenterManager.lottieJsonContent
    val lottiePauseAtFrame by presenterManager.lottiePauseAtFrame
    val lottiePauseFrame by presenterManager.lottiePauseFrame
    val lottiePauseDurationMs by presenterManager.lottiePauseDurationMs
    val lottieTrigger by presenterManager.lottieTrigger
    val lottieProgress by presenterManager.lottieProgress
    val lottieFrame by presenterManager.lottieFrame
    val mediaTransitionAlpha by presenterManager.mediaTransitionAlpha
    val websiteUrl by presenterManager.websiteUrl
    val activeScene by presenterManager.activeScene
    val displayedQuestion by presenterManager.displayedQuestion
    val qaTransitionAlpha by presenterManager.qaTransitionAlpha
    val showQRCodeOnDisplay by presenterManager.showQRCodeOnDisplay
    val displayedDictionaryEntry by presenterManager.displayedDictionaryEntry
    val presenterNotes by presenterManager.presenterNotes

    val proj = appSettings.projectionSettings

    // Mode-level crossfade duration (shared, per-screen active flag computed inside each window)
    val modeCrossfadeDuration = maxOf(
        if (appSettings.bibleSettings.crossfade) appSettings.bibleSettings.transitionDuration.toInt() else 0,
        if (appSettings.songSettings.crossfade) appSettings.songSettings.transitionDuration.toInt() else 0
    ).coerceAtLeast(100)

    // Fade-out before clearing display
    val clearRequested by presenterManager.clearDisplayRequested
    LaunchedEffect(clearRequested) {
        if (!clearRequested) return@LaunchedEffect
        val mode = presenterManager.presentingMode.value
        // Don't fade the content alpha if any screen is locked to this mode —
        // that screen is still showing the content and the alpha must stay at 1.
        val modeIsLocked = presenterManager.screenLocks.value.values.any { it == mode }
        val shouldFade = !modeIsLocked && when (mode) {
            Presenting.BIBLE -> appSettings.bibleSettings.fadeOut
            Presenting.LYRICS -> appSettings.songSettings.fadeOut
            else -> false
        }
        if (shouldFade) {
            val duration = when (mode) {
                Presenting.BIBLE -> appSettings.bibleSettings.transitionDuration.toInt()
                Presenting.LYRICS -> appSettings.songSettings.transitionDuration.toInt()
                else -> 500
            }.coerceAtLeast(100)
            val anim = Animatable(1f)
            anim.animateTo(0f, tween(durationMillis = duration)) {
                when (mode) {
                    Presenting.BIBLE -> presenterManager.setBibleTransitionAlpha(this.value)
                    Presenting.LYRICS -> presenterManager.setSongTransitionAlpha(this.value)
                    else -> {}
                }
            }
        }
        // Set mode to NONE — alphas stay at 0 until next go-live triggers fade-in
        presenterManager.setPresentingMode(Presenting.NONE)
    }

    // Centralized Bible transition: one animation drives all windows so they stay in sync
    // When hold is active, skip updating displayedVerses so the user can browse freely
    val bibleHold by presenterManager.bibleHold
    LaunchedEffect(selectedVerses, bibleHold) {
        if (bibleHold) return@LaunchedEffect
        val bs = appSettings.bibleSettings
        // All transitions (crossfade, fade in/out) are handled inside BiblePresenter
        presenterManager.setDisplayedVerses(selectedVerses)
        presenterManager.setBibleTransitionAlpha(1f)
    }

    // Centralized Song transition
    LaunchedEffect(lyricSection, lyricSectionVersion) {
        val ss = appSettings.songSettings
        // Skip animation when section content hasn't changed (e.g. line navigation within same verse)
        if (lyricSection == presenterManager.displayedLyricSection.value) {
            presenterManager.setSongTransitionAlpha(1f)
            return@LaunchedEffect
        }
        // Skip fade in line mode — only one line visible, instant swap is cleaner
        val isLineMode = ss.fullscreenDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                ss.lowerThirdDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                ss.lookAheadDisplayMode == Constants.SONG_DISPLAY_MODE_LINE ||
                ss.lowerThirdLookAheadDisplayMode == Constants.SONG_DISPLAY_MODE_LINE
        if (isLineMode) {
            presenterManager.setDisplayedLyricSection(lyricSection)
            presenterManager.setSongTransitionAlpha(1f)
            return@LaunchedEffect
        }
        // All transitions (crossfade, fade in/out) are handled inside SongPresenter
        presenterManager.setDisplayedLyricSection(lyricSection)
        presenterManager.setSongTransitionAlpha(1f)
    }

    // Centralized Picture transition
    LaunchedEffect(selectedImagePath) {
        val current = presenterManager.displayedImagePath.value
        when {
            current == null || animationType == AnimationType.NONE -> {
                presenterManager.setDisplayedImagePath(selectedImagePath)
                presenterManager.setPictureTransitionAlpha(1f)
                presenterManager.setPreviousDisplayedImagePath(null)
            }
            animationType == AnimationType.FADE -> {
                val halfDuration = transitionDuration / 2
                val anim = Animatable(1f)
                anim.animateTo(0f, tween(halfDuration)) {
                    presenterManager.setPictureTransitionAlpha(value)
                }
                presenterManager.setDisplayedImagePath(selectedImagePath)
                anim.animateTo(1f, tween(halfDuration)) {
                    presenterManager.setPictureTransitionAlpha(value)
                }
            }
            animationType == AnimationType.CROSSFADE -> {
                presenterManager.setPreviousDisplayedImagePath(current)
                presenterManager.setDisplayedImagePath(selectedImagePath)
                presenterManager.setPictureTransitionAlpha(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(transitionDuration)) {
                    presenterManager.setPictureTransitionAlpha(value)
                }
                presenterManager.setPreviousDisplayedImagePath(null)
            }
            animationType == AnimationType.SLIDE_LEFT || animationType == AnimationType.SLIDE_RIGHT -> {
                presenterManager.setPreviousDisplayedImagePath(current)
                presenterManager.setDisplayedImagePath(selectedImagePath)
                presenterManager.setPictureTransitionAlpha(1f)
                presenterManager.setPictureSlideOffset(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(transitionDuration)) {
                    presenterManager.setPictureSlideOffset(value)
                }
                presenterManager.setPreviousDisplayedImagePath(null)
                presenterManager.setPictureSlideOffset(1f)
            }
        }
    }

    // Animated-presentation frame clock: one evaluation per display frame while a build step
    // animates, published to every output window via presenterManager.presentationFrame.
    LaunchedEffect(Unit) {
        presenterManager.runPresentationClock()
    }

    // Centralized Slide transition
    LaunchedEffect(selectedSlide) {
        val current = presenterManager.displayedSlide.value
        when {
            current == null || animationType == AnimationType.NONE -> {
                presenterManager.setDisplayedSlide(selectedSlide)
                presenterManager.setSlideTransitionAlpha(1f)
                presenterManager.setPreviousDisplayedSlide(null)
            }
            animationType == AnimationType.FADE -> {
                val halfDuration = transitionDuration / 2
                val anim = Animatable(1f)
                anim.animateTo(0f, tween(halfDuration)) {
                    presenterManager.setSlideTransitionAlpha(value)
                }
                presenterManager.setDisplayedSlide(selectedSlide)
                anim.animateTo(1f, tween(halfDuration)) {
                    presenterManager.setSlideTransitionAlpha(value)
                }
            }
            animationType == AnimationType.CROSSFADE -> {
                presenterManager.setPreviousDisplayedSlide(current)
                presenterManager.setDisplayedSlide(selectedSlide)
                presenterManager.setSlideTransitionAlpha(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(transitionDuration)) {
                    presenterManager.setSlideTransitionAlpha(value)
                }
                presenterManager.setPreviousDisplayedSlide(null)
            }
            animationType == AnimationType.SLIDE_LEFT || animationType == AnimationType.SLIDE_RIGHT -> {
                presenterManager.setPreviousDisplayedSlide(current)
                presenterManager.setDisplayedSlide(selectedSlide)
                presenterManager.setSlideTransitionAlpha(1f)
                presenterManager.setSlideSlideOffset(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(transitionDuration)) {
                    presenterManager.setSlideSlideOffset(value)
                }
                presenterManager.setPreviousDisplayedSlide(null)
                presenterManager.setSlideSlideOffset(1f)
            }
        }
    }

    // Centralized Announcements transition
    LaunchedEffect(announcementText) {
        val annSettings = appSettings.announcementsSettings
        val isFade = annSettings.animationType == Constants.ANIMATION_FADE
        val isNone = annSettings.animationType == Constants.ANIMATION_NONE
        val wasEmpty = presenterManager.displayedAnnouncementText.value.isEmpty()
        val fadeDuration = 500
        val sliderSum = 30500L // 500 + 30000, matches AnnouncementsTab speed slider
        val displayDuration = (sliderSum - annSettings.animationDuration).coerceAtLeast(500)
        val loopCount = annSettings.loopCount

        if (!isFade && !isNone) {
            // Directional slides — just swap text, animation handled in AnnouncementsPresenter
            presenterManager.setDisplayedAnnouncementText(announcementText)
            presenterManager.setAnnouncementTransitionAlpha(1f)
        } else if (announcementText.isEmpty()) {
            // Cleared by user or loop finished — fade out if fade, instant if none
            if (isFade && !wasEmpty) {
                val anim = Animatable(1f)
                anim.animateTo(0f, tween(fadeDuration)) {
                    presenterManager.setAnnouncementTransitionAlpha(value)
                }
            }
            presenterManager.setDisplayedAnnouncementText("")
            presenterManager.setAnnouncementTransitionAlpha(1f)
        } else {
            // Show text with timed display
            presenterManager.setDisplayedAnnouncementText(announcementText)

            // Fade in (only for fade animation)
            if (isFade) {
                presenterManager.setAnnouncementTransitionAlpha(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(fadeDuration)) {
                    presenterManager.setAnnouncementTransitionAlpha(value)
                }
            } else {
                presenterManager.setAnnouncementTransitionAlpha(1f)
            }

            if (loopCount > 0) {
                // Finite loops: display for duration × loopCount, then clear
                delay(displayDuration * loopCount)

                // Fade out (only for fade animation)
                if (isFade) {
                    val anim = Animatable(1f)
                    anim.animateTo(0f, tween(fadeDuration)) {
                        presenterManager.setAnnouncementTransitionAlpha(value)
                    }
                }
                // Clear display and exit presenting mode
                presenterManager.setAnnouncementText("")
                presenterManager.setDisplayedAnnouncementText("")
                presenterManager.requestClearDisplay()
            }
            // loopCount == 0: infinite — stay visible until user manually stops
        }
    }

    // Centralized Lottie (lower third) animation — one animation drives all windows
    val lottieComposition by rememberLottieComposition(key = lottieJsonContent) {
        LottieCompositionSpec.JsonString(lottieJsonContent)
    }
    LaunchedEffect(lottieComposition, lottiePauseAtFrame, lottiePauseFrame, lottiePauseDurationMs, lottieTrigger) {
        // The live Compottie GPU-vector renderer (used below whenever pre-rendered frames aren't
        // ready yet) can silently render nothing partway through a clip on some GPU/driver
        // combinations. Pre-rendered raw frames don't share that failure mode (each is a static
        // bitmap, independently rendered ahead of time). So this loop polls
        // presenterManager.lottieFrameCount LIVE on every tick (not just once at effect-start) and
        // switches over to raw-frame playback the instant frames become available — continuing
        // from the same elapsed-time position, so there's no visible jump — instead of committing
        // to whichever path was ready first for the whole clip.
        try {
            val comp = lottieComposition
            val initialFrameCount = presenterManager.lottieFrameCount.value
            val totalDurMs = when {
                comp != null -> ((comp.durationFrames / comp.frameRate) * 1000f).toLong().coerceAtLeast(1L)
                initialFrameCount != null -> (initialFrameCount * 1000L / presenterManager.lottiePrerenderFps.value).coerceAtLeast(1L)
                else -> return@LaunchedEffect
            }
            val hasPause = lottiePauseAtFrame && lottiePauseFrame in 0f..1f
            val pauseAtMs = if (hasPause) (totalDurMs * lottiePauseFrame).toLong() else -1L
            val grandTotalMs = totalDurMs + (if (hasPause) lottiePauseDurationMs else 0L)

            fun progressAt(elapsedMs: Long): Float {
                if (!hasPause) return (elapsedMs.toFloat() / totalDurMs).coerceIn(0f, 1f)
                return when {
                    elapsedMs < pauseAtMs -> (elapsedMs.toFloat() / totalDurMs).coerceIn(0f, lottiePauseFrame)
                    elapsedMs < pauseAtMs + lottiePauseDurationMs -> lottiePauseFrame
                    else -> {
                        val postElapsed = elapsedMs - pauseAtMs - lottiePauseDurationMs
                        val postTotalMs = (totalDurMs - pauseAtMs).coerceAtLeast(1L)
                        (lottiePauseFrame + (postElapsed.toFloat() / postTotalMs) * (1f - lottiePauseFrame)).coerceIn(0f, 1f)
                    }
                }
            }

            // Vsync-driven clock: elapsed time comes from real frame timestamps, so a missed
            // display frame self-corrects on the next one instead of accumulating drift the way
            // a fixed-delay tick would.
            val startNanos = withFrameNanos { it }
            var elapsedMs = 0L
            while (true) {
                val frameCount = presenterManager.lottieFrameCount.value
                val progress = progressAt(elapsedMs)
                if (frameCount != null) {
                    val idx = (progress * (frameCount - 1)).roundToInt().coerceIn(0, frameCount - 1)
                    presenterManager.setLottieCurrentFrameIndex(idx)
                } else {
                    presenterManager.setLottieProgress(progress)
                }
                if (elapsedMs >= grandTotalMs) break
                val nowNanos = withFrameNanos { it }
                elapsedMs = ((nowNanos - startNanos) / 1_000_000).coerceAtMost(grandTotalMs)
            }
            // Snap to the final state (re-check readiness one last time in case it just became ready).
            val finalFrameCount = presenterManager.lottieFrameCount.value
            if (finalFrameCount != null) {
                presenterManager.setLottieCurrentFrameIndex(finalFrameCount - 1)
            } else {
                presenterManager.setLottieProgress(1f)
            }
            presenterManager.requestClearDisplay()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CrashReporter.reportException(e, "Lottie playback LaunchedEffect")
            throw e
        }
    }

    // Shared primary/normal output content — driven by a ScreenAssignment so behavior (crossfade,
    // lower-third layout, per-type visibility, language mode, backgrounds) is identical whether the
    // caller is a real per-screen output window or the developer-only windowed test output below.
    // screenNumber is null when there's no physical screen to label (e.g. the test window).
    val presenterOutputContent: @Composable (screenAssignment: ScreenAssignment, effectiveMode: Presenting, screenNumber: Int?) -> Unit = { screenAssignment, effectiveMode, screenNumber ->
        val primaryRole = screenAssignment.primaryOutputRole
        val showBg = if (screenAssignment.isLowerThird) screenAssignment.showLowerThirdBackground else screenAssignment.showFullscreenBackground
        CompositionLocalProvider(LocalMediaViewModel provides mediaViewModel) {
            if (screenAssignment.displayMode == Constants.DISPLAY_MODE_STAGE_MONITOR) {
                // Stage monitor: dedicated presenter-confidence layout
                StageMonitorScreen(
                    sm = appSettings.stageMonitorSettings,
                    presentingMode = presentingMode,
                    announcementActive = effectiveMode == Presenting.ANNOUNCEMENTS,
                    currentLyricSection = displayedLyricSection,
                    allLyricSections = allLyricSections,
                    songDisplaySectionIndex = songDisplaySectionIndex,
                    displayedVerses = displayedVerses,
                    nextVerses = nextVerses,
                    announcementText = displayedAnnouncementText,
                    displayedImagePath = displayedImagePath,
                    displayedSlide = displayedSlide,
                    presenterNotes = presenterNotes,
                    activeScene = activeScene,
                    displayedQuestion = displayedQuestion,
                    qaSettings = appSettings.qaSettings,
                    displayedDictionaryEntry = displayedDictionaryEntry,
                    dictionarySettings = appSettings.dictionarySettings,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PresenterScreen(
                    modifier = Modifier.fillMaxSize(),
                    appSettings = appSettings,
                    outputRole = primaryRole,
                    isLowerThird = screenAssignment.isLowerThird,
                    showBackground = showBg
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                                    mediaViewModel.pause()
                                    presenterManager.requestClearDisplay()
                                    true
                                } else false
                            }
                    ) {
                        var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                        val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                        if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                        Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                            when (mode) {
                                Presenting.BIBLE ->
                                    if (screenAssignment.showBible) {
                                        BiblePresenter(
                                            selectedVerses = displayedVerses,
                                            appSettings = appSettings,
                                            isLowerThird = screenAssignment.isLowerThird,
                                            isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                            outputRole = primaryRole,
                                            transitionAlpha = bibleTransitionAlpha,
                                            showBackground = showBg && screenAssignment.showBibleBackground,
                                            crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                            bibleTranslations = screenAssignment.bibleTranslations
                                        )
                                    }

                                Presenting.LYRICS ->
                                    if (screenAssignment.showSongs) {
                                        SongPresenter(
                                            lyricSection = displayedLyricSection,
                                            appSettings = appSettings,
                                            isLowerThird = screenAssignment.isLowerThird,
                                            isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                            outputRole = primaryRole,
                                            transitionAlpha = songTransitionAlpha,
                                            displayLineIndex = songDisplayLineIndex,
                                            lookAheadEnabled = screenAssignment.songLookAhead,
                                            allLyricSections = allLyricSections,
                                            displaySectionIndex = songDisplaySectionIndex,
                                            showBackground = showBg && screenAssignment.showSongsBackground,
                                            crossfadeEnabled = appSettings.songSettings.crossfade,
                                            languageOverride = screenAssignment.songMode
                                        )
                                    }

                                Presenting.PICTURES ->
                                    if (screenAssignment.showPictures)
                                        PicturePresenter(
                                            imagePath = displayedImagePath,
                                            previousImagePath = previousDisplayedImagePath,
                                            transitionAlpha = pictureTransitionAlpha,
                                            slideOffset = pictureSlideOffset,
                                            animationType = animationType
                                        )

                                Presenting.PRESENTATION ->
                                    if (screenAssignment.showPictures)
                                        PresentationPresenter(
                                            frame = presentationFrame,
                                            slide = displayedSlide,
                                            previousSlide = previousDisplayedSlide,
                                            transitionAlpha = slideTransitionAlpha,
                                            slideOffset = slideSlideOffset,
                                            animationType = animationType,
                                            frozen = slideFrozen
                                        )

                                Presenting.MEDIA ->
                                    if (screenAssignment.showMedia) {
                                        if (mediaViewModel.isAudioFile) {
                                            // Audio: playback handled by hidden VideoPlayer in MainDesktop
                                            // Projection shows background only
                                        } else {
                                            MediaPresenter(
                                                modifier = Modifier.fillMaxSize(),
                                                audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                                transitionAlpha = mediaTransitionAlpha
                                            )
                                        }
                                    }

                                Presenting.LOWER_THIRD ->
                                    if (screenAssignment.showStreaming)
                                        LowerThirdPresenter(
                                            composition = lottieComposition,
                                            progress = { presenterManager.lottieProgress.value },
                                            appSettings = appSettings,
                                            frame = lottieFrame
                                        )

                                Presenting.ANNOUNCEMENTS ->
                                    if (screenAssignment.showAnnouncements)
                                        AnnouncementsPresenter(
                                            text = displayedAnnouncementText,
                                            appSettings = appSettings,
                                            outputRole = primaryRole,
                                            transitionAlpha = announcementTransitionAlpha,
                                            onFinished = clearAnnouncementOnFinish,
                                            showBackground = showBg
                                        )

                                Presenting.WEBSITE ->
                                    if (screenAssignment.showWebsite) WebsitePresenter(
                                        url = websiteUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                        onBrowserCreated = { browser -> presenterManager.setLiveBrowser(browser) },
                                        onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                        onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                        audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId
                                    )

                                Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                                Presenting.QA ->
                                    if (screenAssignment.showQA) {
                                        if (showQRCodeOnDisplay) {
                                            QAQRCodePresenter(
                                                url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                                qaSettings = appSettings.qaSettings,
                                                transitionAlpha = qaTransitionAlpha,
                                            )
                                        } else {
                                            QAPresenter(
                                                question = displayedQuestion,
                                                qaSettings = appSettings.qaSettings,
                                                transitionAlpha = qaTransitionAlpha,
                                            )
                                        }
                                    }

                                Presenting.STT ->
                                    if (screenAssignment.showSTT) {
                                        STTPresenter(
                                            segments = sttManager.segments,
                                            inProgressText = sttManager.inProgressText.value,
                                            translationSegments = sttManager.translationSegments,
                                            inProgressTranslation = sttManager.inProgressTranslation.value,
                                            highlightedWords = sttManager.highlightedWords,
                                            sttSettings = appSettings.sttSettings,
                                        )
                                    }
                                Presenting.DICTIONARY ->
                                    if (screenAssignment.showDictionary)
                                        DictionaryPresenter(
                                            dictionarySettings = appSettings.dictionarySettings,
                                            entry = displayedDictionaryEntry,
                                            outputRole = primaryRole,
                                            transitionAlpha = 1f
                                        )
                                Presenting.NONE -> { /* nothing */
                                }
                            }
                        }

                        // Clear live browser ref when leaving WEBSITE mode
                        LaunchedEffect(presentingMode) {
                            if (presentingMode != Presenting.WEBSITE) {
                                presenterManager.setLiveBrowser(null)
                            }
                        }

                        if (screenNumber != null && identifyingScreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.75f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.screen_number, screenNumber),
                                    color = Color.White,
                                    fontSize = 96.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Identify the OS primary monitor and build list of non-primary screens
    val defaultDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    val availableScreens = screens.indices.filter { screens[it] != defaultDevice }

    val deckLinkDeviceCount = if (DeckLinkManager.isAvailable()) DeckLinkManager.listDevices().size else 0
    val windowCount = availableScreens.size + deckLinkDeviceCount
    // Dev convenience: on a single-monitor dev machine there's no non-primary monitor or DeckLink
    // device to open a real output window on. Show Output 1 as an ordinary window instead of not
    // rendering at all, driven by the same "Toggle Presenter Displays" button/state.
    val devWindowedFallback = (!BuildConfig.IS_RELEASE || DevFlags.forceDevWindow) && windowCount == 0
    // On a machine with no real output, open DevFlags.devWindowCount fallback windows (default 1).
    // A count > 1 simulates several independent outputs on one monitor for developing/testing
    // per-output features — each window is its own output slot (index, assignment, screen lock).
    val devFallbackCount = if (devWindowedFallback) proj.devWindowCount.coerceAtLeast(1) else 0
    for (i in 0 until (windowCount + devFallbackCount)) {
        if (devWindowedFallback && i >= windowCount) {
            val fallbackIndex = i - windowCount
            val screenAssignment = proj.getAssignment(fallbackIndex)
            val effectiveMode = screenLocks[fallbackIndex] ?: presentingMode
            // Cascade the windows so multiple dev outputs don't stack exactly on top of each other.
            val fallbackWindowState = remember(fallbackIndex) {
                WindowState(
                    width = 960.dp,
                    height = 540.dp,
                    position = WindowPosition(
                        x = (40 + fallbackIndex * 48).dp,
                        y = (40 + fallbackIndex * 48).dp,
                    ),
                )
            }
            Window(
                visible = showPresenterWindow,
                title = stringResource(Res.string.presenter_view_title, fallbackIndex + 1),
                icon = painterResource(Res.drawable.ic_app_icon),
                onCloseRequest = { presenterManager.setShowPresenterWindow(false) },
                state = fallbackWindowState,
                undecorated = false,
                resizable = true,
                alwaysOnTop = presenterManager.devWindowAlwaysOnTop.value,
            ) {
                presenterOutputContent(screenAssignment, effectiveMode, fallbackIndex + 1)
            }
            continue
        }
        val screenAssignment = proj.getAssignment(i)
        val effectiveMode = screenLocks[i] ?: presentingMode

        // DeckLink outputs: render via offscreen Window + pixel capture
        if (screenAssignment.targetType == "decklink") {
            if (showPresenterWindow && screenAssignment.targetDisplay >= 0) {
                val deckLinkRole = screenAssignment.primaryOutputRole
                DeckLinkComposeOutput(
                    deviceIndex = screenAssignment.targetDisplay,
                    outputRole = deckLinkRole,
                    appSettings = appSettings,
                    mediaViewModel = mediaViewModel,
                    isLowerThird = screenAssignment.isLowerThird,
                ) {
                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                    val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(
                        targetState = effectiveMode,
                        animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()
                    ) { mode ->
                    when (mode) {
                        Presenting.BIBLE ->
                            if (screenAssignment.showBible) {
                                BiblePresenter(
                                    selectedVerses = displayedVerses,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = deckLinkRole,
                                    transitionAlpha = bibleTransitionAlpha,
                                    crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                    bibleTranslations = screenAssignment.bibleTranslations
                                )
                            }

                        Presenting.LYRICS ->
                            if (screenAssignment.showSongs) {
                                SongPresenter(
                                    lyricSection = displayedLyricSection,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = deckLinkRole,
                                    transitionAlpha = songTransitionAlpha,
                                    displayLineIndex = songDisplayLineIndex,
                                    lookAheadEnabled = screenAssignment.songLookAhead,
                                    allLyricSections = allLyricSections,
                                    displaySectionIndex = songDisplaySectionIndex,
                                    crossfadeEnabled = appSettings.songSettings.crossfade,
                                    languageOverride = screenAssignment.songMode
                                )
                            }

                        Presenting.PICTURES ->
                            if (screenAssignment.showPictures)
                                PicturePresenter(
                                    imagePath = displayedImagePath,
                                    previousImagePath = previousDisplayedImagePath,
                                    transitionAlpha = pictureTransitionAlpha,
                                    slideOffset = pictureSlideOffset,
                                    animationType = animationType
                                )

                        Presenting.PRESENTATION ->
                            if (screenAssignment.showPictures)
                                PresentationPresenter(
                                    frame = presentationFrame,
                                    slide = displayedSlide,
                                    previousSlide = previousDisplayedSlide,
                                    transitionAlpha = slideTransitionAlpha,
                                    slideOffset = slideSlideOffset,
                                    animationType = animationType,
                                    frozen = slideFrozen
                                )

                        Presenting.MEDIA ->
                            if (screenAssignment.showMedia) {
                                if (mediaViewModel.isAudioFile) {
                                    // Audio: background only
                                } else {
                                    MediaPresenter(
                                        modifier = Modifier.fillMaxSize(),
                                        audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                        transitionAlpha = mediaTransitionAlpha
                                    )
                                }
                            }

                        Presenting.LOWER_THIRD ->
                            if (screenAssignment.showStreaming)
                                LowerThirdPresenter(
                                    composition = lottieComposition,
                                    progress = { presenterManager.lottieProgress.value },
                                    appSettings = appSettings,
                                    frame = lottieFrame
                                )

                        Presenting.ANNOUNCEMENTS ->
                            if (screenAssignment.showAnnouncements)
                                AnnouncementsPresenter(
                                    text = displayedAnnouncementText,
                                    appSettings = appSettings,
                                    outputRole = deckLinkRole,
                                    transitionAlpha = announcementTransitionAlpha,
                                    onFinished = clearAnnouncementOnFinish
                                )

                        Presenting.WEBSITE ->
                            if (screenAssignment.showWebsite) WebsitePresenter(
                                url = websiteUrl,
                                modifier = Modifier.fillMaxSize(),
                                onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                outputRole = Constants.OUTPUT_ROLE_KEY
                            )

                        Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                        Presenting.QA ->
                            if (screenAssignment.showQA) {
                                if (showQRCodeOnDisplay) {
                                    QAQRCodePresenter(
                                        url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                        qaSettings = appSettings.qaSettings,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                } else {
                                    QAPresenter(
                                        question = displayedQuestion,
                                        qaSettings = appSettings.qaSettings,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                }
                            }


                        Presenting.STT ->
                            if (screenAssignment.showSTT) {
                                STTPresenter(
                                    segments = sttManager.segments,
                                    inProgressText = sttManager.inProgressText.value,
                                    translationSegments = sttManager.translationSegments,
                                    inProgressTranslation = sttManager.inProgressTranslation.value,
                                    highlightedWords = sttManager.highlightedWords,
                                    sttSettings = appSettings.sttSettings,
                                )
                            }
                        Presenting.DICTIONARY ->
                            if (screenAssignment.showDictionary)
                                DictionaryPresenter(
                                    dictionarySettings = appSettings.dictionarySettings,
                                    entry = displayedDictionaryEntry,
                                    outputRole = deckLinkRole,
                                    transitionAlpha = 1f
                                )
                        Presenting.NONE -> { /* nothing */ }
                    }
                    }
                }
            }

            // DeckLink key output
            if (showPresenterWindow && screenAssignment.hasKeyOutput && screenAssignment.keyTargetType == "decklink" && screenAssignment.keyTargetDisplay >= 0) {
                DeckLinkComposeOutput(
                    deviceIndex = screenAssignment.keyTargetDisplay,
                    outputRole = Constants.OUTPUT_ROLE_KEY,
                    appSettings = appSettings,
                    mediaViewModel = mediaViewModel,
                    isLowerThird = screenAssignment.isLowerThird,
                ) {
                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                    val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    when (mode) {
                        Presenting.BIBLE ->
                            if (screenAssignment.showBible) {
                                BiblePresenter(
                                    selectedVerses = displayedVerses,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = bibleTransitionAlpha,
                                    crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                    bibleTranslations = screenAssignment.bibleTranslations
                                )
                            }

                        Presenting.LYRICS ->
                            if (screenAssignment.showSongs) {
                                SongPresenter(
                                    lyricSection = displayedLyricSection,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = songTransitionAlpha,
                                    displayLineIndex = songDisplayLineIndex,
                                    lookAheadEnabled = screenAssignment.songLookAhead,
                                    allLyricSections = allLyricSections,
                                    displaySectionIndex = songDisplaySectionIndex,
                                    crossfadeEnabled = appSettings.songSettings.crossfade,
                                    languageOverride = screenAssignment.songMode
                                )
                            }

                        Presenting.PICTURES ->
                            if (screenAssignment.showPictures)
                                PicturePresenter(
                                    imagePath = displayedImagePath,
                                    previousImagePath = previousDisplayedImagePath,
                                    transitionAlpha = pictureTransitionAlpha,
                                    slideOffset = pictureSlideOffset,
                                    animationType = animationType,
                                    outputRole = Constants.OUTPUT_ROLE_KEY
                                )

                        Presenting.PRESENTATION ->
                            if (screenAssignment.showPictures)
                                PresentationPresenter(
                                    frame = presentationFrame,
                                    slide = displayedSlide,
                                    previousSlide = previousDisplayedSlide,
                                    transitionAlpha = slideTransitionAlpha,
                                    slideOffset = slideSlideOffset,
                                    animationType = animationType,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    frozen = slideFrozen
                                )

                        Presenting.MEDIA ->
                            if (screenAssignment.showMedia) {
                                if (mediaViewModel.isAudioFile) {
                                    // Audio: background only
                                } else {
                                    MediaPresenter(
                                        modifier = Modifier.fillMaxSize(),
                                        audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                        transitionAlpha = mediaTransitionAlpha,
                                        outputRole = Constants.OUTPUT_ROLE_KEY
                                    )
                                }
                            }

                        Presenting.LOWER_THIRD ->
                            if (screenAssignment.showStreaming)
                                LowerThirdPresenter(
                                    composition = lottieComposition,
                                    progress = { presenterManager.lottieProgress.value },
                                    appSettings = appSettings,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    frame = lottieFrame
                                )

                        Presenting.ANNOUNCEMENTS ->
                            if (screenAssignment.showAnnouncements)
                                AnnouncementsPresenter(
                                    text = displayedAnnouncementText,
                                    appSettings = appSettings,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = announcementTransitionAlpha,
                                    onFinished = {
                                        presenterManager.setAnnouncementText("")
                                        presenterManager.setDisplayedAnnouncementText("")
                                    }
                                )

                        Presenting.WEBSITE ->
                            if (screenAssignment.showWebsite) WebsitePresenter(
                                url = websiteUrl,
                                modifier = Modifier.fillMaxSize(),
                                onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                outputRole = Constants.OUTPUT_ROLE_KEY
                            )

                        Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                        Presenting.QA ->
                            if (screenAssignment.showQA) {
                                if (showQRCodeOnDisplay) {
                                    QAQRCodePresenter(
                                        url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                        qaSettings = appSettings.qaSettings,
                                        outputRole = Constants.OUTPUT_ROLE_KEY,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                } else {
                                    QAPresenter(
                                        question = displayedQuestion,
                                        qaSettings = appSettings.qaSettings,
                                        outputRole = Constants.OUTPUT_ROLE_KEY,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                }
                            }


                        Presenting.STT ->
                            if (screenAssignment.showSTT) {
                                STTPresenter(
                                    segments = sttManager.segments,
                                    inProgressText = sttManager.inProgressText.value,
                                    translationSegments = sttManager.translationSegments,
                                    inProgressTranslation = sttManager.inProgressTranslation.value,
                                    highlightedWords = sttManager.highlightedWords,
                                    sttSettings = appSettings.sttSettings,
                                )
                            }
                        Presenting.DICTIONARY ->
                            if (screenAssignment.showDictionary)
                                DictionaryPresenter(
                                    dictionarySettings = appSettings.dictionarySettings,
                                    entry = displayedDictionaryEntry,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = 1f
                                )
                        Presenting.NONE -> { /* nothing */ }
                    }
                    }
                }
            }

            // Key output on a regular screen when primary is DeckLink
            if (showPresenterWindow && screenAssignment.hasKeyOutput && screenAssignment.keyTargetType == "screen") {
                val keyScreenIndex = findScreenIndexByBounds(
                    screens,
                    screenAssignment.keyTargetBoundsX,
                    screenAssignment.keyTargetBoundsY,
                    screenAssignment.keyTargetBoundsW,
                    screenAssignment.keyTargetBoundsH
                ) ?: screenAssignment.keyTargetDisplay
                if (keyScreenIndex in screens.indices) {
                    val keyWindowState = remember(i, keyScreenIndex) {
                        val b = screens[keyScreenIndex].defaultConfiguration.bounds
                        WindowState(
                            placement = WindowPlacement.Floating,
                            position = WindowPosition(b.x.dp, b.y.dp),
                            width = b.width.dp,
                            height = b.height.dp
                        )
                    }

                    Window(
                        visible = true,
                        title = "Key Output ${i + 1}",
                        icon = painterResource(Res.drawable.ic_app_icon),
                        onCloseRequest = { presenterManager.setShowPresenterWindow(false) },
                        state = keyWindowState,
                        undecorated = true,
                        resizable = false,
                        alwaysOnTop = true,
                    ) {
                        CompositionLocalProvider(LocalMediaViewModel provides mediaViewModel) {
                            PresenterScreen(
                                modifier = Modifier.fillMaxSize(),
                                appSettings = appSettings,
                                outputRole = Constants.OUTPUT_ROLE_KEY
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                                    val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    when (mode) {
                                        Presenting.BIBLE ->
                                            if (screenAssignment.showBible) {
                                                BiblePresenter(
                                                    selectedVerses = displayedVerses,
                                                    appSettings = appSettings,
                                                    isLowerThird = screenAssignment.isLowerThird,
                                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                                    transitionAlpha = bibleTransitionAlpha,
                                                    crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                                    bibleTranslations = screenAssignment.bibleTranslations
                                                )
                                            }

                                        Presenting.LYRICS ->
                                            if (screenAssignment.showSongs) {
                                                SongPresenter(
                                                    lyricSection = displayedLyricSection,
                                                    appSettings = appSettings,
                                                    isLowerThird = screenAssignment.isLowerThird,
                                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                                    transitionAlpha = songTransitionAlpha,
                                                    displayLineIndex = songDisplayLineIndex,
                                                    lookAheadEnabled = screenAssignment.songLookAhead,
                                                    allLyricSections = allLyricSections,
                                                    displaySectionIndex = songDisplaySectionIndex,
                                                    crossfadeEnabled = appSettings.songSettings.crossfade,
                                                    languageOverride = screenAssignment.songMode
                                                )
                                            }

                                        Presenting.PICTURES ->
                                            if (screenAssignment.showPictures)
                                                PicturePresenter(
                                                    imagePath = displayedImagePath,
                                                    previousImagePath = previousDisplayedImagePath,
                                                    transitionAlpha = pictureTransitionAlpha,
                                                    slideOffset = pictureSlideOffset,
                                                    animationType = animationType,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY
                                                )

                                        Presenting.PRESENTATION ->
                                            if (screenAssignment.showPictures)
                                                PresentationPresenter(
                                    frame = presentationFrame,
                                    slide = displayedSlide,
                                    previousSlide = previousDisplayedSlide,
                                    transitionAlpha = slideTransitionAlpha,
                                    slideOffset = slideSlideOffset,
                                    animationType = animationType,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    frozen = slideFrozen
                                )

                                        Presenting.MEDIA ->
                                            if (screenAssignment.showMedia) {
                                                if (mediaViewModel.isAudioFile) {
                                                    // Audio: background only
                                                } else {
                                                    MediaPresenter(
                                                        modifier = Modifier.fillMaxSize(),
                                                        audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                                        transitionAlpha = mediaTransitionAlpha,
                                                        outputRole = Constants.OUTPUT_ROLE_KEY
                                                    )
                                                }
                                            }

                                        Presenting.LOWER_THIRD ->
                                            if (screenAssignment.showStreaming)
                                                LowerThirdPresenter(
                                                    composition = lottieComposition,
                                                    progress = { presenterManager.lottieProgress.value },
                                                    appSettings = appSettings,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                                    frame = lottieFrame
                                                )

                                        Presenting.ANNOUNCEMENTS ->
                                            if (screenAssignment.showAnnouncements)
                                                AnnouncementsPresenter(
                                                    text = displayedAnnouncementText,
                                                    appSettings = appSettings,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                                    transitionAlpha = announcementTransitionAlpha,
                                                    onFinished = {
                                                        presenterManager.setAnnouncementText("")
                                                        presenterManager.setDisplayedAnnouncementText("")
                                                    }
                                                )

                                        Presenting.WEBSITE ->
                                            if (screenAssignment.showWebsite) WebsitePresenter(
                                                url = websiteUrl,
                                                modifier = Modifier.fillMaxSize(),
                                                onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                                onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                                onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                                audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                                outputRole = Constants.OUTPUT_ROLE_KEY
                                            )

                                        Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                                        Presenting.QA ->
                                            if (screenAssignment.showQA) {
                                                if (showQRCodeOnDisplay) {
                                                    QAQRCodePresenter(
                                                        url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                                        qaSettings = appSettings.qaSettings,
                                                        outputRole = Constants.OUTPUT_ROLE_KEY,
                                                        transitionAlpha = qaTransitionAlpha,
                                                    )
                                                } else {
                                                    QAPresenter(
                                                        question = displayedQuestion,
                                                        qaSettings = appSettings.qaSettings,
                                                        outputRole = Constants.OUTPUT_ROLE_KEY,
                                                        transitionAlpha = qaTransitionAlpha,
                                                    )
                                                }
                                            }


                                        Presenting.STT ->
                                            if (screenAssignment.showSTT) {
                                                STTPresenter(
                                                    segments = sttManager.segments,
                                                    inProgressText = sttManager.inProgressText.value,
                                                    translationSegments = sttManager.translationSegments,
                                                    inProgressTranslation = sttManager.inProgressTranslation.value,
                                                    highlightedWords = sttManager.highlightedWords,
                                                    sttSettings = appSettings.sttSettings,
                                                )
                                            }
                                        Presenting.DICTIONARY ->
                                            if (screenAssignment.showDictionary)
                                                DictionaryPresenter(
                                                    dictionarySettings = appSettings.dictionarySettings,
                                                    entry = displayedDictionaryEntry,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                                    transitionAlpha = 1f
                                                )
                                        Presenting.NONE -> { /* nothing */ }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            continue
        }

        if (screenAssignment.targetDisplay == Constants.KEY_TARGET_NONE) continue

        // Resolve target display
        val targetScreenIndex = findScreenIndexByBounds(
            screens,
            screenAssignment.targetBoundsX,
            screenAssignment.targetBoundsY,
            screenAssignment.targetBoundsW,
            screenAssignment.targetBoundsH
        ) ?: if (screenAssignment.targetDisplay >= 0 && screenAssignment.targetDisplay < screens.size) {
            screenAssignment.targetDisplay
        } else {
            availableScreens.getOrNull(i) ?: continue
        }

        // Skip if the target screen doesn't exist
        if (targetScreenIndex < 0 || targetScreenIndex >= screens.size) continue

        // Per-output background toggle
        val showBg = if (screenAssignment.isLowerThird) screenAssignment.showLowerThirdBackground else screenAssignment.showFullscreenBackground

        // Derive output role from key target configuration
        val primaryRole = screenAssignment.primaryOutputRole

        val windowState = remember(i) {
            val b = screens[targetScreenIndex].defaultConfiguration.bounds
            WindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition(b.x.dp, b.y.dp),
                width = b.width.dp,
                height = b.height.dp
            )
        }

        LaunchedEffect(targetScreenIndex) {
            val b = screens[targetScreenIndex].defaultConfiguration.bounds
            windowState.position = WindowPosition(b.x.dp, b.y.dp)
            windowState.size = DpSize(b.width.dp, b.height.dp)
        }

        // Primary window (fill or normal)
        val presenterTitle = stringResource(Res.string.presenter_view_title, i + 1)
        Window(
            visible = showPresenterWindow,
            title = presenterTitle,
            icon = painterResource(Res.drawable.ic_app_icon),
            onCloseRequest = { presenterManager.setShowPresenterWindow(false) },
            state = windowState,
            undecorated = true,
            resizable = false,
            alwaysOnTop = true,
        ) {
            presenterOutputContent(screenAssignment, effectiveMode, i + 1)
        }

        // Key output window — spawned when a key target is configured
        if (screenAssignment.hasKeyOutput && screenAssignment.keyTargetType != "decklink") {
            val keyScreenIndex = findScreenIndexByBounds(
                screens,
                screenAssignment.keyTargetBoundsX,
                screenAssignment.keyTargetBoundsY,
                screenAssignment.keyTargetBoundsW,
                screenAssignment.keyTargetBoundsH
            ) ?: screenAssignment.keyTargetDisplay
            if (keyScreenIndex in screens.indices) {
                val keyWindowState = remember(i, keyScreenIndex) {
                    val b = screens[keyScreenIndex].defaultConfiguration.bounds
                    WindowState(
                        placement = WindowPlacement.Floating,
                        position = WindowPosition(b.x.dp, b.y.dp),
                        width = b.width.dp,
                        height = b.height.dp
                    )
                }

                val keyOutputTitle = stringResource(Res.string.key_output_title, i + 1)
                Window(
                    visible = showPresenterWindow,
                    title = keyOutputTitle,
                    icon = painterResource(Res.drawable.ic_app_icon),
                    onCloseRequest = { presenterManager.setShowPresenterWindow(false) },
                    state = keyWindowState,
                    undecorated = true,
                    resizable = false,
                    alwaysOnTop = true,
                ) {
                    CompositionLocalProvider(LocalMediaViewModel provides mediaViewModel) {
                        PresenterScreen(
                            modifier = Modifier.fillMaxSize(),
                            appSettings = appSettings,
                            outputRole = Constants.OUTPUT_ROLE_KEY
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                                            mediaViewModel.pause()
                                            presenterManager.requestClearDisplay()
                                            true
                                        } else false
                                    }
                            ) {
                                var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                                val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                                if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                                Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    when (mode) {
                                    Presenting.BIBLE ->
                                        if (screenAssignment.showBible) {
                                            BiblePresenter(
                                                selectedVerses = displayedVerses,
                                                appSettings = appSettings,
                                                isLowerThird = screenAssignment.isLowerThird,
                                                isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                                outputRole = Constants.OUTPUT_ROLE_KEY,
                                                transitionAlpha = bibleTransitionAlpha,
                                                crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                                bibleTranslations = screenAssignment.bibleTranslations
                                            )
                                        }

                                    Presenting.LYRICS ->
                                        if (screenAssignment.showSongs) {
                                            SongPresenter(
                                                lyricSection = displayedLyricSection,
                                                appSettings = appSettings,
                                                isLowerThird = screenAssignment.isLowerThird,
                                                isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                                outputRole = Constants.OUTPUT_ROLE_KEY,
                                                transitionAlpha = songTransitionAlpha,
                                                displayLineIndex = songDisplayLineIndex,
                                                lookAheadEnabled = screenAssignment.songLookAhead,
                                                allLyricSections = allLyricSections,
                                                displaySectionIndex = songDisplaySectionIndex,
                                                crossfadeEnabled = appSettings.songSettings.crossfade,
                                                languageOverride = screenAssignment.songMode
                                            )
                                        }

                                    Presenting.PICTURES ->
                                        if (screenAssignment.showPictures)
                                            PicturePresenter(
                                                imagePath = displayedImagePath,
                                                previousImagePath = previousDisplayedImagePath,
                                                transitionAlpha = pictureTransitionAlpha,
                                                slideOffset = pictureSlideOffset,
                                                animationType = animationType,
                                                outputRole = Constants.OUTPUT_ROLE_KEY
                                            )

                                    Presenting.PRESENTATION ->
                                        if (screenAssignment.showPictures)
                                            PresentationPresenter(
                                                frame = presentationFrame,
                                                slide = displayedSlide,
                                                previousSlide = previousDisplayedSlide,
                                                transitionAlpha = slideTransitionAlpha,
                                                slideOffset = slideSlideOffset,
                                                animationType = animationType,
                                                outputRole = Constants.OUTPUT_ROLE_KEY
                                            )

                                    Presenting.MEDIA ->
                                        if (screenAssignment.showMedia) {
                                            if (mediaViewModel.isAudioFile) {
                                                // Audio: background only
                                            } else {
                                                MediaPresenter(
                                                    modifier = Modifier.fillMaxSize(),
                                                    audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                                    transitionAlpha = mediaTransitionAlpha,
                                                    outputRole = Constants.OUTPUT_ROLE_KEY
                                                )
                                            }
                                        }

                                    Presenting.LOWER_THIRD ->
                                        if (screenAssignment.showStreaming)
                                            LowerThirdPresenter(
                                                composition = lottieComposition,
                                                progress = { presenterManager.lottieProgress.value },
                                                appSettings = appSettings,
                                                outputRole = Constants.OUTPUT_ROLE_KEY,
                                                frame = lottieFrame
                                            )

                                    Presenting.ANNOUNCEMENTS ->
                                        if (screenAssignment.showAnnouncements)
                                            AnnouncementsPresenter(
                                                text = displayedAnnouncementText,
                                                appSettings = appSettings,
                                                outputRole = Constants.OUTPUT_ROLE_KEY,
                                                transitionAlpha = announcementTransitionAlpha,
                                                onFinished = {
                                                    presenterManager.setAnnouncementText("")
                                                    presenterManager.setDisplayedAnnouncementText("")
                                                }
                                            )

                                    Presenting.WEBSITE ->
                                        if (screenAssignment.showWebsite) WebsitePresenter(
                                            url = websiteUrl,
                                            modifier = Modifier.fillMaxSize(),
                                            onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                            onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                            onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                            audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                            outputRole = Constants.OUTPUT_ROLE_KEY
                                        )

                                    Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                                    Presenting.QA ->
                                        if (screenAssignment.showQA) {
                                            if (showQRCodeOnDisplay) {
                                                QAQRCodePresenter(
                                                    url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                                    qaSettings = appSettings.qaSettings,
                                                    transitionAlpha = qaTransitionAlpha,
                                                )
                                            } else {
                                                QAPresenter(
                                                    question = displayedQuestion,
                                                    qaSettings = appSettings.qaSettings,
                                                    transitionAlpha = qaTransitionAlpha,
                                                )
                                            }
                                        }


                                    Presenting.STT ->
                                        if (screenAssignment.showSTT) {
                                            STTPresenter(
                                                segments = sttManager.segments,
                                                inProgressText = sttManager.inProgressText.value,
                                                translationSegments = sttManager.translationSegments,
                                                inProgressTranslation = sttManager.inProgressTranslation.value,
                                                highlightedWords = sttManager.highlightedWords,
                                                sttSettings = appSettings.sttSettings,
                                            )
                                        }
                                    Presenting.DICTIONARY ->
                                        if (screenAssignment.showDictionary)
                                            DictionaryPresenter(
                                                dictionarySettings = appSettings.dictionarySettings,
                                                entry = displayedDictionaryEntry,
                                                outputRole = Constants.OUTPUT_ROLE_KEY,
                                                transitionAlpha = 1f
                                            )
                                    Presenting.NONE -> { /* nothing */
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Key output on DeckLink when primary is a regular screen
        if (screenAssignment.targetType != "decklink" && screenAssignment.hasKeyOutput && screenAssignment.keyTargetType == "decklink" && screenAssignment.keyTargetDisplay >= 0) {
            if (showPresenterWindow) {
                DeckLinkComposeOutput(
                    deviceIndex = screenAssignment.keyTargetDisplay,
                    outputRole = Constants.OUTPUT_ROLE_KEY,
                    appSettings = appSettings,
                    mediaViewModel = mediaViewModel,
                    isLowerThird = screenAssignment.isLowerThird,
                ) {
                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                    val screenCrossfadeActive = (appSettings.bibleSettings.crossfade || appSettings.songSettings.crossfade) && effectiveMode != Presenting.NONE && prevEffectiveMode != Presenting.NONE
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    when (mode) {
                        Presenting.BIBLE ->
                            if (screenAssignment.showBible) {
                                BiblePresenter(
                                    selectedVerses = displayedVerses,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = bibleTransitionAlpha,
                                    crossfadeEnabled = appSettings.bibleSettings.crossfade,
                                    bibleTranslations = screenAssignment.bibleTranslations
                                )
                            }

                        Presenting.LYRICS ->
                            if (screenAssignment.showSongs) {
                                SongPresenter(
                                    lyricSection = displayedLyricSection,
                                    appSettings = appSettings,
                                    isLowerThird = screenAssignment.isLowerThird,
                                    isLowerThirdVertical = screenAssignment.isLowerThirdVertical,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = songTransitionAlpha,
                                    displayLineIndex = songDisplayLineIndex,
                                    lookAheadEnabled = screenAssignment.songLookAhead,
                                    allLyricSections = allLyricSections,
                                    displaySectionIndex = songDisplaySectionIndex,
                                    crossfadeEnabled = appSettings.songSettings.crossfade,
                                    languageOverride = screenAssignment.songMode
                                )
                            }

                        Presenting.PICTURES ->
                            if (screenAssignment.showPictures)
                                PicturePresenter(
                                    imagePath = displayedImagePath,
                                    previousImagePath = previousDisplayedImagePath,
                                    transitionAlpha = pictureTransitionAlpha,
                                    slideOffset = pictureSlideOffset,
                                    animationType = animationType,
                                    outputRole = Constants.OUTPUT_ROLE_KEY
                                )

                        Presenting.PRESENTATION ->
                            if (screenAssignment.showPictures)
                                PresentationPresenter(
                                    frame = presentationFrame,
                                    slide = displayedSlide,
                                    previousSlide = previousDisplayedSlide,
                                    transitionAlpha = slideTransitionAlpha,
                                    slideOffset = slideSlideOffset,
                                    animationType = animationType,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    frozen = slideFrozen
                                )

                        Presenting.MEDIA ->
                            if (screenAssignment.showMedia) {
                                if (mediaViewModel.isAudioFile) {
                                    // Audio: background only
                                } else {
                                    MediaPresenter(
                                        modifier = Modifier.fillMaxSize(),
                                        audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                        transitionAlpha = mediaTransitionAlpha,
                                        outputRole = Constants.OUTPUT_ROLE_KEY
                                    )
                                }
                            }

                        Presenting.LOWER_THIRD ->
                            if (screenAssignment.showStreaming)
                                LowerThirdPresenter(
                                    composition = lottieComposition,
                                    progress = { presenterManager.lottieProgress.value },
                                    appSettings = appSettings,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    frame = lottieFrame
                                )

                        Presenting.ANNOUNCEMENTS ->
                            if (screenAssignment.showAnnouncements)
                                AnnouncementsPresenter(
                                    text = displayedAnnouncementText,
                                    appSettings = appSettings,
                                    outputRole = Constants.OUTPUT_ROLE_KEY,
                                    transitionAlpha = announcementTransitionAlpha,
                                    onFinished = {
                                        presenterManager.setAnnouncementText("")
                                        presenterManager.setDisplayedAnnouncementText("")
                                    }
                                )

                        Presenting.WEBSITE ->
                            if (screenAssignment.showWebsite) WebsitePresenter(
                                url = websiteUrl,
                                modifier = Modifier.fillMaxSize(),
                                onSnapshot = { bitmap -> presenterManager.setWebSnapshot(bitmap) },
                                onUrlChanged = { newUrl -> presenterManager.setWebsiteUrl(newUrl) },
                                onTitleChanged = { title -> presenterManager.setWebPageTitle(title) },
                                audioDeviceId = appSettings.projectionSettings.audioOutputDeviceId,
                                outputRole = Constants.OUTPUT_ROLE_KEY
                            )

                        Presenting.CANVAS -> { if (screenAssignment.showCanvas) ScenePresenter(scene = activeScene) }

                        Presenting.QA ->
                            if (screenAssignment.showQA) {
                                if (showQRCodeOnDisplay) {
                                    QAQRCodePresenter(
                                        url = "${qaDisplayUrl.ifEmpty { serverUrl }}/qa",
                                        qaSettings = appSettings.qaSettings,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                } else {
                                    QAPresenter(
                                        question = displayedQuestion,
                                        qaSettings = appSettings.qaSettings,
                                        transitionAlpha = qaTransitionAlpha,
                                    )
                                }
                            }


                        Presenting.STT ->
                            if (screenAssignment.showSTT) {
                                STTPresenter(
                                    segments = sttManager.segments,
                                    inProgressText = sttManager.inProgressText.value,
                                    translationSegments = sttManager.translationSegments,
                                    inProgressTranslation = sttManager.inProgressTranslation.value,
                                    highlightedWords = sttManager.highlightedWords,
                                    sttSettings = appSettings.sttSettings,
                                )
                            }
                        Presenting.DICTIONARY ->
                            if (screenAssignment.showDictionary)
                                DictionaryPresenter(
                                    dictionarySettings = appSettings.dictionarySettings,
                                    entry = displayedDictionaryEntry,
                                    outputRole = primaryRole,
                                    transitionAlpha = 1f
                                )
                        Presenting.NONE -> { /* nothing */ }
                    }
                    }
                }
            }
        }
    }
}
