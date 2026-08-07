@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package org.churchpresenter.app.churchpresenter.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.takahirom.roborazzi.captureRoboImage
import org.churchpresenter.app.churchpresenter.ui.theme.ChurchPresenterTheme
import org.churchpresenter.app.churchpresenter.ui.theme.ThemeMode
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal val THEMES = listOf("light" to ThemeMode.LIGHT, "dark" to ThemeMode.DARK)

private const val ROOT = "screenshots"
private val PARTS = File("$ROOT/.parts")

/** [rootIndex] 1 shoots an open popup — a dropdown or menu is a compose root of its own. */
internal fun ComposeUiTest.captureTo(file: File, rootIndex: Int = 0) {
    onAllNodes(isRoot())[rootIndex].captureRoboImage(file.path)
}

/**
 * Runs [shoot] once per theme and writes both renders into one `screenshots/<section>/<name>.png`,
 * light above dark — so a state is written once and reviewed as a single image.
 *
 * Stacked afterwards rather than composed together because each render fills the test window: two
 * in one composition would get half the height each instead of two full-size views.
 */
internal fun stackedThemes(
    section: String,
    name: String,
    trim: Boolean = false,
    shoot: (ThemeMode, File) -> Unit,
) {
    PARTS.mkdirs()
    val parts = THEMES.map { (suffix, mode) ->
        File(PARTS, "${section}_${name}_$suffix.png").also { shoot(mode, it) }
    }
    // Nothing was written: capture is inert outside the Roborazzi tasks, so an ordinary test run
    // still composes every state (a throw there fails the test) without touching the images.
    if (parts.all { it.exists() }) stackVertically(parts, File("$ROOT/$section/$name.png"), trim)
    parts.forEach { it.delete() }
    PARTS.delete()
}

/**
 * A small component in both themes, stacked.
 *
 * [drive] runs before the shot — click to open a menu, type into a field. [rootIndex] 1 then shoots
 * the popup that opened.
 */
internal fun captureComponent(
    section: String,
    name: String,
    rootIndex: Int = 0,
    drive: ComposeUiTest.() -> Unit = {},
    content: @Composable () -> Unit,
) = stackedThemes(section, name, trim = true) { mode, file ->
    runComposeUiTest {
        setContent {
            ChurchPresenterTheme(themeMode = mode) {
                Box(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) { content() }
            }
        }
        drive()
        captureTo(file, rootIndex)
    }
}

private fun stackVertically(parts: List<File>, out: File, trim: Boolean) {
    val images = parts
        .map { ImageIO.read(it) ?: error("unreadable capture: ${it.path}") }
        .map { if (trim) it.trimmed() else it }
    val divider = 2
    val width = images.maxOf { it.width }
    val height = images.sumOf { it.height } + divider * (images.size - 1)

    val stacked = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val canvas = stacked.createGraphics()
    var y = 0
    images.forEachIndexed { index, image ->
        canvas.drawImage(image, 0, y, null)
        y += image.height
        if (index < images.lastIndex) {
            canvas.color = Color.GRAY
            canvas.fillRect(0, y, width, divider)
            y += divider
        }
    }
    canvas.dispose()
    out.parentFile?.mkdirs()
    ImageIO.write(stacked, "png", out)
}

/**
 * The image cropped to its drawn content, with a small margin.
 *
 * A popup is its own compose root and that root is the whole window, so an open menu comes back as a
 * small menu on a screenful of empty background. The background colour is the commonest one around
 * the edge rather than a corner pixel — a component that reaches the top-left corner would otherwise
 * make its own fill the "background" and nothing would crop. An image with no other colour in it is
 * returned untouched.
 */
private fun BufferedImage.trimmed(margin: Int = 8): BufferedImage {
    val border = buildList {
        for (x in 0 until width) { add(getRGB(x, 0)); add(getRGB(x, height - 1)) }
        for (y in 0 until height) { add(getRGB(0, y)); add(getRGB(width - 1, y)) }
    }
    val background = border.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return this
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (getRGB(x, y) == background) continue
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }
    if (right < left || bottom < top) return this

    val x = (left - margin).coerceAtLeast(0)
    val y = (top - margin).coerceAtLeast(0)
    return getSubimage(x, y, (right + margin - x).coerceAtMost(width - x), (bottom + margin - y).coerceAtMost(height - y))
}
