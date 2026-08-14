package org.churchpresenter.app.churchpresenter.data

import org.churchpresenter.app.churchpresenter.data.settings.ScreenAssignment
import org.churchpresenter.app.churchpresenter.utils.Constants
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsProjectionMigrationTest {

    private lateinit var home: File
    private var realHome: String? = null

    @BeforeTest
    fun isolateHome() {
        realHome = System.getProperty("user.home")
        home = Files.createTempDirectory("cp-settings-projection-test").toFile()
        System.setProperty("user.home", home.absolutePath)
    }

    @AfterTest
    fun restoreHome() {
        realHome?.let { System.setProperty("user.home", it) }
        home.deleteRecursively()
    }

    private fun assignments(projectionJson: String): List<ScreenAssignment> =
        SettingsManager()
            .migrateAndDecode("""{"projectionSettings":$projectionJson}""")
            .projectionSettings
            .screenAssignments

    private fun withAssignments(vararg assignmentJson: String): List<ScreenAssignment> =
        assignments("""{"screenAssignments":[${assignmentJson.joinToString(",")}]}""")

    // ── Version 1: showBible/showSongs become modes ─────────────────────────────

    @Test
    fun `a screen with scripture switched off becomes bible mode off`() {
        val assignment = withAssignments("""{"showBible":false}""").single()

        assertEquals(Constants.SONG_LANG_OFF, assignment.bibleMode)
        assertFalse(assignment.showBible)
    }

    @Test
    fun `a screen with songs switched off becomes song mode off`() {
        val assignment = withAssignments("""{"showSongs":false}""").single()

        assertEquals(Constants.SONG_LANG_OFF, assignment.songMode)
        assertFalse(assignment.showSongs)
    }

    @Test
    fun `a screen with both switched off gets both modes off`() {
        val assignment = withAssignments("""{"showBible":false,"showSongs":false}""").single()

        assertEquals(Constants.SONG_LANG_OFF, assignment.bibleMode)
        assertEquals(Constants.SONG_LANG_OFF, assignment.songMode)
    }

    @Test
    fun `switching scripture off leaves songs on`() {
        val assignment = withAssignments("""{"showBible":false}""").single()

        assertTrue(assignment.showSongs, "only the flag that was set may be migrated")
    }

    @Test
    fun `a screen that had both switched on is left at its defaults`() {
        val assignment = withAssignments("""{"showBible":true,"showSongs":true}""").single()

        assertTrue(assignment.showBible)
        assertTrue(assignment.showSongs)
    }

    @Test
    fun `a mode already stored alongside the old flag wins`() {
        val assignment = withAssignments("""{"showBible":false,"bibleMode":"secondary"}""").single()

        assertEquals("secondary", assignment.bibleMode)
    }

    @Test
    fun `only the screen that needs migrating is rewritten`() {
        val migrated = withAssignments("""{"showBible":false}""", """{"showSongs":false}""", "{}")

        assertEquals(3, migrated.size)
        assertEquals(Constants.SONG_LANG_OFF, migrated[0].bibleMode)
        assertTrue(migrated[0].showSongs)
        assertEquals(Constants.SONG_LANG_OFF, migrated[1].songMode)
        assertTrue(migrated[1].showBible)
        assertTrue(migrated[2].showBible)
        assertTrue(migrated[2].showSongs)
    }

    @Test
    fun `the old flags do not survive as their own screen setting`() {
        val assignment = withAssignments("""{"showBible":false,"targetDisplay":2}""").single()

        assertEquals(Constants.SONG_LANG_OFF, assignment.bibleMode)
        assertEquals(2, assignment.targetDisplay)
    }

    @Test
    fun `a settings file that never carried the old flags is untouched`() {
        val assignment = withAssignments("""{"bibleMode":"primary"}""").single()

        assertEquals("primary", assignment.bibleMode)
    }

    @Test
    fun `running the migration a second time changes nothing`() {
        val once = SettingsManager().migrateAndDecode(
            """{"projectionSettings":{"screenAssignments":[{"showBible":false}]}}""",
        )
        val twice = SettingsManager().migrateAndDecode(
            """{"projectionSettings":{"screenAssignments":[{"showBible":false}]}}""",
        )

        assertEquals(once.projectionSettings.screenAssignments, twice.projectionSettings.screenAssignments)
    }

    // ── Version 2: the four numbered screens become a list ──────────────────────

    @Test
    fun `the numbered screen fields become a list in order`() {
        val migrated = assignments(
            """{"numberOfWindows":2,"screen1Assignment":{"targetDisplay":0},"screen2Assignment":{"targetDisplay":1}}""",
        )

        assertEquals(listOf(0, 1), migrated.map { it.targetDisplay })
    }

    @Test
    fun `all four numbered screens are carried across`() {
        val migrated = assignments(
            """{"screen1Assignment":{"targetDisplay":0},"screen2Assignment":{"targetDisplay":1},""" +
                """"screen3Assignment":{"targetDisplay":2},"screen4Assignment":{"targetDisplay":3}}""",
        )

        assertEquals(listOf(0, 1, 2, 3), migrated.map { it.targetDisplay })
    }

    @Test
    fun `a gap in the numbered screens does not leave a hole in the list`() {
        val migrated = assignments(
            """{"screen1Assignment":{"targetDisplay":0},"screen4Assignment":{"targetDisplay":3}}""",
        )

        assertEquals(listOf(0, 3), migrated.map { it.targetDisplay })
    }

    @Test
    fun `a file already carrying a list is left alone`() {
        val migrated = assignments(
            """{"screenAssignments":[{"targetDisplay":7}],"screen1Assignment":{"targetDisplay":0}}""",
        )

        assertEquals(listOf(7), migrated.map { it.targetDisplay })
    }

    @Test
    fun `a file old enough to need both projection migrations gets both`() {
        val migrated = assignments(
            """{"screen1Assignment":{"targetDisplay":0,"showBible":false},"screen2Assignment":{"targetDisplay":1}}""",
        )

        assertEquals(2, migrated.size)
        assertEquals(Constants.SONG_LANG_OFF, migrated[0].bibleMode)
        assertTrue(migrated[1].showBible)
    }

    @Test
    fun `a settings file with no projection block at all still decodes`() {
        val settings = SettingsManager().migrateAndDecode("""{"showBible":false}""")

        assertTrue(settings.projectionSettings.screenAssignments.isNotEmpty())
    }

}
