package org.churchpresenter.app.churchpresenter.dialogs.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.shortcut_settings_change
import churchpresenter.composeapp.generated.resources.shortcut_settings_clear
import churchpresenter.composeapp.generated.resources.shortcut_settings_reset
import churchpresenter.composeapp.generated.resources.shortcut_settings_reset_all
import org.churchpresenter.app.churchpresenter.composables.SettingsSection
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.dialogs.ShortcutCaptureDialog
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.models.ShortcutScope
import org.churchpresenter.app.churchpresenter.utils.ShortcutMap
import org.churchpresenter.app.churchpresenter.utils.labelOrUnbound
import org.jetbrains.compose.resources.stringResource

/** Fits `⌃⇧N` / `Ctrl+Shift+N` and the "Not set" placeholder without the chip resizing per row. */
private val CHIP_WIDTH = 92.dp

/** Keeps "Reset" and "Clear" the same width, so the buttons line up down the column. */
private val REVERT_WIDTH = 56.dp

/** Tighter than the Material default, which is sized for a full-width dialog button. */
private val BUTTON_PADDING = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

/** Test tag for the reset-everything button, which several tests need to locate. */
internal const val SHORTCUT_RESET_ALL_TAG = "shortcut_reset_all"

/** The per-action row's key chip, tagged by action so a test can read one row's binding. */
internal fun shortcutChipTag(action: ShortcutAction) = "shortcut_chip_${action.name}"

/** The per-action rebind button. */
internal fun shortcutChangeTag(action: ShortcutAction) = "shortcut_change_${action.name}"

/**
 * The per-action Reset/Clear button.
 *
 * Tagged per action because every row carries one, so "the Clear button" matches ~40 nodes.
 */
internal fun shortcutRevertTag(action: ShortcutAction) = "shortcut_revert_${action.name}"

/**
 * Keyboard shortcut editing.
 *
 * Follows the `settings` + `onSettingsChange` shape of the other simple tabs — the only transient
 * state is which action is currently being recorded, which is local `remember` state and never
 * reaches [AppSettings].
 *
 * Writes go into `KeyboardShortcutSettings.overrides`, and an action restored to its shipped
 * binding has its entry **removed** rather than rewritten. That keeps "the user never touched this"
 * distinguishable from "the user happened to pick the default", so a future change to a default
 * still reaches them.
 */
@Composable
fun ShortcutSettingsTab(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
) {
    val shortcuts = remember(settings.keyboardShortcutSettings) {
        ShortcutMap.from(settings.keyboardShortcutSettings)
    }
    var capturing by remember { mutableStateOf<ShortcutAction?>(null) }

    fun editOverrides(update: (Map<String, List<KeyChord>>) -> Map<String, List<KeyChord>>) {
        onSettingsChange { s ->
            s.copy(
                keyboardShortcutSettings = s.keyboardShortcutSettings.copy(
                    overrides = update(s.keyboardShortcutSettings.overrides)
                )
            )
        }
    }

    val scopes = ShortcutScope.entries
    val actionsByScope = ShortcutAction.entries.groupBy { it.scope }
    // Split the scopes across two columns by action count rather than scope count, so the two
    // sides come out roughly the same height instead of one holding the two biggest groups.
    val half = ShortcutAction.entries.size / 2
    var running = 0
    val leftScopes = scopes.takeWhile { scope ->
        val fits = running < half
        running += actionsByScope[scope]?.size ?: 0
        fits
    }
    val rightScopes = scopes - leftScopes.toSet()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { editOverrides { emptyMap() } },
                    modifier = Modifier.testTag(SHORTCUT_RESET_ALL_TAG)
                ) { Text(stringResource(Res.string.shortcut_settings_reset_all)) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(leftScopes, rightScopes).forEach { columnScopes ->
                    Column(
                        modifier = Modifier.weight(0.5f).widthIn(min = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columnScopes.forEach { scope ->
                            val actions = actionsByScope[scope].orEmpty()
                            if (actions.isNotEmpty()) {
                                SettingsSection(title = stringResource(scope.titleRes)) {
                                    actions.forEach { action ->
                                        ShortcutBindingRow(
                                            action = action,
                                            keys = shortcuts.labelOrUnbound(action),
                                            customized = shortcuts.isCustomized(action),
                                            onChange = { capturing = action },
                                            onReset = { editOverrides { it - action.name } },
                                            onClear = { editOverrides { it + (action.name to emptyList()) } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    capturing?.let { action ->
        ShortcutCaptureDialog(
            action = action,
            onConfirm = { chord ->
                editOverrides { it + (action.name to listOf(chord)) }
                capturing = null
            },
            onDismiss = { capturing = null },
        )
    }
}

/**
 * One action's row: description, current binding, and the two controls.
 *
 * Deliberately **not** built on `SettingRow`. That gives the label a fixed width and the controls
 * whatever is left, which at this column width left the buttons so narrow that "Clear" wrapped to
 * three lines and every row grew to fit it. Here the description takes the slack instead and the
 * controls keep their natural width.
 */
@Composable
private fun ShortcutBindingRow(
    action: ShortcutAction,
    keys: String,
    customized: Boolean,
    onChange: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(action.descriptionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(CHIP_WIDTH)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = keys,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(shortcutChipTag(action))
            )
        }
        OutlinedButton(
            onClick = onChange,
            shape = RoundedCornerShape(6.dp),
            contentPadding = BUTTON_PADDING,
            modifier = Modifier.testTag(shortcutChangeTag(action))
        ) {
            Text(stringResource(Res.string.shortcut_settings_change), maxLines = 1, softWrap = false)
        }
        // One button, two meanings: an untouched row can only be cleared, a customized one can be
        // put back. Offering both at once would widen every row for a control most never need.
        TextButton(
            onClick = if (customized) onReset else onClear,
            contentPadding = BUTTON_PADDING,
            modifier = Modifier.widthIn(min = REVERT_WIDTH).testTag(shortcutRevertTag(action))
        ) {
            Text(
                text = stringResource(
                    if (customized) Res.string.shortcut_settings_reset
                    else Res.string.shortcut_settings_clear
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
