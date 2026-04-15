package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(private val updateRepository: UpdateRepository) : ViewModel() {

    private val _updateInfo = MutableStateFlow<com.example.kksales.data.remote.api.UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            _isChecking.value = true
            val info = updateRepository.checkForUpdate()
            _updateInfo.value = info
            _isChecking.value = false
        }
    }

    fun startUpdate(apkUrl: String) {
        updateRepository.downloadAndInstallApk(apkUrl)
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    class Factory(private val updateRepository: UpdateRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UpdateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return UpdateViewModel(updateRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
