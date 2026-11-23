package com.hextapp.vpsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hextapp.vpsadmin.ui.VpsViewModel
import com.hextapp.vpsadmin.ui.screens.AddJobScreen
import com.hextapp.vpsadmin.ui.screens.ControlScreen
import com.hextapp.vpsadmin.ui.screens.LogsQueueScreen
import com.hextapp.vpsadmin.ui.screens.MonitorScreen
import com.hextapp.vpsadmin.ui.theme.DangerRed
import com.hextapp.vpsadmin.ui.theme.HextappVpsAdminTheme
import com.hextapp.vpsadmin.ui.theme.SecondaryEmerald

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HextappVpsAdminTheme {
                VpsAdminRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VpsAdminRoot(vm: VpsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(if (state.config.isConfigured) 0 else 3) }

    LaunchedEffect(state.config.isConfigured) {
        if (!state.config.isConfigured) tab = 3
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hextapp VPS Admin") },
                actions = {
                    if (state.config.isConfigured) {
                        IconButton(onClick = { vm.refreshNow() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("VPS Live") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                    label = { Text("Añadir") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    label = { Text("Cola/Logs") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Control") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.error?.let { msg ->
                Surface(
                    color = DangerRed.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.clearMessages() },
                ) {
                    Text(msg, color = DangerRed, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            state.bannerMessage?.let { msg ->
                Surface(
                    color = SecondaryEmerald.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { vm.clearMessages() },
                ) {
                    Text(msg, color = SecondaryEmerald, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            when (tab) {
                0 -> MonitorScreen(
                    status = state.status,
                    onCancelTorrent = vm::cancelTorrent,
                    onPurgeStalled = vm::purgeStalled,
                    onRunNow = vm::runNow,
                    onRunAll = vm::runAllVps,
                    onPurgeAll = vm::purgeAllStalled,
                    onClearFailedDb = { vm.clearFailedSeen("vps1") },
                )
                1 -> AddJobScreen(
                    isSubmitting = state.isSubmitting,
                    isSearching = state.isSearching,
                    hasSearched = state.hasSearched,
                    searchResults = state.searchResults,
                    onSearch = vm::searchReleases,
                    onClearSearch = vm::clearSearch,
                    onSubmitBatch = { rawLines, platform, contentType, targetVps ->
                        vm.submitBatchJobs(rawLines, platform, contentType, targetVps) {
                            tab = 2
                        }
                    },
                    onSubmit = { url, magnet, title, platform, contentType, targetVps ->
                        vm.submitJob(url, magnet, title, platform, contentType, targetVps) {
                            tab = 2
                        }
                    },
                )
                2 -> LogsQueueScreen(
                    status = state.status,
                    selectedVps = state.selectedLogVps,
                    onSelectVps = vm::selectLogVps,
                    onRetrySeen = { vm.retryFailed(seenId = it) },
                    onRetryJob = { vm.retryFailed(jobId = it) },
                    onClearAllFailed = { vm.clearFailedSeen(state.selectedLogVps) },
                )
                3 -> ControlScreen(
                    config = state.config,
                    status = state.status,
                    isSubmitting = state.isSubmitting,
                    isCheckingUpdate = state.isCheckingUpdate,
                    catalogItems = state.catalogItems,
                    isLoadingCatalog = state.isLoadingCatalog,
                    onSaveConnection = vm::saveConnection,
                    onCheckUpdate = { vm.checkForUpdate(silent = false) },
                    onSetBuckets = vm::setBuckets,
                    onRestartScheduler = vm::restartScheduler,
                    onRepackNx6r = vm::repackNx6r,
                    onGitPull = vm::gitPull,
                    onClearFailedSeen = vm::clearFailedSeen,
                    onSetNx6rProfile = vm::setNx6rProfile,
                    onUpdateAndRestartAll = vm::updateAndRestartAllVps,
                    onSetEnvVar = vm::setEnvVar,
                    onTestLlm = vm::testLlm,
                    onLoadCatalog = vm::loadCatalogItems,
                    onDeleteCatalogItem = vm::deleteCatalogItem,
                )
            }
        }
    }

    state.availableUpdate?.let { upd ->
        AlertDialog(
            onDismissRequest = { if (state.downloadProgress == null) vm.dismissUpdate() },
            title = { Text("Nueva version v${upd.versionName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(upd.notes.ifBlank { "Hay una nueva actualizacion disponible en GitHub." })
                    state.downloadProgress?.let { pct ->
                        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("Descargando: $pct%", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = vm::installUpdate,
                    enabled = state.downloadProgress == null,
                ) {
                    Text(if (state.downloadProgress != null) "Descargando..." else "Descargar e instalar")
                }
            },
            dismissButton = {
                if (state.downloadProgress == null) {
                    TextButton(onClick = vm::dismissUpdate) {
                        Text("Mas tarde")
                    }
                }
            },
        )
    }
}
