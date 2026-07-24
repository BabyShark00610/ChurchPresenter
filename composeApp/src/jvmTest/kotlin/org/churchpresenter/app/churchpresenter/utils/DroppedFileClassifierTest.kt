package org.churchpresenter.app.churchpresenter.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How a dropped file's extension maps to a schedule action. A wrong category adds the file as the
 * wrong item type (a video as a picture, a deck as media), so every branch of the classifier is
 * pinned here — plus the unknown-extension fallthrough that must add nothing.
 */
class DroppedFileClassifierTest {

    @Test fun `presentation decks classify as presentation`() {
        for (ext in listOf("ppt", "pptx", "key", "pdf")) {
            assertEquals(DroppedFileAction.PRESENTATION, classifyDroppedFile(ext), ext)
        }
    }

    @Test fun `video and audio classify as media`() {
        for (ext in listOf("mp4", "avi", "mov", "mkv", "webm", "mp3", "wav", "flac")) {
            assertEquals(DroppedFileAction.MEDIA, classifyDroppedFile(ext), ext)
        }
    }

    @Test fun `images classify as picture`() {
        for (ext in IMAGE_EXTENSIONS) {
            assertEquals(DroppedFileAction.PICTURE, classifyDroppedFile(ext), ext)
        }
    }

    @Test fun `a json file classifies as a lower third`() =
        assertEquals(DroppedFileAction.LOWER_THIRD, classifyDroppedFile("json"))

    @Test fun `an unknown extension classifies as none`() {
        assertEquals(DroppedFileAction.NONE, classifyDroppedFile("txt"))
        assertEquals(DroppedFileAction.NONE, classifyDroppedFile(""))
        assertEquals(DroppedFileAction.NONE, classifyDroppedFile("docx"))
    }

    @Test fun `the image extension set is exposed for the folder-count path`() =
        assertTrue("jpg" in IMAGE_EXTENSIONS && "png" in IMAGE_EXTENSIONS)
}
