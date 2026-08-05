package org.churchpresenter.app.churchpresenter

import org.churchpresenter.app.churchpresenter.data.settings.CompanionSatelliteSettings
import org.churchpresenter.app.churchpresenter.data.settings.InstanceLinkSettings
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundSettings
import org.churchpresenter.app.churchpresenter.data.settings.OBSSettings
import org.churchpresenter.app.churchpresenter.utils.UpdateCheckResult
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.data.settings.ServerSettings
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.server.TunnelStatus
import org.churchpresenter.app.churchpresenter.data.Language

/**
 * The decisions `main.kt` makes at startup, held apart from the entry point that makes them.
 *
 * Everything here is pure: no windows, no sockets, no Compose. What is left in `main.kt` opens real
 * AWT windows and binds real ports, which a headless test cannot do — so the rules those paths obey
 * live here instead, where they can be stated and checked directly.
 */

/**
 * Whether skiko should be pinned to Metal.
 *
 * Only on macOS, where leaving skiko to choose falls back to OpenGL and crashes on some machines.
 * Matched on the name rather than a platform enum because that is what the property carries.
 */
internal fun shouldForceMetalRenderer(osName: String): Boolean =
    osName.lowercase().contains("mac")

/** The port the single-instance lock binds, honouring the override a second dev instance sets. */
internal fun singleInstanceLockPort(override: String?, default: Int): Int =
    override?.toIntOrNull() ?: default

/**
 * The language to start in: the saved one when it is still a language this build has, English when
 * it is not — a settings file naming a language since removed must not stop the app starting.
 */
internal fun resolveStartupLanguage(savedCode: String): Language =
    Language.entries.find { it.code == savedCode } ?: Language.ENGLISH

/** How many DeckLink outputs are available, which is none at all when the driver is not present. */
internal fun deckLinkOutputCount(available: Boolean, deviceCount: () -> Int): Int =
    if (available) deviceCount() else 0

/**
 * Whether a Companion connection should be brought up on this pass.
 *
 * Three separate reasons, and the middle one is the only one that is a setting: something is
 * already live for it, the operator asked for it to auto-connect, or they have just edited it —
 * an edit is an explicit action, so it connects even with auto-connect off. A connection merely
 * seen for the first time at startup does none of these, which keeps startup opt-in.
 */
internal fun shouldConnectCompanion(
    hasLiveSlot: Boolean,
    autoConnect: Boolean,
    lastSeen: CompanionSatelliteSettings?,
    current: CompanionSatelliteSettings,
): Boolean = hasLiveSlot || autoConnect || (lastSeen != null && lastSeen != current)

/** Whether a connection needs a device id minted before it can be used — Companion rejects a blank one. */
internal fun needsGeneratedDeviceId(connection: CompanionSatelliteSettings): Boolean =
    connection.deviceId.isBlank()

/** [connections] with [id]'s device id replaced, leaving every other connection alone. */
internal fun withGeneratedDeviceId(
    connections: List<CompanionSatelliteSettings>,
    id: String,
    deviceId: String,
): List<CompanionSatelliteSettings> =
    connections.map { if (it.id == id) it.copy(deviceId = deviceId) else it }

/** Whether the instance link should dial out on its own: switched on, set to, and actually addressed. */
internal fun shouldAutoConnectInstanceLink(link: InstanceLinkSettings): Boolean =
    link.enabled && link.autoConnect && link.primaryHost.isNotBlank() && link.primaryPort > 0

/**
 * Whether a live link should be dropped. Switching the link off has to disconnect it now rather
 * than leave it running until the next launch.
 */
internal fun shouldDisconnectInstanceLink(link: InstanceLinkSettings): Boolean = !link.enabled

/** Whether the operator's connect/disconnect intent is a change worth persisting. */
internal fun instanceLinkEnabledChanged(current: InstanceLinkSettings, enabled: Boolean): Boolean =
    current.enabled != enabled

/**
 * The key remote callers must present, which is none at all when the operator has not switched key
 * checking on.
 *
 * Read in two places — the Q&A admin panel and the presentation remote — which is why it is one
 * function rather than the same expression written twice: two copies can disagree about whether a
 * key is required, and only one of the two screens would then refuse callers.
 */
internal fun activeApiKey(settings: ServerSettings): String =
    if (settings.apiKeyEnabled) settings.apiKey else ""

/** Whether the tunnel is up. */
internal fun isTunnelConnected(status: TunnelStatus): Boolean = status is TunnelStatus.Connected

/**
 * Whether the tunnel has just gone down, as opposed to being down all along.
 *
 * Edge-triggered on purpose: the URLs handed out over the tunnel are only worth clearing at the
 * moment it drops. Treating "is not connected" as the signal would clear them repeatedly, including
 * before one was ever established.
 */
internal fun tunnelJustDropped(previouslyConnected: Boolean, isConnected: Boolean): Boolean =
    previouslyConnected && !isConnected

/** The browser-source output configured at [index], or an unconfigured one when there is none. */
internal fun browserSourceOutputAt(outputs: List<ScreenAssignment>, index: Int): ScreenAssignment =
    outputs.getOrNull(index) ?: ScreenAssignment()

/** Whether media is what is on the output, which is what connected phones are told. */
internal fun isMediaLive(presentingMode: Presenting): Boolean = presentingMode == Presenting.MEDIA

/**
 * The song position to announce, or none when songs are not what is live.
 *
 * A follower given a position while something else is live would jump to a section of a song nobody
 * is singing, so the mode is checked before the index is read rather than after.
 */
internal fun livePositionOrNull(source: Presenting, forMode: Presenting, index: Int): Int? =
    if (source == forMode) index else null

/** A field worth sending, or nothing — an empty string and "not set" mean the same thing on the wire. */
internal fun nullIfEmpty(value: String): String? = value.ifEmpty { null }

/**
 * The canonical verse reference to announce, or none.
 *
 * Only for scripture, and only once the verse names a book the loaded bible actually knows: the code
 * is resolved through that bible, so a verse from a translation that is no longer loaded — or a
 * partially-filled verse mid-selection — has no code rather than a wrong one.
 */
internal fun <T> liveVerseCode(
    source: Presenting,
    bookName: String,
    chapter: Int,
    verseNumber: Int,
    bookIdByName: (String) -> Int?,
    codeReference: (bookId: Int, chapter: Int, verse: Int) -> T?,
): T? {
    if (source != Presenting.BIBLE) return null
    if (bookName.isEmpty()) return null
    val bookId = bookIdByName(bookName) ?: return null
    return codeReference(bookId, chapter, verseNumber)
}

/** Whether OBS should be connected to, as opposed to disconnected from. */
internal fun shouldConnectObs(settings: OBSSettings): Boolean = settings.enabled

/**
 * Whether a slide announced by the primary is one that can actually be fetched.
 *
 * The snapshot sent on connect always carries this event, with an empty id when the primary has no
 * presentation open — asking for that slide's bytes would be a request for nothing, answered with a
 * 404 and logged as a failed mirror.
 */
internal fun hasFetchableSlide(slideId: String): Boolean = slideId.isNotBlank()

/**
 * Whether the mirrored background cache has to be emptied before fetching.
 *
 * The per-file check is "does this exist locally", so a background the primary has replaced under
 * the same name would otherwise never be re-downloaded — the stale copy would satisfy the check
 * forever.
 */
internal fun shouldInvalidateBackgroundCache(backgroundsUpdatedSignal: Int): Boolean =
    backgroundsUpdatedSignal > 0

/**
 * The settings the output should actually render with.
 *
 * Only the rendering paths see the mirrored backgrounds; editing and persistence keep using this
 * instance's own, so a follower never saves the primary's backgrounds over its own configuration.
 */
internal fun withMirroredBackgrounds(
    settings: AppSettings,
    mirrored: BackgroundSettings?,
): AppSettings = if (mirrored == null) settings else settings.copy(backgroundSettings = mirrored)

/** Whether this is the first update check this install has ever run. */
internal fun isFirstEverUpdateCheck(lastCheckTimestamp: Long): Boolean = lastCheckTimestamp == 0L

/**
 * Whether an update check's outcome should be put in front of the operator.
 *
 * The first check ever is shown whatever it found — that is the one chance to ask how often they
 * want checking done. Every check after it only interrupts when there is actually an update, so a
 * routine "you are up to date" never appears unasked.
 */
internal fun shouldShowUpdateResult(firstEverCheck: Boolean, result: UpdateCheckResult): Boolean =
    firstEverCheck || result is UpdateCheckResult.Available

/**
 * Whether the window should be restored to the position and size it was left at.
 *
 * Only a floating window has its own geometry worth restoring, and only once it has actually been
 * saved: a negative coordinate is the "never saved" value, and restoring to it would put the window
 * off-screen.
 */
internal fun shouldRestoreWindowGeometry(isFloating: Boolean, savedX: Int): Boolean =
    isFloating && savedX >= 0

/** Whether media is what the presentation-live flag should report. */
internal fun isPresentationLive(presentingMode: Presenting): Boolean =
    presentingMode == Presenting.PRESENTATION
