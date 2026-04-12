package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.repository.UpdateRepository
import kotlinx.coroutines.launch

class UpdateViewModel(private val updateRepository: UpdateRepository) : ViewModel() {

    fun checkForUpdates() {
        viewModelScope.launch {
            val updateInfo = updateRepository.checkForUpdate()
            if (updateInfo != null) {
                // Här kan man trigga en dialog i UI:t, 
                // men vi kör direkt på nedladdning enligt ditt önskemål
                updateRepository.downloadAndInstallApk(updateInfo.apkUrl)
            }
        }
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
