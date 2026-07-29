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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.data.settings.BibleTranslationSettings
import org.churchpresenter.app.churchpresenter.models.SelectedVerse
import org.churchpresenter.app.churchpresenter.composables.LoopingVideoBackground
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.Utils.parseHexColor
import org.churchpresenter.app.churchpresenter.utils.Utils.systemFontFamilyOrDefault
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.min

private fun binarySearchFitScale(
    minScale: Float = 0.15f,
    iterations: Int = 8,
    fits: (scale: Float) -> Boolean
): Float {
    var lo = minScale
    var hi = 1f
    repeat(iterations) {
        val mid = (lo + hi) / 2f
        if (fits(mid)) lo = mid else hi = mid
    }
    return lo
}

@Composable
fun BiblePresenter(
    modifier: Modifier = Modifier,
    selectedVerses: List<SelectedVerse>,
    appSettings: AppSettings,
    isLowerThird: Boolean = false,
    // Only changes the band's geometry (a right-anchored vertical strip instead of a bottom
    // horizontal band) — isLowerThird alone still selects all the *LowerThird* styling fields
    // (fonts/colors/sizes/etc.) for both orientations, so there's one style profile to maintain.
    isLowerThirdVertical: Boolean = false,
    outputRole: String = Constants.OUTPUT_ROLE_NORMAL,
    transitionAlpha: Float = 1f,
    showBackground: Boolean = true,
    crossfadeEnabled: Boolean = false,
    /** Positions in the translation stack this output shows; empty means all of them. */
    bibleTranslations: List<Int> = emptyList(),
) {
    // Filter to the translations this screen is assigned. Selecting one no longer promotes it into
    // the primary slot the way the old "secondary" mode did: styling is looked up per verse by its
    // own translation, so a verse keeps its own colours and font wherever it lands.
    val effectiveVerses =
        if (bibleTranslations.isEmpty()) selectedVerses
        else selectedVerses.filterIndexed { index, _ -> index in bibleTranslations }
            .ifEmpty { selectedVerses.take(1) }
    val isKey = outputRole == Constants.OUTPUT_ROLE_KEY
    val bs = appSettings.bibleSettings
    // The first two translations, which the lower third renders. Full screen draws the whole stack
    // instead; a missing entry falls back to defaults so an unconfigured slot still has a style.
    val translationStack = bs.translationList()
    val t0 = translationStack.getOrNull(0) ?: BibleTranslationSettings()
    val t1 = translationStack.getOrNull(1) ?: BibleTranslationSettings()

    // Resolve font families — use lower-third-specific values when applicable
    val primaryBibleFontStyle = remember(
        if (isLowerThird) t0.lowerThirdTextFontType else t0.textFontType
    ) {
        systemFontFamilyOrDefault(if (isLowerThird) t0.lowerThirdTextFontType else t0.textFontType)
    }
    val primaryBibleReferenceFontStyle = remember(
        if (isLowerThird) t0.lowerThirdReferenceFontType else t0.referenceFontType
    ) {
        systemFontFamilyOrDefault(if (isLowerThird) t0.lowerThirdReferenceFontType else t0.referenceFontType)
    }
    val secondaryBibleFontStyle = remember(
        if (isLowerThird) t1.lowerThirdTextFontType else t1.textFontType
    ) {
        systemFontFamilyOrDefault(if (isLowerThird) t1.lowerThirdTextFontType else t1.textFontType)
    }
    val secondaryBibleReferenceFontStyle = remember(
        if (isLowerThird) t1.lowerThirdReferenceFontType else t1.referenceFontType
    ) {
        systemFontFamilyOrDefault(if (isLowerThird) t1.lowerThirdReferenceFontType else t1.referenceFontType)
    }

    val primaryBible = effectiveVerses.firstOrNull() ?: return
    val secondaryBible = effectiveVerses.getOrNull(1)

    // Resolve colors — key mode forces white for a proper key signal
    val primaryBibleTextColor = remember(
        if (isLowerThird) t0.lowerThirdTextColor else t0.textColor, isKey
    ) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) t0.lowerThirdTextColor else t0.textColor)
    }
    val primaryBibleReferenceTextColor = remember(
        if (isLowerThird) t0.lowerThirdReferenceColor else t0.referenceColor, isKey
    ) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) t0.lowerThirdReferenceColor else t0.referenceColor)
    }
    val secondaryBibleTextColor = remember(
        if (isLowerThird) t1.lowerThirdTextColor else t1.textColor, isKey
    ) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) t1.lowerThirdTextColor else t1.textColor)
    }
    val secondaryBibleReferenceTextColor = remember(
        if (isLowerThird) t1.lowerThirdReferenceColor else t1.referenceColor, isKey
    ) {
        if (isKey) Color.White
        else parseHexColor(if (isLowerThird) t1.lowerThirdReferenceColor else t1.referenceColor)
    }

    // Resolve bold/italic/underline/shadow — use lower-third-specific values when applicable
    val pBold = if (isLowerThird) t0.lowerThirdTextBold else t0.textBold
    val pItalic = if (isLowerThird) t0.lowerThirdTextItalic else t0.textItalic
    val pUnderline = if (isLowerThird) t0.lowerThirdTextUnderline else t0.textUnderline
    val pShadow = if (isLowerThird) t0.lowerThirdTextShadow else t0.textShadow
    val prBold = if (isLowerThird) t0.lowerThirdReferenceBold else t0.referenceBold
    val prItalic = if (isLowerThird) t0.lowerThirdReferenceItalic else t0.referenceItalic
    val prUnderline = if (isLowerThird) t0.lowerThirdReferenceUnderline else t0.referenceUnderline
    val prShadow = if (isLowerThird) t0.lowerThirdReferenceShadow else t0.referenceShadow
    val sBold = if (isLowerThird) t1.lowerThirdTextBold else t1.textBold
    val sItalic = if (isLowerThird) t1.lowerThirdTextItalic else t1.textItalic
    val sUnderline = if (isLowerThird) t1.lowerThirdTextUnderline else t1.textUnderline
    val sShadow = if (isLowerThird) t1.lowerThirdTextShadow else t1.textShadow
    val srBold = if (isLowerThird) t1.lowerThirdReferenceBold else t1.referenceBold
    val srItalic = if (isLowerThird) t1.lowerThirdReferenceItalic else t1.referenceItalic
    val srUnderline = if (isLowerThird) t1.lowerThirdReferenceUnderline else t1.referenceUnderline
    val srShadow = if (isLowerThird) t1.lowerThirdReferenceShadow else t1.referenceShadow

    // Per-element shadow helpers
    fun makeShadow(color: String, size: Int, opacity: Int, alphaScale: Float = 0.78f): Shadow {
        val base = parseHexColor(color)
        val mul = size / 100f
        val alpha = (opacity / 100f).coerceIn(0f, 1f)
        return Shadow(
            color = base.copy(alpha = alpha * alphaScale),
            offset = Offset(2f * mul, 2f * mul),
            blurRadius = 4f * mul
        )
    }

    val pBibleShadowVal = makeShadow(
        if (isLowerThird) t0.lowerThirdTextShadowColor else t0.textShadowColor,
        if (isLowerThird) t0.lowerThirdTextShadowSize else t0.textShadowSize,
        if (isLowerThird) t0.lowerThirdTextShadowOpacity else t0.textShadowOpacity
    )
    val pRefShadowVal = makeShadow(
        if (isLowerThird) t0.lowerThirdReferenceShadowColor else t0.referenceShadowColor,
        if (isLowerThird) t0.lowerThirdReferenceShadowSize else t0.referenceShadowSize,
        if (isLowerThird) t0.lowerThirdReferenceShadowOpacity else t0.referenceShadowOpacity
    )
    val sBibleShadowVal = makeShadow(
        if (isLowerThird) t1.lowerThirdTextShadowColor else t1.textShadowColor,
        if (isLowerThird) t1.lowerThirdTextShadowSize else t1.textShadowSize,
        if (isLowerThird) t1.lowerThirdTextShadowOpacity else t1.textShadowOpacity
    )
    val sRefShadowVal = makeShadow(
        if (isLowerThird) t1.lowerThirdReferenceShadowColor else t1.referenceShadowColor,
        if (isLowerThird) t1.lowerThirdReferenceShadowSize else t1.referenceShadowSize,
        if (isLowerThird) t1.lowerThirdReferenceShadowOpacity else t1.referenceShadowOpacity
    )

    // Text styles from settings
    val primaryBibleTextStyle = TextStyle(
        fontWeight = if (pBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (pItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (pUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (pShadow) pBibleShadowVal else null
    )
    val primaryReferenceTextStyle = TextStyle(
        fontWeight = if (prBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (prItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (prUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (prShadow) pRefShadowVal else null
    )
    val secondaryBibleTextStyle = TextStyle(
        fontWeight = if (sBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (sItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (sUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (sShadow) sBibleShadowVal else null
    )
    val secondaryReferenceTextStyle = TextStyle(
        fontWeight = if (srBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (srItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (srUnderline) TextDecoration.Underline else TextDecoration.None,
        shadow = if (srShadow) sRefShadowVal else null
    )

    val primaryBibleHorizontalAlignment = when (
        if (isLowerThird) appSettings.bibleSettings.primaryBibleLowerThirdHorizontalAlignment
        else appSettings.bibleSettings.primaryBibleHorizontalAlignment
    ) {
        Constants.LEFT -> TextAlign.Start
        Constants.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }

    val primaryBibleReferenceHorizontalAlignment = when (
        if (isLowerThird) appSettings.bibleSettings.primaryReferenceLowerThirdHorizontalAlignment
        else appSettings.bibleSettings.primaryReferenceHorizontalAlignment
    ) {
        Constants.LEFT -> TextAlign.Start
        Constants.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }

    val secondaryBibleHorizontalAlignment = when (
        if (isLowerThird) appSettings.bibleSettings.secondaryBibleLowerThirdHorizontalAlignment
        else appSettings.bibleSettings.secondaryBibleHorizontalAlignment
    ) {
        Constants.LEFT -> TextAlign.Start
        Constants.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }

    val secondaryBibleReferenceHorizontalAlignment = when (
        if (isLowerThird) appSettings.bibleSettings.secondaryReferenceLowerThirdHorizontalAlignment
        else appSettings.bibleSettings.secondaryReferenceHorizontalAlignment
    ) {
        Constants.LEFT -> TextAlign.Start
        Constants.RIGHT -> TextAlign.End
        else -> TextAlign.Center
    }

    val primaryBibleReferencePosition = if (isLowerThird) t0.lowerThirdReferencePosition else t0.referencePosition
    val secondaryBibleReferencePosition = if (isLowerThird) t1.lowerThirdReferencePosition else t1.referencePosition

    // Combine vertical alignment with horizontal center
    val contentAlignment = when (appSettings.bibleSettings.verticalAlignment) {
        Constants.TOP -> Alignment.TopCenter
        Constants.BOTTOM -> Alignment.BottomCenter
        else -> Alignment.Center  // MIDDLE or default
    }

    val bgConfig = if (isLowerThird) appSettings.backgroundSettings.bibleLowerThirdBackground
    else appSettings.backgroundSettings.bibleBackground

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
                if (file.exists()) org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
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
        useVideoBackground -> Modifier.background(Color.Black) // video rendered as overlay
        effectiveType == Constants.BACKGROUND_IMAGE && backgroundImageBitmap != null ->
            Modifier.alpha(effectiveOpacity).paint(painter = BitmapPainter(backgroundImageBitmap), contentScale = ContentScale.Crop)

        effectiveType == Constants.BACKGROUND_IMAGE ->
            Modifier.background(Color.Black)

        else ->
            Modifier.background(backgroundColor.copy(alpha = effectiveOpacity))
    }

    // Fade-in on first appearance (covers background + text)
    val fadeInDuration = appSettings.bibleSettings.transitionDuration.toInt().coerceAtLeast(100)
    var enterAlpha by remember { mutableStateOf(if (appSettings.bibleSettings.fadeIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (appSettings.bibleSettings.fadeIn && enterAlpha < 1f) {
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(durationMillis = fadeInDuration)) {
                enterAlpha = this.value
            }
            enterAlpha = 1f
        }
    }

    BoxWithConstraints(
        modifier.fillMaxSize()
            .graphicsLayer { alpha = transitionAlpha * enterAlpha }
            .then(if (!isLowerThird) bgModifier else Modifier)
    ) {
        if (useVideoBackground && !isLowerThird) {
            LoopingVideoBackground(
                videoPath = effectiveVideoPath,
                modifier = Modifier.fillMaxSize().alpha(effectiveOpacity)
            )
        }
        val density = LocalDensity.current
        val widthScale = with(density) { maxWidth.toPx() / 1920f }
        val heightScale = with(density) { maxHeight.toPx() / 1080f }
        val scaleFactor = min(widthScale, heightScale).coerceIn(0.5f, 3.0f)

        // Scale shadow to be visible at projection resolution
        fun scaleElementShadow(color: String, size: Int, opacity: Int): Shadow {
            val base = parseHexColor(color)
            val mul = size / 100f
            val alpha = (opacity / 100f).coerceIn(0f, 1f)
            return Shadow(
                color = base.copy(alpha = alpha),
                offset = Offset(6f * scaleFactor * mul, 6f * scaleFactor * mul),
                blurRadius = 12f * scaleFactor * mul
            )
        }
        val primaryBibleTextStyleScaled = if (pShadow)
            primaryBibleTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) t0.lowerThirdTextShadowColor else t0.textShadowColor,
                if (isLowerThird) t0.lowerThirdTextShadowSize else t0.textShadowSize,
                if (isLowerThird) t0.lowerThirdTextShadowOpacity else t0.textShadowOpacity
            )) else primaryBibleTextStyle
        val primaryReferenceTextStyleScaled = if (prShadow)
            primaryReferenceTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) t0.lowerThirdReferenceShadowColor else t0.referenceShadowColor,
                if (isLowerThird) t0.lowerThirdReferenceShadowSize else t0.referenceShadowSize,
                if (isLowerThird) t0.lowerThirdReferenceShadowOpacity else t0.referenceShadowOpacity
            )) else primaryReferenceTextStyle
        val secondaryBibleTextStyleScaled = if (sShadow)
            secondaryBibleTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) t1.lowerThirdTextShadowColor else t1.textShadowColor,
                if (isLowerThird) t1.lowerThirdTextShadowSize else t1.textShadowSize,
                if (isLowerThird) t1.lowerThirdTextShadowOpacity else t1.textShadowOpacity
            )) else secondaryBibleTextStyle
        val secondaryReferenceTextStyleScaled = if (srShadow)
            secondaryReferenceTextStyle.copy(shadow = scaleElementShadow(
                if (isLowerThird) t1.lowerThirdReferenceShadowColor else t1.referenceShadowColor,
                if (isLowerThird) t1.lowerThirdReferenceShadowSize else t1.referenceShadowSize,
                if (isLowerThird) t1.lowerThirdReferenceShadowOpacity else t1.referenceShadowOpacity
            )) else secondaryReferenceTextStyle

        val effectivePrimaryBibleSize =
            if (isLowerThird) appSettings.bibleSettings.primaryBibleLowerThirdFontSize else appSettings.bibleSettings.primaryBibleFontSize
        val effectivePrimaryReferenceSize =
            if (isLowerThird) appSettings.bibleSettings.primaryReferenceLowerThirdFontSize else appSettings.bibleSettings.primaryReferenceFontSize
        val effectiveSecondaryBibleSize =
            if (isLowerThird) appSettings.bibleSettings.secondaryBibleLowerThirdFontSize else appSettings.bibleSettings.secondaryBibleFontSize
        val effectiveSecondaryReferenceSize =
            if (isLowerThird) appSettings.bibleSettings.secondaryReferenceLowerThirdFontSize else appSettings.bibleSettings.secondaryReferenceFontSize
        val scaledPrimaryBibleSize = (effectivePrimaryBibleSize * scaleFactor).sp
        val scaledPrimaryReferenceSize = (effectivePrimaryReferenceSize * scaleFactor).sp
        val scaledSecondaryBibleSize = (effectiveSecondaryBibleSize * scaleFactor).sp
        val scaledSecondaryReferenceSize = (effectiveSecondaryReferenceSize * scaleFactor).sp
        val leftOffSet = ((appSettings.projectionSettings.windowLeft + appSettings.bibleSettings.marginLeft) * scaleFactor).dp
        val rightOffSet = ((appSettings.projectionSettings.windowRight + appSettings.bibleSettings.marginRight) * scaleFactor).dp
        val topOffSet = ((appSettings.projectionSettings.windowTop + appSettings.bibleSettings.marginTop) * scaleFactor).dp
        val bottomOffSet = ((appSettings.projectionSettings.windowBottom + appSettings.bibleSettings.marginBottom) * scaleFactor).dp

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

        // Outer box for padding/alignment — not animated
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftOffSet, end = rightOffSet, top = topOffSet, bottom = bottomOffSet),
            contentAlignment = if (isLowerThird) Alignment.BottomCenter else contentAlignment
        ) {
            val innerModifier = if (isLowerThird)
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(appSettings.projectionSettings.lowerThirdHeightPercent / 100f)
                    .align(Alignment.BottomCenter)
                    .clipToBounds()
            else
                Modifier.align(contentAlignment)

            val textMeasurer = rememberTextMeasurer()

            // Helper: build reference text for a verse
            fun buildRefText(verse: SelectedVerse, showAbbr: Boolean): String {
                val abbr = if (showAbbr && verse.bibleAbbreviation.isNotEmpty()) verse.bibleAbbreviation else ""
                val verseRef = if (verse.verseRange.isNotEmpty()) verse.verseRange else verse.verseNumber.toString()
                return "$abbr ${verse.bookName} ${verse.chapter}:$verseRef"
            }

            // Only animate the text content — background is never inside this block
            @Composable
            fun TextContent(verses: List<SelectedVerse>) {
                val primary = verses.first()
                val secondary = verses.getOrNull(1)
                val primaryVerseRef = if (primary.verseRange.isNotEmpty()) primary.verseRange else primary.verseNumber.toString()
                val secondaryVerseRef = secondary?.let { if (it.verseRange.isNotEmpty()) it.verseRange else it.verseNumber.toString() } ?: ""
                // A settings file that names only a secondary bible still means "bilingual" -- the
                // condition this replaced keyed off exactly that, and dropping it stopped the second
                // language rendering for those files.
                val isParallelIntended = translationStack.size > 1 || bs.secondaryBible.isNotEmpty()
                val showParallelLayout = isParallelIntended && secondary != null && (!isLowerThird || t1.lowerThirdEnabled)
                val showSecondary = secondary != null && showParallelLayout

                // Full screen always draws the ordered stack, however many translations there are:
                // each line reads its own style profile and a shared fit scale keeps the whole stack
                // on screen. The lower third keeps its own one/two-language layouts below, because a
                // narrow band cannot usefully hold more than a couple of languages anyway.
                if (!isLowerThird) {
                    val configured = translationStack
                    // Only draw as many languages as this machine is set up for. A verse can arrive
                    // carrying a second translation -- from a linked instance -- while nothing here
                    // is configured to show one, and it must not appear unasked.
                    val allowed = if (isParallelIntended) maxOf(configured.size, 2) else 1
                    val visible = verses.take(allowed).mapIndexedNotNull { index, verse ->
                        val style = configured.firstOrNull { it.fileName == verse.translationFileName }
                            ?: configured.getOrNull(index)
                            ?: BibleTranslationSettings(fileName = verse.translationFileName)
                        verse to style
                    }
                    BoxWithConstraints(modifier = innerModifier) {
                        val widthConstraint = Constraints(maxWidth = constraints.maxWidth)
                        fun alignment(value: String) = when (value) {
                            Constants.LEFT -> TextAlign.Start
                            Constants.RIGHT -> TextAlign.End
                            else -> TextAlign.Center
                        }
                        fun textStyle(item: BibleTranslationSettings): TextStyle {
                            val shadowEnabled = item.textShadow
                            val shadow = if (shadowEnabled) scaleElementShadow(
                                item.textShadowColor,
                                item.textShadowSize,
                                item.textShadowOpacity,
                            ) else null
                            return TextStyle(
                                fontWeight = if (item.textBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (item.textItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (item.textUnderline) TextDecoration.Underline else TextDecoration.None,
                                shadow = shadow,
                            )
                        }
                        fun referenceStyle(item: BibleTranslationSettings): TextStyle {
                            val shadowEnabled = item.referenceShadow
                            val shadow = if (shadowEnabled) scaleElementShadow(
                                item.referenceShadowColor,
                                item.referenceShadowSize,
                                item.referenceShadowOpacity,
                            ) else null
                            return TextStyle(
                                fontWeight = if (item.referenceBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (item.referenceItalic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (item.referenceUnderline) TextDecoration.Underline else TextDecoration.None,
                                shadow = shadow,
                            )
                        }
                        fun totalHeight(scale: Float): Int {
                            val contentHeight = visible.sumOf { (verse, item) ->
                                val textSize = (item.textFontSize * scaleFactor * scale).sp
                                val refSize = (item.referenceFontSize * scaleFactor * scale).sp
                                val textFont = systemFontFamilyOrDefault(item.textFontType)
                                val refFont = systemFontFamilyOrDefault(item.referenceFontType)
                                textMeasurer.measure(verse.verseText, textStyle(item).copy(fontFamily = textFont, fontSize = textSize), constraints = widthConstraint).size.height +
                                    textMeasurer.measure(buildRefText(verse, item.showAbbreviation), referenceStyle(item).copy(fontFamily = refFont, fontSize = refSize), constraints = widthConstraint).size.height
                            }
                            val gapCount = (visible.size - 1).coerceAtLeast(0)
                            val gapHeight = with(density) {
                                (bs.multiTranslationSpacing * scale).dp.roundToPx()
                            }
                            val dividerHeight = if (bs.multiTranslationDivider) {
                                with(density) { 1.dp.roundToPx() }
                            } else {
                                0
                            }
                            return contentHeight + gapCount * (gapHeight + dividerHeight)
                        }
                        val fitScale = if (totalHeight(1f) > constraints.maxHeight) {
                            // A fixed 15% floor was enough for two translations but could leave the
                            // fourth block below the clip boundary. Parallel stacks prioritise
                            // keeping every configured translation on screen.
                            binarySearchFitScale(minScale = 0.03f, iterations = 10) { scale ->
                                totalHeight(scale) <= constraints.maxHeight
                            }
                        } else 1f
                        Column(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            visible.forEachIndexed { index, (verse, item) ->
                                val textSize = (item.textFontSize * scaleFactor * fitScale).sp
                                val refSize = (item.referenceFontSize * scaleFactor * fitScale).sp
                                val textFont = systemFontFamilyOrDefault(item.textFontType)
                                val refFont = systemFontFamilyOrDefault(item.referenceFontType)
                                val textColor = if (isKey) Color.White else parseHexColor(item.textColor)
                                val refColor = if (isKey) Color.White else parseHexColor(item.referenceColor)
                                val textAlign = alignment(item.textHorizontalAlignment)
                                val refAlign = alignment(item.referenceHorizontalAlignment)
                                val refPosition = item.referencePosition
                                if (refPosition == Constants.POSITION_ABOVE) {
                                    Text(buildRefText(verse, item.showAbbreviation), Modifier.fillMaxWidth(), color = refColor, fontFamily = refFont, fontSize = refSize, textAlign = refAlign, style = referenceStyle(item))
                                }
                                Text(verse.verseText, Modifier.fillMaxWidth(), color = textColor, fontFamily = textFont, fontSize = textSize, textAlign = textAlign, style = textStyle(item))
                                if (refPosition == Constants.POSITION_BELOW) {
                                    Text(buildRefText(verse, item.showAbbreviation), Modifier.fillMaxWidth(), color = refColor, fontFamily = refFont, fontSize = refSize, textAlign = refAlign, style = referenceStyle(item))
                                }
                                if (index < visible.lastIndex) {
                                    Spacer(
                                        modifier = Modifier.height(
                                            (bs.multiTranslationSpacing * fitScale / 2f).dp,
                                        ),
                                    )
                                    if (bs.multiTranslationDivider) {
                                        HorizontalDivider(
                                            color = if (isKey) {
                                                Color.White
                                            } else {
                                                Color.White.copy(alpha = 0.45f)
                                            },
                                            thickness = 1.dp,
                                        )
                                    }
                                    Spacer(
                                        modifier = Modifier.height(
                                            (bs.multiTranslationSpacing * fitScale / 2f).dp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    return
                }

                // isLowerThirdVertical forces bilingual/parallel content to stack (one below the
                // other) instead of the side-by-side Row split below — same band/geometry as
                // horizontal otherwise, see the routing to the single-column "else" branch.
                if (showParallelLayout && isLowerThird && !isLowerThirdVertical) {
                    val sec = secondary
                    // Lower third: side-by-side Row layout (50/50) with matched auto-fit
                    BoxWithConstraints(
                        modifier = innerModifier,
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Pre-compute fit scales for both halves, then use the min so fonts match
                        val halfWidth = (constraints.maxWidth - with(density) { 16.dp.roundToPx() }) / 2
                        val halfConstraint = Constraints(maxWidth = halfWidth.coerceAtLeast(1))
                        // Use 90% of available height as safety margin for line spacing/shadow/padding offsets
                        val availH = (constraints.maxHeight * 0.90f).toInt()

                        val primaryRefText = buildRefText(primary, appSettings.bibleSettings.primaryShowAbbreviation)
                        val secondaryRefText = buildRefText(sec, appSettings.bibleSettings.secondaryShowAbbreviation)

                        // Binary search for the largest scale where both primary and secondary fit
                        // Scale both verse AND reference text together so everything shrinks proportionally
                        val initialPRefH = textMeasurer.measure(primaryRefText, primaryReferenceTextStyle.copy(fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize), constraints = halfConstraint).size.height
                        val initialSRefH = textMeasurer.measure(secondaryRefText, secondaryReferenceTextStyle.copy(fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize), constraints = halfConstraint).size.height
                        val initialPH = textMeasurer.measure(primary.verseText, primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize), constraints = halfConstraint).size.height
                        val initialSH = textMeasurer.measure(sec.verseText, secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize), constraints = halfConstraint).size.height
                        val needsScaling = (initialPRefH + initialPH > availH) || (initialSRefH + initialSH > availH)

                        val matchedScale = if (needsScaling) {
                            binarySearchFitScale { scale ->
                                val pRefH = textMeasurer.measure(primaryRefText, primaryReferenceTextStyle.copy(fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize * scale), constraints = halfConstraint).size.height
                                val sRefH = textMeasurer.measure(secondaryRefText, secondaryReferenceTextStyle.copy(fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize * scale), constraints = halfConstraint).size.height
                                val pH = textMeasurer.measure(primary.verseText, primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize * scale), constraints = halfConstraint).size.height
                                val sH = textMeasurer.measure(sec.verseText, secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize * scale), constraints = halfConstraint).size.height
                                (pRefH + pH <= availH) && (sRefH + sH <= availH)
                            }
                        } else 1f
                        val pBibleSize = scaledPrimaryBibleSize * matchedScale
                        val sBibleSize = scaledSecondaryBibleSize * matchedScale
                        // Use the smaller of the two so both sides display at the same visual size
                        val matchedBibleSize = if (sBibleSize.value < pBibleSize.value) sBibleSize else pBibleSize
                        val scaledPrimaryRefSize = scaledPrimaryReferenceSize * matchedScale
                        val scaledSecondaryRefSize = scaledSecondaryReferenceSize * matchedScale

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left half: primary bible
                            Column(Modifier.weight(1f).fillMaxHeight().wrapContentHeight(Alignment.Bottom)) {
                                if (primaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryRefSize, text = primaryRefText, color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                                }
                                Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleHorizontalAlignment, fontFamily = primaryBibleFontStyle, fontSize = matchedBibleSize, text = primary.verseText, color = primaryBibleTextColor, style = primaryBibleTextStyleScaled)
                                if (primaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryRefSize, text = primaryRefText, color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                                }
                            }
                            // Right half: secondary bible
                            Column(Modifier.weight(1f).fillMaxHeight().wrapContentHeight(Alignment.Bottom)) {
                                if (secondaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryRefSize, text = secondaryRefText, color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                }
                                Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleHorizontalAlignment, fontFamily = secondaryBibleFontStyle, fontSize = matchedBibleSize, text = sec.verseText, color = secondaryBibleTextColor, style = secondaryBibleTextStyleScaled)
                                if (secondaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryRefSize, text = secondaryRefText, color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                }
                            }
                        }
                    }
                } else if (showParallelLayout && !isLowerThird) {
                    // Parallel layout: rigid 50/50 split with matched auto-fit — fullscreen only;
                    // lower-third (either orientation) falls through to the single-column branch
                    // below, which is constrained to the band via innerModifier.
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val widthConstraint = Constraints(maxWidth = constraints.maxWidth)
                        val halfH = constraints.maxHeight / 2

                        val primaryRefText = buildRefText(primary, appSettings.bibleSettings.primaryShowAbbreviation)
                        val primaryRefH = textMeasurer.measure(primaryRefText, primaryReferenceTextStyle.copy(fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize), constraints = widthConstraint).size.height
                        val secondaryRefText = buildRefText(secondary, appSettings.bibleSettings.secondaryShowAbbreviation)
                        val secondaryRefH = textMeasurer.measure(secondaryRefText, secondaryReferenceTextStyle.copy(fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize), constraints = widthConstraint).size.height

                        // Binary search for the largest scale where both primary and secondary fit their halves
                        val initialPH = textMeasurer.measure(primary.verseText, primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize), constraints = widthConstraint).size.height
                        val initialSH = textMeasurer.measure(secondary.verseText, secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize), constraints = widthConstraint).size.height
                        val needsScaling = (primaryRefH + initialPH > halfH) || (secondaryRefH + initialSH > halfH)

                        val matchedScale = if (needsScaling) {
                            binarySearchFitScale { scale ->
                                val pH = textMeasurer.measure(primary.verseText, primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize * scale), constraints = widthConstraint).size.height
                                val sH = textMeasurer.measure(secondary.verseText, secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize * scale), constraints = widthConstraint).size.height
                                (primaryRefH + pH <= halfH) && (secondaryRefH + sH <= halfH)
                            }
                        } else 1f
                        val pBibleSize = scaledPrimaryBibleSize * matchedScale
                        val sBibleSize = scaledSecondaryBibleSize * matchedScale
                        // Use the smaller of the two so both halves display at the same visual size
                        val matchedBibleSize = if (sBibleSize.value < pBibleSize.value) sBibleSize else pBibleSize

                        Column(modifier = Modifier.fillMaxSize()) {
                            // ── Top half: primary bible ──
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = contentAlignment
                            ) {
                                Column(Modifier.fillMaxWidth().wrapContentHeight()) {
                                    if (primaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                        Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize, text = primaryRefText, color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                                    }
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleHorizontalAlignment, fontFamily = primaryBibleFontStyle, fontSize = matchedBibleSize, text = primary.verseText, color = primaryBibleTextColor, style = primaryBibleTextStyleScaled)
                                    if (primaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                        Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize, text = primaryRefText, color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                                    }
                                }
                            }
                            // ── Bottom half: secondary bible ──
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = contentAlignment
                            ) {
                                Column(Modifier.fillMaxWidth().wrapContentHeight()) {
                                    if (secondaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                        Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize, text = secondaryRefText, color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                    }
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleHorizontalAlignment, fontFamily = secondaryBibleFontStyle, fontSize = matchedBibleSize, text = secondary.verseText, color = secondaryBibleTextColor, style = secondaryBibleTextStyleScaled)
                                    if (secondaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                        Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize, text = secondaryRefText, color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Original single-column layout (no secondary, or lower-third)
                    BoxWithConstraints(
                        modifier = innerModifier,
                        contentAlignment = if (isLowerThird) Alignment.BottomCenter else contentAlignment
                    ) {
                        // Auto-scale bible text if it overflows the available height
                        val widthConstraint = Constraints(maxWidth = constraints.maxWidth)
                        val primaryRefText = buildRefText(primary, appSettings.bibleSettings.primaryShowAbbreviation)
                        val primaryRefH = textMeasurer.measure(
                            text = primaryRefText,
                            style = primaryReferenceTextStyle.copy(fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize),
                            constraints = widthConstraint
                        ).size.height
                        val primaryVerseH = textMeasurer.measure(
                            text = primary.verseText,
                            style = primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize),
                            constraints = widthConstraint
                        ).size.height

                        val secondaryRefH = if (showSecondary) {
                            val secRefText = buildRefText(secondary, appSettings.bibleSettings.secondaryShowAbbreviation)
                            textMeasurer.measure(
                                text = secRefText,
                                style = secondaryReferenceTextStyle.copy(fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize),
                                constraints = widthConstraint
                            ).size.height
                        } else 0
                        val secondaryVerseH = if (showSecondary) {
                            textMeasurer.measure(
                                text = secondary.verseText,
                                style = secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize),
                                constraints = widthConstraint
                            ).size.height
                        } else 0

                        val totalH = primaryRefH + primaryVerseH + secondaryRefH + secondaryVerseH
                        val fixedH = primaryRefH + secondaryRefH
                        val maxH = constraints.maxHeight
                        val fitScale = if (totalH > maxH) {
                            binarySearchFitScale { scale ->
                                val pH = textMeasurer.measure(primary.verseText, primaryBibleTextStyle.copy(fontFamily = primaryBibleFontStyle, fontSize = scaledPrimaryBibleSize * scale), constraints = widthConstraint).size.height
                                val sH = if (showSecondary) {
                                    textMeasurer.measure(secondary.verseText, secondaryBibleTextStyle.copy(fontFamily = secondaryBibleFontStyle, fontSize = scaledSecondaryBibleSize * scale), constraints = widthConstraint).size.height
                                } else 0
                                fixedH + pH + sH <= maxH
                            }
                        } else 1f
                        val fittedPrimaryBibleSize = scaledPrimaryBibleSize * fitScale
                        val fittedSecondaryBibleSize = scaledSecondaryBibleSize * fitScale
                        // Use the smaller so both primary and secondary display at the same visual size
                        val matchedFittedSize = if (showSecondary && fittedSecondaryBibleSize.value < fittedPrimaryBibleSize.value) fittedSecondaryBibleSize else fittedPrimaryBibleSize

                        Column(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            verticalArrangement = if (isLowerThird) Arrangement.Bottom else Arrangement.Top
                        ) {
                            if (primaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                val bookNameOrAbbr = if (appSettings.bibleSettings.primaryShowAbbreviation && primary.bibleAbbreviation.isNotEmpty()) primary.bibleAbbreviation else ""
                                Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize, text = "$bookNameOrAbbr ${primary.bookName} ${primary.chapter}:$primaryVerseRef", color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                            }
                            Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleHorizontalAlignment, fontFamily = primaryBibleFontStyle, fontSize = matchedFittedSize, text = primary.verseText, color = primaryBibleTextColor, style = primaryBibleTextStyleScaled)
                            if (primaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                val bookNameOrAbbr = if (appSettings.bibleSettings.primaryShowAbbreviation && primary.bibleAbbreviation.isNotEmpty()) primary.bibleAbbreviation else ""
                                Text(modifier = Modifier.fillMaxWidth(), textAlign = primaryBibleReferenceHorizontalAlignment, fontFamily = primaryBibleReferenceFontStyle, fontSize = scaledPrimaryReferenceSize, text = "$bookNameOrAbbr ${primary.bookName} ${primary.chapter}:$primaryVerseRef", color = primaryBibleReferenceTextColor, style = primaryReferenceTextStyleScaled)
                            }
                            if (showSecondary) {
                                if (secondaryBibleReferencePosition == Constants.POSITION_ABOVE) {
                                    val bookNameOrAbbr = if (appSettings.bibleSettings.secondaryShowAbbreviation && secondary.bibleAbbreviation.isNotEmpty()) secondary.bibleAbbreviation else ""
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize, text = "$bookNameOrAbbr ${secondary.bookName} ${secondary.chapter}:$secondaryVerseRef", color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                }
                                Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleHorizontalAlignment, fontFamily = secondaryBibleFontStyle, fontSize = matchedFittedSize, text = secondary.verseText, color = secondaryBibleTextColor, style = secondaryBibleTextStyleScaled)
                                if (secondaryBibleReferencePosition == Constants.POSITION_BELOW) {
                                    val bookNameOrAbbr = if (appSettings.bibleSettings.secondaryShowAbbreviation && secondary.bibleAbbreviation.isNotEmpty()) secondary.bibleAbbreviation else ""
                                    Text(modifier = Modifier.fillMaxWidth(), textAlign = secondaryBibleReferenceHorizontalAlignment, fontFamily = secondaryBibleReferenceFontStyle, fontSize = scaledSecondaryReferenceSize, text = "$bookNameOrAbbr ${secondary.bookName} ${secondary.chapter}:$secondaryVerseRef", color = secondaryBibleReferenceTextColor, style = secondaryReferenceTextStyleScaled)
                                }
                            }
                        }
                    }
                }
            }

            if (crossfadeEnabled || appSettings.bibleSettings.fadeIn || appSettings.bibleSettings.fadeOut) {
                // Transition system:
                // - Fade in: first appearance fades from transparent
                // - Crossfade: switching verses blends old/new simultaneously
                // - Fade out: handled externally when clearing display
                val duration = bs.transitionDuration.toInt().coerceAtLeast(100)
                val isCrossfade = crossfadeEnabled
                var displayedCurrent by remember { mutableStateOf(effectiveVerses) }
                var displayedPrevious by remember { mutableStateOf<List<SelectedVerse>>(emptyList()) }
                var currentAlpha by remember { mutableStateOf(1f) }
                var previousAlpha by remember { mutableStateOf(0f) }
                val pendingQueue = remember { kotlinx.coroutines.channels.Channel<List<SelectedVerse>>(kotlinx.coroutines.channels.Channel.CONFLATED) }

                // Queue verse changes
                LaunchedEffect(effectiveVerses) {
                    if (displayedCurrent != effectiveVerses) {
                        pendingQueue.send(effectiveVerses)
                    }
                }

                // Process verse switches (crossfade between verses)
                LaunchedEffect(Unit) {
                    for (nextVerses in pendingQueue) {
                        if (displayedCurrent == nextVerses) continue

                        if (isCrossfade) {
                            // Crossfade: both layers animate simultaneously
                            displayedPrevious = displayedCurrent
                            displayedCurrent = nextVerses
                            previousAlpha = 1f
                            currentAlpha = 0f
                            val anim = Animatable(0f)
                            anim.animateTo(1f, tween(durationMillis = duration)) {
                                currentAlpha = this.value
                                previousAlpha = 1f - this.value
                            }
                        } else {
                            // No crossfade — just swap instantly
                            displayedCurrent = nextVerses
                        }
                        currentAlpha = 1f
                        previousAlpha = 0f
                        displayedPrevious = emptyList()
                    }
                }

                // transitionAlpha handles fade out (driven from main.kt when clearing display)
                Box(modifier = Modifier.matchParentSize().graphicsLayer { alpha = transitionAlpha }) {
                    if (displayedPrevious.isNotEmpty() && previousAlpha > 0f) {
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
                    TextContent(effectiveVerses)
                }
            }
        }
    }
}
