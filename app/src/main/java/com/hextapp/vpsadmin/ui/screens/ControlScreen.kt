package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hextapp.vpsadmin.BuildConfig
import com.hextapp.vpsadmin.data.CatalogItem
import com.hextapp.vpsadmin.data.ConnectionConfig
import com.hextapp.vpsadmin.data.VpsStatusData
import com.hextapp.vpsadmin.ui.theme.DangerRed
import com.hextapp.vpsadmin.ui.theme.PrimaryCyan
import com.hextapp.vpsadmin.ui.theme.WarningAmber

@Composable
fun ControlScreen(
    config: ConnectionConfig,
    status: VpsStatusData?,
    isSubmitting: Boolean,
    isCheckingUpdate: Boolean,
    catalogItems: List<CatalogItem>,
    isLoadingCatalog: Boolean,
    onSaveConnection: (baseUrl: String, token: String) -> Unit,
    onCheckUpdate: () -> Unit,
    onSetBuckets: (vpsId: String, bucketOrder: String) -> Unit,
    onRestartScheduler: (vpsId: String) -> Unit,
    onRepackNx6r: (vpsId: String, platform: String, limit: Int) -> Unit,
    onGitPull: (vpsId: String) -> Unit,
    onClearFailedSeen: (vpsId: String) -> Unit,
    onSetNx6rProfile: (vpsId: String, profile: String) -> Unit,
    onUpdateAndRestartAll: () -> Unit,
    onSetEnvVar: (vpsId: String, key: String, value: String) -> Unit,
    onTestLlm: (vpsId: String) -> Unit,
    onLoadCatalog: () -> Unit,
    onDeleteCatalogItem: (id: String) -> Unit,
) {
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var token by remember(config.token) { mutableStateOf(config.token) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Conexion privada Tailscale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Activa la app Tailscale en tu movil y conecta a la IP privada de VPS 3 (ej: http://100.x.y.z:8787).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("URL Tailscale VPS 3") },
                    placeholder = { Text("http://100.x.y.z:8787") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Bearer Token privado") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onSaveConnection(baseUrl, token) },
                    enabled = !isSubmitting && baseUrl.isNotBlank() && token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Guardar y probar conexion")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Actualizaciones (GitHub)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Version actual: v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCheckUpdate,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isCheckingUpdate) "Comprobando..." else "Buscar actualizacion")
                }
            }
        }

        val nodes = status?.nodes.orEmpty()
        if (nodes.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Flota (${nodes.size} VPS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onUpdateAndRestartAll,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Actualizar y reiniciar los ${nodes.size} VPS")
                    }
                }
            }

            Text("Control por VPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            nodes.forEach { node ->
                var bucketInput by remember(node.id, node.bucketOrder) { mutableStateOf(node.bucketOrder) }
                var envKey by remember(node.id) { mutableStateOf("OLLAMA_MODEL") }
                var envVal by remember(node.id) { mutableStateOf(node.ollamaModel) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${node.id.uppercase()} • IA: ${node.ollamaHost.ifBlank { "local" }} • NX6R: ${node.nx6rProfile}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedTextField(
                            value = bucketInput,
                            onValueChange = { bucketInput = it },
                            label = { Text("PIPELINE_BUCKET_ORDER") },
                            placeholder = { Text("pc_game,retro,apk,mac_ios,pc_app") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSetBuckets(node.id, bucketInput) },
                                enabled = !isSubmitting && bucketInput.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Aplicar buckets")
                            }
                            OutlinedButton(
                                onClick = { onGitPull(node.id) },
                                enabled = !isSubmitting,
                            ) {
                                Text("Git Pull")
                            }
                            OutlinedButton(
                                onClick = { onRestartScheduler(node.id) },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                            ) {
                                Text("Reiniciar")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("archive", "mobile", "ultra").forEach { prof ->
                                val selected = node.nx6rProfile == prof
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSetNx6rProfile(node.id, prof) },
                                    label = { Text(prof.uppercase()) },
                                    enabled = !isSubmitting,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onRepackNx6r(node.id, "ANDROID", 20) },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("NX6R Android (20)")
                            }
                            OutlinedButton(
                                onClick = { onRepackNx6r(node.id, "WINDOWS", 10) },
                                enabled = !isSubmitting,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("NX6R PC (10)")
                            }
                        }
                        if (node.nx6rTelemetry.isNotEmpty()) {
                            val last = node.nx6rTelemetry.last()
                            val pct = if (last.rawBytes > 0) ((last.rawBytes - last.outBytes) * 100.0 / last.rawBytes) else 0.0
                            Text(
                                text = "Último NX6R: ${last.file} (${"%.1f".format(pct)}% ahorro, ${last.chunks} chunks)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { onClearFailedSeen(node.id) },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Limpiar fallos en MongoDB (pipeline_seen)")
                        }

                        HorizontalDivider()
                        Text("Ajuste remoto (.env) y LLM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("OLLAMA_MODEL", "NEXUS_LEVEL", "MAX_FILE_SIZE_MB").forEach { k ->
                                FilterChip(
                                    selected = envKey == k,
                                    onClick = { envKey = k },
                                    label = { Text(k.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = envVal,
                                onValueChange = { envVal = it },
                                label = { Text(envKey) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { onSetEnvVar(node.id, envKey, envVal.trim()) },
                                enabled = !isSubmitting && envVal.isNotBlank(),
                            ) {
                                Text("Guardar")
                            }
                        }
                        OutlinedButton(
                            onClick = { onTestLlm(node.id) },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Probar Ollama / LLM en ${node.id.uppercase()}")
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Catálogo reciente (items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onLoadCatalog, enabled = !isLoadingCatalog) {
                        Text(if (isLoadingCatalog) "Cargando..." else "Refrescar")
                    }
                }
                if (catalogItems.isEmpty()) {
                    Text(
                        "Pulsa Refrescar para ver los últimos elementos publicados en MongoDB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    catalogItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title.ifBlank { item.id }, fontWeight = FontWeight.Medium)
                                val mb = item.sizeBytes / (1024.0 * 1024.0)
                                Text(
                                    "${item.platform} • ${item.contentType} • ${"%.1f".format(mb)} MiB • ${item.owner}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryCyan,
                                )
                            }
                            TextButton(
                                onClick = { onDeleteCatalogItem(item.id) },
                                enabled = !isSubmitting,
                            ) {
                                Text("Borrar", color = DangerRed)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
