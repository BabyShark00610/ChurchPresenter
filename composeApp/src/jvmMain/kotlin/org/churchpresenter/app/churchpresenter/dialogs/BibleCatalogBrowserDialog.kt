package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import churchpresenter.composeapp.generated.resources.Res
import churchpresenter.composeapp.generated.resources.bible_catalog_attribution
import churchpresenter.composeapp.generated.resources.bible_catalog_download
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_archive
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_convert
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_generic
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_incomplete
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_network
import churchpresenter.composeapp.generated.resources.bible_catalog_download_error_write
import churchpresenter.composeapp.generated.resources.bible_catalog_empty
import churchpresenter.composeapp.generated.resources.bible_catalog_error_generic
import churchpresenter.composeapp.generated.resources.bible_catalog_error_network
import churchpresenter.composeapp.generated.resources.bible_catalog_error_rate_limited
import churchpresenter.composeapp.generated.resources.bible_catalog_installed
import churchpresenter.composeapp.generated.resources.bible_catalog_installed_summary
import churchpresenter.composeapp.generated.resources.bible_catalog_language_all
import churchpresenter.composeapp.generated.resources.bible_catalog_language_count
import churchpresenter.composeapp.generated.resources.bible_catalog_license_accept
import churchpresenter.composeapp.generated.resources.bible_catalog_license_body
import churchpresenter.composeapp.generated.resources.bible_catalog_license_source_ebible
import churchpresenter.composeapp.generated.resources.bible_catalog_license_source_zefania
import churchpresenter.composeapp.generated.resources.bible_catalog_license_title
import churchpresenter.composeapp.generated.resources.bible_catalog_license_unknown
import churchpresenter.composeapp.generated.resources.bible_catalog_loading
import churchpresenter.composeapp.generated.resources.bible_catalog_no_directory
import churchpresenter.composeapp.generated.resources.bible_catalog_overwrite_confirm
import churchpresenter.composeapp.generated.resources.bible_catalog_phase_converting
import churchpresenter.composeapp.generated.resources.bible_catalog_phase_downloading
import churchpresenter.composeapp.generated.resources.bible_catalog_phase_extracting
import churchpresenter.composeapp.generated.resources.bible_catalog_phase_installing
import churchpresenter.composeapp.generated.resources.bible_catalog_redownload
import churchpresenter.composeapp.generated.resources.bible_catalog_retry
import churchpresenter.composeapp.generated.resources.bible_catalog_rights
import churchpresenter.composeapp.generated.resources.bible_catalog_search_placeholder
import churchpresenter.composeapp.generated.resources.bible_catalog_size_mb
import churchpresenter.composeapp.generated.resources.bible_catalog_source_ebible
import churchpresenter.composeapp.generated.resources.bible_catalog_source_zefania
import churchpresenter.composeapp.generated.resources.bible_catalog_stale_notice
import churchpresenter.composeapp.generated.resources.bible_catalog_title
import churchpresenter.composeapp.generated.resources.cancel
import churchpresenter.composeapp.generated.resources.ok
import org.churchpresenter.app.churchpresenter.LocalMainWindowState
import org.churchpresenter.app.churchpresenter.centeredOnMainWindow
import org.churchpresenter.app.churchpresenter.composables.DropdownSettingsField
import org.churchpresenter.app.churchpresenter.composables.SettingsTextField
import org.churchpresenter.app.churchpresenter.data.BibleModule
import org.churchpresenter.app.churchpresenter.data.BibleSource
import org.churchpresenter.app.churchpresenter.data.BibleSourceId
import org.churchpresenter.app.churchpresenter.data.EBibleSource
import org.churchpresenter.app.churchpresenter.data.InstallPhase
import org.churchpresenter.app.churchpresenter.data.ZefaniaSource
import org.churchpresenter.app.churchpresenter.viewmodel.BibleCatalogError
import org.churchpresenter.app.churchpresenter.viewmodel.BibleCatalogViewModel
import org.churchpresenter.app.churchpresenter.viewmodel.BibleDownloadError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Browses the available Bible archives and installs the chosen translations into [storageDirectory].
 *
 * One tab per archive, each with its own view model so switching tabs keeps each list's search and
 * scroll position. eBible.org comes first: it is far larger, and it states each translation's
 * copyright in the list rather than only after the file has been downloaded and opened.
 *
 * Each install downloads a module and converts it on this machine, so the row reports which stage
 * it is at. The dialog stays open afterwards — people normally collect two or three translations in
 * one sitting — and reports each one through [onBibleInstalled] so the settings tab behind it can
 * refresh its Primary/Secondary dropdowns straight away.
 */
@Composable
fun BibleCatalogBrowserDialog(
    storageDirectory: String,
    onDismiss: () -> Unit,
    onBibleInstalled: (fileName: String) -> Unit
) {
    val sources: List<Pair<BibleSource, StringResource>> = remember {
        listOf(
            EBibleSource to Res.string.bible_catalog_source_ebible,
            ZefaniaSource to Res.string.bible_catalog_source_zefania,
        )
    }
    val viewModels = remember(storageDirectory) {
        sources.map { (source, _) -> BibleCatalogViewModel(source, storageDirectory) }
    }
    DisposableEffect(viewModels) {
        onDispose { viewModels.forEach { it.dispose() } }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val viewModel = viewModels[selectedTab]

    // Each tab loads the first time it is opened, so the second archive costs nothing unless asked for.
    LaunchedEffect(viewModel) {
        viewModel.refreshInstalled()
        viewModel.load()
    }

    // Every download is confirmed, deliberately with no "don't ask again": the licence differs per
    // translation, so an acknowledgement given for one says nothing about the next.
    var pendingInstall by remember { mutableStateOf<BibleModule?>(null) }

    val mainWindowState = LocalMainWindowState.current
    val dialogState = rememberDialogState(
        position = centeredOnMainWindow(mainWindowState, 720.dp, 700.dp),
        width = 720.dp,
        height = 700.dp
    )

    DialogWindow(
        onCloseRequest = onDismiss,
        state = dialogState,
        title = stringResource(Res.string.bible_catalog_title),
        resizable = true
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.bible_catalog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    sources.forEachIndexed { index, (_, label) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(stringResource(label)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                FilterRow(viewModel)
                Spacer(Modifier.height(12.dp))

                Messages(viewModel)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        viewModel.isLoading && viewModel.modules.isEmpty() -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(Res.string.bible_catalog_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        viewModel.catalogError != null && viewModel.modules.isEmpty() -> {
                            TextButton(
                                onClick = { viewModel.load() },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(stringResource(Res.string.bible_catalog_retry))
                            }
                        }
                        viewModel.visibleModules.isEmpty() -> {
                            Text(
                                text = stringResource(Res.string.bible_catalog_empty),
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        else -> {
                            val listState = rememberLazyListState()
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(viewModel.visibleModules, key = { it.key }) { module ->
                                    ModuleRow(
                                        module = module,
                                        showDate = module.displayName.trim().lowercase() in viewModel.duplicateDisplayNames,
                                        isInstalled = viewModel.isInstalled(module),
                                        isInstalling = viewModel.installingKey == module.key,
                                        phase = viewModel.installPhase,
                                        progress = viewModel.installProgress,
                                        anyInstallRunning = viewModel.installingKey != null,
                                        onInstall = { pendingInstall = module }
                                    )
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(listState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.bible_catalog_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    // Not "Cancel": each install has already happened by the time this is pressed,
                    // so there is nothing here to call off.
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            }
        }

        pendingInstall?.let { module ->
            LicenceConfirmation(
                module = module,
                isReinstall = viewModel.isInstalled(module),
                onConfirm = {
                    pendingInstall = null
                    viewModel.install(module, onBibleInstalled)
                },
                onDismiss = { pendingInstall = null }
            )
        }

        viewModel.lastInstalled?.let { installed ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissInstalledNotice() },
                title = { Text(stringResource(Res.string.bible_catalog_installed)) },
                text = {
                    Column {
                        Text(stringResource(Res.string.bible_catalog_installed_summary, installed.title, installed.books))
                        if (installed.rights.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.bible_catalog_rights, installed.rights),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissInstalledNotice() }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }
    }
}

/**
 * Shown before every download.
 *
 * The archives carry public-domain texts next to ones licensed for congregational use only, so this
 * names the licence of the translation actually being installed rather than a generic warning —
 * and it says so plainly when the translation declares none, which is the case that most warrants
 * a look before the text goes on a screen in front of a congregation.
 *
 * Re-downloading folds into the same dialog rather than stacking a second one on top.
 */
@Composable
private fun LicenceConfirmation(
    module: BibleModule,
    isReinstall: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.bible_catalog_license_title)) },
        text = {
            // Some copyright statements run to several lines, and the notice sits below them, so
            // this can outgrow a short window.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = module.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (module.copyright.isNotBlank()) {
                        stringResource(Res.string.bible_catalog_rights, module.copyright)
                    } else {
                        stringResource(Res.string.bible_catalog_license_unknown)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(8.dp))
                // What the archive itself vouches for differs sharply between the two, and that is
                // the part someone deciding whether they may project this text actually needs.
                Text(
                    text = stringResource(sourceLicenceStringRes(module.sourceId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.bible_catalog_license_body),
                    style = MaterialTheme.typography.bodySmall
                )
                if (isReinstall) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.bible_catalog_overwrite_confirm, module.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.bible_catalog_license_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun FilterRow(viewModel: BibleCatalogViewModel) {
    val allLanguagesLabel = stringResource(Res.string.bible_catalog_language_all)
    // Display label -> language code. The label carries the count, so the map is what turns the
    // user's pick back into the code the filter works on.
    val languageLabels = viewModel.languages.associate { option ->
        stringResource(Res.string.bible_catalog_language_count, option.code, option.count) to option.code
    }
    val selectedLabel = languageLabels.entries
        .firstOrNull { it.value == viewModel.selectedLanguage }?.key
        ?: allLanguagesLabel

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SettingsTextField(
            value = viewModel.query,
            onValueChange = { viewModel.query = it },
            placeholder = { Text(stringResource(Res.string.bible_catalog_search_placeholder)) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Spacer(Modifier.width(12.dp))
        DropdownSettingsField(
            value = selectedLabel,
            options = listOf(allLanguagesLabel) + languageLabels.keys,
            onValueChange = { label -> viewModel.selectedLanguage = languageLabels[label] },
            modifier = Modifier.width(220.dp)
        )
    }
}

@Composable
private fun Messages(viewModel: BibleCatalogViewModel) {
    if (viewModel.isStale) {
        Text(
            text = stringResource(Res.string.bible_catalog_stale_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
    }
    viewModel.catalogError?.let { error ->
        Text(
            text = stringResource(catalogErrorStringRes(error)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
    }
    viewModel.installError?.let { error ->
        Text(
            text = stringResource(installErrorStringRes(error)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Narrow enough that the button reads as the row's action rather than a column of its own, and wide
 * enough for "Installed  Re-download" and the progress bar to sit in the same footprint — so a row
 * doesn't reflow when an install starts.
 */
private val ACTION_COLUMN_WIDTH = 148.dp

@Composable
private fun ModuleRow(
    module: BibleModule,
    showDate: Boolean,
    isInstalled: Boolean,
    isInstalling: Boolean,
    phase: InstallPhase?,
    progress: Float,
    anyInstallRunning: Boolean,
    onInstall: () -> Unit
) {
    // A long list of near-identical rows is hard to track a cursor across, so the row under the
    // pointer is tinted — the same treatment the content tabs give their lists.
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val rowBackground by animateColorAsState(
        targetValue = if (hovered) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        } else {
            Color.Transparent
        },
        label = "bibleCatalogRowHover"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .background(rowBackground, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Above the name, not below it: eBible states each translation's licence in its
            // catalogue, and what someone may legally project matters before the title does.
            if (module.copyright.isNotBlank()) {
                Text(
                    text = module.copyright,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = module.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = moduleSubtitle(module, showDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.width(ACTION_COLUMN_WIDTH), contentAlignment = Alignment.CenterEnd) {
            when {
                isInstalling -> Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(phaseStringRes(phase)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                isInstalled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.bible_catalog_installed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(6.dp))
                    TextButton(
                        onClick = onInstall,
                        enabled = !anyInstallRunning,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.bible_catalog_redownload),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                else -> Button(
                    onClick = onInstall,
                    enabled = !anyInstallRunning,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.bible_catalog_download),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun moduleSubtitle(module: BibleModule, showDate: Boolean): String {
    val parts = mutableListOf<String>()
    if (module.identifier.isNotBlank()) parts.add(module.identifier)
    if (module.language.isNotBlank()) parts.add(module.language)
    if (module.sizeBytes > 0) {
        val megabytes = "%.1f".format(module.sizeBytes / (1024.0 * 1024.0))
        parts.add(stringResource(Res.string.bible_catalog_size_mb, megabytes))
    }
    // Only where the name alone is ambiguous — see BibleCatalogViewModel.duplicateDisplayNames.
    if (showDate && module.releaseDate.isNotBlank()) parts.add(module.releaseDate)
    return parts.joinToString(" · ")
}

private fun phaseStringRes(phase: InstallPhase?): StringResource = when (phase) {
    InstallPhase.EXTRACTING -> Res.string.bible_catalog_phase_extracting
    InstallPhase.CONVERTING -> Res.string.bible_catalog_phase_converting
    InstallPhase.INSTALLING -> Res.string.bible_catalog_phase_installing
    else -> Res.string.bible_catalog_phase_downloading
}

private fun sourceLicenceStringRes(sourceId: BibleSourceId): StringResource = when (sourceId) {
    BibleSourceId.EBIBLE -> Res.string.bible_catalog_license_source_ebible
    BibleSourceId.ZEFANIA -> Res.string.bible_catalog_license_source_zefania
}

private fun catalogErrorStringRes(error: BibleCatalogError): StringResource = when (error) {
    BibleCatalogError.NETWORK_ERROR -> Res.string.bible_catalog_error_network
    BibleCatalogError.RATE_LIMITED -> Res.string.bible_catalog_error_rate_limited
    BibleCatalogError.FAILURE -> Res.string.bible_catalog_error_generic
}

private fun installErrorStringRes(error: BibleDownloadError): StringResource = when (error) {
    BibleDownloadError.NETWORK_ERROR -> Res.string.bible_catalog_download_error_network
    BibleDownloadError.HTTP_ERROR -> Res.string.bible_catalog_download_error_generic
    BibleDownloadError.CHECKSUM_MISMATCH -> Res.string.bible_catalog_download_error_incomplete
    BibleDownloadError.CORRUPT_ARCHIVE -> Res.string.bible_catalog_download_error_archive
    BibleDownloadError.CONVERSION_FAILED -> Res.string.bible_catalog_download_error_convert
    BibleDownloadError.WRITE_FAILED -> Res.string.bible_catalog_download_error_write
    BibleDownloadError.NO_DIRECTORY -> Res.string.bible_catalog_no_directory
}
