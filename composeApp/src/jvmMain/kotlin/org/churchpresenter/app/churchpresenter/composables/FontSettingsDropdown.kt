package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.churchpresenter.app.churchpresenter.utils.Utils.systemFontFamilyOrDefault

/**
 * Font-family picker: a [SearchableDropdownField] that renders every name in its own font, so the
 * list is a preview as well as a menu.
 *
 * The list runs to whatever the machine has installed, which is why it filters as you type.
 */
@Composable
fun FontSettingsDropdown(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String,
    fonts: List<String>,
    onValueChange: (String) -> Unit
) {
    SearchableDropdownField(
        value = value,
        options = fonts,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        valueTextStyle = MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = remember(value) { systemFontFamilyOrDefault(value) },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        itemContent = { font ->
            val fontFamily = remember(font) { systemFontFamilyOrDefault(font) }
            Text(font, style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily))
        },
    )
}
