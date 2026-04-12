package com.example.kksales.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val CURRENT_USER_ID = intPreferencesKey("current_user_id")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE] ?: "sv" // Standard till svenska
        }

    val languageBlocking: String
        get() = runBlocking { language.first() }

    val currentUserId: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_USER_ID]
        }

    val rememberMe: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REMEMBER_ME] ?: false
        }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false
        }

    suspend fun setCurrentUserId(userId: Int, remember: Boolean = false) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
            preferences[REMEMBER_ME] = remember
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID)
        }
    }
}
