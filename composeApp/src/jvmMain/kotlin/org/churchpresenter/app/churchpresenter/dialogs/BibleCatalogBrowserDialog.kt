package org.churchpresenter.app.churchpresenter.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
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
import churchpresenter.composeapp.generated.resources.bible_catalog_loading
import churchpresenter.composeapp.generated.resources.bible_catalog_no_directory
import churchpresenter.composeapp.generated.resources.bible_catalog_overwrite_confirm
import churchpresenter.composeapp.generated.resources.bible_catalog_overwrite_title
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

    var pendingOverwrite by remember { mutableStateOf<BibleModule?>(null) }

    val mainWindowState = LocalMainWindowState.current
    val dialogState = rememberDialogState(
        position = centeredOnMainWindow(mainWindowState, 900.dp, 700.dp),
        width = 900.dp,
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
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
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
                                        onInstall = {
                                            if (viewModel.isInstalled(module)) {
                                                pendingOverwrite = module
                                            } else {
                                                viewModel.install(module, onBibleInstalled)
                                            }
                                        }
                                    )
                                }
                            }
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

        pendingOverwrite?.let { module ->
            AlertDialog(
                onDismissRequest = { pendingOverwrite = null },
                title = { Text(stringResource(Res.string.bible_catalog_overwrite_title)) },
                text = { Text(stringResource(Res.string.bible_catalog_overwrite_confirm, module.displayName)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingOverwrite = null
                        viewModel.install(module, onBibleInstalled)
                    }) {
                        Text(stringResource(Res.string.bible_catalog_redownload))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingOverwrite = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = module.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = moduleSubtitle(module, showDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            // eBible states the copyright in its catalogue, so it can be read before downloading.
            if (module.copyright.isNotBlank()) {
                Text(
                    text = module.copyright,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.width(210.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                isInstalling -> Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(phaseStringRes(phase)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                isInstalled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.bible_catalog_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onInstall, enabled = !anyInstallRunning) {
                        Text(stringResource(Res.string.bible_catalog_redownload))
                    }
                }
                else -> Button(onClick = onInstall, enabled = !anyInstallRunning) {
                    Text(stringResource(Res.string.bible_catalog_download))
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
