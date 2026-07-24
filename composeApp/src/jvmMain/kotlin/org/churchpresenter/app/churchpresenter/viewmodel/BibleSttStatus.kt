package org.churchpresenter.app.churchpresenter.viewmodel

/**
 * The speech-to-text status shown on the Bible tab, as a value rather than a string. The rule is a
 * priority ladder over ~10 connection booleans (engine start, engine link, the engine's own upstream
 * STT socket, the app's own STT connection, whether transcript is arriving, reconnect/connect state).
 * It was an inline `when` in BibleTab; extracting it lets the ordering — which case wins when several
 * conditions hold at once — be tested apart from the Compose string lookup that renders it.
 */
internal enum class BibleSttStatus {
    ENGINE_UNAVAILABLE,
    NO_BIBLE,
    ENGINE_CONNECTING,
    ENGINE_STT_DOWN,
    WAITING_FOR_STT,
    LISTENING,
    RECONNECTING,
    UNREACHABLE,
    CONNECTING,
    NOT_CONNECTED,
}

/**
 * The winning [BibleSttStatus] for the current connection state. Order matters: a hard problem
 * (engine failed, no bible, engine reachable but its STT socket down) outranks the app's own
 * "listening", which outranks the reconnect/connect/idle states.
 */
internal fun bibleSttStatus(
    engineStartFailed: Boolean,
    noBibleSelected: Boolean,
    sttConnected: Boolean,
    engineConnected: Boolean,
    engineSttDown: Boolean,
    sttReceiving: Boolean,
    hasDetectedReferences: Boolean,
    sttReconnecting: Boolean,
    sttConnectError: Boolean,
    sttConnecting: Boolean,
): BibleSttStatus = when {
    engineStartFailed -> BibleSttStatus.ENGINE_UNAVAILABLE
    noBibleSelected -> BibleSttStatus.NO_BIBLE
    sttConnected && !engineConnected -> BibleSttStatus.ENGINE_CONNECTING
    engineConnected && engineSttDown -> BibleSttStatus.ENGINE_STT_DOWN
    sttConnected && !sttReceiving && !hasDetectedReferences -> BibleSttStatus.WAITING_FOR_STT
    sttConnected -> BibleSttStatus.LISTENING
    sttReconnecting -> BibleSttStatus.RECONNECTING
    sttConnectError -> BibleSttStatus.UNREACHABLE
    sttConnecting -> BibleSttStatus.CONNECTING
    else -> BibleSttStatus.NOT_CONNECTED
}
