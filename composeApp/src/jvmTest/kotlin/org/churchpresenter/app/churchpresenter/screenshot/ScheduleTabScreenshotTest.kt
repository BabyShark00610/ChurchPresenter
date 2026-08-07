@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.churchpresenter.app.churchpresenter.tabs.scheduleTab
import org.churchpresenter.app.churchpresenter.utils.Constants
import org.churchpresenter.app.churchpresenter.viewmodel.ScheduleViewModel
import kotlin.test.Test

class ScheduleTabScreenshotTest {

    private fun shoot(
        name: String,
        itemZoomPercent: Int = 100,
        width: Dp? = null,
        seed: ScheduleViewModel.() -> Unit = { everyItemType() },
        drive: ComposeUiTest.(ScheduleViewModel) -> Unit = {},
    ) = stackedThemes(SECTION, name) { mode, file ->
        scheduleTab(
            itemZoomPercent = itemZoomPercent,
            width = width,
            seed = seed,
            themeMode = mode,
        ) { vm, _ ->
            drive(vm)
            captureTo(file)
        }
    }

    private fun ScheduleViewModel.everyItemType() {
        addLabel("Welcome", "#FFFFFF", "#203040")
        addSong(songNumber = 42, title = "Amazing Grace", songbook = "Hymnal")
        addBibleVerse(
            bookName = "John",
            chapter = 3,
            verseNumber = 16,
            verseText = "For God so loved the world, that he gave his only begotten Son.",
        )
        addPresentation(
            filePath = "/decks/sermon.pptx",
            fileName = "sermon.pptx",
            slideCount = 24,
            fileType = "pptx",
        )
        addPicture(folderPath = "/media/baptism", folderName = "Baptism", imageCount = 37)
        addMedia(mediaUrl = "/media/welcome.mp4", mediaTitle = "Welcome loop", mediaType = "video")
        addLowerThird(
            presetId = "lt-1",
            presetLabel = "Guest speaker",
            pauseAtFrame = true,
            pauseDurationMs = 4000,
        )
        addAnnouncement(text = "Fellowship lunch after the service")
        addWebsite(url = "https://example.org/notices", title = "Notices")
        addScene(sceneId = "scene-1", sceneName = "Countdown scene")
        addDictionary(
            number = "H2617",
            word = "חֶסֶד",
            transliteration = "chesed",
            definition = "steadfast love",
        )
    }

    @Test
    fun `every item type`() = shoot("every_item_type")

    @Test
    fun `every timer mode`() = shoot(
        "timers",
        seed = {
            addAnnouncement(text = "", isTimer = true, timerMinutes = 5)
            addAnnouncement(text = "", isTimer = true, timerHours = 1, timerMinutes = 30, timerSeconds = 15)
            addAnnouncement(
                text = "",
                isTimer = true,
                timerMode = Constants.TIMER_MODE_CLOCK,
                targetHour = 10,
                targetMinute = 30,
            )
            addAnnouncement(text = "", isTimer = true, timerMode = Constants.TIMER_MODE_COUNT_UP)
            addAnnouncement(text = "", isTimer = true, timerMode = Constants.TIMER_MODE_CLOCK_DISPLAY)
        },
    )

    @Test
    fun `labels in their own colours`() = shoot(
        "labels_coloured",
        seed = {
            addLabel("Welcome", "#FFFFFF", "#203040")
            addLabel("Worship", "#1B5E20", "#C8E6C9")
            addLabel("Sermon", "#FFFFFF", "#B71C1C")
            addLabel("Communion", "#4A148C", "#E1BEE7")
            addLabel("Sending", "#000000", "#FFD54F")
        },
    )

    @Test
    fun `a long announcement is truncated`() = shoot(
        "announcement_truncated",
        seed = {
            addAnnouncement(
                text = "The fellowship lunch will be held in the hall directly after the service, " +
                    "and everyone is very welcome to stay",
            )
        },
    )

    @Test
    fun `a plan imported from Planning Center`() = shoot(
        "planning_center_import",
        seed = {
            addLabel("Pre-Service", "#FFFFFF", "#6750A4")
            addSong(songNumber = 0, title = "Build My Life", songbook = "Planning Center")
            addLabel("Worship", "#FFFFFF", "#6750A4")
            addSong(songNumber = 0, title = "Goodness Of God", songbook = "Planning Center")
            addLabel("Message", "#FFFFFF", "#6750A4")
            addBibleVerse(
                bookName = "Romans",
                chapter = 8,
                verseNumber = 28,
                verseText = "And we know that all things work together for good.",
            )
            addPresentation(
                filePath = "/planning-center/sermon-slides.pptx",
                fileName = "sermon-slides.pptx",
                slideCount = 18,
                fileType = "pptx",
            )
        },
    )

    @Test
    fun `scene and dictionary rows`() = shoot(
        "scene_and_dictionary",
        seed = {
            addScene(sceneId = "scene-1", sceneName = "Countdown scene")
            addDictionary(
                number = "H2617",
                word = "חֶסֶד",
                transliteration = "chesed",
                definition = "steadfast love",
            )
        },
    )

    @Test
    fun `an empty schedule`() = shoot("empty", seed = {})

    @Test
    fun `an item selected`() = shoot("item_selected") { vm ->
        vm.scheduleItems.getOrNull(1)?.let { vm.selectItem(it.id) }
        waitForIdle()
    }

    @Test
    fun `redo available after an undo`() = shoot("toolbar_redo_available") { vm ->
        vm.undo()
        waitForIdle()
    }

    @Test
    fun `a narrow panel wraps the toolbar`() = shoot("narrow_panel", width = 240.dp)

    @Test
    fun `density extra compact`() = shoot("density_extra_compact", itemZoomPercent = 55)

    @Test
    fun `density compact`() = shoot("density_compact", itemZoomPercent = 70)

    @Test
    fun `density detailed`() = shoot("density_detailed", itemZoomPercent = 150)

    @Test
    fun `density extra detailed`() = shoot("density_extra_detailed", itemZoomPercent = 200)

    private companion object {
        const val SECTION = "scheduleTab"
    }
}
