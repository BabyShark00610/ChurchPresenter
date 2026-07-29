package org.churchpresenter.app.churchpresenter.data

/**
 * Uppercase language code to English language name, for the download browser's language filter.
 *
 * eBible's own catalogue already publishes a name for every one of the ~1,240 codes it carries,
 * keyed by the same uppercase three-letter codes the Zefania archive names its folders with — so it
 * doubles as the lookup for both tabs, and there is no thousand-row table here to keep current.
 *
 * [UNLISTED] covers only the gap: the codes the Zefania archive uses that eBible has no row for at
 * all. That is partly the ISO 639-2/B bibliographic spellings (`CZE`, `GER`, `FRE`, `DUT`, `CHI`)
 * that eBible writes the 639-3 way, and partly languages eBible simply does not publish — Afrikaans,
 * Bulgarian, Norwegian, Swahili among them. Bounded by the archive's folder list rather than by ISO
 * 639 as a whole, which is what keeps it at two dozen-odd entries instead of several thousand.
 */
internal object BibleLanguageNames {

    /**
     * Every Zefania language folder with no matching row in the eBible catalogue.
     *
     * Measured against both published catalogues: of the archive's 63 language folders, these 29 are
     * absent from eBible entirely. A folder added later for a language eBible also lacks will show
     * its bare code until it is added here — visible, but harmless.
     */
    private val UNLISTED = mapOf(
        "AFR" to "Afrikaans",
        "ALB" to "Albanian",
        "ARA" to "Arabic",
        "BAQ" to "Basque",
        "BUL" to "Bulgarian",
        "CHI" to "Chinese",
        "CHU" to "Church Slavonic",
        "CZE" to "Czech",
        "ESP" to "Esperanto",
        "FRE" to "French",
        "GAE" to "Gaelic",
        "GER" to "German",
        "GLA" to "Scottish Gaelic",
        "GOT" to "Gothic",
        "GRE" to "Greek",
        "JAM" to "Jamaican Creole",
        "KAB" to "Kabyle",
        "LAV" to "Latvian",
        "MAO" to "Maori",
        "NDS" to "Low German",
        "NL" to "Dutch",
        "NOR" to "Norwegian",
        "RUM" to "Romanian",
        "SCR" to "Croatian",
        "SHU" to "Chadian Arabic",
        "SWA" to "Swahili",
        "SYR" to "Syriac",
        "UND" to "Unknown",
        "XKL" to "Kenyang",
    )

    /**
     * Merges a catalogue's own names over [UNLISTED].
     *
     * The catalogue wins wherever it has a row: it is published data and this map is a snapshot.
     */
    internal fun resolve(catalogue: Map<String, String>): Map<String, String> = UNLISTED + catalogue

    /**
     * The lookup as it stands, using whatever eBible data is already cached.
     *
     * With no eBible catalogue fetched yet this still returns [UNLISTED], so a cold first visit to
     * the Zefania tab names most of what it lists rather than nothing.
     */
    suspend fun table(): Map<String, String> = resolve(EBibleSource.cachedLanguageNames())
}
