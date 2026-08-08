package org.churchpresenter.app.churchpresenter.screenshot

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The two rules that decide whether a screenshot is compared in CI at all.
 *
 * Both fail **silently**. `.github/workflows/screenshots.yml` records with `--tests
 * '*ScreenshotTest*'` and matches images between the two sides of the comparison by their path
 * relative to [SCREENSHOT_ROOT], so a class named something else is simply never rendered, and an
 * image written outside that root simply has no counterpart. Neither produces a failure, a warning,
 * or a missing-file error — the image just stops being looked at, and stays that way for as long as
 * nobody thinks to check. Each has already happened: a batch of 63 images was dead on arrival for
 * the naming rule, and four files plus [PresenterScreenshotTest] had their own path literal.
 *
 * So these are asserted here rather than left to review. Reading the sources is the only way: what
 * the record task picks up is decided by a class *name*, and where an image lands is decided by a
 * path *literal*, neither of which exists as a value at runtime.
 *
 * Sources resolve relative to the module directory, the same way [AppPreviewSupport]'s fixtures do.
 */
class ScreenshotInvariantsTest {

    private val packageDir =
        File("src/jvmTest/kotlin/org/churchpresenter/app/churchpresenter/screenshot")

    /** Every `.kt` in the screenshot package, paired with its text. */
    private fun sources(): List<Pair<File, String>> {
        val files = packageDir.listFiles { f: File -> f.extension == "kt" }?.sortedBy { it.name }
        if (files.isNullOrEmpty()) {
            // A moved package or a changed working directory would otherwise leave every assertion
            // below vacuously true — the exact silence this class exists to end.
            fail("no sources found at ${packageDir.absolutePath}; this test can no longer see what it checks")
        }
        return files.map { it to it.readText() }
    }

    @Test
    fun `the screenshot package is where this test thinks it is`() {
        val names = sources().map { (file, _) -> file.name }
        assertTrue(names.size > 20, "expected the whole screenshot package, saw ${names.size} files")
        assertTrue("ScreenshotSupport.kt" in names, "saw $names")
    }

    @Test
    fun `every class that takes a screenshot is named so CI renders it`() {
        val offenders = sources().mapNotNull { (file, text) ->
            if (!text.contains("@Test")) return@mapNotNull null
            val declared = CLASS_DECLARATION.find(text)?.groupValues?.get(1) ?: return@mapNotNull null
            // This class itself asserts about sources rather than rendering any; it needs no images
            // in CI and deliberately sits outside the record filter.
            if (declared == this::class.simpleName) return@mapNotNull null
            if (declared.endsWith("ScreenshotTest")) null else "${file.name}: class $declared"
        }
        assertEquals(
            emptyList(), offenders,
            "the record job runs --tests '*ScreenshotTest*'; a class outside that pattern is never " +
                "rendered in CI and its images are never compared"
        )
    }

    /**
     * `SCREENSHOT_ROOT` plus every constant in the package defined *from* it — `ROOT` in
     * [AppPreviewSupport] is `"$SCREENSHOT_ROOT/previewApp"`, and a capture through that is under
     * the root just as surely as one naming it outright. Resolved rather than allow-listed, so a new
     * sub-root is accepted the moment it is derived correctly and never when it is not.
     */
    private fun rootDerivedNames(): Set<String> {
        val derived = sources().flatMap { (_, text) ->
            ROOT_DERIVED_CONSTANT.findAll(text).map { it.groupValues[1] }
        }
        return derived.toSet() + "SCREENSHOT_ROOT"
    }

    @Test
    fun `every capture is written under the screenshot root`() {
        val roots = rootDerivedNames()
        val offenders = sources().flatMap { (file, text) ->
            text.lineSequence().withIndex()
                .filter { (_, line) -> CAPTURE_CALL.containsMatchIn(line) }
                // A capture either builds its path from a root or takes a File the caller built;
                // only a path literal spelled out on the spot can miss it.
                .filter { (_, line) -> line.contains(".png\"") && roots.none { it in line } }
                .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
        }
        assertEquals(
            emptyList(), offenders,
            "images are matched between the two sides of the comparison by their path relative to " +
                "SCREENSHOT_ROOT, so one written elsewhere is not reported as differing — it has no " +
                "counterpart and silently stops being compared"
        )
    }

    @Test
    fun `every path that names the root derives it rather than restating it`() {
        val literal = Regex(""""[^"]*build/screenshots""")
        val offenders = sources()
            // This file spells the path out to search for it; matching itself would be noise.
            .filterNot { (file, _) -> file.name == "${this::class.simpleName}.kt" }
            .flatMap { (file, text) ->
                text.lineSequence().withIndex()
                    .filter { (_, line) -> literal.containsMatchIn(line) }
                    .filterNot { (_, line) -> line.contains("SCREENSHOT_ROOT =") }
                    .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
            }
        assertEquals(
            emptyList(), offenders,
            "SCREENSHOT_ROOT is the single definition of that path; a second copy moves out of step " +
                "with it without anything noticing"
        )
    }

    @Test
    fun `no screenshot is committed`() {
        // Under build/, and the old committed location stays empty. A recorded image reaching a
        // commit is how 15-of-16 platform churn got into the history before 2026-08-07.
        assertTrue(
            SCREENSHOT_ROOT.startsWith("build/"),
            "screenshots must land under build/ so they cannot be added to a commit, was $SCREENSHOT_ROOT"
        )
        val abandoned = File("screenshots")
        assertTrue(
            !abandoned.exists() || abandoned.walkTopDown().none { it.extension == "png" },
            "${abandoned.absolutePath} holds recorded images again; nothing reads them and git keeps " +
                "every version of a binary for ever"
        )
    }

    private companion object {
        val CLASS_DECLARATION = Regex("""^(?:internal |private )?class (\w+)""", RegexOption.MULTILINE)
        val CAPTURE_CALL = Regex("""captureRoboImage\(|captureTo\(""")

        /** e.g. `private const val ROOT = "$SCREENSHOT_ROOT/previewApp"` — captures `ROOT`. */
        val ROOT_DERIVED_CONSTANT =
            Regex("""val (\w+)\s*=\s*(?:File\()?"[^"]*\$\{?SCREENSHOT_ROOT""")
    }
}
