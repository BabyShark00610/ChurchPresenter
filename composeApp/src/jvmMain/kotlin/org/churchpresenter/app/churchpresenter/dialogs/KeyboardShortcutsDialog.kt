package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.keyboard_shortcuts_title
import churchpresenter.composeapp.generated.resources.ok
import churchpresenter.composeapp.generated.resources.shortcut_category_mouse
import churchpresenter.composeapp.generated.resources.shortcut_description_context_menu
import churchpresenter.composeapp.generated.resources.shortcut_description_go_live
import churchpresenter.composeapp.generated.resources.shortcut_description_reorder_item
import churchpresenter.composeapp.generated.resources.shortcut_key_double_click
import churchpresenter.composeapp.generated.resources.shortcut_key_right_click
import churchpresenter.composeapp.generated.resources.shortcut_key_shift_drag
import churchpresenter.composeapp.generated.resources.symbol_ok
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.centeredOnMainWindow
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.models.ShortcutScope
import org.churchpresenter.app.churchpresenter.utils.LocalShortcuts
import org.churchpresenter.app.churchpresenter.utils.labelOrUnbound
import org.jetbrains.compose.resources.stringResource

@Composable
fun KeyboardShortcutsDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val mainWindowState = LocalMainWindowState.current
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(
            position = centeredOnMainWindow(mainWindowState, 680.dp, 600.dp),
            width = 680.dp,
            height = 600.dp
        ),
        title = stringResource(Res.string.keyboard_shortcuts_title),
        resizable = true
    ) {
        KeyboardShortcutsDialogContent(onDismiss = onDismiss)
    }
}

/**
 * The shortcut reference, generated from the binding registry.
 *
 * Every keyboard row here is `ShortcutAction` rendered through the same `ShortcutMap` the handlers
 * consult, so the list cannot describe a key the app does not respond to. It used to be ~70
 * hand-written rows paired with hand-written key strings, and it had drifted: Page Up/Down, `B` and
 * `.` were all handled but appeared nowhere in this dialog.
 *
 * Mouse gestures are still written out by hand at the bottom — they are not key bindings, are not
 * rebindable, and have no registry entry to render from.
 */
@Composable
internal fun KeyboardShortcutsDialogContent(onDismiss: () -> Unit) {
    val shortcuts = LocalShortcuts.current
    val actionsByScope = ShortcutAction.entries.groupBy { it.scope }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scope order is the enum's own order, which is menus → global → per tab.
                ShortcutScope.entries.forEach { scope ->
                    val actions = actionsByScope[scope].orEmpty()
                    if (actions.isNotEmpty()) {
                        ShortcutsCategory(stringResource(scope.titleRes)) {
                            actions.forEach { action ->
                                ShortcutRow(
                                    keys = shortcuts.labelOrUnbound(action),
                                    description = stringResource(action.descriptionRes)
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

            // Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(shape = RoundedCornerShape(6.dp), onClick = onDismiss) {
                    Text("${stringResource(Res.string.symbol_ok)} ${stringResource(Res.string.ok)}")
                }
            }
        }
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
