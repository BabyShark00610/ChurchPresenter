package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle

/**
 * A checkbox, radio button or switch together with its label, where **the label is part of the
 * control**.
 *
 * The pattern these replace — a bare `Text` sitting next to a `Checkbox` inside a plain `Row` — looks
 * identical on screen but only the small square is clickable. Everyone aims at the word, which is the
 * larger target and the one that reads as the thing being chosen, and nothing happens. It is also
 * inaccessible: with no association between the two, a screen reader announces an unlabelled checkbox
 * and a stray line of text.
 *
 * The fix is Material 3's own recommendation: put `toggleable`/`selectable` on the row with the right
 * [Role], and pass `null` as the control's own callback so the row owns the single click. That gives
 * one click target covering control and label, one semantics node, and a label that is announced with
 * its state.
 *
 * `enabled` propagates to both halves, so a disabled row is inert *and* reports itself disabled rather
 * than silently ignoring presses.
 */

@Composable
fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    controlModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // null: the row above already handles the click, and a control with its own handler would
        // publish a second, competing click target inside the first.
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, modifier = controlModifier)
        Text(text = label, style = style)
    }
}

@Composable
fun LabeledRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    controlModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = controlModifier)
        Text(text = label, style = style)
    }
}

@Composable
fun LabeledSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    controlModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, modifier = controlModifier)
        Text(text = label, style = style)
    }
}
