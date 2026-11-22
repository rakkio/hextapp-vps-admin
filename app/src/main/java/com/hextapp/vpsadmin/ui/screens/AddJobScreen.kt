package com.hextapp.vpsadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddJobScreen(
    isSubmitting: Boolean,
    onSubmit: (url: String, magnet: String, title: String, platform: String, contentType: String, targetVps: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var magnet by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("auto") }
    var contentType by remember { mutableStateOf("auto") }
    var targetVps by remember { mutableStateOf("auto") }

    val platforms = listOf("auto" to "Auto", "windows" to "Windows", "macos" to "macOS", "android" to "Android", "cross-platform" to "Retro")
    val types = listOf("auto" to "Auto", "game" to "Juego", "app" to "App", "rom" to "ROM")
    val vpsList = listOf("auto" to "Auto", "vps1" to "VPS 1", "vps2" to "VPS 2", "vps3" to "VPS 3")

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
            text = "Pega un enlace web (repack.info, fitgirl, 1337x...), un magnet:? o escribe el nombre de un juego/app para buscarlo automaticamente.",
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
            label = { Text("Titulo del juego o app (para buscar o nombrar)") },
            placeholder = { Text("Ej: Elden Ring") },
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

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                onSubmit(url.trim(), magnet.trim(), title.trim(), platform, contentType, targetVps)
                url = ""
                magnet = ""
                title = ""
            },
            enabled = !isSubmitting && (url.isNotBlank() || magnet.isNotBlank() || title.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Encolar descarga en VPS")
            }
        }
    }
}
