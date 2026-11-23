package com.hextapp.vpsadmin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AdminApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val mediaJson = "application/json; charset=utf-8".toMediaType()

    suspend fun checkHealth(config: ConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${config.baseUrl}/health")
                .header("Authorization", "Bearer ${config.token}")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${resp.body?.string().orEmpty().take(150)}")
                }
            }
        }
    }

    suspend fun fetchStatus(config: ConnectionConfig): Result<VpsStatusData> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${config.baseUrl}/vps/status")
                .header("Authorization", "Bearer ${config.token}")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${body.take(180)}")
                }
                val parsed = json.decodeFromString<StatusResponse>(body)
                if (!parsed.success || parsed.data == null) {
                    error(parsed.error ?: "Respuesta invalida del servidor")
                }
                parsed.data
            }
        }
    }

    suspend fun submitJob(
        config: ConnectionConfig,
        url: String,
        magnet: String,
        title: String,
        platform: String,
        contentType: String,
        targetVps: String,
    ): Result<Unit> = postJson(
        config = config,
        path = "/vps/jobs",
        payload = buildJsonObject {
            put("url", url)
            put("magnet", magnet)
            put("title", title)
            put("platform", platform)
            put("contentType", contentType)
            put("targetVps", targetVps)
        },
    )

    suspend fun sendCommand(
        config: ConnectionConfig,
        targetVps: String,
        action: String,
        payload: JsonObject = buildJsonObject {},
    ): Result<Unit> = postJson(
        config = config,
        path = "/vps/command",
        payload = buildJsonObject {
            put("targetVps", targetVps)
            put("action", action)
            put("payload", payload)
        },
    )

    suspend fun searchReleases(
        config: ConnectionConfig,
        title: String,
        platform: String,
    ): Result<List<SearchCandidate>> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildJsonObject {
                put("title", title)
                put("platform", platform)
            }
            val req = Request.Builder()
                .url("${config.baseUrl}/vps/search")
                .header("Authorization", "Bearer ${config.token}")
                .post(payload.toString().toRequestBody(mediaJson))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${body.take(180)}")
                }
                val parsed = json.decodeFromString<SearchResponse>(body)
                if (!parsed.success) {
                    error(parsed.error ?: "Error buscando candidatos")
                }
                parsed.data
            }
        }
    }

    suspend fun retryFailed(
        config: ConnectionConfig,
        seenId: String? = null,
        jobId: String? = null,
    ): Result<Unit> = postJson(
        config = config,
        path = "/vps/retry-failed",
        payload = buildJsonObject {
            if (!seenId.isNullOrBlank()) put("seenId", seenId)
            if (!jobId.isNullOrBlank()) put("jobId", jobId)
        },
    )

    private suspend fun postJson(
        config: ConnectionConfig,
        path: String,
        payload: JsonObject,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${config.baseUrl}$path")
                .header("Authorization", "Bearer ${config.token}")
                .post(payload.toString().toRequestBody(mediaJson))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${body.take(180)}")
                }
                val parsed = json.decodeFromString<GenericResponse>(body)
                if (!parsed.success) {
                    error(parsed.error ?: "Error en la peticion")
                }
            }
        }
    }
}
