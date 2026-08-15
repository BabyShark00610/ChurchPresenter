package org.churchpresenter.app.churchpresenter.presenter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import kotlin.math.min
import org.churchpresenter.app.churchpresenter.composables.LoopingVideoBackground
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.withSongAppearance

import org.churchpresenter.app.churchpresenter.models.LyricSection
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.calculateAutoFitForAllSections
import org.churchpresenter.app.churchpresenter.utils.songLineGroup
import org.churchpresenter.app.churchpresenter.utils.songLineGroupStart
import org.churchpresenter.app.churchpresenter.utils.songLineGroups
import org.churchpresenter.app.churchpresenter.utils.songLinesPerSlide
import org.churchpresenter.app.churchpresenter.utils.Utils.parseHexColor
import org.churchpresenter.app.churchpresenter.utils.Utils.systemFontFamilyOrDefault
import org.jetbrains.skia.Image
import java.io.File

private const val SHADOW_OFFSET_PX = 6f
private const val INDICATOR_REPEAT_COUNT = 3

@Composable
fun SongPresenter(
    modifier: Modifier = Modifier,
    lyricSection: LyricSection,
    appSettings: AppSettings,
    isLowerThird: Boolean = false,
    // Only changes the band's geometry (a right-anchored vertical strip instead of a bottom
    // horizontal band) — isLowerThird alone still selects all the *LowerThird* styling fields
    // for both orientations, so there's one style profile to maintain.
    isLowerThirdVertical: Boolean = false,
    outputRole: String = Constants.OUTPUT_ROLE_NORMAL,
    transitionAlpha: Float = 1f,
    displayLineIndex: Int = -1,
    lookAheadEnabled: Boolean = false,
    allLyricSections: List<LyricSection> = emptyList(),
    displaySectionIndex: Int = -1,
    showBackground: Boolean = true,
    crossfadeEnabled: Boolean = false,
    languageOverride: String = "",
) {
    // When languageOverride is set by the per-screen songMode, use it instead of the global setting.
    val isKey = outputRole == Constants.OUTPUT_ROLE_KEY
    // The song's own look folded over the shared one, ONCE, at the single binding everything else
    // reads through — see `withSongAppearance`. `appSettings.songSettings` must not be read directly
    // below this line or the override silently does not apply there.
    val ss = appSettings.songSettings
        .withSongAppearance(appSettings.songAppearanceFor(lyricSection.songId), isLowerThird)
    val effectiveLangDisplay = if (languageOverride.isNotBlank()) languageOverride else {
        if (lookAheadEnabled) {
            if (isLowerThird) ss.lowerThirdLookAheadLanguageDisplay else ss.lookAheadLanguageDisplay
        } else {
            if (isLowerThird) ss.lowerThirdLanguageDisplay else ss.fullscreenLanguageDisplay
        }
    }

    // Resolve font families per fullscreen / lower third
    val titleFontFamily = remember(ss.titleFontType, ss.titleLowerThirdFontType, isLowerThird) {
        systemFontFamilyOrDefault(if (isLowerThird) ss.titleLowerThirdFontType else ss.titleFontType)
    }
    val lyricsFontFamily = remember(ss.lyricsFontType, ss.lyricsLowerThirdFontType,
        ss.lookAheadFontType, ss.lowerThirdLookAheadFontType, isLowerThird, lookAheadEnabled) {
        if (lookAheadEnabled) {
            systemFontFamilyOrDefault(if (isLowerThird) ss.lowerThirdLookAheadFontType else ss.lookAheadFontType)
        } else {
            systemFontFamilyOrDefault(if (isLowerThird) ss.lyricsLowerThirdFontType else ss.lyricsFontType)
        }
    }

    // Resolve colors — key mode forces white for a proper key signal
    val titleColor = remember(ss.titleColor, ss.titleLowerThirdColor, isLowerThird, isKey) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) ss.titleLowerThirdColor else ss.titleColor)
    }
    val lyricsColor = remember(ss.lyricsColor, ss.lyricsLowerThirdColor,
        ss.lookAheadColor, ss.lowerThirdLookAheadColor, isLowerThird, lookAheadEnabled, isKey) {
        if (isKey) Color.White
        else if (lookAheadEnabled) {
            parseHexColor(if (isLowerThird) ss.lowerThirdLookAheadColor else ss.lookAheadColor)
        } else {
            parseHexColor(if (isLowerThird) ss.lyricsLowerThirdColor else ss.lyricsColor)
        }
    }
    // Look-ahead next section preview font settings (resolved per fullscreen / lower third)
    val laColor = remember(ss.lookAheadNextColor, ss.lowerThirdLookAheadNextColor, isLowerThird, isKey) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) ss.lowerThirdLookAheadNextColor else ss.lookAheadNextColor)
    }
    val laFontFamily = remember(ss.lookAheadNextFontType, ss.lowerThirdLookAheadNextFontType, isLowerThird) {
        systemFontFamilyOrDefault(if (isLowerThird) ss.lowerThirdLookAheadNextFontType else ss.lookAheadNextFontType)
    }
    val laFontSize = if (isLowerThird) ss.lowerThirdLookAheadNextFontSize else ss.lookAheadNextFontSize
    val laBold = if (isLowerThird) ss.lowerThirdLookAheadNextBold else ss.lookAheadNextBold
    val laItalic = if (isLowerThird) ss.lowerThirdLookAheadNextItalic else ss.lookAheadNextItalic
    val laUnderline = if (isLowerThird) ss.lowerThirdLookAheadNextUnderline else ss.lookAheadNextUnderline
    val laShadowEnabled = if (isLowerThird) ss.lowerThirdLookAheadNextShadow else ss.lookAheadNextShadow
    val laShadowColor = parseHexColor(if (isLowerThird) ss.lowerThirdLookAheadNextShadowColor else ss.lookAheadNextShadowColor)
    val laShadowSizeMul = (if (isLowerThird) ss.lowerThirdLookAheadNextShadowSize else ss.lookAheadNextShadowSize) / 100f
    val laShadowAlpha = ((if (isLowerThird) ss.lowerThirdLookAheadNextShadowOpacity else ss.lookAheadNextShadowOpacity) / 100f).coerceIn(0f, 1f)

    // Per-element shadow customization (resolved per fullscreen / lower third)
    fun makeSongShadow(color: String, size: Int, opacity: Int, alphaScale: Float = 0.78f): Shadow {
        val base = parseHexColor(color)
        val mul = size / 100f
        val alpha = (opacity / 100f).coerceIn(0f, 1f)
        return Shadow(
            color = base.copy(alpha = alpha * alphaScale),
            offset = Offset(2f * mul, 2f * mul),
            blurRadius = 4f * mul
        )
    }
    val titleBaseShadow = makeSongShadow(
        if (isLowerThird) ss.titleLowerThirdShadowColor else ss.titleShadowColor,
        if (isLowerThird) ss.titleLowerThirdShadowSize else ss.titleShadowSize,
        if (isLowerThird) ss.titleLowerThirdShadowOpacity else ss.titleShadowOpacity
    )
    val lyricsBaseShadow = makeSongShadow(
        if (isLowerThird) ss.lyricsLowerThirdShadowColor else ss.lyricsShadowColor,
        if (isLowerThird) ss.lyricsLowerThirdShadowSize else ss.lyricsShadowSize,
        if (isLowerThird) ss.lyricsLowerThirdShadowOpacity else ss.lyricsShadowOpacity
    )

    // Text styles derived from settings (resolved per fullscreen / lower third)
    val effectiveTitleBold = if (isLowerThird) ss.titleLowerThirdBold else ss.titleBold
    val effectiveTitleItalic = if (isLowerThird) ss.titleLowerThirdItalic else ss.titleItalic
    val effectiveTitleUnderline = if (isLowerThird) ss.titleLowerThirdUnderline else ss.titleUnderline
    val effectiveTitleShadow = if (isLowerThird) ss.titleLowerThirdShadow else ss.titleShadow
    val titleTextStyle = TextStyle(
        fontWeight = if (effectiveTitleBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (effectiveTitleItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (effectiveTitleUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (effectiveTitleShadow) titleBaseShadow else null
    )
    val effectiveLyricsBold = if (lookAheadEnabled) {
        if (isLowerThird) ss.lowerThirdLookAheadBold else ss.lookAheadBold
    } else if (isLowerThird) ss.lyricsLowerThirdBold else ss.lyricsBold
    val effectiveLyricsItalic = if (lookAheadEnabled) {
        if (isLowerThird) ss.lowerThirdLookAheadItalic else ss.lookAheadItalic
    } else if (isLowerThird) ss.lyricsLowerThirdItalic else ss.lyricsItalic
    val effectiveLyricsUnderline = if (lookAheadEnabled) {
        if (isLowerThird) ss.lowerThirdLookAheadUnderline else ss.lookAheadUnderline
    } else if (isLowerThird) ss.lyricsLowerThirdUnderline else ss.lyricsUnderline
    val effectiveLyricsShadow = if (lookAheadEnabled) {
        if (isLowerThird) ss.lowerThirdLookAheadShadow else ss.lookAheadShadow
    } else if (isLowerThird) ss.lyricsLowerThirdShadow else ss.lyricsShadow
    val lyricsTextStyle = TextStyle(
        fontWeight = if (effectiveLyricsBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (effectiveLyricsItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (effectiveLyricsUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (effectiveLyricsShadow) lyricsBaseShadow else null
    )
    val contentAlignment = when (ss.lyricsAlignment) {
        Constants.TOP -> Alignment.TopCenter
        Constants.BOTTOM -> Alignment.BottomCenter
        else -> Alignment.Center
    }
    val lyricsHorizontalAlignment = getTextAlign(
        if (lookAheadEnabled) {
            if (isLowerThird) ss.lowerThirdLookAheadHorizontalAlignment else ss.lookAheadHorizontalAlignment
        } else {
            if (isLowerThird) ss.lyricsLowerThirdHorizontalAlignment else ss.lyricsHorizontalAlignment
        }
    )
    val titleHorizontalAlignment = getTextAlign(
        if (isLowerThird) ss.titleLowerThirdHorizontalAlignment else ss.titleHorizontalAlignment
    )
    val songNumberHorizontalAlignment = getTextAlign(
        if (isLowerThird) ss.songNumberLowerThirdHorizontalAlignment else ss.songNumberHorizontalAlignment
    )
    // Three levels now, innermost first: the song's own background, then the shared song background
    // for this surface, then — when that one is left on "Default" — the global default resolved
    // below. A song without its own entry behaves exactly as it did before.
    //
    // The song's background covers the lower third too: it is one setting on one song, and having it
    // silently not apply to whichever surface happens to be live would be the more surprising rule.
    val bgConfig = appSettings.songBackgroundFor(lyricSection.songId)
        ?: if (isLowerThird) appSettings.backgroundSettings.songLowerThirdBackground
        else appSettings.backgroundSettings.songBackground

    // Resolve effective background type/paths (handle Default → inherit from global)
    // For fill/key output: force black background, skip images/videos
    val effectiveType: String
    val effectiveImagePath: String
    val effectiveVideoPath: String
    var backgroundColor: Color
    val effectiveOpacity: Float

    if (!showBackground) {
        // Browser Source scenes blank to transparent (OBS keying); projector windows to black
        effectiveType = if (LocalTransparentBlanking.current) Constants.BACKGROUND_TRANSPARENT
        else Constants.BACKGROUND_COLOR
        effectiveImagePath = ""
        effectiveVideoPath = ""
        backgroundColor = Color.Black
        effectiveOpacity = 1.0f
    } else if (bgConfig.backgroundType == Constants.BACKGROUND_DEFAULT) {
        val defaults = appSettings.backgroundSettings
        if (isLowerThird) {
            effectiveType = defaults.defaultLowerThirdBackgroundType
            effectiveImagePath = defaults.defaultLowerThirdBackgroundImage
            effectiveVideoPath = defaults.defaultLowerThirdBackgroundVideo
            backgroundColor = parseHexColor(defaults.defaultLowerThirdBackgroundColor)
            effectiveOpacity = defaults.defaultLowerThirdBackgroundOpacity
        } else {
            effectiveType = defaults.defaultBackgroundType
            effectiveImagePath = defaults.defaultBackgroundImage
            effectiveVideoPath = defaults.defaultBackgroundVideo
            backgroundColor = parseHexColor(defaults.defaultBackgroundColor)
            effectiveOpacity = defaults.defaultBackgroundOpacity
        }
    } else {
        effectiveType = bgConfig.backgroundType
        effectiveImagePath = bgConfig.backgroundImage
        effectiveVideoPath = bgConfig.backgroundVideo
        backgroundColor = parseHexColor(bgConfig.backgroundColor)
        effectiveOpacity = bgConfig.backgroundOpacity
    }

    val backgroundImageBitmap = remember(effectiveType, effectiveImagePath, isLowerThird) {
        if (effectiveType == Constants.BACKGROUND_IMAGE && effectiveImagePath.isNotEmpty()) {
            try {
                val file = File(effectiveImagePath)
                if (file.exists()) Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val useVideoBackground = effectiveType == Constants.BACKGROUND_VIDEO && effectiveVideoPath.isNotEmpty()

    val bgModifier: Modifier = when {
        effectiveType == Constants.BACKGROUND_TRANSPARENT -> Modifier
        effectiveType == Constants.BACKGROUND_GRADIENT -> Modifier
        useVideoBackground -> Modifier.background(Color.Black)
        effectiveType == Constants.BACKGROUND_IMAGE && backgroundImageBitmap != null ->
            Modifier.alpha(effectiveOpacity).paint(painter = BitmapPainter(backgroundImageBitmap), contentScale = ContentScale.Crop)

        effectiveType == Constants.BACKGROUND_IMAGE ->
            Modifier.background(Color.Black)

        else ->
            Modifier.background(backgroundColor.copy(alpha = effectiveOpacity))
    }

    // Fade-in on first appearance (covers background + text)
    val fadeInDuration = ss.transitionDuration.toInt().coerceAtLeast(100)
    var enterAlpha by remember { mutableStateOf(if (ss.fadeIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (ss.fadeIn && enterAlpha < 1f) {
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(durationMillis = fadeInDuration)) {
                enterAlpha = this.value
            }
            enterAlpha = 1f
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = transitionAlpha * enterAlpha }
            .then(if (!isLowerThird) bgModifier else Modifier)
    ) {
        if (useVideoBackground && !isLowerThird) {
            LoopingVideoBackground(
                videoPath = effectiveVideoPath,
                modifier = Modifier.fillMaxSize().alpha(effectiveOpacity)
            )
        }
        // Measured in dp against the 1920x1080 reference space, NOT in pixels.
        //
        // The factor is applied to `.sp` and `.dp` below, which are density-independent, so deriving
        // it from `toPx()` made it wrong by exactly the display's density: on a monitor at 150%
        // Windows scaling a 1920px-wide output reports 1920px but only 1280dp, the old form gave a
        // scale of 1.0, and every glyph and margin came out half again too large. Auto-fit — which
        // measures at Density(1f) and guarantees a line fits 1728 reference units — was then
        // overruled at draw time, and long lines spilled past the edge or broke mid-word.
        // StageMonitorScreen already measures in `.value` for the same reason.
        val widthScale = maxWidth.value / 1920f
        val heightScale = maxHeight.value / 1080f
        val scaleFactor = min(widthScale, heightScale).coerceIn(0.5f, 3.0f)

        // Scale shadow to be visible at projection resolution
        fun scaleElementShadow(color: String, size: Int, opacity: Int): Shadow {
            val base = parseHexColor(color)
            val mul = size / 100f
            val alpha = (opacity / 100f).coerceIn(0f, 1f)
            return Shadow(
                color = base.copy(alpha = alpha),
                offset = Offset(SHADOW_OFFSET_PX * scaleFactor * mul, SHADOW_OFFSET_PX * scaleFactor * mul),
                blurRadius = 12f * scaleFactor * mul
            )
        }
        val titleTextStyleScaled = if (effectiveTitleShadow)
            titleTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) ss.titleLowerThirdShadowColor else ss.titleShadowColor,
                if (isLowerThird) ss.titleLowerThirdShadowSize else ss.titleShadowSize,
                if (isLowerThird) ss.titleLowerThirdShadowOpacity else ss.titleShadowOpacity
            )) else titleTextStyle
        val lyricsTextStyleScaled = if (effectiveLyricsShadow)
            lyricsTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) ss.lyricsLowerThirdShadowColor else ss.lyricsShadowColor,
                if (isLowerThird) ss.lyricsLowerThirdShadowSize else ss.lyricsShadowSize,
                if (isLowerThird) ss.lyricsLowerThirdShadowOpacity else ss.lyricsShadowOpacity
            )) else lyricsTextStyle
        val effectiveTitleFontSize = if (isLowerThird) ss.titleLowerThirdFontSize else ss.titleFontSize
        val scaledTitleFontSize = (effectiveTitleFontSize * scaleFactor).sp
        val settingsLyricsFontSize = if (lookAheadEnabled) {
            if (isLowerThird) ss.lowerThirdLookAheadFontSize else ss.lookAheadFontSize
        } else if (isLowerThird) ss.lyricsLowerThirdFontSize else ss.lyricsFontSize
        val effectiveSongNumberFontSize =
            if (isLowerThird) ss.songNumberLowerThirdFontSize else ss.songNumberFontSize

        // Auto-fit: compute the largest font size that fits ALL sections without line wrapping.
        // Uses the reference 1920×1080 coordinate space (margins subtracted).
        val autoFitTextMeasurer = rememberTextMeasurer()
        val autoFitFontSize = remember(allLyricSections, isLowerThird, lookAheadEnabled, languageOverride, ss, appSettings.projectionSettings) {
            if (allLyricSections.isEmpty()) null
            else {
                val ld = effectiveLangDisplay
                val hasBilingual = allLyricSections.any { it.secondaryLines.isNotEmpty() }
                val sideBySide = ld == Constants.SONG_LANG_BOTH &&
                        ss.bilingualLayout == Constants.BILINGUAL_SIDE_BY_SIDE && hasBilingual
                val topBottom = ld == Constants.SONG_LANG_BOTH &&
                        ss.bilingualLayout == Constants.BILINGUAL_TOP_BOTTOM && hasBilingual

                val fullWidth = 1920 - appSettings.projectionSettings.windowLeft - appSettings.projectionSettings.windowRight -
                        ss.marginLeft - ss.marginRight
                // In side-by-side bilingual mode, each column gets half the width
                val refWidth = if (sideBySide) fullWidth / 2 else fullWidth
                val fullHeight = if (isLowerThird) {
                    (1080 * appSettings.projectionSettings.lowerThirdHeightPercent / 100) -
                            appSettings.projectionSettings.windowTop - appSettings.projectionSettings.windowBottom -
                            ss.marginTop - ss.marginBottom
                } else {
                    1080 - appSettings.projectionSettings.windowTop - appSettings.projectionSettings.windowBottom -
                            ss.marginTop - ss.marginBottom
                }
                // In top/bottom bilingual mode, each language gets half the height
                val refHeight = if (topBottom) fullHeight / 2 else fullHeight
                val baseStyle = TextStyle(
                    fontWeight = if (effectiveLyricsBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (effectiveLyricsItalic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = lyricsFontFamily
                )
                // Resolve display mode to know if we're in line mode
                val fitDisplayMode = if (lookAheadEnabled) {
                    if (isLowerThird) ss.lowerThirdLookAheadDisplayMode else ss.lookAheadDisplayMode
                } else {
                    if (isLowerThird) ss.lowerThirdDisplayMode else ss.fullscreenDisplayMode
                }
                val fitIsLineMode = fitDisplayMode == Constants.SONG_DISPLAY_MODE_LINE

                // For lookahead: combine each section with its next section so auto-fit
                // accounts for displaying both simultaneously at the same font size.
                val sectionsForFit = if (fitIsLineMode) {
                    // Line mode never puts a whole verse on screen — it puts one group of
                    // `linesPerSlide` lines there, plus the following group when look-ahead is on.
                    // Fitting the verse anyway is what made a single line render far smaller than the
                    // screen allowed. Fit the groups instead; the binary search still takes the
                    // largest of them, so the size stays constant across the whole song.
                    val fitLinesPerSlide = songLinesPerSlide(ss, isLowerThird)
                    val groups = allLyricSections.flatMap { songLineGroups(it.lines, fitLinesPerSlide) }
                    val secondaryGroups =
                        allLyricSections.flatMap { songLineGroups(it.secondaryLines, fitLinesPerSlide) }
                    groups.indices.map { i ->
                        val next = if (lookAheadEnabled) groups.getOrElse(i + 1) { emptyList() } else emptyList()
                        val secondary = secondaryGroups.getOrElse(i) { emptyList() }
                        val secondaryNext =
                            if (lookAheadEnabled) secondaryGroups.getOrElse(i + 1) { emptyList() } else emptyList()
                        LyricSection(
                            lines = groups[i] + next,
                            secondaryLines = if (secondaryGroups.isNotEmpty()) secondary + secondaryNext else emptyList()
                        )
                    }
                } else if (lookAheadEnabled) {
                    // Verse mode: combine full section with next section
                    allLyricSections.mapIndexed { i, section ->
                        val next = allLyricSections.getOrNull(i + 1)
                        if (next != null) {
                            section.copy(
                                lines = section.lines + next.lines,
                                secondaryLines = if (section.secondaryLines.isNotEmpty() || next.secondaryLines.isNotEmpty())
                                    section.secondaryLines + next.secondaryLines else emptyList()
                            )
                        } else section
                    }
                } else allLyricSections
                // Compute reserved height for title/song number above the verse
                val referenceDensity = Density(1f)
                val fitTitleDisplay = if (isLowerThird) ss.titleLowerThirdDisplay else ss.titleDisplay
                val fitTitlePosition = if (isLowerThird) ss.titleLowerThirdPosition else ss.titlePosition
                val fitNumberDisplay = if (isLowerThird) ss.showNumberLowerThird else ss.showNumber
                val fitNumberPosition = if (isLowerThird) ss.songNumberLowerThirdPosition else ss.songNumberPosition
                val fitTitleFontSize = if (isLowerThird) ss.titleLowerThirdFontSize else ss.titleFontSize
                val fitNumberFontSize = if (isLowerThird) ss.songNumberLowerThirdFontSize else ss.songNumberFontSize

                var reserved = 0
                if (fitTitleDisplay != Constants.NONE && fitTitlePosition == Constants.ABOVE_VERSE) {
                    val titleStyle = TextStyle(fontSize = fitTitleFontSize.sp, fontFamily = titleFontFamily)
                    val longestTitle = allLyricSections.maxOfOrNull { it.title.length }?.let { len ->
                        allLyricSections.first { it.title.length == len }.title
                    } ?: ""
                    if (longestTitle.isNotEmpty()) {
                        reserved += autoFitTextMeasurer.measure(longestTitle, titleStyle, density = referenceDensity).size.height
                    }
                }
                if (fitNumberDisplay != Constants.NONE && fitNumberPosition == Constants.ABOVE_VERSE) {
                    val numStyle = TextStyle(fontSize = fitNumberFontSize.sp, fontFamily = titleFontFamily)
                    val maxNum = allLyricSections.maxOfOrNull { it.songNumber } ?: 0
                    if (maxNum > 0) {
                        reserved += autoFitTextMeasurer.measure(maxNum.toString(), numStyle, density = referenceDensity).size.height
                    }
                }

                calculateAutoFitForAllSections(
                    textMeasurer = autoFitTextMeasurer,
                    sections = sectionsForFit,
                    baseStyle = baseStyle,
                    availableWidth = refWidth,
                    availableHeight = refHeight,
                    reservedHeight = reserved,
                    // Only leave room for the marker when it can actually be drawn — with it off,
                    // reserving its height would keep the text a size smaller than it needs to be.
                    includeEndIndicator = ss.showEndOfSongIndicator,
                    // Must match the `softWrap` the lyric Text is drawn with, or the size is fitted
                    // to a layout the presenter is not going to produce.
                    allowWrap = ss.wordWrap
                )
            }
        }
        // Bilingual flags for layout decisions (outside remember, always fresh)
        val langDisplay = effectiveLangDisplay
        val autoFitEnabled = if (lookAheadEnabled) {
            if (isLowerThird) ss.lowerThirdLookAheadFontSizeAutoFit else ss.lookAheadFontSizeAutoFit
        } else {
            if (isLowerThird) ss.lyricsLowerThirdFontSizeAutoFit else ss.lyricsFontSizeAutoFit
        }
        // A size set against this section's name beats both the song's size and auto-fit. It has to
        // beat auto-fit: that is a whole-song calculation producing ONE size for every slide, so
        // there is no way for it to also honour a section asking to be bigger than the rest.
        val sectionFontSize = appSettings
            .songAppearanceFor(lyricSection.songId)
            ?.sectionFontSize(lyricSection.header)
            ?.takeIf { !lookAheadEnabled }
        val effectiveLyricsFontSize = if (sectionFontSize != null) {
            sectionFontSize
        } else if (autoFitEnabled) {
            (autoFitFontSize ?: settingsLyricsFontSize).coerceAtMost(settingsLyricsFontSize)
        } else settingsLyricsFontSize

        val scaledLyricsFontSize = (effectiveLyricsFontSize * scaleFactor).sp
        val scaledSongNumberFontSize = (effectiveSongNumberFontSize * scaleFactor).sp

        val leftOffSet = ((appSettings.projectionSettings.windowLeft + ss.marginLeft) * scaleFactor).dp
        val rightOffSet = ((appSettings.projectionSettings.windowRight + ss.marginRight) * scaleFactor).dp
        val topOffSet = ((appSettings.projectionSettings.windowTop + ss.marginTop) * scaleFactor).dp
        val bottomOffSet = ((appSettings.projectionSettings.windowBottom + ss.marginBottom) * scaleFactor).dp

        if (isLowerThird) {
            val lowerThirdFraction = appSettings.projectionSettings.lowerThirdHeightPercent / 100f
            // Background stretches full width at bottom third, text respects padding on top —
            // same band geometry for horizontal and vertical; isLowerThirdVertical only forces
            // bilingual content to stack instead of side-by-side, see TextContent below.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(lowerThirdFraction)
                    .align(Alignment.BottomCenter)
                    .then(if (effectiveType == Constants.BACKGROUND_IMAGE && backgroundImageBitmap != null) Modifier else bgModifier)
            ) {
                if (effectiveType == Constants.BACKGROUND_IMAGE && backgroundImageBitmap != null) {
                    Image(
                        painter = BitmapPainter(backgroundImageBitmap),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier.fillMaxSize().alpha(effectiveOpacity)
                    )
                }
                if (useVideoBackground) {
                    LoopingVideoBackground(videoPath = effectiveVideoPath, modifier = Modifier.fillMaxSize().alpha(effectiveOpacity))
                }
            }
            // Gradient overlay
            if (bgConfig.gradientEnabled) {
                val gradientTop = parseHexColor(bgConfig.gradientTopColor).copy(alpha = bgConfig.gradientTopOpacity)
                val gradientBottom = parseHexColor(bgConfig.gradientBottomColor).copy(alpha = bgConfig.gradientBottomOpacity)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(lowerThirdFraction)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to gradientTop,
                                    bgConfig.gradientPosition to gradientBottom,
                                    1.0f to gradientBottom
                                )
                            )
                        )
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                // Only the vertical margins are applied here. The horizontal pair is applied further
                // in — on the title/number rows and on the lyric block inside `LyricScrim` — so that
                // the scrim itself is a genuinely full-width node rather than one drawing outside its
                // own bounds. It cannot draw outside them: the crossfade wraps this subtree in a
                // `graphicsLayer`, and while its alpha is animating the layer clips to these bounds,
                // which is exactly why the band appeared narrow for the length of every transition
                // and then snapped wide. Text position is unchanged — the same two values, applied
                // one level down.
                .padding(top = topOffSet, bottom = bottomOffSet),
            contentAlignment = if (isLowerThird) Alignment.BottomCenter else contentAlignment
        ) {
            val innerModifier = if (isLowerThird)
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(appSettings.projectionSettings.lowerThirdHeightPercent / 100f)
                    .align(Alignment.BottomCenter)
            else
                Modifier

            // Only animate the text content — background is never inside this block
            @Composable
            fun TextContent(section: LyricSection) {
                val titleDisplay = if (isLowerThird) ss.titleLowerThirdDisplay else ss.titleDisplay
                val numberDisplay = if (isLowerThird) ss.showNumberLowerThird else ss.showNumber
                val shouldShowTitle = shouldShowText(titleDisplay, section)
                val shouldShowSongNumber = shouldShowText(numberDisplay, section) && section.songNumber > 0
                // "Configured" means not set to "None" — title/number could appear on some slides
                val titleConfigured = titleDisplay != Constants.NONE
                val numberConfigured = numberDisplay != Constants.NONE && section.songNumber > 0
                val effectiveTitlePosition = if (isLowerThird) ss.titleLowerThirdPosition else ss.titlePosition
                val effectiveSongNumberPosition = if (isLowerThird) ss.songNumberLowerThirdPosition else ss.songNumberPosition
                // isLowerThirdVertical forces bilingual content to stack (one below the other)
                // instead of side-by-side — see the useSideBySide gate further below — same
                // band/geometry as horizontal otherwise.
                BoxWithConstraints(
                    modifier = innerModifier,
                    contentAlignment = if (isLowerThird) Alignment.BottomCenter else contentAlignment
                ) {

                    val allDisplayLines = section.lines
                    // Resolve per-mode settings based on fullscreen vs lower third
                    // When lookAheadEnabled, the entire screen uses lookahead's own display mode
                    val displayMode = if (lookAheadEnabled) {
                        if (isLowerThird) ss.lowerThirdLookAheadDisplayMode else ss.lookAheadDisplayMode
                    } else {
                        if (isLowerThird) ss.lowerThirdDisplayMode else ss.fullscreenDisplayMode
                    }
                    // Look-ahead portion uses same display mode as the screen
                    val laDisplayMode = displayMode
                    val laLangDisplay = effectiveLangDisplay
                    val laIsLineMode = laDisplayMode == Constants.SONG_DISPLAY_MODE_LINE

                    // Never for the title slide: it is a heading and a credit line that belong
                    // together, not a verse to be dealt out a line at a time.
                    val isLineMode = displayMode == Constants.SONG_DISPLAY_MODE_LINE &&
                        section.type != Constants.SECTION_TYPE_TITLE_SLIDE
                    val effectiveLineIndex = if (isLineMode && displayLineIndex < 0) 0 else displayLineIndex
                    // How many lines this surface puts on one slide, and where the cursor's group
                    // starts. At 1 both collapse to the original single-line behaviour.
                    val linesPerSlide = songLinesPerSlide(ss, isLowerThird)
                    val groupStart = songLineGroupStart(effectiveLineIndex, linesPerSlide)
                    val groupEndExclusive = groupStart + linesPerSlide

                    // Get next section for look-ahead
                    val nextSection: LyricSection? = if (lookAheadEnabled && displaySectionIndex >= 0) {
                        allLyricSections.getOrNull(displaySectionIndex + 1)?.takeIf { it.lines.isNotEmpty() }
                    } else null

                    // Build main display lines (current section)
                    val mainLines: List<String>
                    if (isLineMode && effectiveLineIndex >= 0 && effectiveLineIndex < allDisplayLines.size) {
                        mainLines = songLineGroup(allDisplayLines, effectiveLineIndex, linesPerSlide)
                    } else {
                        mainLines = allDisplayLines
                    }

                    // Build look-ahead primary lines
                    val laLines: List<String> = if (nextSection != null) {
                        if (laIsLineMode) {
                            // Look-ahead = next slide's worth of lines, however many that is
                            if (isLineMode && effectiveLineIndex >= 0) {
                                // Main is line mode: if there's a further group in the same section, show it; otherwise the next section's first group
                                if (groupEndExclusive < allDisplayLines.size) {
                                    songLineGroup(allDisplayLines, groupEndExclusive, linesPerSlide)
                                } else {
                                    songLineGroup(nextSection.lines, 0, linesPerSlide)
                                }
                            } else {
                                // Main is verse mode: first group of next section
                                songLineGroup(nextSection.lines, 0, linesPerSlide)
                            }
                        } else {
                            // Look-ahead = 1 verse: all lines of next section
                            nextSection.lines
                        }
                    } else if (lookAheadEnabled && isLineMode && effectiveLineIndex >= 0 && groupEndExclusive < allDisplayLines.size) {
                        // No next section but there are further lines in the current section.
                        // (Upstream tightened the old one-line form to `in 0 until size - 1`; the
                        // group form below is that same bound generalised past a group of one.)
                        if (laIsLineMode) songLineGroup(allDisplayLines, groupEndExclusive, linesPerSlide) else emptyList()
                    } else {
                        emptyList()
                    }

                    // Combine main + look-ahead

                    // Build main secondary lines (for bilingual)
                    val mainSecondaryLines: List<String> = if (section.secondaryLines.isNotEmpty()) {
                        if (isLineMode && effectiveLineIndex >= 0 && effectiveLineIndex < section.secondaryLines.size) {
                            songLineGroup(section.secondaryLines, effectiveLineIndex, linesPerSlide)
                        } else {
                            section.secondaryLines
                        }
                    } else emptyList()

                    // Build look-ahead secondary lines
                    val laSecondaryLines: List<String> = if (nextSection != null && nextSection.secondaryLines.isNotEmpty()) {
                        if (laIsLineMode) {
                            if (isLineMode && effectiveLineIndex >= 0) {
                                if (groupEndExclusive < (section.secondaryLines.size)) {
                                    songLineGroup(section.secondaryLines, groupEndExclusive, linesPerSlide)
                                } else {
                                    songLineGroup(nextSection.secondaryLines, 0, linesPerSlide)
                                }
                            } else {
                                songLineGroup(nextSection.secondaryLines, 0, linesPerSlide)
                            }
                        } else {
                            nextSection.secondaryLines
                        }
                    } else if (lookAheadEnabled && isLineMode && effectiveLineIndex >= 0 && groupEndExclusive < (section.secondaryLines.size)) {
                        if (laIsLineMode) songLineGroup(section.secondaryLines, groupEndExclusive, linesPerSlide) else emptyList()
                    } else {
                        emptyList()
                    }


                    // Apply language display to main lines
                    val effectiveDisplayLines: List<String>
                    val effectiveSecondaryDisplayLines: List<String>

                    when (langDisplay) {
                        Constants.SONG_LANG_SECONDARY -> {
                            effectiveDisplayLines = mainSecondaryLines.ifEmpty { mainLines }
                            effectiveSecondaryDisplayLines = emptyList()
                        }
                        Constants.SONG_LANG_BOTH -> {
                            effectiveDisplayLines = mainLines
                            effectiveSecondaryDisplayLines = mainSecondaryLines
                        }
                        else -> { // PRIMARY
                            effectiveDisplayLines = mainLines
                            effectiveSecondaryDisplayLines = emptyList()
                        }
                    }

                    // Apply language display to look-ahead lines
                    val effectiveLaLines: List<String>
                    val effectiveLaSecondaryLines: List<String>

                    when (laLangDisplay) {
                        Constants.SONG_LANG_SECONDARY -> {
                            effectiveLaLines = laSecondaryLines.ifEmpty { laLines }
                            effectiveLaSecondaryLines = emptyList()
                        }
                        Constants.SONG_LANG_BOTH -> {
                            effectiveLaLines = laLines
                            effectiveLaSecondaryLines = laSecondaryLines
                        }
                        else -> { // PRIMARY
                            effectiveLaLines = laLines
                            effectiveLaSecondaryLines = emptyList()
                        }
                    }

                    // Combined primary lines with look-ahead start index
                    val combinedPrimaryLines = effectiveDisplayLines + effectiveLaLines
                    val primaryLaStart = if (effectiveLaLines.isNotEmpty()) effectiveDisplayLines.size else -1

                    // Combined secondary lines with look-ahead start index
                    val combinedSecondaryLines = effectiveSecondaryDisplayLines + effectiveLaSecondaryLines
                    val secondaryLaStart = if (effectiveLaSecondaryLines.isNotEmpty()) effectiveSecondaryDisplayLines.size else -1

                    val effectiveTitle = if (langDisplay == Constants.SONG_LANG_SECONDARY && section.secondaryTitle.isNotEmpty()) {
                        section.secondaryTitle
                    } else {
                        section.title
                    }

                    val hasBilingual = combinedSecondaryLines.isNotEmpty()
                    // A Row-split side-by-side layout doesn't fit a narrow vertical band — falls
                    // through to the top/bottom bilingual branch below, which already special-cases
                    // isLowerThird (true for vertical too) with a compact stacked layout.
                    val useSideBySide = ss.bilingualLayout == Constants.BILINGUAL_SIDE_BY_SIDE && !isLowerThirdVertical

                    // Look-ahead text style with full font controls
                    val laBaseShadow = Shadow(
                        color = laShadowColor.copy(alpha = laShadowAlpha),
                        offset = Offset(6f * scaleFactor * laShadowSizeMul, 6f * scaleFactor * laShadowSizeMul),
                        blurRadius = 12f * scaleFactor * laShadowSizeMul
                    )
                    val lookAheadTextStyle = TextStyle(
                        fontWeight = if (laBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (laItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (laUnderline) TextDecoration.Underline else TextDecoration.None,
                        shadow = if (laShadowEnabled) laBaseShadow else null
                    )
                    // Look-ahead next uses auto-fit capped at its own configured max
                    val laAutoFitEnabled = if (isLowerThird) ss.lowerThirdLookAheadNextFontSizeAutoFit else ss.lookAheadNextFontSizeAutoFit
                    val effectiveLaFontSize = if (laAutoFitEnabled) {
                        (autoFitFontSize ?: laFontSize).coerceAtMost(laFontSize)
                    } else laFontSize
                    val scaledLaFontSize = (effectiveLaFontSize * scaleFactor).sp

                    // ── Readability scrim ──
                    // A band of darkness behind the words, sized to the words rather than to the
                    // screen, so it tracks however many lines the slide currently holds. The edges
                    // fade rather than cut, which is what stops it reading as a black rectangle
                    // pasted over the photo.
                    val scrimEnabled = if (isLowerThird) ss.lyricsLowerThirdScrimEnabled else ss.lyricsScrimEnabled
                    val scrimColor = parseHexColor(if (isLowerThird) ss.lyricsLowerThirdScrimColor else ss.lyricsScrimColor)
                    val scrimAlpha = ((if (isLowerThird) ss.lyricsLowerThirdScrimOpacity else ss.lyricsScrimOpacity)
                        .coerceIn(0, 100)) / 100f
                    val scrimSoftness = ((if (isLowerThird) ss.lyricsLowerThirdScrimSoftness else ss.lyricsScrimSoftness)
                        .coerceIn(0, 100)) / 200f  // 100% means the fade meets in the middle
                    val scrimPadding = (if (isLowerThird) ss.lyricsLowerThirdScrimPadding else ss.lyricsScrimPadding)
                        .coerceAtLeast(0)
                    val scrimWidthFraction = ((if (isLowerThird) ss.lyricsLowerThirdScrimWidthPercent else ss.lyricsScrimWidthPercent)
                        .coerceIn(10, 100)) / 100f
                    // Key/fill output carries the alpha channel for the hardware keyer, so the scrim
                    // is part of the fill, not the key — the key must stay a clean matte of the text.
                    val scrimVisible = scrimEnabled && scrimAlpha > 0f && outputRole != Constants.OUTPUT_ROLE_KEY
                    val scrimBrush = remember(scrimColor, scrimAlpha, scrimSoftness) {
                        val solid = scrimColor.copy(alpha = scrimAlpha)
                        if (scrimSoftness <= 0f) {
                            Brush.verticalGradient(0f to solid, 1f to solid)
                        } else {
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                scrimSoftness to solid,
                                (1f - scrimSoftness) to solid,
                                1f to Color.Transparent
                            )
                        }
                    }

                    /**
                     * Wraps a block of lyrics in its readability band, and — scrim or no scrim — puts
                     * back the horizontal margins that [Box] above no longer applies.
                     *
                     * The band is a real full-width node, sized by layout rather than painted past
                     * its own edges, so it is whatever width it claims to be from the first frame of
                     * a transition rather than growing into it.
                     */
                    @Composable
                    fun LyricScrim(content: @Composable () -> Unit) {
                        @Composable
                        fun Margined() {
                            Box(modifier = Modifier.padding(start = leftOffSet, end = rightOffSet)) {
                                content()
                            }
                        }
                        if (!scrimVisible) {
                            Margined()
                            return
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(scrimWidthFraction)
                                .wrapContentHeight()
                                .background(scrimBrush)
                                // After the background on purpose: the band has to extend past the
                                // text, so this padding is inside what gets painted.
                                .padding(vertical = (scrimPadding * scaleFactor).dp),
                            contentAlignment = Alignment.Center
                        ) { Margined() }
                    }

                    @Composable
                    fun LyricLine(lineIdx: Int, line: String, laStart: Int) {
                        val isLookAheadLine = laStart >= 0 && lineIdx >= laStart
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = lyricsHorizontalAlignment,
                            fontFamily = if (isLookAheadLine) laFontFamily else lyricsFontFamily,
                            fontSize = if (isLookAheadLine) scaledLaFontSize else scaledLyricsFontSize,
                            softWrap = ss.wordWrap,
                            text = line,
                            color = if (isLookAheadLine) laColor else lyricsColor,
                            style = if (isLookAheadLine) lookAheadTextStyle else lyricsTextStyleScaled
                        )
                    }

                    @Composable
                    fun LookAheadSpacer(idx: Int, laStart: Int) {
                        if (laStart >= 0 && idx == laStart && !laIsLineMode) {
                            Spacer(modifier = Modifier.padding(top = (12 * scaleFactor).dp))
                        }
                    }

                    @Composable
                    fun EndOfSongIndicator() {
                        // Nothing at all when it is switched off — not an invisible row. Reserving the
                        // space is only worth it while the marker can actually appear; otherwise it
                        // silently costs every slide a line of height, and the readability band would
                        // stretch to cover an empty row under the words.
                        if (!ss.showEndOfSongIndicator) return
                        // Always reserve space so lyrics don't shift when the indicator appears on the last section
                        // In line mode the indicator belongs on the LAST SLIDE of the section, which
                        // with multi-line slides is the last group — not merely the last line.
                        val visible = section.isLastSection && (!isLineMode || groupEndExclusive >= allDisplayLines.size)
                        val indicatorAlpha = if (visible) 1f else 0f
                        Spacer(modifier = Modifier.padding(top = (4 * scaleFactor).dp))
                        val indicatorPad = " ".repeat(ss.endOfSongIndicatorSpacing)
                        val indicatorText = "$indicatorPad*$indicatorPad"
                        Row(modifier = Modifier.fillMaxWidth().alpha(indicatorAlpha), horizontalArrangement = Arrangement.Center) {
                            repeat(INDICATOR_REPEAT_COUNT) { Text(text = indicatorText, fontSize = scaledLyricsFontSize, color = lyricsColor, style = lyricsTextStyleScaled) }
                        }
                    }

                    // Invisible placeholder to reserve space for missing lookahead on last section
                    @Composable
                    fun LookAheadPlaceholder() {
                        if (lookAheadEnabled && effectiveLaLines.isEmpty() && effectiveDisplayLines.isNotEmpty()) {
                            if (!laIsLineMode) {
                                Spacer(modifier = Modifier.padding(top = (12 * scaleFactor).dp))
                            }
                            Column(modifier = Modifier.alpha(0f)) {
                                effectiveDisplayLines.forEach { line ->
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = lyricsHorizontalAlignment,
                                        fontFamily = laFontFamily,
                                        fontSize = scaledLaFontSize,
                                        softWrap = ss.wordWrap,
                                        text = line,
                                        color = laColor,
                                        style = lookAheadTextStyle
                                    )
                                }
                            }
                        }
                    }

                    // Renders title and/or song number for a given position (ABOVE_VERSE or BELOW_VERSE)
                    val samePosition = effectiveTitlePosition == effectiveSongNumberPosition
                    val sameHorizontal = (if (isLowerThird) ss.songNumberLowerThirdHorizontalAlignment else ss.songNumberHorizontalAlignment) ==
                            (if (isLowerThird) ss.titleLowerThirdHorizontalAlignment else ss.titleHorizontalAlignment)
                    val numberBeforeTitle = ss.songNumberBeforeTitle

                    @Composable
                    fun NumberPart(modifier: Modifier = Modifier, visibilityAlpha: Float = 1f) {
                        Text(
                            modifier = modifier.alpha(visibilityAlpha),
                            textAlign = songNumberHorizontalAlignment,
                            fontFamily = titleFontFamily,
                            fontSize = scaledSongNumberFontSize,
                            text = section.songNumber.toString(),
                            color = titleColor,
                            style = titleTextStyleScaled
                        )
                    }

                    @Composable
                    fun TitlePart(modifier: Modifier = Modifier, visibilityAlpha: Float = 1f) {
                        Text(
                            modifier = modifier.alpha(visibilityAlpha),
                            textAlign = titleHorizontalAlignment,
                            fontFamily = titleFontFamily,
                            fontSize = scaledTitleFontSize,
                            text = effectiveTitle,
                            color = titleColor,
                            style = titleTextStyleScaled
                        )
                    }

                    @Composable
                    fun TitleAndNumberRow(position: String, invisible: Boolean = false) {
                        // "configured" = setting is not None (could appear on some slides)
                        val hasTitleHere = titleConfigured && effectiveTitlePosition == position
                        val hasNumberHere = numberConfigured && effectiveSongNumberPosition == position
                        if (!hasTitleHere && !hasNumberHere) return

                        // Alpha: fully invisible when used as a balancing spacer,
                        // otherwise visible on this slide or invisible (reserving space)
                        val titleAlpha = if (invisible) 0f else if (shouldShowTitle) 1f else 0f
                        val numberAlpha = if (invisible) 0f else if (shouldShowSongNumber) 1f else 0f

                        // The horizontal song margins, which the enclosing box no longer applies —
                        // see the comment on its padding. Put back on each element rather than on a
                        // wrapper around them: this composable is called both into a Column (where
                        // the number and title stack) and into a Box (where they overlap), so any
                        // single wrapper would change one of those two.
                        val marginModifier = Modifier.fillMaxWidth().padding(start = leftOffSet, end = rightOffSet)
                        if (hasTitleHere && hasNumberHere && samePosition) {
                            if (sameHorizontal) {
                                val sharedHAlign = if (isLowerThird) ss.songNumberLowerThirdHorizontalAlignment else ss.songNumberHorizontalAlignment
                                val arrangement = when (sharedHAlign) {
                                    Constants.LEFT -> Arrangement.Start
                                    Constants.CENTER -> Arrangement.Center
                                    else -> Arrangement.End
                                }
                                Row(modifier = marginModifier, horizontalArrangement = arrangement) {
                                    if (numberBeforeTitle) {
                                        NumberPart(visibilityAlpha = numberAlpha); Spacer(modifier = Modifier.padding(horizontal = (4 * scaleFactor).dp)); TitlePart(visibilityAlpha = titleAlpha)
                                    } else {
                                        TitlePart(visibilityAlpha = titleAlpha); Spacer(modifier = Modifier.padding(horizontal = (4 * scaleFactor).dp)); NumberPart(visibilityAlpha = numberAlpha)
                                    }
                                }
                            } else {
                                NumberPart(modifier = marginModifier, visibilityAlpha = numberAlpha)
                                TitlePart(modifier = marginModifier, visibilityAlpha = titleAlpha)
                            }
                        } else if (hasNumberHere) {
                            NumberPart(modifier = marginModifier, visibilityAlpha = numberAlpha)
                        } else if (hasTitleHere) {
                            TitlePart(modifier = marginModifier, visibilityAlpha = titleAlpha)
                        }
                    }

                    // Determine which positions have content for balancing
                    val hasBottomContent = (titleConfigured && effectiveTitlePosition == Constants.BELOW_VERSE) ||
                            (numberConfigured && effectiveSongNumberPosition == Constants.BELOW_VERSE)

                    // Outer column fills the content area; title/number at edges, lyrics centered
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top section: items positioned "above verse"
                        TitleAndNumberRow(Constants.ABOVE_VERSE)

                        // Lyrics area + bottom title/number overlaid (z-stacked).
                        // The bottom title/number floats over the lyrics so it doesn't
                        // steal vertical space and cut off lyrics text.
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            // Lyrics fill the entire remaining space
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = if (isLowerThird) Alignment.BottomCenter else contentAlignment
                            ) {
                                if (hasBilingual) {
                                    if (useSideBySide) {
                                        LyricScrim {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                                                combinedPrimaryLines.forEachIndexed { idx, line ->
                                                    LookAheadSpacer(idx, primaryLaStart)
                                                    LyricLine(idx, line, primaryLaStart)
                                                }
                                                EndOfSongIndicator()
                                                LookAheadPlaceholder()
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                                                combinedSecondaryLines.forEachIndexed { idx, line ->
                                                    LookAheadSpacer(idx, secondaryLaStart)
                                                    LyricLine(idx, line, secondaryLaStart)
                                                }
                                                EndOfSongIndicator()
                                                LookAheadPlaceholder()
                                            }
                                        }
                                        }
                                    } else {
                                        // Top/bottom bilingual layout
                                        if (isLowerThird) {
                                            // Lower third: compact layout, no height splitting
                                            LyricScrim {
                                            Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                                combinedPrimaryLines.forEachIndexed { idx, line ->
                                                    LookAheadSpacer(idx, primaryLaStart)
                                                    LyricLine(idx, line, primaryLaStart)
                                                }
                                                EndOfSongIndicator()
                                                LookAheadPlaceholder()
                                                Spacer(modifier = Modifier.padding(top = (12 * scaleFactor).dp))
                                                combinedSecondaryLines.forEachIndexed { idx, line ->
                                                    LookAheadSpacer(idx, secondaryLaStart)
                                                    LyricLine(idx, line, secondaryLaStart)
                                                }
                                                EndOfSongIndicator()
                                                LookAheadPlaceholder()
                                            }
                                            }
                                        } else {
                                            // Full screen: each language gets its own half
                                            val halfAlignment = contentAlignment
                                            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = halfAlignment) {
                                                    // One band per half, not one behind the pair: the
                                                    // two languages sit in separate halves of the
                                                    // screen with empty space between them, and a
                                                    // single band would darken that gap too.
                                                    LyricScrim {
                                                    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                                        combinedPrimaryLines.forEachIndexed { idx, line ->
                                                            LookAheadSpacer(idx, primaryLaStart)
                                                            LyricLine(idx, line, primaryLaStart)
                                                        }
                                                        EndOfSongIndicator()
                                                        LookAheadPlaceholder()
                                                    }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.padding(top = (12 * scaleFactor).dp))
                                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = halfAlignment) {
                                                    LyricScrim {
                                                    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                                        combinedSecondaryLines.forEachIndexed { idx, line ->
                                                            LookAheadSpacer(idx, secondaryLaStart)
                                                            LyricLine(idx, line, secondaryLaStart)
                                                        }
                                                        EndOfSongIndicator()
                                                        LookAheadPlaceholder()
                                                    }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Single language layout
                                    LyricScrim {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                        verticalArrangement = if (isLowerThird) Arrangement.Bottom else Arrangement.Top
                                    ) {
                                        combinedPrimaryLines.forEachIndexed { idx, line ->
                                            LookAheadSpacer(idx, primaryLaStart)
                                            LyricLine(idx, line, primaryLaStart)
                                        }
                                        EndOfSongIndicator()
                                        LookAheadPlaceholder()
                                    }
                                    }
                                }
                            }

                            // Bottom title/number overlaid at the bottom of the lyrics area
                            if (hasBottomContent) {
                                Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                                    TitleAndNumberRow(Constants.BELOW_VERSE)
                                }
                            }
                        }
                    }
                }
            }

            if (crossfadeEnabled || ss.fadeIn || ss.fadeOut) {
                val duration = ss.transitionDuration.toInt().coerceAtLeast(100)
                val isCrossfade = crossfadeEnabled
                var displayedCurrent by remember { mutableStateOf(lyricSection) }
                var displayedPrevious by remember { mutableStateOf(LyricSection()) }
                var currentAlpha by remember { mutableStateOf(1f) }
                var previousAlpha by remember { mutableStateOf(0f) }
                val pendingQueue = remember { kotlinx.coroutines.channels.Channel<LyricSection>(kotlinx.coroutines.channels.Channel.CONFLATED) }

                // Queue section changes
                LaunchedEffect(lyricSection) {
                    if (displayedCurrent != lyricSection) {
                        pendingQueue.send(lyricSection)
                    }
                }

                // Process section switches (crossfade between sections)
                LaunchedEffect(Unit) {
                    for (nextSection in pendingQueue) {
                        if (displayedCurrent == nextSection) continue

                        if (isCrossfade) {
                            displayedPrevious = displayedCurrent
                            displayedCurrent = nextSection
                            previousAlpha = 1f
                            currentAlpha = 0f
                            val anim = Animatable(0f)
                            anim.animateTo(1f, tween(durationMillis = duration)) {
                                currentAlpha = this.value
                                previousAlpha = 1f - this.value
                            }
                        } else {
                            displayedCurrent = nextSection
                        }
                        currentAlpha = 1f
                        previousAlpha = 0f
                        displayedPrevious = LyricSection()
                    }
                }

                Box(modifier = Modifier.matchParentSize().graphicsLayer { alpha = transitionAlpha }) {
                    if (displayedPrevious.lines.isNotEmpty() && previousAlpha > 0f) {
                        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = previousAlpha }) {
                            TextContent(displayedPrevious)
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = currentAlpha }) {
                        TextContent(displayedCurrent)
                    }
                }
            } else {
                Box(modifier = Modifier.graphicsLayer { alpha = transitionAlpha }) {
                    TextContent(lyricSection)
                }
            }
        }
    }
}

private fun shouldShowText(display: String, lyricSection: LyricSection): Boolean {
    return when (display) {
        Constants.EVERY_PAGE -> true
        Constants.FIRST_PAGE -> {
            // Show only on the first verse section (header null, ends with "1", or verse with no number)
            val header = lyricSection.header ?: return true  // null header = first/only section
            // Chorus/bridge sections are not "first page"
            if (lyricSection.type == Constants.SECTION_TYPE_CHORUS) return false
            val inner = header.trim().removePrefix("[").removePrefix("{").removeSuffix("]").removeSuffix("}").trim()
            // The trailing number is compared as a number, not as a string ending in "1" — that read
            // verses 11, 21 and 31 as the opening slide, so the title came back over them part-way
            // through any hymn long enough to have eleven sections.
            val sectionNumber = inner.takeLastWhile { it.isDigit() }.toIntOrNull()
            sectionNumber == 1 || !inner.any { it.isDigit() }
        }

        else -> false
    }
}

private fun getTextAlign(alignment: String): TextAlign {
    return when (alignment) {
        Constants.LEFT -> TextAlign.Start
        Constants.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }
}
