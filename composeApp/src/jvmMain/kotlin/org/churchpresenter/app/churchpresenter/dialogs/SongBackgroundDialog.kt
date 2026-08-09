package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.cancel
import churchpresenter.composeapp.generated.resources.save
import churchpresenter.composeapp.generated.resources.song_background_follow_general
import churchpresenter.composeapp.generated.resources.song_background_own
import churchpresenter.composeapp.generated.resources.song_background_title
import org.churchpresenter.app.churchpresenter.composables.LabeledCheckbox
import org.churchpresenter.app.churchpresenter.data.settings.BackgroundConfig
import org.churchpresenter.app.churchpresenter.dialogs.tabs.BackgroundColumn
import org.churchpresenter.app.churchpresenter.ui.theme.AppThemeWrapper
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.centeredOnMainWindow
import org.jetbrains.compose.resources.stringResource

/**
 * Gives one song a background of its own, overriding the shared song background for as long as it is
 * set. Reached from the song list's right-click menu.
 *
 * Opened per song and keyed on nothing else: the window is only composed while it is showing, so the
 * edit buffer below starts from [background] each time it opens.
 */
@Composable
fun SongBackgroundDialog(
    isVisible: Boolean,
    songTitle: String,
    background: BackgroundConfig?,
    theme: ThemeMode,
    onDismiss: () -> Unit,
    onSave: (BackgroundConfig?) -> Unit,
) {
    if (!isVisible) return

    val mainWindowState = LocalMainWindowState.current
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(
            position = centeredOnMainWindow(mainWindowState, 520.dp, 640.dp),
            width = 520.dp,
            height = 640.dp
        ),
        title = stringResource(Res.string.song_background_title),
        resizable = true
    ) {
        SongBackgroundContent(
            songTitle = songTitle,
            background = background,
            theme = theme,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

/**
 * The dialog's body, apart from the `DialogWindow` that carries it so it stays composable on a
 * headless machine — the same split [EditSongDialog] uses and for the same reason.
 */
@Composable
internal fun SongBackgroundContent(
    songTitle: String,
    background: BackgroundConfig?,
    theme: ThemeMode,
    onDismiss: () -> Unit,
    onSave: (BackgroundConfig?) -> Unit,
) {
    // Absent (null) and configured are two different states, and the checkbox is what moves between
    // them. They are held apart rather than collapsed into a "Default" background type because that
    // type already means something else here — fall through to the GLOBAL default rather than to the
    // shared song background — and a single control cannot express both.
    var useOwn by remember(songTitle, background) { mutableStateOf(background != null) }
    var draft by remember(songTitle, background) {
        mutableStateOf(background ?: BackgroundConfig(backgroundType = Constants.BACKGROUND_IMAGE))
    }

    AppThemeWrapper(theme = theme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))

                LabeledCheckbox(
                    checked = useOwn,
                    onCheckedChange = { useOwn = it },
                    controlModifier = Modifier.size(24.dp),
                    label = stringResource(Res.string.song_background_own),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                ) {
                    if (useOwn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BackgroundColumn(
                            subtitle = "",
                            config = draft,
                            onConfigChange = { draft = it },
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.song_background_follow_general),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    Button(onClick = { onSave(if (useOwn) draft else null) }) {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}
