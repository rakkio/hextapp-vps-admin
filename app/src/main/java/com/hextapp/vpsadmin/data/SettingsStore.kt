package com.hextapp.vpsadmin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vps_admin_settings")

data class ConnectionConfig(
    val baseUrl: String = "",
    val token: String = "",
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()
}

class SettingsStore(private val context: Context) {
    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyToken = stringPreferencesKey("api_token")

    val configFlow: Flow<ConnectionConfig> = context.dataStore.data.map { prefs ->
        ConnectionConfig(
            baseUrl = prefs[keyBaseUrl].orEmpty(),
            token = prefs[keyToken].orEmpty(),
        )
    }

    suspend fun save(baseUrl: String, token: String) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        val normalizedUrl = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            cleanUrl
        } else if (cleanUrl.isNotEmpty()) {
            "http://$cleanUrl"
        } else {
            ""
        }
        context.dataStore.edit { prefs ->
            prefs[keyBaseUrl] = normalizedUrl
            prefs[keyToken] = token.trim()
        }
    }
}
