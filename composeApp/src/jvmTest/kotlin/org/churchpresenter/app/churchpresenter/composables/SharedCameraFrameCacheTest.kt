package org.churchpresenter.app.churchpresenter.composables

import org.churchpresenter.app.churchpresenter.models.SceneSource
import org.churchpresenter.app.churchpresenter.models.SourceTransform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * [SharedCameraFrameCache] multiplexes real camera/DeckLink capture processes, which this suite
 * cannot spin up (no physical camera, no ffmpeg pipeline to feed it). What *is* covered: the pure
 * cache-key logic ([SharedCameraFrameCache.keyFor], widened to `internal`), and the [acquire]/
 * [release] ref-counting bookkeeping — exercised with a `devicePath` scheme
 * (`"bogus://"`) that `runFfmpegCapture` recognizes as invalid and returns from immediately,
 * without ever spawning a process, so the background capture coroutine has nothing unsafe to do.
 * [killFfmpegProcess] needs no camera at all — it's tested directly against a short-lived, real
 * child process. [buildFfmpegCommand] is the pure command-line-building logic pulled out of
 * `runFfmpegCapture` specifically so it could be tested without ever starting ffmpeg.
 */
class SharedCameraFrameCacheTest {

    private fun camera(
        devicePath: String = "bogus://x",
        isDeckLink: Boolean = false,
        deckLinkIndex: Int = -1,
        videoFormat0: String = "1920x1080@30",
    ) =
        SceneSource.CameraSource(
            id = "cam", name = "Cam", transform = SourceTransform(),
            devicePath = devicePath, videoFormat = videoFormat0,
            videoConnection = 2, isDeckLink = isDeckLink, deckLinkIndex = deckLinkIndex,
        )

    // ── keyFor ─────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `keyFor builds a decklink key when isDeckLink is true with a non-negative index`() {
        val source = camera(isDeckLink = true, deckLinkIndex = 3)
        assertEquals("decklink:3:1920x1080@30:2", SharedCameraFrameCache.keyFor(source))
    }

    @Test
    fun `keyFor falls back to an ffmpeg key when isDeckLink is true but the index is negative`() {
        val source = camera(devicePath = "avfoundation://0", isDeckLink = true, deckLinkIndex = -1)
        assertEquals("ffmpeg:avfoundation://0:1920x1080@30", SharedCameraFrameCache.keyFor(source))
    }

    @Test
    fun `keyFor builds an ffmpeg key when isDeckLink is false`() {
        val source = camera(devicePath = "v4l2:///dev/video0", isDeckLink = false)
        assertEquals("ffmpeg:v4l2:///dev/video0:1920x1080@30", SharedCameraFrameCache.keyFor(source))
    }

    // ── acquire / release bookkeeping ─────────────────────────────────────────────────────────

    @Test
    fun `releasing a source that was never acquired does not throw`() {
        SharedCameraFrameCache.release(camera(devicePath = "bogus://never-acquired"))
    }

    @Test
    fun `acquire starts with no frame and no error`() {
        val source = camera(devicePath = "bogus://fresh")
        val flows = SharedCameraFrameCache.acquire(source)
        try {
            assertNull(flows.frame.value)
            assertNull(flows.error.value)
        } finally {
            SharedCameraFrameCache.release(source)
        }
    }

    @Test
    fun `two acquires of the same key share the same underlying flows`() {
        val source = camera(devicePath = "bogus://shared")
        val first = SharedCameraFrameCache.acquire(source)
        val second = SharedCameraFrameCache.acquire(source)
        try {
            assertSame(first.frame, second.frame)
            assertSame(first.error, second.error)
        } finally {
            // Balance both acquires — the first release only drops the ref count to 1.
            SharedCameraFrameCache.release(source)
            SharedCameraFrameCache.release(source)
        }
    }

    // ── buildFfmpegCommand ─────────────────────────────────────────────────────────────────────

    @Test
    fun `buildFfmpegCommand returns null for an unrecognized device path scheme`() {
        assertNull(buildFfmpegCommand(camera(devicePath = "bogus://x", videoFormat0 = "")))
    }

    @Test
    fun `buildFfmpegCommand builds a dshow command with the device name and no format args`() {
        val cmd = buildFfmpegCommand(camera(devicePath = "dshow://:dshow-vdev=Integrated Webcam", videoFormat0 = ""))
        assertEquals(
            listOf("ffmpeg", "-f", "dshow", "-i", "video=Integrated Webcam", "-an", "-vf", "fps=30", "-pix_fmt", "bgra", "-f", "rawvideo", "-"),
            cmd,
        )
    }

    @Test
    fun `buildFfmpegCommand builds a v4l2 command with video_size and framerate when the format matches`() {
        val cmd = buildFfmpegCommand(camera(devicePath = "v4l2:///dev/video0", videoFormat0 = "1280x720@30"))
        assertEquals(
            listOf(
                "ffmpeg", "-f", "v4l2", "-video_size", "1280x720", "-framerate", "30",
                "-i", "/dev/video0", "-an", "-vf", "fps=30", "-pix_fmt", "bgra", "-f", "rawvideo", "-",
            ),
            cmd,
        )
    }

    @Test
    fun `buildFfmpegCommand builds an avfoundation command appending the none audio index`() {
        val cmd = buildFfmpegCommand(camera(devicePath = "avfoundation://0", videoFormat0 = ""))
        assertEquals(
            listOf("ffmpeg", "-f", "avfoundation", "-i", "0:none", "-an", "-vf", "fps=30", "-pix_fmt", "bgra", "-f", "rawvideo", "-"),
            cmd,
        )
    }

    @Test
    fun `buildFfmpegCommand omits video_size and framerate when the format string does not match the pattern`() {
        val cmd = buildFfmpegCommand(camera(devicePath = "v4l2:///dev/video0", videoFormat0 = "not-a-real-format"))
        assertEquals(
            listOf("ffmpeg", "-f", "v4l2", "-i", "/dev/video0", "-an", "-vf", "fps=30", "-pix_fmt", "bgra", "-f", "rawvideo", "-"),
            cmd,
        )
    }

    // ── parseFfmpegVideoDimensions ─────────────────────────────────────────────────────────────

    @Test
    fun `parseFfmpegVideoDimensions reads width and height from a real ffmpeg stream announcement`() {
        val line = "Stream #0:0: Video: rawvideo (BGRA / 0x41524742), bgra, 1280x720, 30 fps, 30 tbr"
        assertEquals(1280 to 720, parseFfmpegVideoDimensions(line))
    }

    @Test
    fun `parseFfmpegVideoDimensions ignores lines with no Video marker`() {
        assertNull(parseFfmpegVideoDimensions("Input #0, dshow, from 'video=Integrated Webcam':"))
    }

    @Test
    fun `parseFfmpegVideoDimensions ignores Video lines that are not the bgra stream`() {
        assertNull(parseFfmpegVideoDimensions("Stream #0:0: Video: rawvideo (RGB[24] / 0x18424752), rgb24, 640x480, 30 fps"))
    }

    @Test
    fun `parseFfmpegVideoDimensions ignores a bgra Video line with no dimensions after it`() {
        assertNull(parseFfmpegVideoDimensions("Stream #0:0: Video: rawvideo, bgra, unknown size, 30 fps"))
    }

    // ── bgraBytesToArgbPixels ──────────────────────────────────────────────────────────────────

    @Test
    fun `bgraBytesToArgbPixels packs BGRA bytes into ARGB ints`() {
        // One pixel: B=0x11 G=0x22 R=0x33 A=0xFF -> ARGB packed 0xFF332211
        val frameBuf = byteArrayOf(0x11, 0x22, 0x33.toByte(), 0xFF.toByte())
        val pixelBuf = IntArray(1)

        bgraBytesToArgbPixels(frameBuf, pixelBuf)

        assertEquals(0xFF332211.toInt(), pixelBuf[0])
    }

    @Test
    fun `bgraBytesToArgbPixels converts every pixel in order`() {
        val frameBuf = byteArrayOf(
            0x00, 0x00, 0x00, 0xFF.toByte(), // pixel 0: opaque black
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // pixel 1: opaque white
        )
        val pixelBuf = IntArray(2)

        bgraBytesToArgbPixels(frameBuf, pixelBuf)

        assertEquals(0xFF000000.toInt(), pixelBuf[0])
        assertEquals(0xFFFFFFFF.toInt(), pixelBuf[1])
    }

    // ── killFfmpegProcess ──────────────────────────────────────────────────────────────────────

    @Test
    fun `killFfmpegProcess terminates an already-exited process without throwing`() {
        val process = ProcessBuilder("true").start()
        process.waitFor()
        killFfmpegProcess(process)
    }

    @Test
    fun `killFfmpegProcess on a forced Windows os name falls back gracefully when taskkill is unavailable`() {
        val savedOsName = System.getProperty("os.name", "")
        try {
            System.setProperty("os.name", "Windows 11")
            val process = ProcessBuilder("true").start()
            process.waitFor()
            killFfmpegProcess(process) // taskkill isn't on PATH here — both inner try/catches absorb it
        } finally {
            System.setProperty("os.name", savedOsName)
        }
    }
}
