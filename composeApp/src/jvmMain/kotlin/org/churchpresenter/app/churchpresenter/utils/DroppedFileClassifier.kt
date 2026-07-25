package org.churchpresenter.app.churchpresenter.utils

/**
 * Which schedule action a dropped file maps to, decided by its extension. Extracted from
 * ScheduleTab's `handleDroppedFiles` so the extension → action rule is tested without touching the
 * filesystem or the view model. The folder-vs-file and image-count handling stays in the tab (it is
 * genuine File I/O); only this pure classification moves here.
 */

/** Image extensions — also used by the tab to count pictures inside a dropped folder. */
internal val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val VIDEO_EXTENSIONS = setOf("mp4", "avi", "mov", "mkv", "webm")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac")
private val PRESENTATION_EXTENSIONS = setOf("ppt", "pptx", "key", "pdf")

internal enum class DroppedFileAction { PRESENTATION, MEDIA, PICTURE, LOWER_THIRD, NONE }

/** The action a single dropped file (already lower-cased [extension]) should be added as. */
internal fun classifyDroppedFile(extension: String): DroppedFileAction = when {
    extension in PRESENTATION_EXTENSIONS -> DroppedFileAction.PRESENTATION
    extension in VIDEO_EXTENSIONS || extension in AUDIO_EXTENSIONS -> DroppedFileAction.MEDIA
    extension in IMAGE_EXTENSIONS -> DroppedFileAction.PICTURE
    extension == "json" -> DroppedFileAction.LOWER_THIRD
    else -> DroppedFileAction.NONE
}
