package com.hextapp.vpsadmin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hextapp.vpsadmin.data.AdminApiClient
import com.hextapp.vpsadmin.data.AvailableUpdate
import com.hextapp.vpsadmin.data.CatalogItem
import com.hextapp.vpsadmin.data.ConnectionConfig
import com.hextapp.vpsadmin.data.SearchCandidate
import com.hextapp.vpsadmin.data.SettingsStore
import com.hextapp.vpsadmin.data.UpdateChecker
import com.hextapp.vpsadmin.data.VpsStatusData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DashboardUiState(
    val config: ConnectionConfig = ConnectionConfig(),
    val status: VpsStatusData? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val bannerMessage: String? = null,
    val selectedLogVps: String = "vps1",
    val availableUpdate: AvailableUpdate? = null,
    val isCheckingUpdate: Boolean = false,
    val downloadProgress: Int? = null,
    val isSearching: Boolean = false,
    val searchResults: List<SearchCandidate> = emptyList(),
    val hasSearched: Boolean = false,
    val catalogItems: List<CatalogItem> = emptyList(),
    val isLoadingCatalog: Boolean = false,
)

class VpsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val api = AdminApiClient()
    private val updater = UpdateChecker()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        checkForUpdate(silent = true)
        viewModelScope.launch {
            store.configFlow.collect { cfg ->
                _uiState.update { it.copy(config = cfg) }
                if (cfg.isConfigured) {
                    refreshNow()
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val cfg = _uiState.value.config
                if (cfg.isConfigured && !_uiState.value.isLoading) {
                    refreshSilent(cfg)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, bannerMessage = null) }
    }

    fun selectLogVps(vpsId: String) {
        _uiState.update { it.copy(selectedLogVps = vpsId) }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    fun checkForUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            updater.checkLatest()
                .onSuccess { upd ->
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = upd,
                            bannerMessage = if (upd == null && !silent) "Ya tienes la ultima version" else it.bannerMessage,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            error = if (silent) it.error else "No se pudo comprobar actualizaciones: ${err.message}",
                        )
                    }
                }
        }
    }

    fun installUpdate() {
        val upd = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadProgress = 0, error = null) }
            updater.downloadAndInstall(getApplication(), upd.apkUrl) { pct ->
                _uiState.update { it.copy(downloadProgress = pct) }
            }.onSuccess {
                _uiState.update { it.copy(downloadProgress = null, availableUpdate = null) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(downloadProgress = null, error = "Error descargando actualizacion: ${err.message}")
                }
            }
        }
    }

    fun saveConnection(baseUrl: String, token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, bannerMessage = null) }
            store.save(baseUrl, token)
            val cfg = ConnectionConfig(
                baseUrl = baseUrl.trim().trimEnd('/').let {
                    if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
                },
                token = token.trim(),
            )
            api.checkHealth(cfg)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            bannerMessage = "Conectado correctamente al servidor privado",
                        )
                    }
                    refreshNow()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "Configuracion guardada, pero fallo la prueba: ${err.message}",
                        )
                    }
                }
        }
    }

    fun refreshNow() {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            refreshSilent(cfg)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshSilent(cfg: ConnectionConfig) {
        api.fetchStatus(cfg)
            .onSuccess { data ->
                _uiState.update { state ->
                    val validNode = data.nodes.firstOrNull { it.id == state.selectedLogVps }?.id
                        ?: data.nodes.firstOrNull()?.id
                        ?: state.selectedLogVps
                    state.copy(status = data, error = null, selectedLogVps = validNode)
                }
            }
            .onFailure { err ->
                _uiState.update { it.copy(error = err.message ?: "Error de red Tailscale") }
            }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchResults = emptyList(), hasSearched = false) }
    }

    fun searchReleases(title: String, platform: String, sourceGroup: String = "all") {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured || title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null, hasSearched = false) }
            api.searchReleases(cfg, title.trim(), platform, sourceGroup)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(isSearching = false, searchResults = list, hasSearched = true)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isSearching = false, error = "Error buscando: ${err.message}", hasSearched = true)
                    }
                }
        }
    }

    fun submitBatchJobs(
        rawLines: String,
        platform: String,
        contentType: String,
        targetVps: String,
        onDone: () -> Unit = {},
    ) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        val entries = rawLines.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (entries.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, bannerMessage = null) }
            var okCount = 0
            entries.forEach { entry ->
                val isMagnet = entry.startsWith("magnet:", ignoreCase = true)
                val isUrl = entry.startsWith("http://", ignoreCase = true) || entry.startsWith("https://", ignoreCase = true)
                val res = api.submitJob(
                    config = cfg,
                    url = if (isUrl) entry else "",
                    magnet = if (isMagnet) entry else "",
                    title = if (!isUrl && !isMagnet) entry else "",
                    platform = platform,
                    contentType = contentType,
                    targetVps = targetVps,
                )
                if (res.isSuccess) okCount++
            }
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    bannerMessage = "Encolados $okCount de ${entries.size} trabajos",
                )
            }
            refreshSilent(cfg)
            if (okCount > 0) onDone()
        }
    }

    fun submitJob(
        url: String,
        magnet: String,
        title: String,
        platform: String,
        contentType: String,
        targetVps: String,
        onDone: () -> Unit = {},
    ) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null, bannerMessage = null) }
            api.submitJob(cfg, url, magnet, title, platform, contentType, targetVps)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmitting = false, bannerMessage = "Trabajo encolado correctamente")
                    }
                    refreshSilent(cfg)
                    onDone()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, error = err.message) }
                }
        }
    }

    fun cancelTorrent(targetVps: String, hash: String) {
        sendCommand(targetVps, "cancel_torrent", buildJsonObject { put("hash", hash) }, "Cancelacion enviada")
    }

    fun purgeStalled(targetVps: String) {
        sendCommand(targetVps, "purge_stalled", buildJsonObject {}, "Purga de torrents enviada")
    }

    fun runNow(targetVps: String) {
        sendCommand(targetVps, "run_now", buildJsonObject {}, "Ciclo inmediato solicitado a $targetVps")
    }

    fun restartScheduler(targetVps: String) {
        sendCommand(targetVps, "restart_scheduler", buildJsonObject {}, "Reinicio enviado a $targetVps")
    }

    fun setBuckets(targetVps: String, bucketOrder: String) {
        sendCommand(
            targetVps,
            "set_buckets",
            buildJsonObject { put("bucketOrder", bucketOrder) },
            "Buckets actualizados para $targetVps",
        )
    }

    fun repackNx6r(targetVps: String, platform: String = "ANDROID", limit: Int = 20) {
        sendCommand(
            targetVps,
            "repack_nx6r",
            buildJsonObject {
                put("platform", platform)
                put("limit", limit)
            },
            "Conversión NX6R ($platform, máx $limit) enviada a $targetVps",
        )
    }

    fun gitPull(targetVps: String) {
        sendCommand(targetVps, "git_pull", buildJsonObject {}, "Actualización git pull enviada a $targetVps")
    }

    fun clearFailedSeen(targetVps: String) {
        sendCommand(targetVps, "clear_failed_seen", buildJsonObject {}, "Limpieza de fallos BD enviada")
    }

    fun setNx6rProfile(targetVps: String, profile: String) {
        sendCommand(
            targetVps,
            "set_nx6r_profile",
            buildJsonObject { put("profile", profile) },
            "Perfil NX6R '$profile' aplicado en $targetVps",
        )
    }

    fun runAllVps() {
        val nodes = _uiState.value.status?.nodes?.map { it.id }.orEmpty().ifEmpty { listOf("vps1", "vps2", "vps3") }
        sendBatchCommand(nodes, "run_now", buildJsonObject {}, "Ciclo inmediato enviado a ${nodes.size} VPS")
    }

    fun purgeAllStalled() {
        val nodes = _uiState.value.status?.nodes?.map { it.id }.orEmpty().ifEmpty { listOf("vps1", "vps2", "vps3") }
        sendBatchCommand(nodes, "purge_stalled", buildJsonObject {}, "Purga stalled enviada a ${nodes.size} VPS")
    }

    fun updateAndRestartAllVps() {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        val nodes = _uiState.value.status?.nodes?.map { it.id }.orEmpty().ifEmpty { listOf("vps1", "vps2", "vps3") }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            nodes.forEach { vps ->
                api.sendCommand(cfg, vps, "git_pull", buildJsonObject {})
                api.sendCommand(cfg, vps, "restart_scheduler", buildJsonObject {})
            }
            _uiState.update {
                it.copy(isSubmitting = false, bannerMessage = "Git pull + reinicio enviado a ${nodes.size} VPS")
            }
            refreshSilent(cfg)
        }
    }

    fun setEnvVar(targetVps: String, key: String, value: String) {
        sendCommand(
            targetVps,
            "set_env",
            buildJsonObject {
                put("key", key)
                put("value", value)
            },
            "$key=$value guardado en $targetVps",
        )
    }

    fun testLlm(targetVps: String) {
        sendCommand(targetVps, "test_llm", buildJsonObject {}, "Prueba LLM enviada a $targetVps")
    }

    fun loadCatalogItems() {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCatalog = true, error = null) }
            api.fetchCatalogItems(cfg)
                .onSuccess { items ->
                    _uiState.update { it.copy(isLoadingCatalog = false, catalogItems = items) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoadingCatalog = false, error = err.message) }
                }
        }
    }

    fun deleteCatalogItem(id: String) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            api.deleteCatalogItem(cfg, id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            catalogItems = it.catalogItems.filterNot { item -> item.id == id },
                            bannerMessage = "Elemento eliminado del catálogo",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, error = err.message) }
                }
        }
    }

    private fun sendBatchCommand(
        targets: List<String>,
        action: String,
        payload: kotlinx.serialization.json.JsonObject,
        successMsg: String,
    ) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            targets.forEach { vps -> api.sendCommand(cfg, vps, action, payload) }
            _uiState.update { it.copy(isSubmitting = false, bannerMessage = successMsg) }
            refreshSilent(cfg)
        }
    }

    private fun sendCommand(
        targetVps: String,
        action: String,
        payload: kotlinx.serialization.json.JsonObject,
        successMsg: String,
    ) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            api.sendCommand(cfg, targetVps, action, payload)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, bannerMessage = successMsg) }
                    refreshSilent(cfg)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, error = err.message) }
                }
        }
    }

    fun retryFailed(seenId: String? = null, jobId: String? = null) {
        val cfg = _uiState.value.config
        if (!cfg.isConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            api.retryFailed(cfg, seenId, jobId)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, bannerMessage = "Marcado para reintentar") }
                    refreshSilent(cfg)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, error = err.message) }
                }
        }
    }
}
