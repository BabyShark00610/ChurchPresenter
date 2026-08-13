package org.churchpresenter.app.churchpresenter.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.churchpresenter.app.churchpresenter.utils.TimerStateManager
import org.jetbrains.compose.resources.stringResource
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.canvas_bg_color
import churchpresenter.composeapp.generated.resources.canvas_color_1
import churchpresenter.composeapp.generated.resources.canvas_color_2
import churchpresenter.composeapp.generated.resources.canvas_font_color
import churchpresenter.composeapp.generated.resources.canvas_gradient
import churchpresenter.composeapp.generated.resources.canvas_source_shape
import churchpresenter.composeapp.generated.resources.canvas_shape_show_stroke
import churchpresenter.composeapp.generated.resources.canvas_shape_stroke_color
import churchpresenter.composeapp.generated.resources.canvas_shape_fill_color
import churchpresenter.composeapp.generated.resources.canvas_shape_stroke_width
import churchpresenter.composeapp.generated.resources.canvas_angle
import churchpresenter.composeapp.generated.resources.canvas_opacity
import churchpresenter.composeapp.generated.resources.canvas_source_browser
import churchpresenter.composeapp.generated.resources.canvas_source_clock
import churchpresenter.composeapp.generated.resources.canvas_source_qrcode
import churchpresenter.composeapp.generated.resources.canvas_source_camera
import churchpresenter.composeapp.generated.resources.canvas_source_screen_capture
import churchpresenter.composeapp.generated.resources.canvas_clock_mode
import churchpresenter.composeapp.generated.resources.canvas_clock_format
import churchpresenter.composeapp.generated.resources.canvas_clock_show_hours
import churchpresenter.composeapp.generated.resources.canvas_clock_show_seconds
import churchpresenter.composeapp.generated.resources.canvas_clock_font_size
import churchpresenter.composeapp.generated.resources.canvas_clock_target_hour
import churchpresenter.composeapp.generated.resources.canvas_clock_target_minute
import churchpresenter.composeapp.generated.resources.canvas_clock_target_second
import churchpresenter.composeapp.generated.resources.canvas_text_color
import churchpresenter.composeapp.generated.resources.canvas_text_bg_color
import churchpresenter.composeapp.generated.resources.canvas_text_bold
import churchpresenter.composeapp.generated.resources.canvas_qr_type
import churchpresenter.composeapp.generated.resources.canvas_qr_content
import churchpresenter.composeapp.generated.resources.canvas_qr_foreground
import churchpresenter.composeapp.generated.resources.canvas_qr_background
import churchpresenter.composeapp.generated.resources.canvas_qr_wifi_ssid
import churchpresenter.composeapp.generated.resources.canvas_qr_wifi_password
import churchpresenter.composeapp.generated.resources.canvas_qr_wifi_encryption
import churchpresenter.composeapp.generated.resources.canvas_qr_wifi_hidden
import churchpresenter.composeapp.generated.resources.canvas_qr_error_correction
import churchpresenter.composeapp.generated.resources.canvas_camera_device
import churchpresenter.composeapp.generated.resources.canvas_camera_ffmpeg_hint
import churchpresenter.composeapp.generated.resources.canvas_camera_v4l2_hint
import churchpresenter.composeapp.generated.resources.canvas_camera_none_found
import churchpresenter.composeapp.generated.resources.canvas_camera_refresh
import churchpresenter.composeapp.generated.resources.canvas_camera_format
import churchpresenter.composeapp.generated.resources.canvas_camera_format_auto
import churchpresenter.composeapp.generated.resources.canvas_camera_connection
import churchpresenter.composeapp.generated.resources.canvas_camera_mode
import churchpresenter.composeapp.generated.resources.canvas_camera_mode_auto
import churchpresenter.composeapp.generated.resources.canvas_capture_x
import churchpresenter.composeapp.generated.resources.canvas_capture_y
import churchpresenter.composeapp.generated.resources.canvas_capture_width
import churchpresenter.composeapp.generated.resources.canvas_capture_height
import churchpresenter.composeapp.generated.resources.canvas_capture_mode
import churchpresenter.composeapp.generated.resources.canvas_capture_mode_region
import churchpresenter.composeapp.generated.resources.canvas_capture_mode_window
import churchpresenter.composeapp.generated.resources.canvas_capture_window
import churchpresenter.composeapp.generated.resources.canvas_capture_refresh_windows
import churchpresenter.composeapp.generated.resources.canvas_capture_interval
import churchpresenter.composeapp.generated.resources.position
import churchpresenter.composeapp.generated.resources.canvas_source_name
import churchpresenter.composeapp.generated.resources.canvas_rotation
import churchpresenter.composeapp.generated.resources.canvas_file_path
import churchpresenter.composeapp.generated.resources.canvas_browse
import churchpresenter.composeapp.generated.resources.canvas_scale
import churchpresenter.composeapp.generated.resources.canvas_scale_fit
import churchpresenter.composeapp.generated.resources.canvas_scale_fill
import churchpresenter.composeapp.generated.resources.canvas_scale_stretch
import churchpresenter.composeapp.generated.resources.canvas_scale_none
import churchpresenter.composeapp.generated.resources.canvas_expand_text_field
import churchpresenter.composeapp.generated.resources.canvas_text_content
import churchpresenter.composeapp.generated.resources.close
import churchpresenter.composeapp.generated.resources.canvas_line_spacing
import churchpresenter.composeapp.generated.resources.canvas_font
import churchpresenter.composeapp.generated.resources.canvas_align_horizontal
import churchpresenter.composeapp.generated.resources.canvas_align_vertical
import churchpresenter.composeapp.generated.resources.canvas_render_width
import churchpresenter.composeapp.generated.resources.canvas_render_height
import churchpresenter.composeapp.generated.resources.canvas_fps
import churchpresenter.composeapp.generated.resources.canvas_custom_css
import churchpresenter.composeapp.generated.resources.canvas_browser_url
import churchpresenter.composeapp.generated.resources.canvas_select_image_title
import churchpresenter.composeapp.generated.resources.canvas_select_video_title
import churchpresenter.composeapp.generated.resources.canvas_image_files
import churchpresenter.composeapp.generated.resources.canvas_video_files
import churchpresenter.composeapp.generated.resources.canvas_qr_type_url
import churchpresenter.composeapp.generated.resources.canvas_qr_type_text
import churchpresenter.composeapp.generated.resources.canvas_qr_type_email
import churchpresenter.composeapp.generated.resources.canvas_qr_type_phone
import churchpresenter.composeapp.generated.resources.canvas_qr_type_sms
import churchpresenter.composeapp.generated.resources.canvas_qr_type_wifi
import churchpresenter.composeapp.generated.resources.canvas_qr_type_vcard
import churchpresenter.composeapp.generated.resources.canvas_clock_mode_clock
import churchpresenter.composeapp.generated.resources.canvas_clock_mode_countdown
import churchpresenter.composeapp.generated.resources.canvas_clock_format_24h
import churchpresenter.composeapp.generated.resources.canvas_clock_format_12h
import churchpresenter.composeapp.generated.resources.canvas_decklink_io_warning
import churchpresenter.composeapp.generated.resources.canvas_properties
import churchpresenter.composeapp.generated.resources.canvas_source_color
import churchpresenter.composeapp.generated.resources.canvas_source_image
import churchpresenter.composeapp.generated.resources.canvas_source_text
import churchpresenter.composeapp.generated.resources.canvas_source_video
import churchpresenter.composeapp.generated.resources.canvas_video_loop
import churchpresenter.composeapp.generated.resources.canvas_video_volume
import churchpresenter.composeapp.generated.resources.canvas_transform
import churchpresenter.composeapp.generated.resources.canvas_transparent_bg
import churchpresenter.composeapp.generated.resources.canvas_transform_x
import churchpresenter.composeapp.generated.resources.canvas_transform_y
import churchpresenter.composeapp.generated.resources.canvas_transform_w
import churchpresenter.composeapp.generated.resources.canvas_transform_h
import churchpresenter.composeapp.generated.resources.canvas_qr_default_text
import churchpresenter.composeapp.generated.resources.canvas_decklink_device
import churchpresenter.composeapp.generated.resources.timer_start
import churchpresenter.composeapp.generated.resources.timer_reset
import churchpresenter.composeapp.generated.resources.pause
import churchpresenter.composeapp.generated.resources.ic_folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.churchpresenter.app.churchpresenter.dialogs.filechooser.FileChooser
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.models.SceneSource
import org.churchpresenter.app.churchpresenter.utils.rememberSystemFonts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.jetbrains.compose.resources.painterResource
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.churchpresenter.app.churchpresenter.composables.LabeledCheckbox

@Composable
fun SourcePropertiesPanel(
    source: SceneSource,
    modifier: Modifier = Modifier,
    appSettings: AppSettings? = null,
    fileChooser: FileChooser = FileChooser.platformInstance,
    onSourceUpdate: (SceneSource) -> Unit
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(Res.string.canvas_properties),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        PropertyTextField(stringResource(Res.string.canvas_source_name), source.name) { newName ->
            onSourceUpdate(updateName(source, newName))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(Res.string.canvas_transform), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val t = source.transform
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PropertyFloatField(stringResource(Res.string.canvas_transform_x), t.x, Modifier.weight(1f)) { v ->
                onSourceUpdate(updateTransform(source, t.copy(x = v)))
            }
            PropertyFloatField(stringResource(Res.string.canvas_transform_y), t.y, Modifier.weight(1f)) { v ->
                onSourceUpdate(updateTransform(source, t.copy(y = v)))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PropertyFloatField(stringResource(Res.string.canvas_transform_w), t.width, Modifier.weight(1f)) { v ->
                onSourceUpdate(updateTransform(source, t.copy(width = v.coerceAtLeast(0.01f))))
            }
            PropertyFloatField(stringResource(Res.string.canvas_transform_h), t.height, Modifier.weight(1f)) { v ->
                onSourceUpdate(updateTransform(source, t.copy(height = v.coerceAtLeast(0.01f))))
            }
        }

        PropertySliderWithInput(stringResource(Res.string.canvas_rotation), t.rotation, -180f, 180f, "°") { v ->
            onSourceUpdate(updateTransform(source, t.copy(rotation = v)))
        }
        PropertySlider(stringResource(Res.string.canvas_opacity), t.opacity, 0f, 1f) { v ->
            onSourceUpdate(updateTransform(source, t.copy(opacity = v)))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        when (source) {
            is SceneSource.ImageSource -> ImageProperties(source, onSourceUpdate, fileChooser)
            is SceneSource.TextSource -> TextProperties(source, onSourceUpdate)
            is SceneSource.ColorSource -> ColorProperties(source, onSourceUpdate)
            is SceneSource.VideoSource -> VideoProperties(source, onSourceUpdate, fileChooser)
            is SceneSource.BrowserSource -> BrowserProperties(source, onSourceUpdate)
            is SceneSource.ShapeSource -> ShapeProperties(source, onSourceUpdate)
            is SceneSource.ClockSource -> ClockProperties(source, onSourceUpdate)
            is SceneSource.QRCodeSource -> QRCodeProperties(source, onSourceUpdate)
            is SceneSource.CameraSource -> CameraProperties(source, onSourceUpdate)
            is SceneSource.ScreenCaptureSource -> ScreenCaptureProperties(source, onSourceUpdate)
            is SceneSource.BibleSource -> BibleProperties(source, onSourceUpdate, appSettings)
        }
    }
}

@Composable
private fun ImageProperties(source: SceneSource.ImageSource, onUpdate: (SceneSource) -> Unit, fileChooser: FileChooser) {
    val scope = rememberCoroutineScope()
    val strFilePath = stringResource(Res.string.canvas_file_path)
    val strSelectImage = stringResource(Res.string.canvas_select_image_title)
    val strImageFiles = stringResource(Res.string.canvas_image_files)
    val strBrowse = stringResource(Res.string.canvas_browse)
    val fitLabel = stringResource(Res.string.canvas_scale_fit)
    val fillLabel = stringResource(Res.string.canvas_scale_fill)
    val stretchLabel = stringResource(Res.string.canvas_scale_stretch)
    val noneLabel = stringResource(Res.string.canvas_scale_none)

    Text(stringResource(Res.string.canvas_source_image), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PropertyTextField(strFilePath, source.filePath, Modifier.weight(1f)) { v ->
            onUpdate(source.copy(filePath = v))
        }
        Button(
            onClick = {
                scope.launch {
                    val imageFilter = FileNameExtensionFilter(
                        strImageFiles,
                        "png", "jpg", "jpeg", "gif", "bmp", "webp", "heic", "heif", "svg"
                    )
                    val startPath = if (source.filePath.isNotEmpty()) {
                        try { Path(source.filePath).parent } catch (_: Exception) { null }
                    } else null
                    val file = fileChooser.chooseSingle(
                        path = startPath,
                        filters = listOf(imageFilter),
                        title = strSelectImage,
                        selectDirectory = false
                    )
                    if (file != null) {
                        onUpdate(source.copy(filePath = file.absolutePathString()))
                    }
                }
            },
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_folder),
                contentDescription = strBrowse,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    val scaleOptions = listOf(fitLabel, fillLabel, stretchLabel, noneLabel)
    val scaleMap = mapOf("FIT" to fitLabel, "FILL" to fillLabel, "STRETCH" to stretchLabel, "NONE" to noneLabel)
    val reverseMap = mapOf(fitLabel to "FIT", fillLabel to "FILL", stretchLabel to "STRETCH", noneLabel to "NONE")
    DropdownSelector(
        label = stringResource(Res.string.canvas_scale),
        items = scaleOptions,
        selected = scaleMap[source.contentScale] ?: fitLabel,
        onSelectedChange = { onUpdate(source.copy(contentScale = reverseMap[it] ?: "FIT")) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TextProperties(source: SceneSource.TextSource, onUpdate: (SceneSource) -> Unit) {
    val isTransparentBg = source.backgroundColor.equals("#00000000", ignoreCase = true)

    val availableFonts = rememberSystemFonts()

    Text(stringResource(Res.string.canvas_source_text), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    var textValue by remember(source.text) { mutableStateOf(source.text) }
    var showTextDialog by remember { mutableStateOf(false) }
    StyledTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            onUpdate(source.copy(text = it))
        },
        label = stringResource(Res.string.canvas_text_content),
        singleLine = false,
        minLines = 2,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(Res.string.canvas_expand_text_field),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { showTextDialog = true }.padding(vertical = 2.dp)
    )
    if (showTextDialog) {
        DialogWindow(
            onCloseRequest = { showTextDialog = false },
            state = rememberDialogState(
                width = 600.dp, height = 450.dp
            ),
            title = stringResource(Res.string.canvas_text_content),
            resizable = true
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    StyledTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            onUpdate(source.copy(text = it))
                        },
                        label = stringResource(Res.string.canvas_text_content),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { showTextDialog = false }, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(Res.string.close))
                        }
                    }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.canvas_line_spacing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SlimSlider(
            value = source.lineSpacing / 100f,
            onValueChange = { onUpdate(source.copy(lineSpacing = (it * 100).toInt())) },
            valueRange = 0.5f..3f,
            trailingLabel = "${source.lineSpacing}%",
            modifier = Modifier.weight(1f)
        )
    }
    FontDropdown(
        label = stringResource(Res.string.canvas_font),
        selected = source.fontFamily,
        fonts = availableFonts,
        onSelectedChange = { onUpdate(source.copy(fontFamily = it)) },
        modifier = Modifier.fillMaxWidth()
    )
    PropertyTextField(stringResource(Res.string.canvas_clock_font_size), source.fontSize.toString()) { v ->
        v.toIntOrNull()?.let { onUpdate(source.copy(fontSize = it)) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(stringResource(Res.string.canvas_align_horizontal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalAlignmentButtons(
                selectedAlignment = source.horizontalAlignment,
                onAlignmentChange = { onUpdate(source.copy(horizontalAlignment = it)) },
                leftValue = "left",
                centerValue = "center",
                rightValue = "right"
            )
        }
        Column {
            Text(stringResource(Res.string.canvas_align_vertical), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VerticalAlignmentButtons(
                selectedAlignment = source.verticalAlignment,
                onAlignmentChange = { onUpdate(source.copy(verticalAlignment = it)) },
                topValue = "top",
                middleValue = "center",
                bottomValue = "bottom"
            )
        }
    }
    ColorPickerField(
        color = source.fontColor,
        onColorChange = { onUpdate(source.copy(fontColor = it)) },
        label = stringResource(Res.string.canvas_font_color)
    )
    LabeledCheckbox(
        checked = isTransparentBg,
        onCheckedChange = { checked ->
                onUpdate(source.copy(
                    backgroundColor = if (checked) "#00000000" else "#000000"
                ))
            },
        label = stringResource(Res.string.canvas_transparent_bg),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    if (!isTransparentBg) {
        ColorPickerField(
            color = source.backgroundColor,
            onColorChange = { onUpdate(source.copy(backgroundColor = it)) },
            label = stringResource(Res.string.canvas_bg_color)
        )
    }
}

@Composable
private fun ColorProperties(source: SceneSource.ColorSource, onUpdate: (SceneSource) -> Unit) {
    Text(stringResource(Res.string.canvas_source_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ColorPickerField(
        color = source.color,
        onColorChange = { onUpdate(source.copy(color = it)) },
        label = stringResource(Res.string.canvas_color_1)
    )
    LabeledCheckbox(
        checked = source.isGradient,
        onCheckedChange = { onUpdate(source.copy(isGradient = it)) },
        label = stringResource(Res.string.canvas_gradient),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    PropertySlider("${stringResource(Res.string.canvas_color_1)} ${stringResource(Res.string.canvas_opacity)}", source.sourceOpacity, 0f, 1f) { v ->
        onUpdate(source.copy(sourceOpacity = v))
    }
    if (source.isGradient) {
        ColorPickerField(
            color = source.gradientColor2,
            onColorChange = { onUpdate(source.copy(gradientColor2 = it)) },
            label = stringResource(Res.string.canvas_color_2)
        )
        PropertySlider("${stringResource(Res.string.canvas_color_2)} ${stringResource(Res.string.canvas_opacity)}", source.gradientColor2Opacity, 0f, 1f) { v ->
            onUpdate(source.copy(gradientColor2Opacity = v))
        }
        PropertySliderWithInput(stringResource(Res.string.canvas_angle), source.gradientAngle, 0f, 360f, "°") { v ->
            onUpdate(source.copy(gradientAngle = v))
        }
        PropertySliderWithInput(stringResource(Res.string.position), source.gradientPosition * 100f, 0f, 100f, "%") { v ->
            onUpdate(source.copy(gradientPosition = (v / 100f).coerceIn(0f, 1f)))
        }
    }
}

@Composable
private fun VideoProperties(source: SceneSource.VideoSource, onUpdate: (SceneSource) -> Unit, fileChooser: FileChooser) {
    val scope = rememberCoroutineScope()
    val strFilePath = stringResource(Res.string.canvas_file_path)
    val strSelectVideo = stringResource(Res.string.canvas_select_video_title)
    val strVideoFiles = stringResource(Res.string.canvas_video_files)
    val strBrowse = stringResource(Res.string.canvas_browse)

    Text(stringResource(Res.string.canvas_source_video), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PropertyTextField(strFilePath, source.filePath, Modifier.weight(1f)) { v ->
            onUpdate(source.copy(filePath = v))
        }
        Button(
            onClick = {
                scope.launch {
                    val videoFilter = FileNameExtensionFilter(
                        strVideoFiles,
                        "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm", "m4v"
                    )
                    val startPath = if (source.filePath.isNotEmpty()) {
                        try { Path(source.filePath).parent } catch (_: Exception) { null }
                    } else null
                    val file = fileChooser.chooseSingle(
                        path = startPath,
                        filters = listOf(videoFilter),
                        title = strSelectVideo,
                        selectDirectory = false
                    )
                    if (file != null) {
                        onUpdate(source.copy(filePath = file.absolutePathString()))
                    }
                }
            },
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_folder),
                contentDescription = strBrowse,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    LabeledCheckbox(
        checked = source.loop,
        onCheckedChange = { onUpdate(source.copy(loop = it)) },
        label = stringResource(Res.string.canvas_video_loop),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )

    PropertySlider(stringResource(Res.string.canvas_video_volume), source.volume, 0f, 1f) { v ->
        onUpdate(source.copy(volume = v))
    }
}

@Composable
private fun BrowserProperties(source: SceneSource.BrowserSource, onUpdate: (SceneSource) -> Unit) {
    Text(stringResource(Res.string.canvas_source_browser), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PropertyTextField(stringResource(Res.string.canvas_browser_url), source.url) { v ->
        onUpdate(source.copy(url = v))
    }

    val currentUrlFlow = remember(source.id) { SharedBrowserFrameCache.getCurrentUrl(source.id) }
    if (currentUrlFlow != null) {
        val currentUrl by currentUrlFlow.collectAsState()
        if (currentUrl.isNotBlank() && currentUrl != source.url) {
            Text(
                text = currentUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PropertyTextField(stringResource(Res.string.canvas_render_width), source.renderWidth.toString(), Modifier.weight(1f)) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(renderWidth = it.coerceIn(320, 3840))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_render_height), source.renderHeight.toString(), Modifier.weight(1f)) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(renderHeight = it.coerceIn(240, 2160))) }
        }
    }
    PropertyTextField(stringResource(Res.string.canvas_fps), source.fps.toString()) { v ->
        v.toIntOrNull()?.let { onUpdate(source.copy(fps = it.coerceIn(1, 60))) }
    }
    LabeledCheckbox(
        checked = source.forceTransparent,
        onCheckedChange = { onUpdate(source.copy(forceTransparent = it)) },
        label = stringResource(Res.string.canvas_transparent_bg),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    PropertyTextField(stringResource(Res.string.canvas_custom_css), source.customCss) { v ->
        onUpdate(source.copy(customCss = v))
    }
}

@Composable
private fun ShapeProperties(source: SceneSource.ShapeSource, onUpdate: (SceneSource) -> Unit) {
    Text(
        stringResource(Res.string.canvas_source_shape),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val isStrokeOnly = source.shapeType in listOf("line", "arrow", "freehand")

    if (!isStrokeOnly) {
        LabeledCheckbox(
            checked = source.showStroke,
            onCheckedChange = { onUpdate(source.copy(showStroke = it)) },
            label = stringResource(Res.string.canvas_shape_show_stroke),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            spacing = 4.dp,
        )
    }

    if (isStrokeOnly || source.showStroke) {
        ColorPickerField(
            color = source.strokeColor,
            onColorChange = { onUpdate(source.copy(strokeColor = it)) },
            label = stringResource(Res.string.canvas_shape_stroke_color)
        )
        PropertySlider("${stringResource(Res.string.canvas_shape_stroke_color)} ${stringResource(Res.string.canvas_opacity)}", source.strokeOpacity, 0f, 1f) { v ->
            onUpdate(source.copy(strokeOpacity = v))
        }
    }

    if (!isStrokeOnly) {
        ColorPickerField(
            color = source.fillColor,
            onColorChange = { onUpdate(source.copy(fillColor = it)) },
            label = stringResource(Res.string.canvas_shape_fill_color)
        )
        PropertySlider("${stringResource(Res.string.canvas_shape_fill_color)} ${stringResource(Res.string.canvas_opacity)}", source.fillOpacity, 0f, 1f) { v ->
            onUpdate(source.copy(fillOpacity = v))
        }
    }

    if (isStrokeOnly || source.showStroke) {
        PropertySliderWithInput(
            stringResource(Res.string.canvas_shape_stroke_width),
            source.strokeWidth, 1f, 20f, "px"
        ) { v ->
            onUpdate(source.copy(strokeWidth = v))
        }
    }

    if (!isStrokeOnly) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        LabeledCheckbox(
            checked = source.isGradient,
            onCheckedChange = { onUpdate(source.copy(isGradient = it)) },
            label = stringResource(Res.string.canvas_gradient),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            spacing = 4.dp,
        )
        if (source.isGradient) {
            ColorPickerField(
                color = source.gradientColor2,
                onColorChange = { onUpdate(source.copy(gradientColor2 = it)) },
                label = stringResource(Res.string.canvas_color_2)
            )
            PropertySlider("${stringResource(Res.string.canvas_color_2)} ${stringResource(Res.string.canvas_opacity)}", source.gradientColor2Opacity, 0f, 1f) { v ->
                onUpdate(source.copy(gradientColor2Opacity = v))
            }
            PropertySliderWithInput(stringResource(Res.string.canvas_angle), source.gradientAngle, 0f, 360f, "\u00B0") { v ->
                onUpdate(source.copy(gradientAngle = v))
            }
            PropertySliderWithInput(stringResource(Res.string.position), source.gradientPosition * 100f, 0f, 100f, "%") { v ->
                onUpdate(source.copy(gradientPosition = (v / 100f).coerceIn(0f, 1f)))
            }
        }
    }
}

@Composable
private fun ClockProperties(source: SceneSource.ClockSource, onUpdate: (SceneSource) -> Unit) {
    Text(
        stringResource(Res.string.canvas_source_clock),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val clockLabel = stringResource(Res.string.canvas_clock_mode_clock)
    val countdownLabel = stringResource(Res.string.canvas_clock_mode_countdown)
    val format24hLabel = stringResource(Res.string.canvas_clock_format_24h)
    val format12hLabel = stringResource(Res.string.canvas_clock_format_12h)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DropdownSelector(
            label = stringResource(Res.string.canvas_clock_mode),
            items = listOf(clockLabel, countdownLabel),
            selected = if (source.mode == "countdown") countdownLabel else clockLabel,
            onSelectedChange = { onUpdate(source.copy(mode = if (it == countdownLabel) "countdown" else "clock")) }
        )
        DropdownSelector(
            label = stringResource(Res.string.canvas_clock_format),
            items = listOf(format24hLabel, format12hLabel),
            selected = if (source.timeFormat == "12h") format12hLabel else format24hLabel,
            onSelectedChange = { onUpdate(source.copy(timeFormat = if (it == format12hLabel) "12h" else "24h")) }
        )
    }
    LabeledCheckbox(
        checked = source.showHours,
        onCheckedChange = { onUpdate(source.copy(showHours = it)) },
        label = stringResource(Res.string.canvas_clock_show_hours),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    LabeledCheckbox(
        checked = source.showSeconds,
        onCheckedChange = { onUpdate(source.copy(showSeconds = it)) },
        label = stringResource(Res.string.canvas_clock_show_seconds),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    LabeledCheckbox(
        checked = source.bold,
        onCheckedChange = { onUpdate(source.copy(bold = it)) },
        label = stringResource(Res.string.canvas_text_bold),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    PropertyTextField(stringResource(Res.string.canvas_clock_font_size), source.fontSize.toString()) { v ->
        v.toIntOrNull()?.let { onUpdate(source.copy(fontSize = it.coerceIn(8, 500))) }
    }
    ColorPickerField(
        color = source.fontColor,
        onColorChange = { onUpdate(source.copy(fontColor = it)) },
        label = stringResource(Res.string.canvas_text_color)
    )
    ColorPickerField(
        color = source.backgroundColor,
        onColorChange = { onUpdate(source.copy(backgroundColor = it)) },
        label = stringResource(Res.string.canvas_text_bg_color)
    )
    if (source.mode == "countdown") {
        PropertyTextField(stringResource(Res.string.canvas_clock_target_hour), source.targetHour.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(targetHour = it.coerceIn(0, 99))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_clock_target_minute), source.targetMinute.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(targetMinute = it.coerceIn(0, 59))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_clock_target_second), source.targetSecond.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(targetSecond = it.coerceIn(0, 59))) }
        }

        val totalSeconds = source.targetHour * 3600 + source.targetMinute * 60 + source.targetSecond
        val timerState = TimerStateManager.getState(source.id, totalSeconds)
        val isRunning = timerState.isRunning
        val remaining = timerState.remainingSeconds

        val hh = remaining / 3600
        val mm = (remaining % 3600) / 60
        val ss = remaining % 60

        Spacer(Modifier.height(8.dp))
        Text(
            "%02d:%02d:%02d".format(hh, mm, ss),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { TimerStateManager.setRunning(source.id, totalSeconds, !isRunning) },
                enabled = remaining > 0 || isRunning,
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(if (isRunning) stringResource(Res.string.pause) else stringResource(Res.string.timer_start), style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { TimerStateManager.reset(source.id, totalSeconds) },
                modifier = Modifier.weight(1f).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(stringResource(Res.string.timer_reset), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun QRCodeProperties(source: SceneSource.QRCodeSource, onUpdate: (SceneSource) -> Unit) {
    Text(
        stringResource(Res.string.canvas_source_qrcode),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val urlLabel    = stringResource(Res.string.canvas_qr_type_url)
    val textLabel   = stringResource(Res.string.canvas_qr_type_text)
    val emailLabel  = stringResource(Res.string.canvas_qr_type_email)
    val phoneLabel  = stringResource(Res.string.canvas_qr_type_phone)
    val smsLabel    = stringResource(Res.string.canvas_qr_type_sms)
    val wifiLabel   = stringResource(Res.string.canvas_qr_type_wifi)
    val vcardLabel  = stringResource(Res.string.canvas_qr_type_vcard)
    val defaultText = stringResource(Res.string.canvas_qr_default_text)
    val typeOptions = listOf(urlLabel, textLabel, emailLabel, phoneLabel, smsLabel, wifiLabel, vcardLabel)
    val typeMap = mapOf(
        "url" to urlLabel, "text" to textLabel, "email" to emailLabel, "phone" to phoneLabel,
        "sms" to smsLabel, "wifi" to wifiLabel, "vcard" to vcardLabel
    )
    val reverseTypeMap = mapOf(
        urlLabel to "url", textLabel to "text", emailLabel to "email", phoneLabel to "phone",
        smsLabel to "sms", wifiLabel to "wifi", vcardLabel to "vcard"
    )
    DropdownSelector(
        label = stringResource(Res.string.canvas_qr_type),
        items = typeOptions,
        selected = typeMap[source.contentType] ?: "URL",
        onSelectedChange = { newType ->
            val type = reverseTypeMap[newType] ?: "url"
            val prefill = when (type) {
                "url" -> "https://example.com"
                "text" -> defaultText
                "email" -> "mailto:name@example.com"
                "phone" -> "tel:+1234567890"
                "sms" -> "smsto:+1234567890:Message"
                "vcard" -> "BEGIN:VCARD\nVERSION:3.0\nFN:Name\nTEL:+1234567890\nEMAIL:name@example.com\nEND:VCARD"
                else -> source.content
            }
            onUpdate(source.copy(contentType = type, content = if (type != "wifi") prefill else source.content))
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (source.contentType == "wifi") {
        PropertyTextField(stringResource(Res.string.canvas_qr_wifi_ssid), source.wifiSsid) { v ->
            onUpdate(source.copy(wifiSsid = v))
        }
        PropertyTextField(stringResource(Res.string.canvas_qr_wifi_password), source.wifiPassword) { v ->
            onUpdate(source.copy(wifiPassword = v))
        }
        DropdownSelector(
            label = stringResource(Res.string.canvas_qr_wifi_encryption),
            items = listOf("WPA", "WPA2", "WPA3", "WEP", "None"),
            selected = source.wifiEncryption,
            onSelectedChange = { onUpdate(source.copy(wifiEncryption = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        LabeledCheckbox(
            checked = source.wifiHidden,
            onCheckedChange = { onUpdate(source.copy(wifiHidden = it)) },
            label = stringResource(Res.string.canvas_qr_wifi_hidden),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            spacing = 4.dp,
        )
    } else {
        PropertyTextField(stringResource(Res.string.canvas_qr_content), source.content) { v ->
            onUpdate(source.copy(content = v))
        }
    }
    ColorPickerField(
        color = source.foregroundColor,
        onColorChange = { onUpdate(source.copy(foregroundColor = it)) },
        label = stringResource(Res.string.canvas_qr_foreground)
    )
    LabeledCheckbox(
        checked = source.transparentBackground,
        onCheckedChange = { onUpdate(source.copy(transparentBackground = it)) },
        label = stringResource(Res.string.canvas_transparent_bg),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        spacing = 4.dp,
    )
    if (!source.transparentBackground) {
        ColorPickerField(
            color = source.backgroundColor,
            onColorChange = { onUpdate(source.copy(backgroundColor = it)) },
            label = stringResource(Res.string.canvas_qr_background)
        )
    }
    DropdownSelector(
        label = stringResource(Res.string.canvas_qr_error_correction),
        items = listOf("L", "M", "Q", "H"),
        selected = source.errorCorrection,
        onSelectedChange = { onUpdate(source.copy(errorCorrection = it)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CameraProperties(source: SceneSource.CameraSource, onUpdate: (SceneSource) -> Unit) {
    Text(
        stringResource(Res.string.canvas_source_camera),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val deckLinkDeviceFormat = stringResource(Res.string.canvas_decklink_device)
    var devices by remember { mutableStateOf(listCameraDevicesWithDeckLink(deckLinkDeviceFormat)) }
    val noCamerasLabel = stringResource(Res.string.canvas_camera_none_found)

    Button(
        onClick = { devices = listCameraDevicesWithDeckLink(deckLinkDeviceFormat) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(stringResource(Res.string.canvas_camera_refresh), style = MaterialTheme.typography.labelSmall)
    }

    if (devices.isNotEmpty()) {
        val items = devices.map { it.displayName }
        DropdownSelector(
            label = stringResource(Res.string.canvas_camera_device),
            items = items,
            selected = selectedCameraName(devices, source),
            onSelectedChange = { selected ->
                val device = devices.find { it.displayName == selected }
                if (device != null) {
                    onUpdate(cameraSourceOn(source, device))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (source.isDeckLink && source.deckLinkIndex >= 0) {

            if (DeckLinkManager.isOutputActive(source.deckLinkIndex)) {
                Text(
                    text = stringResource(Res.string.canvas_decklink_io_warning),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            var connections by remember { mutableStateOf<List<DeckLinkManager.VideoConnection>>(emptyList()) }
            var modes by remember { mutableStateOf<List<DeckLinkManager.InputMode>>(emptyList()) }

            LaunchedEffect(source.deckLinkIndex) {
                withContext(Dispatchers.IO) {
                    connections = DeckLinkManager.listVideoConnections(source.deckLinkIndex)
                    modes = DeckLinkManager.listInputModes(source.deckLinkIndex)
                }
            }

            LaunchedEffect(connections, source.videoConnection) {
                if (source.videoConnection == 0 && connections.isNotEmpty()) {
                    onUpdate(source.copy(videoConnection = connections.first().value))
                }
            }

            if (connections.isNotEmpty()) {
                val connItems = connections.map { it.name }
                DropdownSelector(
                    label = stringResource(Res.string.canvas_camera_connection),
                    items = connItems,
                    selected = selectedConnectionName(connections, source.videoConnection),
                    onSelectedChange = { selected ->
                        val conn = connections.find { it.name == selected }
                        if (conn != null) {
                            onUpdate(source.copy(videoConnection = conn.value))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val autoLabel = stringResource(Res.string.canvas_camera_mode_auto)
            val modeItems = listOf(autoLabel) + modes.map { it.name }
            DropdownSelector(
                label = stringResource(Res.string.canvas_camera_mode),
                items = modeItems,
                selected = selectedModeName(modes, source.videoFormat, autoLabel),
                onSelectedChange = { selected ->
                    if (selected == autoLabel) {
                        onUpdate(source.copy(videoFormat = ""))
                    } else {
                        val mode = modes.find { it.name == selected }
                        if (mode != null) {
                            onUpdate(source.copy(videoFormat = mode.encodedValue))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else if (source.devicePath.isNotEmpty() && !source.isDeckLink) {

            var formats by remember { mutableStateOf<List<CameraFormat>>(emptyList()) }
            LaunchedEffect(source.devicePath) {
                formats = withContext(Dispatchers.IO) {
                    listCameraFormats(source.devicePath, source.deviceName)
                }
            }

            val autoLabel = stringResource(Res.string.canvas_camera_format_auto)
            val formatItems = listOf(autoLabel) + formats.map { it.displayName }
            DropdownSelector(
                label = stringResource(Res.string.canvas_camera_format),
                items = formatItems,
                selected = selectedFormatName(formats, source.videoFormat, autoLabel),
                onSelectedChange = { selected ->
                    if (selected == autoLabel) {
                        onUpdate(source.copy(videoFormat = ""))
                    } else {
                        val fmt = formats.find { it.displayName == selected }
                        if (fmt != null) {
                            onUpdate(source.copy(videoFormat = fmt.encodedValue))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Text(
            noCamerasLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val osName = System.getProperty("os.name", "").lowercase()
    if (osName.contains("linux") && devices.isEmpty()) {
        Text(
            stringResource(Res.string.canvas_camera_v4l2_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (!osName.contains("linux")) {
        val ffmpegAvailable by remember { mutableStateOf(isFfmpegAvailable()) }
        if (!ffmpegAvailable) {
            Text(
                stringResource(Res.string.canvas_camera_ffmpeg_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScreenCaptureProperties(source: SceneSource.ScreenCaptureSource, onUpdate: (SceneSource) -> Unit) {
    Text(
        stringResource(Res.string.canvas_source_screen_capture),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val regionLabel = stringResource(Res.string.canvas_capture_mode_region)
    val windowLabel = stringResource(Res.string.canvas_capture_mode_window)
    DropdownSelector(
        label = stringResource(Res.string.canvas_capture_mode),
        items = listOf(regionLabel, windowLabel),
        selected = if (source.captureMode == "window") windowLabel else regionLabel,
        onSelectedChange = {
            val mode = if (it == windowLabel) "window" else "region"
            onUpdate(source.copy(captureMode = mode))
        },
        modifier = Modifier.fillMaxWidth()
    )
    if (source.captureMode == "window") {
        var windows by remember { mutableStateOf(listOpenWindows()) }
        val windowTitles = windows.map { it.title }

        Button(
            onClick = { windows = listOpenWindows() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(Res.string.canvas_capture_refresh_windows), style = MaterialTheme.typography.labelSmall)
        }

        if (windowTitles.isNotEmpty()) {
            DropdownSelector(
                label = stringResource(Res.string.canvas_capture_window),
                items = windowTitles,
                selected = if (source.windowTitle in windowTitles) source.windowTitle else windowTitles.first(),
                onSelectedChange = { selected ->
                    val win = windows.find { it.title == selected }
                    val idStr = if (win != null && win.id != 0L) "0x%x".format(win.id) else ""
                    onUpdate(source.copy(windowTitle = selected, windowId = idStr))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        PropertyTextField(stringResource(Res.string.canvas_capture_x), source.captureX.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(captureX = it.coerceAtLeast(0))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_capture_y), source.captureY.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(captureY = it.coerceAtLeast(0))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_capture_width), source.captureWidth.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(captureWidth = it.coerceAtLeast(1))) }
        }
        PropertyTextField(stringResource(Res.string.canvas_capture_height), source.captureHeight.toString()) { v ->
            v.toIntOrNull()?.let { onUpdate(source.copy(captureHeight = it.coerceAtLeast(1))) }
        }
    }
    PropertySliderWithInput(stringResource(Res.string.canvas_capture_interval), source.captureInterval.toFloat(), 33f, 1000f, "ms") { v ->
        onUpdate(source.copy(captureInterval = v.toInt()))
    }
}
