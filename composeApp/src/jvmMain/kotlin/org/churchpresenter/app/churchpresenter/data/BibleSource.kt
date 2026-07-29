package org.churchpresenter.app.churchpresenter.data

import org.churchpresenter.app.churchpresenter.utils.Constants
import java.io.File

/** The archives the downloader can install from, in the order they are offered. */
enum class BibleSourceId { EBIBLE, ZEFANIA }

/** Which portion of scripture a translation covers. */
enum class Testament { OLD, NEW, FULL }

private val NT_TOKEN = Regex("\\bNT\\b")
private val OT_TOKEN = Regex("\\bOT\\b")

/**
 * One downloadable translation, as shown in the browse list.
 *
 * Everything here comes from the source's catalogue, without downloading the translation itself —
 * which is what lets the list show sizes and mark what is already installed while staying at one
 * network request per source.
 */
data class BibleModule(
    val sourceId: BibleSourceId,
    /** How the source locates the download: a repository path, or a translation id. */
    val downloadKey: String,
    /** Verifies the bytes when the source publishes one; blank when it doesn't. */
    val checksum: String = "",
    val sizeBytes: Long = 0,
    val language: String,
    /**
     * English name for [language], when the source publishes one; blank when it doesn't.
     *
     * Only eBible carries this. The Zefania archive names its language folders by code alone, so
     * that path resolves the name through [BibleLanguageNames] instead.
     */
    val languageName: String = "",
    val identifier: String,
    val displayName: String,
    /** Blank when the source only reveals it inside the file, as the Zefania archive does. */
    val copyright: String = "",
    val releaseDate: String = "",
    val fileStem: String
) {
    val fileName: String get() = "$fileStem.${Constants.EXTENSION_SPB}"

    /** Stable across sources, so an install in one tab can't be confused with one in another. */
    val key: String get() = "${sourceId.name}:$downloadKey"

    /**
     * Neither archive publishes a testament field, so this is inferred from the name itself: a
     * standalone "NT" or "OT" token names the portion; a name with neither names the full Bible.
     */
    val testament: Testament
        get() {
            val hasNt = NT_TOKEN.containsMatchIn(displayName)
            val hasOt = OT_TOKEN.containsMatchIn(displayName)
            return when {
                hasNt && !hasOt -> Testament.NEW
                hasOt && !hasNt -> Testament.OLD
                else -> Testament.FULL
            }
        }
}

sealed interface BibleCatalogOutcome {
    /** [stale] means this came from the on-disk copy because the source was unreachable. */
    data class Success(val modules: List<BibleModule>, val stale: Boolean = false) : BibleCatalogOutcome
    /** The host is refusing requests for now; [resetEpochSeconds] says until when, if known. */
    data class RateLimited(val resetEpochSeconds: Long?) : BibleCatalogOutcome
    data object NetworkError : BibleCatalogOutcome
    data object Failure : BibleCatalogOutcome
}

enum class InstallPhase { DOWNLOADING, EXTRACTING, CONVERTING, INSTALLING }

/** [fraction] spans the whole install, so the bar never resets between phases. */
data class InstallProgress(val phase: InstallPhase, val fraction: Float)

sealed interface BibleInstallOutcome {
    data class Success(val file: File, val title: String, val books: Int, val rights: String) : BibleInstallOutcome
    data class HttpError(val status: Int) : BibleInstallOutcome
    data object NetworkError : BibleInstallOutcome
    /** The bytes that arrived don't match the size or hash the catalogue listed. */
    data object ChecksumMismatch : BibleInstallOutcome
    /** Not an archive, or nothing usable inside it. */
    data object CorruptArchive : BibleInstallOutcome
    /** It parsed but produced no scripture. */
    data object ConversionFailed : BibleInstallOutcome
    data object WriteFailed : BibleInstallOutcome
    data object NoDirectory : BibleInstallOutcome
}

/**
 * An archive of downloadable Bibles.
 *
 * Each source owns both halves of the job — listing what it has, and turning one of its downloads
 * into an installed `.spb` — because the two are tightly coupled: eBible publishes USFX with a
 * separate book-name list, the Zefania archive publishes a single XML in a zip, and neither knows
 * anything about the other's format.
 */
interface BibleSource {
    val sourceId: BibleSourceId

    suspend fun catalog(nowMillis: Long = System.currentTimeMillis()): BibleCatalogOutcome

    suspend fun install(
        module: BibleModule,
        targetDir: File,
        onProgress: (InstallProgress) -> Unit = {},
    ): BibleInstallOutcome
}
