package org.churchpresenter.app.churchpresenter

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.ic_app_icon
import churchpresenter.composeapp.generated.resources.key_output_title
import churchpresenter.composeapp.generated.resources.presenter_view_title
import churchpresenter.composeapp.generated.resources.screen_number
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.churchpresenter.app.churchpresenter.BuildConfig
import org.churchpresenter.app.churchpresenter.composables.DeckLinkManager
import org.churchpresenter.app.churchpresenter.data.Bible
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.models.AnimationType
import org.churchpresenter.app.churchpresenter.presenter.AnnouncementsPresenter
import org.churchpresenter.app.churchpresenter.presenter.BiblePresenter
import org.churchpresenter.app.churchpresenter.presenter.DeckLinkComposeOutput
import org.churchpresenter.app.churchpresenter.presenter.DictionaryPresenter
import org.churchpresenter.app.churchpresenter.presenter.LowerThirdPresenter
import org.churchpresenter.app.churchpresenter.presenter.MediaPresenter
import org.churchpresenter.app.churchpresenter.presenter.PicturePresenter
import org.churchpresenter.app.churchpresenter.presenter.PresentationPresenter
import org.churchpresenter.app.churchpresenter.presenter.Presenting
import org.churchpresenter.app.churchpresenter.presenter.QAPresenter
import org.churchpresenter.app.churchpresenter.presenter.QAQRCodePresenter
import org.churchpresenter.app.churchpresenter.presenter.STTPresenter
import org.churchpresenter.app.churchpresenter.presenter.ScenePresenter
import org.churchpresenter.app.churchpresenter.presenter.SongPresenter
import org.churchpresenter.app.churchpresenter.presenter.WebsitePresenter
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.CrashReporter
import org.churchpresenter.app.churchpresenter.utils.DevFlags
import org.churchpresenter.app.churchpresenter.utils.findScreenIndexByBounds
import org.churchpresenter.app.churchpresenter.utils.isSongLineMode
import org.churchpresenter.app.churchpresenter.viewmodel.LocalMediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.MediaViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import org.churchpresenter.app.churchpresenter.viewmodel.STTManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PresenterWindows(
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
    val lyricSection by presenterManager.lyricSection
    val lyricSectionVersion by presenterManager.lyricSectionVersion
    val selectedImagePath by presenterManager.selectedImagePath
    val selectedSlide by presenterManager.selectedSlide
    val animationType by presenterManager.animationType
    val transitionDuration by presenterManager.transitionDuration
    val announcementText by presenterManager.announcementText
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

    val proj = appSettings.projectionSettings

    val modeCrossfadeDuration = modeCrossfadeDuration(appSettings.bibleSettings, appSettings.songSettings)

    val clearRequested by presenterManager.clearDisplayRequested
    LaunchedEffect(clearRequested) {
        if (!clearRequested) return@LaunchedEffect
        val mode = presenterManager.presentingMode.value
        val modeIsLocked = isAnyScreenLockedTo(presenterManager.screenLocks.value, mode)
        val shouldFade = shouldFadeOnClear(
            mode, modeIsLocked, appSettings.bibleSettings, appSettings.songSettings,
        )
        if (shouldFade) {
            val duration = fadeOutDuration(mode, appSettings.bibleSettings, appSettings.songSettings)
            val anim = Animatable(1f)
            anim.animateTo(0f, tween(durationMillis = duration)) {
                when (mode) {
                    Presenting.BIBLE -> presenterManager.setBibleTransitionAlpha(this.value)
                    Presenting.LYRICS -> presenterManager.setSongTransitionAlpha(this.value)
                    else -> {}
                }
            }
        }
        presenterManager.setPresentingMode(Presenting.NONE)
    }

    val bibleHold by presenterManager.bibleHold
    LaunchedEffect(selectedVerses, bibleHold) {
        if (bibleHold) return@LaunchedEffect
        presenterManager.setDisplayedVerses(selectedVerses)
        presenterManager.setBibleTransitionAlpha(1f)
    }

    LaunchedEffect(lyricSection, lyricSectionVersion) {
        val ss = appSettings.songSettings
        if (lyricSection == presenterManager.displayedLyricSection.value) {
            presenterManager.setSongTransitionAlpha(1f)
            return@LaunchedEffect
        }
        val isLineMode = isSongLineMode(ss)
        if (isLineMode) {
            presenterManager.setDisplayedLyricSection(lyricSection)
            presenterManager.setSongTransitionAlpha(1f)
            return@LaunchedEffect
        }
        presenterManager.setDisplayedLyricSection(lyricSection)
        presenterManager.setSongTransitionAlpha(1f)
    }

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

    LaunchedEffect(Unit) {
        presenterManager.runPresentationClock()
    }

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

    LaunchedEffect(announcementText) {
        val annSettings = appSettings.announcementsSettings
        val isFade = isFadeAnnouncement(annSettings.animationType)
        val wasEmpty = presenterManager.displayedAnnouncementText.value.isEmpty()
        val fadeDuration = 500
        val sliderSum = 30500L // 500 + 30000, matches AnnouncementsTab speed slider
        val loopCount = annSettings.loopCount

        if (isSlidingAnnouncement(annSettings.animationType)) {
            presenterManager.setDisplayedAnnouncementText(announcementText)
            presenterManager.setAnnouncementTransitionAlpha(1f)
        } else if (announcementText.isEmpty()) {
            if (shouldFadeOutAnnouncement(isFade, wasEmpty)) {
                val anim = Animatable(1f)
                anim.animateTo(0f, tween(fadeDuration)) {
                    presenterManager.setAnnouncementTransitionAlpha(value)
                }
            }
            presenterManager.setDisplayedAnnouncementText("")
            presenterManager.setAnnouncementTransitionAlpha(1f)
        } else {
            presenterManager.setDisplayedAnnouncementText(announcementText)

            if (isFade) {
                presenterManager.setAnnouncementTransitionAlpha(0f)
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(fadeDuration)) {
                    presenterManager.setAnnouncementTransitionAlpha(value)
                }
            } else {
                presenterManager.setAnnouncementTransitionAlpha(1f)
            }

            if (isFiniteAnnouncementLoop(loopCount)) {
                delay(announcementDisplayMs(sliderSum, annSettings.animationDuration.toLong(), loopCount))

                if (isFade) {
                    val anim = Animatable(1f)
                    anim.animateTo(0f, tween(fadeDuration)) {
                        presenterManager.setAnnouncementTransitionAlpha(value)
                    }
                }
                presenterManager.setAnnouncementText("")
                presenterManager.setDisplayedAnnouncementText("")
                presenterManager.requestClearDisplay()
            }
        }
    }

    val lottieComposition by rememberLottieComposition(key = lottieJsonContent) {
        LottieCompositionSpec.JsonString(lottieJsonContent)
    }
    LaunchedEffect(lottieComposition, lottiePauseAtFrame, lottiePauseFrame, lottiePauseDurationMs, lottieTrigger) {
        try {
            val comp = lottieComposition
            val initialFrameCount = presenterManager.lottieFrameCount.value
            val totalDurMs = when {
                comp != null -> lottieCompositionDurationMs(comp.durationFrames, comp.frameRate)
                initialFrameCount != null ->
                    lottiePrerenderDurationMs(initialFrameCount, presenterManager.lottiePrerenderFps.value)
                else -> return@LaunchedEffect
            }
            val hasPause = lottieHasPause(lottiePauseAtFrame, lottiePauseFrame)
            val pauseAtMs = lottiePauseAtMs(totalDurMs, lottiePauseFrame, hasPause)
            val grandTotalMs = lottieGrandTotalMs(totalDurMs, hasPause, lottiePauseDurationMs)

            fun progressAt(elapsedMs: Long): Float = lottieProgressAt(
                elapsedMs, totalDurMs, hasPause, lottiePauseFrame, pauseAtMs, lottiePauseDurationMs,
            )

            val startNanos = withFrameNanos { it }
            var elapsedMs = 0L
            while (true) {
                val frameCount = presenterManager.lottieFrameCount.value
                val progress = progressAt(elapsedMs)
                if (frameCount != null) {
                    presenterManager.setLottieCurrentFrameIndex(lottieFrameIndexFor(progress, frameCount))
                } else {
                    presenterManager.setLottieProgress(progress)
                }
                if (elapsedMs >= grandTotalMs) break
                val nowNanos = withFrameNanos { it }
                elapsedMs = ((nowNanos - startNanos) / 1_000_000).coerceAtMost(grandTotalMs)
            }
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

    val presenterOutputContent: @Composable (screenAssignment: ScreenAssignment, effectiveMode: Presenting, screenNumber: Int?) -> Unit = { screenAssignment, effectiveMode, screenNumber ->
        PresenterOutputContent(
            screenAssignment, effectiveMode, screenNumber, presenterManager, appSettings,
            mediaViewModel, sttManager, serverUrl, qaDisplayUrl, identifyingScreen,
            lottieComposition, clearAnnouncementOnFinish,
        )
    }

    val defaultDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    val availableScreens = nonPrimaryIndices(screens.toList(), defaultDevice)

    val deckLinkDeviceCount = deckLinkOutputCount(DeckLinkManager.isAvailable()) { DeckLinkManager.listDevices().size }
    val windowCount = presenterWindowCount(availableScreens.size, deckLinkDeviceCount)
    val devWindowedFallback = isDevWindowedFallback(
        BuildConfig.IS_RELEASE, DevFlags.forceDevWindow, windowCount,
    )
    val devFallbackCount = devFallbackWindowCount(devWindowedFallback, proj.devWindowCount)
    for (i in 0 until (windowCount + devFallbackCount)) {
        if (isFallbackWindowSlot(devWindowedFallback, i, windowCount)) {
            val fallbackIndex = fallbackSlotIndex(i, windowCount)
            val screenAssignment = proj.getAssignment(fallbackIndex)
            val effectiveMode = effectiveOutputMode(screenLocks, fallbackIndex, presentingMode)
            val fallbackWindowState = remember(fallbackIndex) {
                WindowState(
                    width = 960.dp,
                    height = 540.dp,
                    position = WindowPosition(
                        x = devFallbackWindowOffsetDp(fallbackIndex).dp,
                        y = devFallbackWindowOffsetDp(fallbackIndex).dp,
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
        val effectiveMode = effectiveOutputMode(screenLocks, i, presentingMode)

        if (isDeckLinkPrimaryOutput(screenAssignment)) {
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
                    val screenCrossfadeActive = isScreenCrossfadeActive(
                        appSettings.bibleSettings, appSettings.songSettings, effectiveMode, prevEffectiveMode,
                    )
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(
                        targetState = effectiveMode,
                        animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()
                    ) { mode ->
                    PresenterModeContent(
                        mode = mode,
                        screenAssignment = screenAssignment,
                        presenterManager = presenterManager,
                        appSettings = appSettings,
                        mediaViewModel = mediaViewModel,
                        sttManager = sttManager,
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        lottieComposition = lottieComposition,
                        clearAnnouncementOnFinish = clearAnnouncementOnFinish,
                        outputRole = deckLinkRole,
                        showBg = showsOutputBackground(screenAssignment),
                        showBackgroundOverride = true,
                    )
                    }
                }
            }

            if (showPresenterWindow && hasDeckLinkKeyOutput(screenAssignment)) {
                DeckLinkComposeOutput(
                    deviceIndex = screenAssignment.keyTargetDisplay,
                    outputRole = Constants.OUTPUT_ROLE_KEY,
                    appSettings = appSettings,
                    mediaViewModel = mediaViewModel,
                    isLowerThird = screenAssignment.isLowerThird,
                ) {
                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                    val screenCrossfadeActive = isScreenCrossfadeActive(
                        appSettings.bibleSettings, appSettings.songSettings, effectiveMode, prevEffectiveMode,
                    )
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    PresenterModeContent(
                        mode = mode,
                        screenAssignment = screenAssignment,
                        presenterManager = presenterManager,
                        appSettings = appSettings,
                        mediaViewModel = mediaViewModel,
                        sttManager = sttManager,
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        lottieComposition = lottieComposition,
                        clearAnnouncementOnFinish = clearAnnouncementOnFinish,
                        outputRole = Constants.OUTPUT_ROLE_KEY,
                        showBg = showsOutputBackground(screenAssignment),
                        showBackgroundOverride = true,
                    )
                    }
                }
            }

            if (showPresenterWindow && hasScreenKeyOutput(screenAssignment)) {
                val keyScreenIndex = keyOutputScreenIndex(
                    findScreenIndexByBounds(
                        screens,
                        screenAssignment.keyTargetBoundsX,
                        screenAssignment.keyTargetBoundsY,
                        screenAssignment.keyTargetBoundsW,
                        screenAssignment.keyTargetBoundsH
                    ),
                    screenAssignment.keyTargetDisplay,
                )
                if (isScreenIndexValid(keyScreenIndex, screens.size)) {
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
                                    val screenCrossfadeActive = isScreenCrossfadeActive(
                                        appSettings.bibleSettings, appSettings.songSettings,
                                        effectiveMode, prevEffectiveMode,
                                    )
                                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    PresenterModeContent(
                        mode = mode,
                        screenAssignment = screenAssignment,
                        presenterManager = presenterManager,
                        appSettings = appSettings,
                        mediaViewModel = mediaViewModel,
                        sttManager = sttManager,
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        lottieComposition = lottieComposition,
                        clearAnnouncementOnFinish = clearAnnouncementOnFinish,
                        outputRole = Constants.OUTPUT_ROLE_KEY,
                        showBg = showsOutputBackground(screenAssignment),
                        showBackgroundOverride = true,
                    )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            continue
        }

        if (hasNoPrimaryTarget(screenAssignment)) continue

        val targetScreenIndex = primaryOutputScreenIndex(
            matchedByBounds = findScreenIndexByBounds(
                screens,
                screenAssignment.targetBoundsX,
                screenAssignment.targetBoundsY,
                screenAssignment.targetBoundsW,
                screenAssignment.targetBoundsH
            ),
            savedDisplay = screenAssignment.targetDisplay,
            screenCount = screens.size,
            positionalFallback = availableScreens.getOrNull(i),
        ) ?: continue

        if (!isScreenIndexValid(targetScreenIndex, screens.size)) continue

        val showBg = showsOutputBackground(screenAssignment)

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

        if (screenAssignment.hasKeyOutput && !isDeckLinkKeyOutput(screenAssignment)) {
            val keyScreenIndex = keyOutputScreenIndex(
                findScreenIndexByBounds(
                    screens,
                    screenAssignment.keyTargetBoundsX,
                    screenAssignment.keyTargetBoundsY,
                    screenAssignment.keyTargetBoundsW,
                    screenAssignment.keyTargetBoundsH
                ),
                screenAssignment.keyTargetDisplay,
            )
            if (isScreenIndexValid(keyScreenIndex, screens.size)) {
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
                                val screenCrossfadeActive = isScreenCrossfadeActive(
                                    appSettings.bibleSettings, appSettings.songSettings,
                                    effectiveMode, prevEffectiveMode,
                                )
                                if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                                Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    PresenterModeContent(
                        mode = mode,
                        screenAssignment = screenAssignment,
                        presenterManager = presenterManager,
                        appSettings = appSettings,
                        mediaViewModel = mediaViewModel,
                        sttManager = sttManager,
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        lottieComposition = lottieComposition,
                        clearAnnouncementOnFinish = clearAnnouncementOnFinish,
                        outputRole = Constants.OUTPUT_ROLE_KEY,
                        showBg = showBg,
                        showBackgroundOverride = true,
                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isDeckLinkPrimaryOutput(screenAssignment) && hasDeckLinkKeyOutput(screenAssignment)) {
            if (showPresenterWindow) {
                DeckLinkComposeOutput(
                    deviceIndex = screenAssignment.keyTargetDisplay,
                    outputRole = Constants.OUTPUT_ROLE_KEY,
                    appSettings = appSettings,
                    mediaViewModel = mediaViewModel,
                    isLowerThird = screenAssignment.isLowerThird,
                ) {
                    var prevEffectiveMode by remember { mutableStateOf(effectiveMode) }
                    val screenCrossfadeActive = isScreenCrossfadeActive(
                        appSettings.bibleSettings, appSettings.songSettings, effectiveMode, prevEffectiveMode,
                    )
                    if (effectiveMode != prevEffectiveMode) prevEffectiveMode = effectiveMode
                    Crossfade(targetState = effectiveMode, animationSpec = if (screenCrossfadeActive) tween(modeCrossfadeDuration) else snap()) { mode ->
                    PresenterModeContent(
                        mode = mode,
                        screenAssignment = screenAssignment,
                        presenterManager = presenterManager,
                        appSettings = appSettings,
                        mediaViewModel = mediaViewModel,
                        sttManager = sttManager,
                        serverUrl = serverUrl,
                        qaDisplayUrl = qaDisplayUrl,
                        lottieComposition = lottieComposition,
                        clearAnnouncementOnFinish = clearAnnouncementOnFinish,
                        outputRole = primaryRole,
                        showBg = showBg,
                        showBackgroundOverride = true,
                    )
                    }
                }
            }
        }
    }
}
