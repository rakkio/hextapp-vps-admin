package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hextapp.vpsadmin.data.QbtTorrent
import com.hextapp.vpsadmin.data.VpsNode
import com.hextapp.vpsadmin.data.VpsStatusData
import com.hextapp.vpsadmin.ui.theme.AccentPurple
import com.hextapp.vpsadmin.ui.theme.DangerRed
import com.hextapp.vpsadmin.ui.theme.PrimaryCyan
import com.hextapp.vpsadmin.ui.theme.SecondaryEmerald
import com.hextapp.vpsadmin.ui.theme.SurfaceVariantDark
import com.hextapp.vpsadmin.ui.theme.WarningAmber

@Composable
fun MonitorScreen(
    status: VpsStatusData?,
    onCancelTorrent: (vpsId: String, hash: String) -> Unit,
    onPurgeStalled: (vpsId: String) -> Unit,
    onRunNow: (vpsId: String) -> Unit,
    onRunAll: () -> Unit,
    onPurgeAll: () -> Unit,
    onClearFailedDb: () -> Unit,
    onRepackNx6r: (vpsId: String) -> Unit = {},
) {
    val nodes = status?.nodes.orEmpty()
    val uploadedCount = status?.seenCounts?.get("uploaded") ?: 0
    val failedCount = status?.seenCounts?.get("failed") ?: 0
    var torrentQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip("24h Subidos", "${status?.uploaded24h ?: 0}", SecondaryEmerald, Modifier.weight(1f))
                SummaryChip("Total OK", "$uploadedCount", PrimaryCyan, Modifier.weight(1f))
                SummaryChip("Fallos", "$failedCount", if (failedCount > 0) WarningAmber else PrimaryCyan, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onRunAll,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("Ejecutar todos", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onPurgeAll,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("Purgar todos", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onClearFailedDb,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text("Limpiar fallos", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            OutlinedTextField(
                value = torrentQuery,
                onValueChange = { torrentQuery = it },
                label = { Text("Filtrar torrents activos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (nodes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        text = "Esperando telemetria de los VPS en MongoDB (pipeline_nodes)...",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(nodes, key = { it.id }) { node ->
                val filteredTorrents = if (torrentQuery.isBlank()) {
                    node.torrents
                } else {
                    node.torrents.filter { it.name.contains(torrentQuery, ignoreCase = true) }
                }
                VpsNodeCard(
                    node = node.copy(torrents = filteredTorrents),
                    totalDlKBs = node.torrents.sumOf { it.dlSpeedKBs },
                    onCancelTorrent = { hash -> onCancelTorrent(node.id, hash) },
                    onPurgeStalled = { onPurgeStalled(node.id) },
                    onRunNow = { onRunNow(node.id) },
                    onRepackNx6r = { onRepackNx6r(node.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun VpsNodeCard(
    node: VpsNode,
    totalDlKBs: Int = 0,
    onCancelTorrent: (String) -> Unit,
    onPurgeStalled: () -> Unit,
    onRunNow: () -> Unit,
    onRepackNx6r: () -> Unit,
) {
    val isOnline = node.status == "online"
    val ramFrac = if (node.memTotalMB > 0) (node.memUsedMB.toFloat() / node.memTotalMB).coerceIn(0f, 1f) else 0f
    val diskFrac = if (node.diskTotalGB > 0) (node.diskUsedGB / node.diskTotalGB).toFloat().coerceIn(0f, 1f) else 0f
    val cpuFrac = (node.cpuPercent / 100.0).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f),
                    if (isOnline) PrimaryCyan.copy(alpha = 0.42f) else DangerRed.copy(alpha = 0.5f),
                    AccentPurple.copy(alpha = 0.25f),
                )
            )
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) SecondaryEmerald else DangerRed),
                    )
                    Text(
                        text = node.id.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (node.bucketOrder.isNotBlank()) {
                        Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = node.bucketOrder,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = PrimaryCyan,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRunNow, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Ejecutar ciclo", tint = SecondaryEmerald)
                    }
                    IconButton(onClick = onRepackNx6r, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Layers, contentDescription = "Repack NX6R", tint = PrimaryCyan)
                    }
                    IconButton(onClick = onPurgeStalled, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Purgar parados", tint = WarningAmber)
                    }
                }
            }

            MetricBar("CPU", "${node.cpuPercent}%", cpuFrac)
            MetricBar("RAM", "${node.memUsedMB} / ${node.memTotalMB} MB", ramFrac)
            MetricBar("Disco", "${node.diskUsedGB} / ${node.diskTotalGB} GB", diskFrac)
            val dlLabel = if (totalDlKBs >= 1024) "${"%.1f".format(totalDlKBs / 1024f)} MB/s" else "$totalDlKBs KB/s"
            Text(
                text = "IA: ${node.ollamaHost.ifBlank { "local" }} • NX6R: ${node.nx6rProfile} • DL: $dlLabel",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryCyan,
            )
            node.nx6rTelemetry.lastOrNull()?.let { tel ->
                val savedPct = if (tel.rawBytes > 0) ((tel.rawBytes - tel.outBytes) * 100.0 / tel.rawBytes) else 0.0
                Text(
                    text = "Último NX6R: ${tel.file} (${"%.1f".format(savedPct)}% ahorro, ${tel.chunks} chunks)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (node.ollamaModel.isNotBlank()) {
                Text(
                    text = "IA: ${node.ollamaModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = SurfaceVariantDark)

            Text(
                text = "Torrents activos (${node.torrents.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (node.torrents.isEmpty()) {
                Text(
                    text = "Sin descargas activas en qBittorrent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                node.torrents.forEach { t ->
                    TorrentRow(t, onCancel = { onCancelTorrent(t.hash) })
                }
            }
        }
    }
}

@Composable
private fun MetricBar(label: String, valueText: String, fraction: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = valueText, style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (fraction > 0.85f) DangerRed else PrimaryCyan,
            trackColor = SurfaceVariantDark,
        )
    }
}

@Composable
private fun TorrentRow(torrent: QbtTorrent, onCancel: () -> Unit) {
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = torrent.name.ifBlank { torrent.hash.take(12) },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${torrent.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryEmerald,
                    )
                    Text(
                        text = "${torrent.dlSpeedKBs} KB/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryCyan,
                    )
                    Text(
                        text = torrent.state,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Cancelar torrent", tint = DangerRed)
            }
        }
    }
}
