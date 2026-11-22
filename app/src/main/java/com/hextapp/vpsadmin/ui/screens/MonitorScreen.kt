package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hextapp.vpsadmin.data.QbtTorrent
import com.hextapp.vpsadmin.data.VpsNode
import com.hextapp.vpsadmin.data.VpsStatusData
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
) {
    val nodes = status?.nodes.orEmpty()
    val uploadedCount = status?.seenCounts?.get("uploaded") ?: 0
    val failedCount = status?.seenCounts?.get("failed") ?: 0

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
                VpsNodeCard(
                    node = node,
                    onCancelTorrent = { hash -> onCancelTorrent(node.id, hash) },
                    onPurgeStalled = { onPurgeStalled(node.id) },
                    onRunNow = { onRunNow(node.id) },
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
    onCancelTorrent: (String) -> Unit,
    onPurgeStalled: () -> Unit,
    onRunNow: () -> Unit,
) {
    val isOnline = node.status == "online"
    val ramFrac = if (node.memTotalMB > 0) (node.memUsedMB.toFloat() / node.memTotalMB).coerceIn(0f, 1f) else 0f
    val diskFrac = if (node.diskTotalGB > 0) (node.diskUsedGB / node.diskTotalGB).toFloat().coerceIn(0f, 1f) else 0f
    val cpuFrac = (node.cpuPercent / 100.0).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
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
                    IconButton(onClick = onPurgeStalled, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Purgar parados", tint = WarningAmber)
                    }
                }
            }

            MetricBar("CPU", "${node.cpuPercent}%", cpuFrac)
            MetricBar("RAM", "${node.memUsedMB} / ${node.memTotalMB} MB", ramFrac)
            MetricBar("Disco", "${node.diskUsedGB} / ${node.diskTotalGB} GB", diskFrac)

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
