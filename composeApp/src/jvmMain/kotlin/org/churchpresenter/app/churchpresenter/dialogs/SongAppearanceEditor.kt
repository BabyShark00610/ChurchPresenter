package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.auto_fit
import churchpresenter.composeapp.generated.resources.color
import churchpresenter.composeapp.generated.resources.display_mode_label
import churchpresenter.composeapp.generated.resources.display_mode_lines_per_slide
import churchpresenter.composeapp.generated.resources.display_mode_one_line
import churchpresenter.composeapp.generated.resources.display_mode_one_verse
import churchpresenter.composeapp.generated.resources.font_size
import churchpresenter.composeapp.generated.resources.font_type
import churchpresenter.composeapp.generated.resources.lyrics_scrim_opacity
import churchpresenter.composeapp.generated.resources.lyrics_scrim_padding
import churchpresenter.composeapp.generated.resources.lyrics_scrim_softness
import churchpresenter.composeapp.generated.resources.lyrics_scrim_width
import churchpresenter.composeapp.generated.resources.song_appearance_intro
import churchpresenter.composeapp.generated.resources.song_appearance_own_background
import churchpresenter.composeapp.generated.resources.song_appearance_own_color
import churchpresenter.composeapp.generated.resources.song_appearance_own_font
import churchpresenter.composeapp.generated.resources.song_appearance_own_scrim
import churchpresenter.composeapp.generated.resources.song_appearance_own_split
import churchpresenter.composeapp.generated.resources.song_appearance_section_default
import churchpresenter.composeapp.generated.resources.song_appearance_section_sizes
import org.churchpresenter.app.churchpresenter.composables.ColorPickerField
import org.churchpresenter.app.churchpresenter.composables.FontSettingsDropdown
import org.churchpresenter.app.churchpresenter.composables.LabeledCheckbox
import org.churchpresenter.app.churchpresenter.composables.NumberSettingsTextField
import org.churchpresenter.app.churchpresenter.composables.TextStyleButtons
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundConfig
import org.churchpresenter.app.churchpresenter.data.settings.SongAppearance
import org.churchpresenter.app.churchpresenter.data.settings.SongSettings
import org.churchpresenter.app.churchpresenter.dialogs.tabs.BackgroundColumn
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.utils.MAX_LINES_PER_SLIDE
import org.jetbrains.compose.resources.stringResource

/** Font sizes offered per song and per section; the same band the global lyric size uses. */
private val SONG_FONT_SIZE_RANGE = 8..150

/**
 * The song editor's Appearance pane: what this one song overrides about how it is presented.
 *
 * Overrides are grouped, and each group has one tick. Untick it and every field in that group goes
 * back to null — "follow the general settings" — so raising the global lyric size still moves every
 * song that never asked for its own. Tick it and the group's fields are seeded from [defaults], so
 * the operator starts from what they were already looking at rather than from a blank.
 *
 * A per-field tick would express inheritance more finely, but a song's font and its size, or the six
 * numbers of a scrim, are only ever chosen together — and twenty checkboxes would bury the six
 * decisions that are actually being made.
 *
 * [defaults] is the shared [SongSettings]'s FULLSCREEN half. The stored override is surface-agnostic;
 * `withSongAppearance` aims it at whichever surface is drawing, so the fullscreen values are simply
 * the sensible thing to seed from and to show as the current look.
 */
@Composable
internal fun SongAppearanceEditor(
    appearance: SongAppearance,
    background: BackgroundConfig?,
    defaults: SongSettings,
    sectionNames: List<String>,
    availableFonts: List<String>,
    onAppearanceChange: (SongAppearance) -> Unit,
    onBackgroundChange: (BackgroundConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.song_appearance_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )

        // ── Font and size ──
        GroupToggle(
            label = stringResource(Res.string.song_appearance_own_font),
            checked = appearance.fontType != null || appearance.fontSize != null,
            testTag = "songAppearanceOwnFont",
            onCheckedChange = { on ->
                onAppearanceChange(
                    if (on) appearance.copy(
                        fontType = defaults.lyricsFontType,
                        fontSize = defaults.lyricsFontSize,
                        fontSizeAutoFit = defaults.lyricsFontSizeAutoFit,
                    ) else appearance.copy(fontType = null, fontSize = null, fontSizeAutoFit = null)
                )
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontSettingsDropdown(
                    modifier = Modifier.width(200.dp),
                    label = stringResource(Res.string.font_type),
                    value = appearance.fontType ?: defaults.lyricsFontType,
                    fonts = availableFonts,
                    onValueChange = { onAppearanceChange(appearance.copy(fontType = it)) },
                )
                NumberSettingsTextField(
                    modifier = Modifier.width(110.dp),
                    label = stringResource(Res.string.font_size),
                    initialText = appearance.fontSize ?: defaults.lyricsFontSize,
                    range = SONG_FONT_SIZE_RANGE,
                    onValueChange = { onAppearanceChange(appearance.copy(fontSize = it)) },
                )
                LabeledCheckbox(
                    checked = appearance.fontSizeAutoFit ?: defaults.lyricsFontSizeAutoFit,
                    onCheckedChange = { onAppearanceChange(appearance.copy(fontSizeAutoFit = it)) },
                    controlModifier = Modifier.size(24.dp),
                    label = stringResource(Res.string.auto_fit),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ── Colour and style ──
        GroupToggle(
            label = stringResource(Res.string.song_appearance_own_color),
            checked = appearance.color != null,
            testTag = "songAppearanceOwnColor",
            onCheckedChange = { on ->
                onAppearanceChange(
                    if (on) appearance.copy(
                        color = defaults.lyricsColor,
                        bold = defaults.lyricsBold,
                        italic = defaults.lyricsItalic,
                        underline = defaults.lyricsUnderline,
                        shadow = defaults.lyricsShadow,
                    ) else appearance.copy(
                        color = null, bold = null, italic = null, underline = null, shadow = null,
                    )
                )
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorPickerField(
                    label = stringResource(Res.string.color),
                    modifier = Modifier.width(130.dp),
                    color = appearance.color ?: defaults.lyricsColor,
                    onColorChange = { onAppearanceChange(appearance.copy(color = it)) },
                )
                TextStyleButtons(
                    bold = appearance.bold ?: defaults.lyricsBold,
                    italic = appearance.italic ?: defaults.lyricsItalic,
                    underline = appearance.underline ?: defaults.lyricsUnderline,
                    shadow = appearance.shadow ?: defaults.lyricsShadow,
                    onBoldChange = { onAppearanceChange(appearance.copy(bold = it)) },
                    onItalicChange = { onAppearanceChange(appearance.copy(italic = it)) },
                    onUnderlineChange = { onAppearanceChange(appearance.copy(underline = it)) },
                    onShadowChange = { onAppearanceChange(appearance.copy(shadow = it)) },
                )
            }
        }

        // ── Darkening behind the words ──
        GroupToggle(
            label = stringResource(Res.string.song_appearance_own_scrim),
            checked = appearance.scrimEnabled != null,
            testTag = "songAppearanceOwnScrim",
            onCheckedChange = { on ->
                onAppearanceChange(
                    if (on) appearance.copy(
                        scrimEnabled = true,
                        scrimColor = defaults.lyricsScrimColor,
                        scrimOpacity = defaults.lyricsScrimOpacity,
                        scrimSoftness = defaults.lyricsScrimSoftness,
                        scrimPadding = defaults.lyricsScrimPadding,
                        scrimWidthPercent = defaults.lyricsScrimWidthPercent,
                    ) else appearance.copy(
                        scrimEnabled = null, scrimColor = null, scrimOpacity = null,
                        scrimSoftness = null, scrimPadding = null, scrimWidthPercent = null,
                    )
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorPickerField(
                        label = stringResource(Res.string.color),
                        modifier = Modifier.width(130.dp),
                        color = appearance.scrimColor ?: defaults.lyricsScrimColor,
                        onColorChange = { onAppearanceChange(appearance.copy(scrimColor = it)) },
                    )
                    NumberSettingsTextField(
                        modifier = Modifier.width(100.dp),
                        label = stringResource(Res.string.lyrics_scrim_opacity),
                        initialText = appearance.scrimOpacity ?: defaults.lyricsScrimOpacity,
                        range = 0..100,
                        onValueChange = { onAppearanceChange(appearance.copy(scrimOpacity = it)) },
                    )
                    NumberSettingsTextField(
                        modifier = Modifier.width(100.dp),
                        label = stringResource(Res.string.lyrics_scrim_softness),
                        initialText = appearance.scrimSoftness ?: defaults.lyricsScrimSoftness,
                        range = 0..100,
                        onValueChange = { onAppearanceChange(appearance.copy(scrimSoftness = it)) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberSettingsTextField(
                        modifier = Modifier.width(100.dp),
                        label = stringResource(Res.string.lyrics_scrim_padding),
                        initialText = appearance.scrimPadding ?: defaults.lyricsScrimPadding,
                        range = 0..400,
                        onValueChange = { onAppearanceChange(appearance.copy(scrimPadding = it)) },
                    )
                    NumberSettingsTextField(
                        modifier = Modifier.width(100.dp),
                        label = stringResource(Res.string.lyrics_scrim_width),
                        initialText = appearance.scrimWidthPercent ?: defaults.lyricsScrimWidthPercent,
                        range = 10..100,
                        onValueChange = { onAppearanceChange(appearance.copy(scrimWidthPercent = it)) },
                    )
                }
            }
        }

        // ── How much of the song goes on one slide ──
        GroupToggle(
            label = stringResource(Res.string.song_appearance_own_split),
            checked = appearance.displayMode != null,
            testTag = "songAppearanceOwnSplit",
            onCheckedChange = { on ->
                onAppearanceChange(
                    if (on) appearance.copy(
                        displayMode = defaults.fullscreenDisplayMode,
                        linesPerSlide = defaults.fullscreenLinesPerSlide,
                    ) else appearance.copy(displayMode = null, linesPerSlide = null)
                )
            },
        ) {
            val mode = appearance.displayMode ?: defaults.fullscreenDisplayMode
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.display_mode_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                    listOf(
                        Constants.SONG_DISPLAY_MODE_VERSE to stringResource(Res.string.display_mode_one_verse),
                        Constants.SONG_DISPLAY_MODE_LINE to stringResource(Res.string.display_mode_one_line),
                    ).forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = mode == value,
                            onClick = { onAppearanceChange(appearance.copy(displayMode = value)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2, baseShape = RoundedCornerShape(4.dp)),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            icon = {},
                        ) { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    }
                }
                if (mode == Constants.SONG_DISPLAY_MODE_LINE) {
                    NumberSettingsTextField(
                        modifier = Modifier.width(180.dp),
                        label = stringResource(Res.string.display_mode_lines_per_slide),
                        initialText = appearance.linesPerSlide ?: defaults.fullscreenLinesPerSlide,
                        range = 1..MAX_LINES_PER_SLIDE,
                        onValueChange = { onAppearanceChange(appearance.copy(linesPerSlide = it)) },
                    )
                }
            }
        }

        // ── Background ──
        GroupToggle(
            label = stringResource(Res.string.song_appearance_own_background),
            checked = background != null,
            testTag = "songAppearanceOwnBackground",
            onCheckedChange = { on ->
                onBackgroundChange(
                    if (on) BackgroundConfig(backgroundType = Constants.BACKGROUND_IMAGE) else null
                )
            },
        ) {
            BackgroundColumn(
                subtitle = "",
                config = background ?: BackgroundConfig(),
                onConfigChange = onBackgroundChange,
            )
        }

        // ── Size of individual sections ──
        // Listed from the lyrics as they are being edited, so a verse added a moment ago is already
        // here. Blank means the section takes the song's size and auto-fit like the rest.
        if (sectionNames.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(Res.string.song_appearance_section_sizes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            sectionNames.forEach { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    val current = appearance.sectionFontSize(name)
                    LabeledCheckbox(
                        checked = current != null,
                        onCheckedChange = { on ->
                            onAppearanceChange(
                                appearance.withSectionFontSize(
                                    name,
                                    if (on) appearance.fontSize ?: defaults.lyricsFontSize else null,
                                )
                            )
                        },
                        controlModifier = Modifier.size(22.dp),
                        label = if (current == null) stringResource(Res.string.song_appearance_section_default) else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (current != null) {
                        NumberSettingsTextField(
                            modifier = Modifier.width(100.dp),
                            initialText = current,
                            range = SONG_FONT_SIZE_RANGE,
                            onValueChange = { onAppearanceChange(appearance.withSectionFontSize(name, it)) },
                        )
                    }
                }
            }
        }
    }
}

/** A group's tick and, once ticked, the controls it governs. */
@Composable
private fun GroupToggle(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    LabeledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        controlModifier = Modifier.size(24.dp),
        label = label,
        modifier = Modifier.testTag(testTag),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (checked) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 4.dp)) { content() }
    }
}

/**
 * The section names in [lyrics], in the order they are written, without repeats.
 *
 * Taken from the text being edited rather than from the parsed song so the list follows a header the
 * moment it is typed. Repeats are dropped because the override is keyed by name: one Chorus entry
 * governs every place the repeat rule puts the chorus.
 */
internal fun sectionNamesIn(lyrics: String): List<String> =
    lyrics.lineSequence()
        .map { it.trim() }
        .filter { (it.startsWith("[") && it.endsWith("]")) || (it.startsWith("{") && it.endsWith("}")) }
        .map { SongAppearance.sectionKey(it) }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
