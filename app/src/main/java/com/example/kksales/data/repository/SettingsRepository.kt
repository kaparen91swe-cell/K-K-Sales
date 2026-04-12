package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.AppSettingsDao
import com.example.kksales.data.local.entity.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: AppSettingsDao) {
    val settings: Flow<AppSettings?> = settingsDao.getSettingsFlow()

    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings().also { updateSettings(it) }
    }
}
