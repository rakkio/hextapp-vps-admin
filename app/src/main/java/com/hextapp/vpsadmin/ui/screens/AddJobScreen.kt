package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hextapp.vpsadmin.data.SearchCandidate
import com.hextapp.vpsadmin.ui.theme.PrimaryCyan
import com.hextapp.vpsadmin.ui.theme.SecondaryEmerald
import com.hextapp.vpsadmin.ui.theme.SurfaceVariantDark
import com.hextapp.vpsadmin.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddJobScreen(
    isSubmitting: Boolean,
    isSearching: Boolean,
    hasSearched: Boolean,
    searchResults: List<SearchCandidate>,
    onSearch: (title: String, platform: String, sourceGroup: String) -> Unit,
    onClearSearch: () -> Unit,
    onSubmitBatch: (rawLines: String, platform: String, contentType: String, targetVps: String) -> Unit,
    onSubmit: (url: String, magnet: String, title: String, platform: String, contentType: String, targetVps: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var magnet by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var batchMode by remember { mutableStateOf(false) }
    var batchText by remember { mutableStateOf("") }
    var sourceGroup by remember { mutableStateOf("all") }
    var platform by remember { mutableStateOf("auto") }
    var contentType by remember { mutableStateOf("auto") }
    var targetVps by remember { mutableStateOf("auto") }

    val platforms = listOf("auto" to "Auto", "windows" to "Windows", "macos" to "macOS", "android" to "Android", "cross-platform" to "Retro")
    val types = listOf("auto" to "Auto", "game" to "Juego", "app" to "App", "rom" to "ROM")
    val vpsList = listOf("auto" to "Auto", "vps1" to "VPS 1", "vps2" to "VPS 2", "vps3" to "VPS 3")
    val sourceGroups = listOf(
        "all" to "Todas (16)",
        "general" to "Generalistas",
        "spanish" to "Español",
        "specialized" to "Especializadas",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Enviar descarga manual a los VPS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pega una URL/magnet o escribe un titulo y pulsa \"Buscar candidatos\" para ver qué encuentran los servidores antes de descargar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL de pagina (opcional)") },
            placeholder = { Text("https://...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = magnet,
            onValueChange = { magnet = it },
            label = { Text("Enlace Magnet (opcional)") },
            placeholder = { Text("magnet:?xt=urn:btih:...") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titulo del juego o app") },
            placeholder = { Text("Ej: Minecraft PE, Spotify, Elden Ring") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Plataforma", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            platforms.forEach { (key, label) ->
                FilterChip(
                    selected = platform == key,
                    onClick = { platform = key },
                    label = { Text(label) },
                )
            }
        }

        Text("Tipo de contenido", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { (key, label) ->
                FilterChip(
                    selected = contentType == key,
                    onClick = { contentType = key },
                    label = { Text(label) },
                )
            }
        }

        Text("VPS destino", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vpsList.forEach { (key, label) ->
                FilterChip(
                    selected = targetVps == key,
                    onClick = { targetVps = key },
                    label = { Text(label) },
                )
            }
        }

        Text("Fuentes de búsqueda", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sourceGroups.forEach { (key, label) ->
                FilterChip(
                    selected = sourceGroup == key,
                    onClick = { sourceGroup = key },
                    label = { Text(label) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            FilterChip(
                selected = batchMode,
                onClick = { batchMode = !batchMode },
                label = { Text("Cola por lotes (múltiples líneas)") },
            )
        }

        if (batchMode) {
            OutlinedTextField(
                value = batchText,
                onValueChange = { batchText = it },
                label = { Text("Pega una URL, magnet o título por línea") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSubmitBatch(batchText, platform, contentType, targetVps)
                    batchText = ""
                },
                enabled = !isSubmitting && batchText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Encolar lote completo")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onSearch(title.trim(), platform, sourceGroup) },
                enabled = !isSearching && !isSubmitting && title.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Buscar candidatos")
                }
            }

            Button(
                onClick = {
                    onSubmit(url.trim(), magnet.trim(), title.trim(), platform, contentType, targetVps)
                    url = ""
                    magnet = ""
                    title = ""
                    onClearSearch()
                },
                enabled = !isSubmitting && (url.isNotBlank() || magnet.isNotBlank() || title.isNotBlank()),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Encolar directo")
                }
            }
        }

        if (hasSearched) {
            HorizontalDivider(color = SurfaceVariantDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Resultados encontrados (${searchResults.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (searchResults.isNotEmpty()) {
                    TextButton(onClick = onClearSearch) {
                        Text("Limpiar")
                    }
                }
            }

            if (searchResults.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = "No se encontró nada en los portales seleccionados para \"$title\". Prueba con otro nombre o pega un magnet/URL directo.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                    )
                }
            } else {
                searchResults.forEach { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(item.source, style = MaterialTheme.typography.labelSmall, color = PrimaryCyan)
                                Text(item.size, style = MaterialTheme.typography.labelSmall, color = SecondaryEmerald)
                                Text("${item.seeders} seeds", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!item.warning.isNullOrBlank()) {
                                Surface(
                                    color = WarningAmber.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = "⚠ ${item.warning}",
                                        color = WarningAmber,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onSubmit(
                                        item.url.orEmpty(),
                                        item.magnet.orEmpty(),
                                        title.trim().ifBlank { item.name },
                                        platform,
                                        contentType,
                                        targetVps,
                                    )
                                    onClearSearch()
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (item.matchesPlatform) "Descargar este" else "Descargar de todas formas")
                            }
                        }
                    }
                }
            }
        }
    }
}
