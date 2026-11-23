package com.hextapp.vpsadmin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    val success: Boolean = false,
    val data: VpsStatusData? = null,
    val error: String? = null,
)

@Serializable
data class GenericResponse(
    val success: Boolean = false,
    val error: String? = null,
)

@Serializable
data class VpsStatusData(
    val nodes: List<VpsNode> = emptyList(),
    val jobs: List<PipelineJob> = emptyList(),
    val commands: List<PipelineCommand> = emptyList(),
    val seenCounts: Map<String, Int> = emptyMap(),
    val recentFailed: List<SeenFailedItem> = emptyList(),
    val uploaded24h: Int = 0,
    val serverTime: String = "",
)

@Serializable
data class Nx6rTelemetryEntry(
    val file: String = "",
    val rawBytes: Long = 0L,
    val outBytes: Long = 0L,
    val profile: String = "archive",
    val chunks: Int = 1,
)

@Serializable
data class VpsNode(
    @SerialName("_id") val id: String = "",
    val owner: String = "",
    val cpuPercent: Double = 0.0,
    val memUsedMB: Int = 0,
    val memTotalMB: Int = 0,
    val diskUsedGB: Double = 0.0,
    val diskTotalGB: Double = 0.0,
    val bucketOrder: String = "",
    val ollamaModel: String = "",
    val ollamaHost: String = "",
    val nx6rProfile: String = "archive",
    val nx6rTelemetry: List<Nx6rTelemetryEntry> = emptyList(),
    val torrents: List<QbtTorrent> = emptyList(),
    val logs: List<String> = emptyList(),
    val lastSeenAt: String = "",
    val status: String = "offline",
)

@Serializable
data class QbtTorrent(
    val hash: String = "",
    val name: String = "",
    val state: String = "",
    val progress: Double = 0.0,
    val dlSpeedKBs: Int = 0,
    val sizeMB: Double = 0.0,
)

@Serializable
data class PipelineJob(
    @SerialName("_id") val id: String = "",
    val url: String? = null,
    val magnet: String? = null,
    val title: String? = null,
    val platform: String = "auto",
    val contentType: String = "auto",
    val targetVps: String = "auto",
    val pickedBy: String? = null,
    val status: String = "pending",
    val error: String? = null,
    val createdAt: String = "",
)

@Serializable
data class PipelineCommand(
    @SerialName("_id") val id: String = "",
    val targetVps: String = "",
    val action: String = "",
    val status: String = "pending",
    val error: String? = null,
    val result: String? = null,
    val createdAt: String = "",
)

@Serializable
data class SeenFailedItem(
    @SerialName("_id") val id: String = "",
    val title: String? = null,
    val source: String? = null,
    val platform: String? = null,
    val error: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SearchResponse(
    val success: Boolean = false,
    val data: List<SearchCandidate> = emptyList(),
    val error: String? = null,
)

@Serializable
data class CatalogItemsResponse(
    val success: Boolean = false,
    val data: List<CatalogItem> = emptyList(),
    val error: String? = null,
)

@Serializable
data class CatalogItem(
    @SerialName("_id") val id: String = "",
    val title: String = "",
    val platform: String = "",
    val contentType: String = "",
    val sizeBytes: Long = 0L,
    val owner: String = "",
    val createdAt: String? = null,
)

@Serializable
data class SearchCandidate(
    val name: String = "",
    val source: String = "",
    val sourceGroup: String = "general",
    val seeders: Int = 0,
    val size: String = "",
    val url: String? = null,
    val magnet: String? = null,
    val matchesPlatform: Boolean = true,
    val warning: String? = null,
)
