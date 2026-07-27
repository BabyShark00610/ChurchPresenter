@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.tabs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Finding a song in the Songs tab.
 *
 * This is the tab's whole job during a service: an operator with a number called from the platform,
 * or half a remembered title, has to get to the right song in one go. So these cover what the search
 * box actually narrows the list to, and — as importantly — what it does *not* drop.
 *
 * The songs are written to disk and read back through the real loader, so nothing here can pass
 * against a fixture the app would not itself produce. See `SongsTabTestSupport` for why this tab is
 * reachable in a test at all.
 */
class SongsTabSearchTest {

    @Test
    fun `every song is listed before anything is searched for, grouped by songbook`() = songsTab { _, _ ->
        // Chorus Book sorts before Hymnal, and within a songbook the rows follow the song number as
        // text — so 1, 12, 2 rather than 1, 2, 12. Pinned as it stands rather than as one might wish.
        assertEquals(
            listOf("How Great Thou Art", "Amazing Grace", "Amazing Love", "Be Thou My Vision"),
            listedTitles(),
            "the tab opens on the whole library, across songbooks",
        )
    }

    @Test
    fun `a title search narrows the list to the matches`() = songsTab { _, _ ->
        search("Amazing")
        assertEquals(
            listOf("Amazing Grace", "Amazing Love"),
            listedTitles(),
            "both Amazing songs match; the others must go",
        )
    }

    @Test
    fun `search matches the middle of a title, not just the start`() = songsTab { _, _ ->
        // The operator remembers "Vision", not "Be Thou".
        search("Vision")
        assertEquals(listOf("Be Thou My Vision"), listedTitles())
    }

    @Test
    fun `search ignores case`() = songsTab { _, _ ->
        search("amazing grace")
        assertTrue(shows("Amazing Grace"), "a lowercase query must still find it")
    }

    @Test
    fun `a song number finds that song`() = songsTab { _, _ ->
        // A number called out from the platform is the most common way in.
        search("12")
        assertTrue(shows("Amazing Love"), "song 12 must be reachable by its number")
    }

    @Test
    fun `a query matching nothing empties the list rather than showing everything`() = songsTab { _, _ ->
        search("Zzzzz No Such Song")
        assertEquals(
            emptyList(),
            listedTitles(),
            "a failed search must not silently fall back to the full library",
        )
    }

    @Test
    fun `clearing the query brings the whole library back`() = songsTab { _, _ ->
        search("Amazing")
        assertEquals(2, listedTitles().size)

        search("")
        assertEquals(4, listedTitles().size, "an emptied box is the same as never having searched")
    }

    @Test
    fun `searching finds songs in every songbook, not only the first`() = songsTab { _, _ ->
        // "How Great Thou Art" lives in Chorus Book while the rest are in Hymnal.
        search("How Great")
        assertEquals(listOf("How Great Thou Art"), listedTitles())
    }

    @Test
    fun `whitespace in the query is significant — a stray space finds nothing`() = songsTab { _, _ ->
        // Current behaviour, pinned rather than endorsed: the query is matched raw, so a leading or
        // trailing space makes the search miss. Reported separately; changing it is a product
        // decision, not something to smuggle in under a test.
        search("  Amazing Grace  ")
        assertEquals(emptyList(), listedTitles(), "the untrimmed query matches nothing")

        search("Amazing Grace")
        assertTrue(shows("Amazing Grace"), "and the same query trimmed finds it")
    }

    // ── What the tab shows around the list ──────────────────────────────────────

    @Test
    fun `the search box and the songbook filter are both offered`() = songsTab { _, _ ->
        assertTrue(shows(SongsLabel.SEARCH_PLACEHOLDER), "the empty box names itself")
        // DropdownSelector merges caption and value into one node, hence the substring match.
        assertTrue(showsContaining(SongsLabel.ALL_SONGBOOKS), "the songbook filter starts unrestricted")
        assertTrue(showsContaining(SongsLabel.CONTAINS), "and the filter mode starts on Contains")
    }

    @Test
    fun `an empty library lists nothing and does not claim to be searching`() = songsTab(songs = emptyList()) { _, _ ->
        assertEquals(emptyList(), listedTitles())
        assertFalse(shows("Amazing Grace"))
    }
}
