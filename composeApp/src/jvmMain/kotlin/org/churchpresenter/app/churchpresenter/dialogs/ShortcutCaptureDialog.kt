package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.cancel
import churchpresenter.composeapp.generated.resources.ok
import churchpresenter.composeapp.generated.resources.shortcut_capture_conflict
import churchpresenter.composeapp.generated.resources.shortcut_capture_prompt
import churchpresenter.composeapp.generated.resources.shortcut_capture_title
import churchpresenter.composeapp.generated.resources.shortcut_unbound
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.centeredOnMainWindow
import org.churchpresenter.app.churchpresenter.models.KeyChord
import org.churchpresenter.app.churchpresenter.models.ShortcutAction
import org.churchpresenter.app.churchpresenter.utils.LocalShortcuts
import org.churchpresenter.app.churchpresenter.utils.label
import org.jetbrains.compose.resources.stringResource

/** Test tags for the capture dialog's controls, which have no stable text to locate them by. */
internal object ShortcutCaptureTags {
    const val SURFACE = "shortcut_capture_surface"
    const val PREVIEW = "shortcut_capture_preview"
    const val CONFLICT = "shortcut_capture_conflict"
    const val CONFIRM = "shortcut_capture_confirm"
}

/**
 * The chord a key event should be recorded as, or null to keep waiting.
 *
 * Split out of the dialog because a `DialogWindow` needs a real window and so cannot be composed by
 * `runComposeUiTest` — this is the whole of the dialog's decision, and it is unit-tested directly.
 *
 * Returns null for a key-up (the press is what counts) and for a bare modifier: holding Ctrl emits
 * its own key-down before the real key arrives, and recording that would bind the action to "Ctrl".
 */
internal fun capturedChord(event: KeyEvent): KeyChord? = when {
    event.type != KeyEventType.KeyDown -> null
    event.key in KeyChord.MODIFIER_KEYS -> null
    else -> KeyChord.of(event)
}

/**
 * Records one key combination for [action].
 *
 * **Escape is captured like any other key, not treated as "cancel"** — it is the default binding
 * for Clear Output, so a dialog that closed on Escape could never rebind it. The way out is the
 * Cancel button.
 */
@Composable
fun ShortcutCaptureDialog(
    action: ShortcutAction,
    onConfirm: (KeyChord) -> Unit,
    onDismiss: () -> Unit,
) {
    val shortcuts = LocalShortcuts.current
    val mainWindowState = LocalMainWindowState.current
    var captured by remember(action) { mutableStateOf<KeyChord?>(null) }
    val focusRequester = remember { FocusRequester() }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(
            position = centeredOnMainWindow(mainWindowState, 420.dp, 240.dp),
            width = 420.dp,
            height = 240.dp
        ),
        title = stringResource(Res.string.shortcut_capture_title),
        resizable = false
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        val conflict = captured?.let { shortcuts.conflictFor(it, action) }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ShortcutCaptureTags.SURFACE)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    // Every key-down is swallowed whether or not it produces a chord, so a stray
                    // Tab or Enter cannot escape into the dialog's own buttons mid-capture.
                    capturedChord(event)?.let { captured = it }
                    event.type == KeyEventType.KeyDown
                },
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(action.descriptionRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.shortcut_capture_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = captured?.label() ?: stringResource(Res.string.shortcut_unbound),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag(ShortcutCaptureTags.PREVIEW)
                    )
                }

                if (conflict != null) {
                    Text(
                        text = stringResource(
                            Res.string.shortcut_capture_conflict,
                            stringResource(conflict.descriptionRes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(ShortcutCaptureTags.CONFLICT)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    Button(
                        shape = RoundedCornerShape(6.dp),
                        enabled = captured != null && conflict == null,
                        onClick = { captured?.let(onConfirm) },
                        modifier = Modifier.testTag(ShortcutCaptureTags.CONFIRM)
                    ) { Text(stringResource(Res.string.ok)) }
                }
            }
        }
    }
}
