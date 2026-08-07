@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.churchpresenter.app.churchpresenter.data.settings.AppSettings
import org.churchpresenter.app.churchpresenter.tabs.PictureLabel
import org.churchpresenter.app.churchpresenter.tabs.RecentPictureFolders
import org.churchpresenter.app.churchpresenter.tabs.openAnimationDropdown
import org.churchpresenter.app.churchpresenter.tabs.openIntervalEditor
import org.churchpresenter.app.churchpresenter.tabs.openTransitionEditor
import org.churchpresenter.app.churchpresenter.tabs.pictureButton
import org.churchpresenter.app.churchpresenter.tabs.picturesTab
import org.churchpresenter.app.churchpresenter.tabs.showsExactly
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.viewmodel.PicturesViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.PresenterManager
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Every state the Pictures tab can be found in, shot in both themes.
 *
 * The folder is built per shot rather than once for the class: [picturesTab] deletes the folder it
 * was given when it returns, and [stackedThemes] runs the body twice — so a folder built outside
 * would be gone by the dark-theme pass. Hence [shoot] takes a *factory*, not a folder.
 *
 * The images are real gradients rather than the 4×4 placeholders the behaviour suites use, because
 * here the thumbnail is the thing being reviewed: a grid of identical grey squares would hide a
 * change to how thumbnails are cropped, scaled or bordered.
 */
class PicturesTabScreenshotTest {

    // ── Fixtures ────────────────────────────────────────────────────────────────

    /** A recognisable photo, so two thumbnails side by side are visibly different images. */
    private fun writePhoto(dir: File, name: String, index: Int) {
        val image = BufferedImage(480, 300, BufferedImage.TYPE_INT_RGB)
        val canvas = image.createGraphics()
        val hue = (index * 0.14f) % 1f
        canvas.paint = GradientPaint(
            0f, 0f, Color.getHSBColor(hue, 0.5f, 0.9f),
            480f, 300f, Color.getHSBColor((hue + 0.09f) % 1f, 0.85f, 0.4f),
        )
        canvas.fillRect(0, 0, 480, 300)
        canvas.dispose()
        ImageIO.write(image, name.substringAfterLast('.'), File(dir, name))
    }

    /**
     * A picture folder at a fixed path, rebuilt from scratch.
     *
     * Deliberately not a temp directory: the tab prints the open folder's absolute path across the
     * top of every shot, so `createTempDirectory` would put a fresh random string into the image on
     * every recording — each re-record a diff, and the base-vs-branch render the screenshots
     * workflow does would report every state as changed, every time. [picturesTab] deletes whatever
     * folder it is handed, so this recreates rather than reuses.
     */
    private fun folderOf(folderName: String, vararg names: String): File {
        // Absolute: the view model keys its thumbnails by File, and a relative one is not equal to
        // the absolute File its own `storageDirectory` reload produces.
        val dir = File(FIXTURES, folderName).absoluteFile
        dir.deleteRecursively()
        dir.mkdirs()
        names.forEachIndexed { index, name -> writePhoto(dir, name, index) }
        return dir
    }

    private fun gallery() = folderOf("Sunday Service", *GALLERY)

    // ── Harness ─────────────────────────────────────────────────────────────────

    private fun shoot(
        name: String,
        folder: () -> File? = { gallery() },
        settings: (AppSettings) -> AppSettings = { it },
        /** Off for the one shot that pins what the tab looks like with no output to go live on. */
        presenter: Boolean = true,
        width: Dp? = null,
        rootIndex: Int = 0,
        drive: ComposeUiTest.(PicturesViewModel) -> Unit = { awaitAll(it) },
    ) = stackedThemes(SECTION, name) { mode, file ->
        picturesTab(
            folder = folder(),
            settings = settings,
            presenterManager = if (presenter) PresenterManager() else null,
            width = width,
            themeMode = mode,
        ) { vm, _ ->
            drive(vm)
            captureTo(file, rootIndex)
        }
    }

    /**
     * Waits until no thumbnail is still loading — until they are decoded the grid is a wall of
     * "Loading…", and a shot taken a frame early bakes one of those in.
     *
     * The decode is waited for on the view model's own map, but that alone is not enough: the map
     * filling is what *triggers* the recomposition that swaps the placeholder for the image, so the
     * placeholder text going away is the signal that the frame about to be captured has the image
     * in it. (Waiting for the drawn thumbnails instead would hang: a narrow panel or a long folder
     * leaves items below the fold that the lazy grid never composes at all.)
     */
    private fun ComposeUiTest.awaitAll(vm: PicturesViewModel) {
        if (vm.images.isEmpty()) return
        waitUntil("every thumbnail decoded") { vm.images.all { it in vm.thumbnails } }
        waitUntil("no thumbnail still loading") { !showsExactly(PictureLabel.LOADING) }
        waitForIdle()
    }

    // ── Recent folders ──────────────────────────────────────────────────────────

    private lateinit var savedFolders: List<String>
    private lateinit var savedPinned: List<String>

    @BeforeTest
    fun snapshotRecents() {
        savedFolders = RecentPictureFolders.folders.toList()
        savedPinned = RecentPictureFolders.pinned.toList()
        RecentPictureFolders.folders.clear()
        RecentPictureFolders.pinned.clear()
    }

    @AfterTest
    fun restoreRecents() {
        RecentPictureFolders.folders.clear()
        RecentPictureFolders.folders.addAll(savedFolders)
        RecentPictureFolders.pinned.clear()
        RecentPictureFolders.pinned.addAll(savedPinned)
    }

    // ── The tab as an operator finds it ─────────────────────────────────────────

    @Test
    fun `no folder chosen yet`() = shoot("no_folder", folder = { null })

    @Test
    fun browsing() = shoot("browsing")

    @Test
    fun `a single image folder`() =
        shoot("single_image", folder = { folderOf("Notices", "Announcement.png") })

    @Test
    fun `a folder deep enough to fill the grid`() =
        shoot("many_images", folder = { folderOf("Slide Wall", *BIG_GALLERY) })

    @Test
    fun `every image format the tab reads`() =
        shoot("image_formats", folder = { folderOf("Mixed Formats", *EVERY_FORMAT) })

    // ── Selection and transport ─────────────────────────────────────────────────

    @Test
    fun `a later image selected`() = shoot("image_selected") { vm ->
        awaitAll(vm)
        vm.selectImage(3)
        waitForIdle()
    }

    @Test
    fun `the slideshow running`() = shoot("playing") { vm ->
        awaitAll(vm)
        pictureButton(PictureLabel.PLAY).performClick()
        waitForIdle()
    }

    @Test
    fun `looping turned off`() = shoot(
        "loop_off",
        settings = { it.copy(pictureSettings = it.pictureSettings.copy(isLooping = false)) },
    )

    // ── Button states ───────────────────────────────────────────────────────────

    @Test
    fun `with no output to go live on the button is gone`() = shoot("no_presenter", presenter = false)

    @Test
    fun `an empty folder disables every action`() = shoot("actions_disabled", folder = { folderOf("Empty Folder") })

    // ── Settings tiles ──────────────────────────────────────────────────────────

    @Test
    fun `settings tiles carrying non-default values`() = shoot(
        "settings_customised",
        settings = {
            it.copy(
                pictureSettings = it.pictureSettings.copy(
                    autoScrollInterval = 12f,
                    transitionDuration = 1200f,
                    animationType = Constants.ANIMATION_SLIDE_LEFT,
                )
            )
        },
    )

    @Test
    fun `the animation picker`() = shoot("animation_picker", rootIndex = 1) { vm ->
        awaitAll(vm)
        openAnimationDropdown()
    }

    @Test
    fun `the auto-scroll interval editor`() = shoot("interval_editor", rootIndex = 1) { vm ->
        awaitAll(vm)
        openIntervalEditor()
    }

    @Test
    fun `the transition duration editor`() = shoot("transition_editor", rootIndex = 1) { vm ->
        awaitAll(vm)
        openTransitionEditor()
    }

    // ── The recent-folders bar ──────────────────────────────────────────────────

    @Test
    fun `the recent folders bar, with the open folder marked`() = shoot(
        "recent_folders",
        folder = {
            gallery().also { open ->
                RecentPictureFolders.folders.clear()
                RecentPictureFolders.pinned.clear()
                RecentPictureFolders.folders.addAll(
                    listOf(open.absolutePath, "/Volumes/Services/photos/Baptism", "/Volumes/Services/photos/Youth Camp"),
                )
                RecentPictureFolders.pinned.add("/Volumes/Services/photos/Every Week")
            }
        },
    )

    // ── How width reshapes it ───────────────────────────────────────────────────

    @Test
    fun `a narrow panel wraps the controls and the grid`() = shoot("narrow_panel", width = 420.dp)

    @Test
    fun `a half-width panel`() = shoot("medium_panel", width = 760.dp)

    private companion object {
        const val SECTION = "picturesTab"

        /** Under `build/`, so the fixtures are throwaway even though their path is fixed. */
        val FIXTURES = File("build/screenshot-fixtures/pictures")

        val GALLERY = arrayOf(
            "01 Welcome.png",
            "02 Sunrise.jpg",
            "03 Baptism.png",
            "04 Youth Camp.jpg",
            "05 Choir.png",
            "06 Missions.jpg",
        )

        val BIG_GALLERY = Array(14) { "%02d Slide %d.png".format(it + 1, it + 1) }

        /** One of each extension the view model decodes without a platform tool — HEIC needs one. */
        val EVERY_FORMAT = arrayOf(
            "01 Banner.png",
            "02 Photo.jpg",
            "03 Scan.jpeg",
            "04 Loop.gif",
            "05 Bitmap.bmp",
        )
    }
}
