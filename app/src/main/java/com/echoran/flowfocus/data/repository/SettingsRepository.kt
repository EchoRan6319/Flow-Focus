package com.echoran.flowfocus.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // WebDAV Keys
    private val WEBDAV_SERVER_URL = stringPreferencesKey("webdav_server_url")
    private val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
    private val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
    private val WEBDAV_SYNC_ENABLED = booleanPreferencesKey("webdav_sync_enabled")

    // Refinement Keys
    private val NOTIFICATION_BLOCKING_ENABLED = booleanPreferencesKey("notification_blocking_enabled")
    private val STRICT_MODE_ENABLED = booleanPreferencesKey("strict_mode_enabled")
    private val HIDE_FROM_RECENTS_ENABLED = booleanPreferencesKey("hide_from_recents_enabled")
    private val CUSTOM_WHITE_NOISE_TRACKS = stringSetPreferencesKey("custom_white_noise_tracks")

    val webDavServerUrl: Flow<String> = dataStore.data.map { it[WEBDAV_SERVER_URL] ?: "https://dav.jianguoyun.com/dav/" }
    val webDavUsername: Flow<String> = dataStore.data.map { it[WEBDAV_USERNAME] ?: "" }
    val webDavPassword: Flow<String> = dataStore.data.map { it[WEBDAV_PASSWORD] ?: "" }
    val isWebDavSyncEnabled: Flow<Boolean> = dataStore.data.map { it[WEBDAV_SYNC_ENABLED] ?: false }

    // Refinement Flows
    val isNotificationBlockingEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATION_BLOCKING_ENABLED] ?: false }
    val isStrictModeEnabled: Flow<Boolean> = dataStore.data.map { it[STRICT_MODE_ENABLED] ?: false }
    val isHideFromRecentsEnabled: Flow<Boolean> = dataStore.data.map { it[HIDE_FROM_RECENTS_ENABLED] ?: false }
    val customWhiteNoiseTracks: Flow<Set<String>> = dataStore.data.map { it[CUSTOM_WHITE_NOISE_TRACKS] ?: emptySet() }

    suspend fun updateWebDavSettings(url: String, user: String, pass: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[WEBDAV_SERVER_URL] = url
            prefs[WEBDAV_USERNAME] = user
            prefs[WEBDAV_PASSWORD] = pass
            prefs[WEBDAV_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setNotificationBlockingEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATION_BLOCKING_ENABLED] = enabled }
    }

    suspend fun setStrictModeEnabled(enabled: Boolean) {
        dataStore.edit { it[STRICT_MODE_ENABLED] = enabled }
    }

    suspend fun setHideFromRecentsEnabled(enabled: Boolean) {
        dataStore.edit { it[HIDE_FROM_RECENTS_ENABLED] = enabled }
    }

    suspend fun addCustomWhiteNoise(uri: String) {
        dataStore.edit { prefs ->
            val current = prefs[CUSTOM_WHITE_NOISE_TRACKS] ?: emptySet()
            prefs[CUSTOM_WHITE_NOISE_TRACKS] = current + uri
        }
    }
}
