package org.churchpresenter.app.churchpresenter.data.settings

import kotlinx.serialization.Serializable
import org.churchpresenter.app.churchpresenter.utils.Constants

/**
 * One independently configurable translation in the parallel Bible stack.
 *
 * Multi-translation appearance profile. Lower-third fields remain readable for settings files
 * written by development builds that briefly exposed them, but multi mode intentionally ignores
 * those fields.
 */
@Serializable
data class BibleTranslationSettings(
    val fileName: String = "",
    val textColor: String = "#FFFFFF",
    val textFontType: String = "Arial",
    val textFontSize: Int = 70,
    val lowerThirdTextFontSize: Int = 28,
    val textHorizontalAlignment: String = Constants.LEFT,
    val lowerThirdTextHorizontalAlignment: String = Constants.LEFT,
    val lowerThirdEnabled: Boolean = true,
    val textBold: Boolean = false,
    val textItalic: Boolean = false,
    val textUnderline: Boolean = false,
    val textShadow: Boolean = false,
    val lowerThirdTextColor: String = "#FFFFFF",
    val lowerThirdTextFontType: String = "Arial",
    val lowerThirdTextBold: Boolean = false,
    val lowerThirdTextItalic: Boolean = false,
    val lowerThirdTextUnderline: Boolean = false,
    val lowerThirdTextShadow: Boolean = false,
    val referenceColor: String = "#FFFFFF",
    val referenceFontType: String = "Arial",
    val referenceFontSize: Int = 70,
    val lowerThirdReferenceFontSize: Int = 24,
    val referencePosition: String = "Below",
    val lowerThirdReferencePosition: String = "Below",
    val referenceHorizontalAlignment: String = Constants.RIGHT,
    val lowerThirdReferenceHorizontalAlignment: String = Constants.RIGHT,
    val showAbbreviation: Boolean = false,
    val referenceBold: Boolean = false,
    val referenceItalic: Boolean = false,
    val referenceUnderline: Boolean = false,
    val referenceShadow: Boolean = false,
    val lowerThirdReferenceColor: String = "#FFFFFF",
    val lowerThirdReferenceFontType: String = "Arial",
    val lowerThirdReferenceBold: Boolean = false,
    val lowerThirdReferenceItalic: Boolean = false,
    val lowerThirdReferenceUnderline: Boolean = false,
    val lowerThirdReferenceShadow: Boolean = false,
    val textShadowColor: String = "#000000",
    val textShadowSize: Int = 100,
    val textShadowOpacity: Int = 90,
    val lowerThirdTextShadowColor: String = "#000000",
    val lowerThirdTextShadowSize: Int = 100,
    val lowerThirdTextShadowOpacity: Int = 90,
    val referenceShadowColor: String = "#000000",
    val referenceShadowSize: Int = 100,
    val referenceShadowOpacity: Int = 90,
    val lowerThirdReferenceShadowColor: String = "#000000",
    val lowerThirdReferenceShadowSize: Int = 100,
    val lowerThirdReferenceShadowOpacity: Int = 90,
)

@Serializable
data class BibleSettings(
    // Bible file management
    val storageDirectory: String = "",
    val bibleFiles: List<String> = emptyList(),

    // Bible selection
    val primaryBible: String = "",
    val secondaryBible: String = "",
    /** Multi-mode source of truth, stored separately from the legacy primary/secondary fields. */
    val translations: List<BibleTranslationSettings> = emptyList(),
    val multiTranslationMode: Boolean = false,
    val multiTranslationSpacing: Int = 24,
    val multiTranslationDivider: Boolean = false,

    // Bible tab column widths (dp); 0 = use default
    val bibleColWidthBook: Int = 200,
    val bibleColWidthChapter: Int = 120,

    // Global vertical alignment (affects all 4 sections)
    val verticalAlignment: String = Constants.BOTTOM,

    // Primary Bible text
    val primaryBibleColor: String = "#FFFFFF",
    val primaryBibleFontType: String = "Arial",
    val primaryBibleFontSize: Int = 70,
    val primaryBibleLowerThirdFontSize: Int = 32,
    val primaryBibleHorizontalAlignment: String = Constants.LEFT,
    val primaryBibleLowerThirdHorizontalAlignment: String = Constants.LEFT,
    val primaryBibleBold: Boolean = false,
    val primaryBibleItalic: Boolean = false,
    val primaryBibleUnderline: Boolean = false,
    val primaryBibleShadow: Boolean = false,
    val primaryBibleLowerThirdColor: String = "#FFFFFF",
    val primaryBibleLowerThirdFontType: String = "Arial",
    val primaryBibleLowerThirdBold: Boolean = false,
    val primaryBibleLowerThirdItalic: Boolean = false,
    val primaryBibleLowerThirdUnderline: Boolean = false,
    val primaryBibleLowerThirdShadow: Boolean = false,

    // Primary Bible book reference
    val primaryReferenceColor: String = "#FFFFFF",
    val primaryReferenceFontType: String = "Arial",
    val primaryReferenceFontSize: Int = 70,
    val primaryReferenceLowerThirdFontSize: Int = 24,
    val primaryReferencePosition: String = "Below", // "Above" or "Below"
    val primaryReferenceLowerThirdPosition: String = "Below",
    val primaryReferenceHorizontalAlignment: String = Constants.RIGHT,
    val primaryReferenceLowerThirdHorizontalAlignment: String = Constants.RIGHT,
    val primaryShowAbbreviation: Boolean = false,
    val primaryReferenceBold: Boolean = false,
    val primaryReferenceItalic: Boolean = false,
    val primaryReferenceUnderline: Boolean = false,
    val primaryReferenceShadow: Boolean = false,
    val primaryReferenceLowerThirdColor: String = "#FFFFFF",
    val primaryReferenceLowerThirdFontType: String = "Arial",
    val primaryReferenceLowerThirdBold: Boolean = false,
    val primaryReferenceLowerThirdItalic: Boolean = false,
    val primaryReferenceLowerThirdUnderline: Boolean = false,
    val primaryReferenceLowerThirdShadow: Boolean = false,

    // Secondary Bible text
    val secondaryBibleColor: String = "#FFFFFF",
    val secondaryBibleFontType: String = "Arial",
    val secondaryBibleFontSize: Int = 70,
    val secondaryBibleLowerThirdFontSize: Int = 28,
    val secondaryBibleHorizontalAlignment: String = Constants.LEFT,
    val secondaryBibleLowerThirdHorizontalAlignment: String = Constants.LEFT,
    val secondaryBibleLowerThirdEnabled: Boolean = true,
    val secondaryBibleBold: Boolean = false,
    val secondaryBibleItalic: Boolean = false,
    val secondaryBibleUnderline: Boolean = false,
    val secondaryBibleShadow: Boolean = false,
    val secondaryBibleLowerThirdColor: String = "#FFFFFF",
    val secondaryBibleLowerThirdFontType: String = "Arial",
    val secondaryBibleLowerThirdBold: Boolean = false,
    val secondaryBibleLowerThirdItalic: Boolean = false,
    val secondaryBibleLowerThirdUnderline: Boolean = false,
    val secondaryBibleLowerThirdShadow: Boolean = false,

    // Secondary Bible book reference
    val secondaryReferenceColor: String = "#FFFFFF",
    val secondaryReferenceFontType: String = "Arial",
    val secondaryReferenceFontSize: Int = 70,
    val secondaryReferenceLowerThirdFontSize: Int = 24,
    val secondaryReferencePosition: String = "Below", // "Above" or "Below"
    val secondaryReferenceLowerThirdPosition: String = "Below",
    val secondaryReferenceHorizontalAlignment: String = Constants.RIGHT,
    val secondaryReferenceLowerThirdHorizontalAlignment: String = Constants.RIGHT,
    val secondaryShowAbbreviation: Boolean = false,
    val secondaryReferenceBold: Boolean = false,
    val secondaryReferenceItalic: Boolean = false,
    val secondaryReferenceUnderline: Boolean = false,
    val secondaryReferenceShadow: Boolean = false,
    val secondaryReferenceLowerThirdColor: String = "#FFFFFF",
    val secondaryReferenceLowerThirdFontType: String = "Arial",
    val secondaryReferenceLowerThirdBold: Boolean = false,
    val secondaryReferenceLowerThirdItalic: Boolean = false,
    val secondaryReferenceLowerThirdUnderline: Boolean = false,
    val secondaryReferenceLowerThirdShadow: Boolean = false,

    // Language for captions
    val captionLanguage: String = "Interface", // "Interface" or "Database"

    // Text margins (additional padding inside global projection offsets)
    val marginTop: Int = 54,
    val marginBottom: Int = 54,
    val marginLeft: Int = 96,
    val marginRight: Int = 96,

    // Shadow customization — per-element
    val primaryBibleShadowColor: String = "#000000",
    val primaryBibleShadowSize: Int = 100,
    val primaryBibleShadowOpacity: Int = 90,
    val primaryBibleLowerThirdShadowColor: String = "#000000",
    val primaryBibleLowerThirdShadowSize: Int = 100,
    val primaryBibleLowerThirdShadowOpacity: Int = 90,

    val primaryReferenceShadowColor: String = "#000000",
    val primaryReferenceShadowSize: Int = 100,
    val primaryReferenceShadowOpacity: Int = 90,
    val primaryReferenceLowerThirdShadowColor: String = "#000000",
    val primaryReferenceLowerThirdShadowSize: Int = 100,
    val primaryReferenceLowerThirdShadowOpacity: Int = 90,

    val secondaryBibleShadowColor: String = "#000000",
    val secondaryBibleShadowSize: Int = 100,
    val secondaryBibleShadowOpacity: Int = 90,
    val secondaryBibleLowerThirdShadowColor: String = "#000000",
    val secondaryBibleLowerThirdShadowSize: Int = 100,
    val secondaryBibleLowerThirdShadowOpacity: Int = 90,

    val secondaryReferenceShadowColor: String = "#000000",
    val secondaryReferenceShadowSize: Int = 100,
    val secondaryReferenceShadowOpacity: Int = 90,
    val secondaryReferenceLowerThirdShadowColor: String = "#000000",
    val secondaryReferenceLowerThirdShadowSize: Int = 100,
    val secondaryReferenceLowerThirdShadowOpacity: Int = 90,

    // Transition animation
    val fadeIn: Boolean = true,
    val fadeOut: Boolean = true,
    val crossfade: Boolean = false,
    val transitionDuration: Float = 500f,
    val splitBrowseMode: Boolean = false,
    val splitLivePanelWidth: Int = 300,
) {
    /** Returns the translations used by the currently selected presentation mode. */
    fun translationList(): List<BibleTranslationSettings> =
        if (multiTranslationMode) multiTranslationList() else legacyTranslationList()

    /** Stable key used by the Bible UI to detect when its loaded module set must be refreshed. */
    fun translationSelectionKey(): Pair<Boolean, List<String>> =
        multiTranslationMode to translationList().map { it.fileName }

    fun withMultiTranslationMode(enabled: Boolean): BibleSettings = copy(
        multiTranslationMode = enabled,
        translations = if (enabled && translations.isEmpty()) legacyTranslationList() else translations,
    )

    private fun legacyTranslationList(): List<BibleTranslationSettings> =
        buildList {
            if (primaryBible.isNotEmpty()) add(primaryTranslation())
            if (secondaryBible.isNotEmpty()) add(secondaryTranslation())
        }

    private fun multiTranslationList(): List<BibleTranslationSettings> = translations

    fun withTranslations(value: List<BibleTranslationSettings>): BibleSettings {
        val cleaned = value.filter { it.fileName.isNotBlank() }.distinctBy { it.fileName }
        return copy(translations = cleaned, multiTranslationMode = true)
    }

    fun updateTranslation(index: Int, transform: (BibleTranslationSettings) -> BibleTranslationSettings): BibleSettings {
        val current = multiTranslationList()
        if (index !in current.indices) return this
        return withTranslations(current.toMutableList().also { it[index] = transform(it[index]) })
    }

    fun addTranslation(fileName: String): BibleSettings {
        if (fileName.isBlank() || multiTranslationList().any { it.fileName == fileName }) return this
        return withTranslations(multiTranslationList() + BibleTranslationSettings(fileName = fileName))
    }

    fun removeTranslation(index: Int): BibleSettings =
        withTranslations(multiTranslationList().filterIndexed { itemIndex, _ -> itemIndex != index })

    fun moveTranslation(index: Int, offset: Int): BibleSettings {
        val target = index + offset
        val current = multiTranslationList()
        if (index !in current.indices || target !in current.indices) return this
        return withTranslations(current.toMutableList().also {
            val item = it.removeAt(index)
            it.add(target, item)
        })
    }

    private fun primaryTranslation() = BibleTranslationSettings(
        fileName = primaryBible,
        textColor = primaryBibleColor, textFontType = primaryBibleFontType,
        textFontSize = primaryBibleFontSize, lowerThirdTextFontSize = primaryBibleLowerThirdFontSize,
        textHorizontalAlignment = primaryBibleHorizontalAlignment,
        lowerThirdTextHorizontalAlignment = primaryBibleLowerThirdHorizontalAlignment,
        textBold = primaryBibleBold, textItalic = primaryBibleItalic,
        textUnderline = primaryBibleUnderline, textShadow = primaryBibleShadow,
        lowerThirdTextColor = primaryBibleLowerThirdColor,
        lowerThirdTextFontType = primaryBibleLowerThirdFontType,
        lowerThirdTextBold = primaryBibleLowerThirdBold, lowerThirdTextItalic = primaryBibleLowerThirdItalic,
        lowerThirdTextUnderline = primaryBibleLowerThirdUnderline, lowerThirdTextShadow = primaryBibleLowerThirdShadow,
        referenceColor = primaryReferenceColor, referenceFontType = primaryReferenceFontType,
        referenceFontSize = primaryReferenceFontSize,
        lowerThirdReferenceFontSize = primaryReferenceLowerThirdFontSize,
        referencePosition = primaryReferencePosition,
        lowerThirdReferencePosition = primaryReferenceLowerThirdPosition,
        referenceHorizontalAlignment = primaryReferenceHorizontalAlignment,
        lowerThirdReferenceHorizontalAlignment = primaryReferenceLowerThirdHorizontalAlignment,
        showAbbreviation = primaryShowAbbreviation,
        referenceBold = primaryReferenceBold, referenceItalic = primaryReferenceItalic,
        referenceUnderline = primaryReferenceUnderline, referenceShadow = primaryReferenceShadow,
        lowerThirdReferenceColor = primaryReferenceLowerThirdColor,
        lowerThirdReferenceFontType = primaryReferenceLowerThirdFontType,
        lowerThirdReferenceBold = primaryReferenceLowerThirdBold,
        lowerThirdReferenceItalic = primaryReferenceLowerThirdItalic,
        lowerThirdReferenceUnderline = primaryReferenceLowerThirdUnderline,
        lowerThirdReferenceShadow = primaryReferenceLowerThirdShadow,
        textShadowColor = primaryBibleShadowColor, textShadowSize = primaryBibleShadowSize,
        textShadowOpacity = primaryBibleShadowOpacity,
        lowerThirdTextShadowColor = primaryBibleLowerThirdShadowColor,
        lowerThirdTextShadowSize = primaryBibleLowerThirdShadowSize,
        lowerThirdTextShadowOpacity = primaryBibleLowerThirdShadowOpacity,
        referenceShadowColor = primaryReferenceShadowColor,
        referenceShadowSize = primaryReferenceShadowSize, referenceShadowOpacity = primaryReferenceShadowOpacity,
        lowerThirdReferenceShadowColor = primaryReferenceLowerThirdShadowColor,
        lowerThirdReferenceShadowSize = primaryReferenceLowerThirdShadowSize,
        lowerThirdReferenceShadowOpacity = primaryReferenceLowerThirdShadowOpacity,
    )

    private fun secondaryTranslation() = BibleTranslationSettings(
        fileName = secondaryBible,
        textColor = secondaryBibleColor, textFontType = secondaryBibleFontType,
        textFontSize = secondaryBibleFontSize, lowerThirdTextFontSize = secondaryBibleLowerThirdFontSize,
        textHorizontalAlignment = secondaryBibleHorizontalAlignment,
        lowerThirdTextHorizontalAlignment = secondaryBibleLowerThirdHorizontalAlignment,
        lowerThirdEnabled = secondaryBibleLowerThirdEnabled,
        textBold = secondaryBibleBold, textItalic = secondaryBibleItalic,
        textUnderline = secondaryBibleUnderline, textShadow = secondaryBibleShadow,
        lowerThirdTextColor = secondaryBibleLowerThirdColor,
        lowerThirdTextFontType = secondaryBibleLowerThirdFontType,
        lowerThirdTextBold = secondaryBibleLowerThirdBold, lowerThirdTextItalic = secondaryBibleLowerThirdItalic,
        lowerThirdTextUnderline = secondaryBibleLowerThirdUnderline, lowerThirdTextShadow = secondaryBibleLowerThirdShadow,
        referenceColor = secondaryReferenceColor, referenceFontType = secondaryReferenceFontType,
        referenceFontSize = secondaryReferenceFontSize,
        lowerThirdReferenceFontSize = secondaryReferenceLowerThirdFontSize,
        referencePosition = secondaryReferencePosition,
        lowerThirdReferencePosition = secondaryReferenceLowerThirdPosition,
        referenceHorizontalAlignment = secondaryReferenceHorizontalAlignment,
        lowerThirdReferenceHorizontalAlignment = secondaryReferenceLowerThirdHorizontalAlignment,
        showAbbreviation = secondaryShowAbbreviation,
        referenceBold = secondaryReferenceBold, referenceItalic = secondaryReferenceItalic,
        referenceUnderline = secondaryReferenceUnderline, referenceShadow = secondaryReferenceShadow,
        lowerThirdReferenceColor = secondaryReferenceLowerThirdColor,
        lowerThirdReferenceFontType = secondaryReferenceLowerThirdFontType,
        lowerThirdReferenceBold = secondaryReferenceLowerThirdBold,
        lowerThirdReferenceItalic = secondaryReferenceLowerThirdItalic,
        lowerThirdReferenceUnderline = secondaryReferenceLowerThirdUnderline,
        lowerThirdReferenceShadow = secondaryReferenceLowerThirdShadow,
        textShadowColor = secondaryBibleShadowColor, textShadowSize = secondaryBibleShadowSize,
        textShadowOpacity = secondaryBibleShadowOpacity,
        lowerThirdTextShadowColor = secondaryBibleLowerThirdShadowColor,
        lowerThirdTextShadowSize = secondaryBibleLowerThirdShadowSize,
        lowerThirdTextShadowOpacity = secondaryBibleLowerThirdShadowOpacity,
        referenceShadowColor = secondaryReferenceShadowColor,
        referenceShadowSize = secondaryReferenceShadowSize, referenceShadowOpacity = secondaryReferenceShadowOpacity,
        lowerThirdReferenceShadowColor = secondaryReferenceLowerThirdShadowColor,
        lowerThirdReferenceShadowSize = secondaryReferenceLowerThirdShadowSize,
        lowerThirdReferenceShadowOpacity = secondaryReferenceLowerThirdShadowOpacity,
    )

    /** Returns a copy with primary and secondary bible settings swapped. */
    fun swapped() = if (multiTranslationMode) {
        moveTranslation(0, 1)
    } else copy(
        primaryBible = secondaryBible,
        secondaryBible = primaryBible,
        primaryBibleColor = secondaryBibleColor,
        primaryBibleFontType = secondaryBibleFontType,
        primaryBibleFontSize = secondaryBibleFontSize,
        primaryBibleLowerThirdFontSize = secondaryBibleLowerThirdFontSize,
        primaryBibleHorizontalAlignment = secondaryBibleHorizontalAlignment,
        primaryBibleLowerThirdHorizontalAlignment = secondaryBibleLowerThirdHorizontalAlignment,
        primaryBibleBold = secondaryBibleBold,
        primaryBibleItalic = secondaryBibleItalic,
        primaryBibleUnderline = secondaryBibleUnderline,
        primaryBibleShadow = secondaryBibleShadow,
        primaryBibleShadowColor = secondaryBibleShadowColor,
        primaryBibleShadowSize = secondaryBibleShadowSize,
        primaryBibleShadowOpacity = secondaryBibleShadowOpacity,
        primaryBibleLowerThirdShadowColor = secondaryBibleLowerThirdShadowColor,
        primaryBibleLowerThirdShadowSize = secondaryBibleLowerThirdShadowSize,
        primaryBibleLowerThirdShadowOpacity = secondaryBibleLowerThirdShadowOpacity,
        primaryBibleLowerThirdColor = secondaryBibleLowerThirdColor,
        primaryBibleLowerThirdFontType = secondaryBibleLowerThirdFontType,
        primaryBibleLowerThirdBold = secondaryBibleLowerThirdBold,
        primaryBibleLowerThirdItalic = secondaryBibleLowerThirdItalic,
        primaryBibleLowerThirdUnderline = secondaryBibleLowerThirdUnderline,
        primaryBibleLowerThirdShadow = secondaryBibleLowerThirdShadow,
        primaryReferenceColor = secondaryReferenceColor,
        primaryReferenceFontType = secondaryReferenceFontType,
        primaryReferenceFontSize = secondaryReferenceFontSize,
        primaryReferenceLowerThirdFontSize = secondaryReferenceLowerThirdFontSize,
        primaryReferencePosition = secondaryReferencePosition,
        primaryReferenceLowerThirdPosition = secondaryReferenceLowerThirdPosition,
        primaryReferenceHorizontalAlignment = secondaryReferenceHorizontalAlignment,
        primaryReferenceLowerThirdHorizontalAlignment = secondaryReferenceLowerThirdHorizontalAlignment,
        primaryShowAbbreviation = secondaryShowAbbreviation,
        primaryReferenceBold = secondaryReferenceBold,
        primaryReferenceItalic = secondaryReferenceItalic,
        primaryReferenceUnderline = secondaryReferenceUnderline,
        primaryReferenceShadow = secondaryReferenceShadow,
        primaryReferenceShadowColor = secondaryReferenceShadowColor,
        primaryReferenceShadowSize = secondaryReferenceShadowSize,
        primaryReferenceShadowOpacity = secondaryReferenceShadowOpacity,
        primaryReferenceLowerThirdShadowColor = secondaryReferenceLowerThirdShadowColor,
        primaryReferenceLowerThirdShadowSize = secondaryReferenceLowerThirdShadowSize,
        primaryReferenceLowerThirdShadowOpacity = secondaryReferenceLowerThirdShadowOpacity,
        primaryReferenceLowerThirdColor = secondaryReferenceLowerThirdColor,
        primaryReferenceLowerThirdFontType = secondaryReferenceLowerThirdFontType,
        primaryReferenceLowerThirdBold = secondaryReferenceLowerThirdBold,
        primaryReferenceLowerThirdItalic = secondaryReferenceLowerThirdItalic,
        primaryReferenceLowerThirdUnderline = secondaryReferenceLowerThirdUnderline,
        primaryReferenceLowerThirdShadow = secondaryReferenceLowerThirdShadow,
        secondaryBibleColor = primaryBibleColor,
        secondaryBibleFontType = primaryBibleFontType,
        secondaryBibleFontSize = primaryBibleFontSize,
        secondaryBibleLowerThirdFontSize = primaryBibleLowerThirdFontSize,
        secondaryBibleHorizontalAlignment = primaryBibleHorizontalAlignment,
        secondaryBibleLowerThirdHorizontalAlignment = primaryBibleLowerThirdHorizontalAlignment,
        secondaryBibleBold = primaryBibleBold,
        secondaryBibleItalic = primaryBibleItalic,
        secondaryBibleUnderline = primaryBibleUnderline,
        secondaryBibleShadow = primaryBibleShadow,
        secondaryBibleShadowColor = primaryBibleShadowColor,
        secondaryBibleShadowSize = primaryBibleShadowSize,
        secondaryBibleShadowOpacity = primaryBibleShadowOpacity,
        secondaryBibleLowerThirdShadowColor = primaryBibleLowerThirdShadowColor,
        secondaryBibleLowerThirdShadowSize = primaryBibleLowerThirdShadowSize,
        secondaryBibleLowerThirdShadowOpacity = primaryBibleLowerThirdShadowOpacity,
        secondaryBibleLowerThirdColor = primaryBibleLowerThirdColor,
        secondaryBibleLowerThirdFontType = primaryBibleLowerThirdFontType,
        secondaryBibleLowerThirdBold = primaryBibleLowerThirdBold,
        secondaryBibleLowerThirdItalic = primaryBibleLowerThirdItalic,
        secondaryBibleLowerThirdUnderline = primaryBibleLowerThirdUnderline,
        secondaryBibleLowerThirdShadow = primaryBibleLowerThirdShadow,
        secondaryReferenceColor = primaryReferenceColor,
        secondaryReferenceFontType = primaryReferenceFontType,
        secondaryReferenceFontSize = primaryReferenceFontSize,
        secondaryReferenceLowerThirdFontSize = primaryReferenceLowerThirdFontSize,
        secondaryReferencePosition = primaryReferencePosition,
        secondaryReferenceLowerThirdPosition = primaryReferenceLowerThirdPosition,
        secondaryReferenceHorizontalAlignment = primaryReferenceHorizontalAlignment,
        secondaryReferenceLowerThirdHorizontalAlignment = primaryReferenceLowerThirdHorizontalAlignment,
        secondaryShowAbbreviation = primaryShowAbbreviation,
        secondaryReferenceBold = primaryReferenceBold,
        secondaryReferenceItalic = primaryReferenceItalic,
        secondaryReferenceUnderline = primaryReferenceUnderline,
        secondaryReferenceShadow = primaryReferenceShadow,
        secondaryReferenceShadowColor = primaryReferenceShadowColor,
        secondaryReferenceShadowSize = primaryReferenceShadowSize,
        secondaryReferenceShadowOpacity = primaryReferenceShadowOpacity,
        secondaryReferenceLowerThirdShadowColor = primaryReferenceLowerThirdShadowColor,
        secondaryReferenceLowerThirdShadowSize = primaryReferenceLowerThirdShadowSize,
        secondaryReferenceLowerThirdShadowOpacity = primaryReferenceLowerThirdShadowOpacity,
        secondaryReferenceLowerThirdColor = primaryReferenceLowerThirdColor,
        secondaryReferenceLowerThirdFontType = primaryReferenceLowerThirdFontType,
        secondaryReferenceLowerThirdBold = primaryReferenceLowerThirdBold,
        secondaryReferenceLowerThirdItalic = primaryReferenceLowerThirdItalic,
        secondaryReferenceLowerThirdUnderline = primaryReferenceLowerThirdUnderline,
        secondaryReferenceLowerThirdShadow = primaryReferenceLowerThirdShadow,
    )
}
