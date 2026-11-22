package com.hextapp.vpsadmin.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.hextapp.vpsadmin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0L,
)

data class AvailableUpdate(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

class UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkLatest(): Result<AvailableUpdate?> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "HextappVpsAdmin/${BuildConfig.VERSION_NAME}")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@runCatching null
                if (!resp.isSuccessful) error("GitHub HTTP ${resp.code}")
                val rel = json.decodeFromString<GithubRelease>(resp.body?.string().orEmpty())
                val remoteVer = rel.tagName.trim().removePrefix("v").removePrefix("V")
                val apk = rel.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: return@runCatching null
                if (isNewerVersion(remoteVer, BuildConfig.VERSION_NAME)) {
                    AvailableUpdate(
                        versionName = remoteVer,
                        notes = rel.body.take(400),
                        apkUrl = apk.downloadUrl,
                        sizeBytes = apk.size,
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(updatesDir, "hextapp-vps-admin-update.apk")
            val req = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "HextappVpsAdmin/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Error descargando APK: HTTP ${resp.code}")
                val body = resp.body ?: error("Respuesta vacia")
                val total = body.contentLength()
                var readTotal = 0L
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            readTotal += n
                            if (total > 0) {
                                onProgress(((readTotal * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            }

            onProgress(100)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(intent)
            }
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(rParts.size, cParts.size)
        for (i in 0 until len) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
