package org.churchpresenter.app.churchpresenter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.churchpresenter.app.churchpresenter.data.BibleCatalogOutcome
import org.churchpresenter.app.churchpresenter.data.BibleInstallOutcome
import org.churchpresenter.app.churchpresenter.data.BibleModule
import org.churchpresenter.app.churchpresenter.data.BibleSource
import org.churchpresenter.app.churchpresenter.data.InstallPhase
import org.churchpresenter.app.churchpresenter.utils.Constants
import java.io.File

enum class BibleCatalogError { NETWORK_ERROR, RATE_LIMITED, FAILURE }

enum class BibleDownloadError {
    NETWORK_ERROR, HTTP_ERROR, CHECKSUM_MISMATCH, CORRUPT_ARCHIVE, CONVERSION_FAILED, WRITE_FAILED, NO_DIRECTORY
}

/** What to tell the user after a translation installed successfully. */
data class InstalledBible(val fileName: String, val title: String, val books: Int, val rights: String)

/**
 * Owned by [org.churchpresenter.app.churchpresenter.dialogs.BibleCatalogBrowserDialog] only — one
 * instance per source tab, created when the dialog opens and disposed when it closes.
 */
class BibleCatalogViewModel(
    private val source: BibleSource,
    private val storageDirectory: String,
    /**
     * Backs the view-model scope. `Dispatchers.Main` in the app, so state lands on the UI thread;
     * tests pass an immediate dispatcher so a load or install finishes before the call returns and
     * they can assert directly instead of polling a clock for it.
     *
     * Not decoration: this is the shape that produced issues #24 and #56 twice over. With the
     * dispatcher fixed, the work queues on the single Swing event thread, a loaded suite misses the
     * deadline, and the test fails by timeout expiry — which `AGENT.md` rules out explicitly.
     */
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {

    private val viewModelScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * One entry per language present in the catalogue, for the filter dropdown.
     *
     * [name] is the English language name where the source publishes one, blank otherwise; the
     * dropdown shows it alongside [code] rather than instead of it, because a handful of distinct
     * codes share an English name and the label has to stay unique.
     */
    data class LanguageOption(val code: String, val name: String, val count: Int)

    var query by mutableStateOf("")
    var selectedLanguage by mutableStateOf<String?>(null)

    var isLoading by mutableStateOf(false)
        private set
    var modules: List<BibleModule> by mutableStateOf(emptyList())
        private set
    var languages: List<LanguageOption> by mutableStateOf(emptyList())
        private set
    var catalogError by mutableStateOf<BibleCatalogError?>(null)
        private set
    var isStale by mutableStateOf(false)
        private set

    /**
     * Display names carried by more than one module, lowercased.
     *
     * Both archives publish some translations twice — usually an older and a newer conversion of
     * the same text — under names that are identical. Those rows get their release date shown so
     * they can be told apart; every other row stays uncluttered.
     *
     * Computed over the whole catalogue rather than the filtered view, so that typing in the search
     * box cannot make a date appear or vanish.
     */
    var duplicateDisplayNames: Set<String> by mutableStateOf(emptySet())
        private set

    var installedFiles: Set<String> by mutableStateOf(emptySet())
        private set
    var installingKey by mutableStateOf<String?>(null)
        private set
    var installPhase by mutableStateOf<InstallPhase?>(null)
        private set
    var installProgress by mutableStateOf(0f)
        private set
    var installError by mutableStateOf<BibleDownloadError?>(null)
        private set
    var lastInstalled by mutableStateOf<InstalledBible?>(null)
        private set

    private var loadJob: Job? = null

    val visibleModules: List<BibleModule>
        get() {
            val needle = query.trim()
            return modules.filter { module ->
                (selectedLanguage == null || module.language.equals(selectedLanguage, ignoreCase = true)) &&
                    (needle.isEmpty() || module.matches(needle))
            }
        }

    private fun BibleModule.matches(needle: String): Boolean =
        displayName.contains(needle, ignoreCase = true) ||
            identifier.contains(needle, ignoreCase = true) ||
            language.contains(needle, ignoreCase = true) ||
            languageName.contains(needle, ignoreCase = true)

    /** Fetches the catalogue. Safe to call again to retry; an in-flight load is not duplicated. */
    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            catalogError = null
            when (val outcome = source.catalog()) {
                is BibleCatalogOutcome.Success -> {
                    modules = outcome.modules
                    languages = outcome.modules
                        .groupBy { it.language.uppercase() }
                        .map { (code, group) ->
                            LanguageOption(
                                code = code,
                                name = group.firstNotNullOfOrNull { it.languageName.takeIf(String::isNotBlank) }
                                    .orEmpty(),
                                count = group.size
                            )
                        }
                        // Alphabetical by what the row actually reads as, so a named language sorts
                        // under its name and an unnamed one stays findable under its code.
                        .sortedWith(compareBy({ it.name.ifBlank { it.code }.lowercase() }, { it.code }))
                    duplicateDisplayNames = outcome.modules
                        .groupingBy { it.displayName.trim().lowercase() }
                        .eachCount()
                        .filterValues { it > 1 }
                        .keys
                    isStale = outcome.stale
                }
                is BibleCatalogOutcome.RateLimited -> catalogError = BibleCatalogError.RATE_LIMITED
                BibleCatalogOutcome.NetworkError -> catalogError = BibleCatalogError.NETWORK_ERROR
                BibleCatalogOutcome.Failure -> catalogError = BibleCatalogError.FAILURE
            }
            isLoading = false
        }
    }

    /**
     * Re-reads which modules are already present. Only the flat installed name counts — a Bible the
     * user obtained elsewhere and filed under a folder of their own is left alone rather than
     * guessed at by title.
     */
    fun refreshInstalled() {
        if (storageDirectory.isBlank()) {
            installedFiles = emptySet()
            return
        }
        installedFiles = File(storageDirectory)
            .listFiles { file -> file.isFile && file.extension.equals(Constants.EXTENSION_SPB, ignoreCase = true) }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    fun isInstalled(module: BibleModule): Boolean = module.fileName in installedFiles

    /**
     * Downloads and converts [module], handing the installed file name — the same value
     * [org.churchpresenter.app.churchpresenter.data.settings.BibleSettings.primaryBible] stores —
     * to [onInstalled].
     */
    fun install(module: BibleModule, onInstalled: (fileName: String) -> Unit) {
        if (storageDirectory.isBlank()) {
            installError = BibleDownloadError.NO_DIRECTORY
            return
        }
        // One install at a time: cancelling midway would leave a half-converted file behind, so a
        // second click while one is running is ignored rather than queued.
        if (installingKey != null) return

        viewModelScope.launch {
            installingKey = module.key
            installPhase = InstallPhase.DOWNLOADING
            installProgress = 0f
            installError = null
            lastInstalled = null

            val outcome = source.install(module, File(storageDirectory)) { progress ->
                installPhase = progress.phase
                installProgress = progress.fraction
            }
            when (outcome) {
                is BibleInstallOutcome.Success -> {
                    installedFiles = installedFiles + module.fileName
                    lastInstalled = InstalledBible(module.fileName, outcome.title, outcome.books, outcome.rights)
                    onInstalled(module.fileName)
                }
                is BibleInstallOutcome.HttpError -> installError = BibleDownloadError.HTTP_ERROR
                BibleInstallOutcome.NetworkError -> installError = BibleDownloadError.NETWORK_ERROR
                BibleInstallOutcome.ChecksumMismatch -> installError = BibleDownloadError.CHECKSUM_MISMATCH
                BibleInstallOutcome.CorruptArchive -> installError = BibleDownloadError.CORRUPT_ARCHIVE
                BibleInstallOutcome.ConversionFailed -> installError = BibleDownloadError.CONVERSION_FAILED
                BibleInstallOutcome.WriteFailed -> installError = BibleDownloadError.WRITE_FAILED
                BibleInstallOutcome.NoDirectory -> installError = BibleDownloadError.NO_DIRECTORY
            }
            installingKey = null
            installPhase = null
            installProgress = 0f
        }
    }

    fun dismissInstalledNotice() {
        lastInstalled = null
    }

    fun dispose() {
        viewModelScope.cancel()
    }
}
