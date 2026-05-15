package com.example.deepseek

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "deepseek_prefs")

object Storage {
    private val SETTINGS = stringPreferencesKey("settings_json")
    private val HISTORY = stringPreferencesKey("history_json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun settings(ctx: Context): Flow<AppSettings> =
        ctx.dataStore.data.map { prefs ->
            prefs[SETTINGS]?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() }
                ?: AppSettings()
        }

    suspend fun saveSettings(ctx: Context, s: AppSettings) {
        ctx.dataStore.edit { it[SETTINGS] = json.encodeToString(AppSettings.serializer(), s) }
    }

    fun history(ctx: Context): Flow<List<ChatMessage>> =
        ctx.dataStore.data.map { prefs ->
            prefs[HISTORY]?.let {
                runCatching { json.decodeFromString<List<ChatMessage>>(it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun saveHistory(ctx: Context, msgs: List<ChatMessage>) {
        ctx.dataStore.edit {
            it[HISTORY] = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(ChatMessage.serializer()),
                msgs
            )
        }
    }
}
