package org.churchpresenter.app.churchpresenter

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Format placeholders in the English strings must be positional.
 *
 * Compose Multiplatform substitutes only the `%<n>$s` / `%<n>$d` form. A bare `%s` is passed
 * through untouched, so `stringResource(Res.string.x, arg)` silently renders the literal text
 * `%s` — it does not throw, and nothing fails until someone reads the screen.
 *
 * That is exactly what happened: three strings added with the shortcuts feature used bare `%s`,
 * and the Undo/Redo tooltips shipped reading `Undo (%s)`. This test is the cheap guard that would
 * have caught it, and it covers the other fifty-odd argument-taking strings too.
 *
 * Only the default English file is checked. Translations are managed separately and this suite must
 * not encourage editing them.
 */
class StringResourceFormatTest {

    private val stringsFile = File("src/jvmMain/composeResources/values/strings.xml")

    /** `name="..."` and the element body, which is all this test needs from the XML. */
    private val entryPattern = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /**
     * A `%` that starts a conversion but is not positional.
     *
     * `%%` is an escaped literal percent and is skipped by consuming it first. Everything the app
     * actually uses is `s` or `d`; a bare `%` followed by anything else (a percent sign next to a
     * word, as in "100% opacity") is not a conversion and is not flagged.
     */
    private val nonPositional = Regex("""%%|%(?![0-9]+\$)([sd])""")

    private fun entries(): List<Pair<String, String>> {
        assertTrue(stringsFile.isFile, "expected the English strings at ${stringsFile.absolutePath}")
        return entryPattern.findAll(stringsFile.readText())
            .map { it.groupValues[1] to it.groupValues[2] }
            .toList()
    }

    @Test
    fun `every format placeholder is positional`() {
        val offenders = entries().filter { (_, body) ->
            nonPositional.findAll(body).any { it.value != "%%" }
        }.map { it.first }

        assertEquals(
            emptyList(),
            offenders,
            "these strings use a bare %s or %d, which Compose renders literally — use %1\$s instead",
        )
    }

    @Test
    fun `the file was actually read, so a bad path cannot make this suite vacuous`() {
        val found = entries()

        assertTrue(found.size > 500, "only ${found.size} strings parsed — the regex or path is wrong")
        assertTrue(found.any { it.first == "tooltip_undo" }, "expected a known string to be present")
    }

    @Test
    fun `a positional placeholder is accepted and a bare one is rejected`() {
        // Pins the matcher itself: a test that silently matched nothing would pass for ever.
        assertTrue(nonPositional.findAll("Undo (%s)").any { it.value != "%%" })
        assertTrue(nonPositional.findAll("Screen %1\$d").none { it.value != "%%" })
        assertTrue(nonPositional.findAll("100%% brightness").none { it.value != "%%" })
        assertTrue(nonPositional.findAll("Zoom 100% now").none { it.value != "%%" })
    }
}
