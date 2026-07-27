package org.churchpresenter.app.churchpresenter.viewmodel

import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.nio.charset.StandardCharsets

class BibleSettingsViewModel {

    // ── State ────────────────────────────────────────────────────────

    private val _storageDirectory = mutableStateOf("")
    val storageDirectory: String get() = _storageDirectory.value

    private val _refreshTrigger = mutableStateOf(0)
    val refreshTrigger: Int get() = _refreshTrigger.value

    private val _selectedFile = mutableStateOf<String?>(null)
    val selectedFile: String? get() = _selectedFile.value

    // ── Derived ──────────────────────────────────────────────────────

    fun filesInDirectory(): List<String> =
        fileManager.getBibleFilesInDirectory(_storageDirectory.value)

    /**
     * Maps each Bible file to the name shown in the pickers.
     *
     * The picker reverse-maps the chosen display name back to its file, so these must be UNIQUE.
     * Two files can carry the same `##Title:` — a collection nests one folder per translation and
     * commonly holds several editions of one version — so a repeated title is qualified with the
     * folder it came from, which is what distinguishes them to the user as well.
     */
    fun fileDisplayNames(files: List<String>): Map<String, String> {
        val dir = _storageDirectory.value
        if (dir.isEmpty()) return emptyMap()
        val titles = files.associateWith { extractBibleTitle(File(dir, it).absolutePath) }
        val duplicated = titles.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return titles.mapValues { (path, title) ->
            if (title !in duplicated) return@mapValues title
            val folder = path.substringBeforeLast('/', "")
            // Two files sharing a title AND a folder can only be told apart by their file names.
            if (folder.isEmpty()) "$title  ($path)" else "$title  ($folder)"
        }
    }

    // ── Actions ──────────────────────────────────────────────────────

    fun setDirectory(path: String) {
        _storageDirectory.value = path
        _selectedFile.value = null
        _refreshTrigger.value++
    }

    fun selectFile(name: String?) {
        _selectedFile.value = name
    }

    fun refresh() {
        _refreshTrigger.value++
    }

    // ── Internal helpers ─────────────────────────────────────────────

    val fileManager = FileManager()

    private fun extractBibleTitle(filePath: String): String {
        return try {
            File(filePath).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                var title: String? = null
                repeat(10) {
                    val line = reader.readLine() ?: return@use (title ?: File(filePath).nameWithoutExtension)
                    if (line.startsWith("##Title:")) {
                        val t = line.substring(8).trim()
                        title = t
                        return@use t
                    }
                }
                title ?: File(filePath).nameWithoutExtension
            }
        } catch (_: Exception) {
            File(filePath).nameWithoutExtension
        }
    }
}

