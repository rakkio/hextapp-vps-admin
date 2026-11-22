package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextapp.vpsadmin.data.VpsStatusData
import com.hextapp.vpsadmin.ui.theme.DangerRed
import com.hextapp.vpsadmin.ui.theme.PrimaryCyan
import com.hextapp.vpsadmin.ui.theme.SecondaryEmerald
import com.hextapp.vpsadmin.ui.theme.SurfaceVariantDark
import com.hextapp.vpsadmin.ui.theme.WarningAmber

@Composable
fun LogsQueueScreen(
    status: VpsStatusData?,
    selectedVps: String,
    onSelectVps: (String) -> Unit,
    onRetrySeen: (String) -> Unit,
    onRetryJob: (String) -> Unit,
) {
    val nodes = status?.nodes.orEmpty()
    val currentLogs = nodes.firstOrNull { it.id == selectedVps }?.logs.orEmpty()
    val jobs = status?.jobs.orEmpty()
    val failedItems = status?.recentFailed.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Logs en vivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (nodes.map { it.id }.ifEmpty { listOf("vps1", "vps2", "vps3") }).forEach { vpsId ->
                    FilterChip(
                        selected = selectedVps == vpsId,
                        onClick = { onSelectVps(vpsId) },
                        label = { Text(vpsId.uppercase()) },
                    )
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (currentLogs.isEmpty()) {
                        Text("Sin lineas recientes de scheduler.log", style = MaterialTheme.typography.bodySmall)
                    } else {
                        currentLogs.takeLast(25).forEach { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Trabajos manuales recientes (${jobs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(jobs, key = { it.id }) { job ->
            val label = job.title ?: job.url ?: job.magnet?.take(48) ?: job.id
            val statusColor = when (job.status) {
                "done" -> SecondaryEmerald
                "failed" -> DangerRed
                "processing" -> PrimaryCyan
                else -> WarningAmber
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${job.targetVps.uppercase()} • ${job.platform} • ${job.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                        if (!job.error.isNullOrBlank()) {
                            Text(job.error, style = MaterialTheme.typography.labelSmall, color = DangerRed, maxLines = 2)
                        }
                    }
                    if (job.status == "failed") {
                        IconButton(onClick = { onRetryJob(job.id) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reintentar job", tint = PrimaryCyan)
                        }
                    }
                }
            }
        }

        item {
            Text("Fallos en pipeline_seen (${failedItems.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(failedItems, key = { it.id }) { item ->
            Surface(color = SurfaceVariantDark, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title ?: item.id, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        if (!item.error.isNullOrBlank()) {
                            Text(item.error, style = MaterialTheme.typography.labelSmall, color = DangerRed, maxLines = 2)
                        }
                    }
                    IconButton(onClick = { onRetrySeen(item.id) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reintentar", tint = PrimaryCyan)
                    }
                }
            }
        }
    }
}
