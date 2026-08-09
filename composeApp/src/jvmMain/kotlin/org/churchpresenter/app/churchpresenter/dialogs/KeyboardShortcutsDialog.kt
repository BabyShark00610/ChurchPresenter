package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.apply
import churchpresenter.composeapp.generated.resources.cancel
import churchpresenter.composeapp.generated.resources.keyboard_shortcuts_title
import churchpresenter.composeapp.generated.resources.ok
import churchpresenter.composeapp.generated.resources.shortcut_category_mouse
import churchpresenter.composeapp.generated.resources.shortcut_description_context_menu
import churchpresenter.composeapp.generated.resources.shortcut_description_go_live
import churchpresenter.composeapp.generated.resources.shortcut_description_reorder_item
import churchpresenter.composeapp.generated.resources.shortcut_key_double_click
import churchpresenter.composeapp.generated.resources.shortcut_key_right_click
import churchpresenter.composeapp.generated.resources.shortcut_key_shift_drag
import churchpresenter.composeapp.generated.resources.shortcut_settings_change
import churchpresenter.composeapp.generated.resources.shortcut_settings_clear
import churchpresenter.composeapp.generated.resources.shortcut_settings_reset
import churchpresenter.composeapp.generated.resources.shortcut_settings_reset_all
import churchpresenter.composeapp.generated.resources.symbol_cancel
import churchpresenter.composeapp.generated.resources.symbol_ok
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.centeredOnMainWindow
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
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

@Composable
fun KeyboardShortcutsDialog(
    isVisible: Boolean,
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val mainWindowState = LocalMainWindowState.current
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(
            position = centeredOnMainWindow(mainWindowState, 760.dp, 720.dp),
            width = 760.dp,
            height = 720.dp
        ),
        title = stringResource(Res.string.keyboard_shortcuts_title),
        resizable = true
    ) {
        KeyboardShortcutsDialogContent(
            initialSettings = settings,
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The shortcut reference, and the one place shortcuts are changed.
 *
 * Every keyboard row is a `ShortcutAction` rendered through the same `ShortcutMap` the handlers
 * consult, so the list cannot describe a key the app does not respond to. It used to be ~70
 * hand-written rows paired with hand-written key strings, and it had drifted: Page Up/Down, `B` and
 * `.` were all handled but appeared nowhere here.
 *
 * Editing was briefly a separate Settings tab, which meant two windows showing the same table and
 * only one of them able to change it. It is merged in here.
 *
 * Edits are **pending** until Apply or OK, like the settings tab this replaced — which is why the
 * capture dialog is handed this composable's own map rather than reading `LocalShortcuts`: the
 * composition local still holds what was last saved, so validating against it would report a
 * binding the user had just cleared as a conflict, and miss one they had just assigned.
 *
 * Mouse gestures are still written out by hand at the bottom — they are not key bindings, are not
 * rebindable, and have no registry entry to render from.
 */
@Composable
internal fun KeyboardShortcutsDialogContent(
    initialSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentSettings by remember { mutableStateOf(initialSettings) }
    var capturing by remember { mutableStateOf<ShortcutAction?>(null) }

    val shortcuts = remember(currentSettings.keyboardShortcutSettings) {
        ShortcutMap.from(currentSettings.keyboardShortcutSettings)
    }
    val actionsByScope = remember { ShortcutAction.entries.groupBy { it.scope } }

    fun editOverrides(update: (Map<String, List<KeyChord>>) -> Map<String, List<KeyChord>>) {
        currentSettings = currentSettings.copy(
            keyboardShortcutSettings = currentSettings.keyboardShortcutSettings.copy(
                overrides = update(currentSettings.keyboardShortcutSettings.overrides)
            )
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { editOverrides { emptyMap() } },
                    modifier = Modifier.testTag(SHORTCUT_RESET_ALL_TAG)
                ) { Text(stringResource(Res.string.shortcut_settings_reset_all)) }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scope order is the enum's own order, which is menus → global → per tab.
                ShortcutScope.entries.forEach { scope ->
                    val actions = actionsByScope[scope].orEmpty()
                    if (actions.isNotEmpty()) {
                        ShortcutsCategory(stringResource(scope.titleRes)) {
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

                ShortcutsCategory(stringResource(Res.string.shortcut_category_mouse)) {
                    ShortcutRow(stringResource(Res.string.shortcut_key_double_click), stringResource(Res.string.shortcut_description_go_live))
                    ShortcutRow(stringResource(Res.string.shortcut_key_right_click), stringResource(Res.string.shortcut_description_context_menu))
                    ShortcutRow(stringResource(Res.string.shortcut_key_shift_drag), stringResource(Res.string.shortcut_description_reorder_item))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(shape = RoundedCornerShape(6.dp), onClick = onDismiss) {
                    Text("${stringResource(Res.string.symbol_cancel)} ${stringResource(Res.string.cancel)}")
                }
                OutlinedButton(shape = RoundedCornerShape(6.dp), onClick = { onSave(currentSettings) }) {
                    Text(stringResource(Res.string.apply))
                }
                Button(
                    shape = RoundedCornerShape(6.dp),
                    onClick = { onSave(currentSettings); onDismiss() }
                ) {
                    Text("${stringResource(Res.string.symbol_ok)} ${stringResource(Res.string.ok)}")
                }
            }
        }
    }

    capturing?.let { action ->
        ShortcutCaptureDialog(
            action = action,
            shortcuts = shortcuts,
            onConfirm = { chord ->
                editOverrides { it + (action.name to listOf(chord)) }
                capturing = null
            },
            onDismiss = { capturing = null },
        )
    }
}

@Composable
fun ShortcutsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * One rebindable action: description, current binding, and the two controls.
 *
 * Deliberately **not** built on `SettingRow`. That gives the label a fixed width and the controls
 * whatever is left, which left the buttons so narrow that "Clear" wrapped to three lines and every
 * row grew to fit it. Here the description takes the slack and the controls keep their width.
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width(CHIP_WIDTH)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
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

@Composable
fun ShortcutRow(keys: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = keys,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .widthIn(min = 80.dp)
        )
    }
}
