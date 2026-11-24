package com.hextapp.vpsadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hextapp.vpsadmin.ui.VpsViewModel
import com.hextapp.vpsadmin.ui.screens.AddJobScreen
import com.hextapp.vpsadmin.ui.screens.ControlScreen
import com.hextapp.vpsadmin.ui.screens.LogsQueueScreen
import com.hextapp.vpsadmin.ui.screens.MonitorScreen
import com.hextapp.vpsadmin.ui.theme.AccentPurple
import com.hextapp.vpsadmin.ui.theme.DangerRed
import com.hextapp.vpsadmin.ui.theme.HextappVpsAdminTheme
import com.hextapp.vpsadmin.ui.theme.PrimaryCyan
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

    val nodes = state.status?.nodes.orEmpty()
    val onlineCount = nodes.count { it.status == "online" }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    title = {
                        Column {
                            Text("Hextapp VPS Admin", fontWeight = FontWeight.Bold)
                            if (state.config.isConfigured) {
                                Text(
                                    text = "$onlineCount/${nodes.size.coerceAtLeast(3)} nodos online • ${if (state.autoRefreshEnabled) "Auto 5s" else "Pausado"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (onlineCount > 0) SecondaryEmerald else PrimaryCyan,
                                )
                            }
                        }
                    },
                    actions = {
                        if (state.config.isConfigured) {
                            IconButton(onClick = { vm.toggleAutoRefresh() }) {
                                Icon(
                                    imageVector = if (state.autoRefreshEnabled) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                                    contentDescription = "Auto-refresh",
                                    tint = if (state.autoRefreshEnabled) SecondaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.refreshNow() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = PrimaryCyan)
                            }
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AccentPurple.copy(alpha = 0.65f), PrimaryCyan.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(AccentPurple.copy(alpha = 0.55f), PrimaryCyan.copy(alpha = 0.4f), AccentPurple.copy(alpha = 0.55f))
                        )
                    ),
                ) {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PrimaryCyan,
                            indicatorColor = AccentPurple.copy(alpha = 0.30f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NavigationBarItem(
                            selected = tab == 0,
                            colors = navColors,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("VPS Live") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            colors = navColors,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                            label = { Text("Añadir") },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            colors = navColors,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            label = { Text("Cola/Logs") },
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            colors = navColors,
                            onClick = { tab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Control") },
                        )
                    }
                }
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
                    onRepackNx6r = vm::repackNx6r,
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
