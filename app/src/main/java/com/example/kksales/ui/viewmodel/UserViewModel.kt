package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.local.entity.UserInventory
import com.example.kksales.data.preferences.UserPreferencesManager
import com.example.kksales.data.repository.TransactionRepository
import com.example.kksales.data.repository.UserInventoryRepository
import com.example.kksales.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModel(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val userInventoryRepository: UserInventoryRepository,
    private val settingsRepository: com.example.kksales.data.repository.SettingsRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val taskRepository: com.example.kksales.data.repository.TaskRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _settings = MutableStateFlow<com.example.kksales.data.local.entity.AppSettings>(com.example.kksales.data.local.entity.AppSettings())
    val settings: StateFlow<com.example.kksales.data.local.entity.AppSettings> = _settings.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _userTasks = MutableStateFlow<List<com.example.kksales.data.local.entity.Task>>(emptyList())
    val userTasks: StateFlow<List<com.example.kksales.data.local.entity.Task>> = _userTasks.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    private val _userInventory = MutableStateFlow<List<UserInventory>>(emptyList())
    val userInventory: StateFlow<List<UserInventory>> = _userInventory.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    val language = userPreferencesManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "sv")

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            userPreferencesManager.setLanguage(lang)
        }
    }

    init {
        viewModelScope.launch {
            userRepository.refreshUsers()
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                if (s != null) _settings.value = s
            }
        }
        
        // Observera alla användare för att se till att Admin alltid finns
        viewModelScope.launch {
            userRepository.allUsers.collect { users ->
                _allUsers.value = users
                if (users.none { it.name == "Kaparen" }) {
                    userRepository.insertUser(
                        User(name = "Kaparen", password = "Johansson91", balance = 0.0, isAdmin = true, isAdminPlus = true)
                    )
                }
            }
        }
        
        viewModelScope.launch {
            userPreferencesManager.currentUserId
                .flatMapLatest { userId ->
                    if (userId != null) {
                        userRepository.getUserFlowById(userId)
                    } else {
                        flowOf(null)
                    }
                }
                .collect { userData ->
                    _user.value = userData
                    if (userData != null) {
                        // Starta transaktionsobservation separat
                        viewModelScope.launch {
                            transactionRepository.getTransactionsByUserId(userData.id).collect {
                                _transactions.value = it
                            }
                        }
                        // Starta lagerobservation
                        viewModelScope.launch {
                            userInventoryRepository.getUserInventory(userData.id).collect {
                                _userInventory.value = it
                            }
                        }
                        // Starta uppdragsobservation
                        viewModelScope.launch {
                            taskRepository.getTasksForUser(userData.id).collect {
                                _userTasks.value = it
                            }
                        }
                    } else {
                        _transactions.value = emptyList()
                        _userInventory.value = emptyList()
                        _userTasks.value = emptyList()
                    }
                }
        }
    }

    private fun loadUserData(userId: Int) {
        viewModelScope.launch {
            val userData = userRepository.getUserById(userId)
            _user.value = userData
            
            transactionRepository.getTransactionsByUserId(userId).collect {
                _transactions.value = it
            }
        }
    }

    fun registerUser(name: String, password: String?, isAdmin: Boolean = false, isAdminPlus: Boolean = false, isReseller: Boolean = false, isLageransvarig: Boolean = false, isTransportor: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val existingUser = userRepository.getUserByName(name)
            if (existingUser != null) {
                onError("Användarnamnet är upptaget")
                return@launch
            }
            val newUser = User(
                name = name, 
                password = password, 
                balance = 0.0, 
                isAdmin = isAdmin,
                isAdminPlus = isAdminPlus,
                isReseller = isReseller,
                isLageransvarig = isLageransvarig,
                isTransportor = isTransportor
            )
            userRepository.insertUser(newUser)
            onSuccess()
        }
    }

    private val _needsFirstPassword = MutableStateFlow<User?>(null)
    val needsFirstPassword: StateFlow<User?> = _needsFirstPassword.asStateFlow()

    fun login(username: String, passwordAttempt: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            val userToLogin = userRepository.getUserByName(username)
            if (userToLogin != null) {
                if (userToLogin.password == null) {
                    // Användaren har inget lösenord än (skapad av admin)
                    _needsFirstPassword.value = userToLogin
                    _loginError.value = null
                } else if (userToLogin.password == passwordAttempt) {
                    userPreferencesManager.setCurrentUserId(userToLogin.id, rememberMe)
                    _loginError.value = null
                    _needsFirstPassword.value = null
                } else {
                    _loginError.value = "Fel lösenord"
                }
            } else {
                _loginError.value = "Användaren hittades inte"
            }
        }
    }

    fun setInitialPassword(user: User, newPassword: String) {
        viewModelScope.launch {
            val updatedUser = user.copy(password = newPassword)
            userRepository.updateUser(updatedUser)
            userPreferencesManager.setCurrentUserId(updatedUser.id, true)
            _needsFirstPassword.value = null
        }
    }

    fun cancelFirstPassword() {
        _needsFirstPassword.value = null
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun toggleAdminStatus(userToUpdate: User) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(isAdmin = !userToUpdate.isAdmin)
            userRepository.updateUser(updatedUser)
        }
    }

    fun toggleAdminPlusStatus(userToUpdate: User) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(isAdminPlus = !userToUpdate.isAdminPlus)
            userRepository.updateUser(updatedUser)
        }
    }

    fun toggleResellerStatus(userToUpdate: User) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(isReseller = !userToUpdate.isReseller)
            userRepository.updateUser(updatedUser)
        }
    }

    fun resetUserBalance(userToUpdate: User) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(balance = 0.0)
            userRepository.updateUser(updatedUser)
        }
    }

    fun resetCashBalance(userToUpdate: User) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(cashBalance = 0.0)
            userRepository.updateUser(updatedUser)
        }
    }

    fun toggleLageransvarigStatus(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user.copy(isLageransvarig = !user.isLageransvarig))
        }
    }

    fun toggleTransportorStatus(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user.copy(isTransportor = !user.isTransportor))
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user)
        }
    }

    fun clearAllTransactions() {
        val currentUser = _user.value
        if (currentUser?.isAdminPlus == true) {
            viewModelScope.launch {
                transactionRepository.deleteAllTransactions()
            }
        }
    }

    fun resetAllUserBalances() {
        val currentUser = _user.value
        if (currentUser?.isAdminPlus == true) {
            viewModelScope.launch {
                userRepository.resetAllBalances()
                userRepository.resetAllCashBalances()
            }
        }
    }

    fun updateProfileIcon(iconName: String) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(profileIcon = iconName)
            userRepository.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateUserRole(userToUpdate: User, newRole: String?) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(role = newRole)
            userRepository.updateUser(updatedUser)
        }
    }

    fun updateVehicleType(userToUpdate: User, vehicleType: String?) {
        viewModelScope.launch {
            val updatedUser = userToUpdate.copy(vehicleType = vehicleType)
            userRepository.updateUser(updatedUser)
            if (_user.value?.id == userToUpdate.id) {
                _user.value = updatedUser
            }
        }
    }

    fun updateSettings(newSettings: com.example.kksales.data.local.entity.AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun toggleDeveloperMode() {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(isDeveloperModeEnabled = !_settings.value.isDeveloperModeEnabled)
            settingsRepository.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun triggerGithubUpdate() {
        viewModelScope.launch {
            // I en verklig miljö skulle detta skicka ett anrop till en backend
            // som triggar en GitHub Action (workflow_dispatch).
            // Här simulerar vi processen.
            println("Triggar GitHub Update: Genererar ny JSON och uppdaterar version...")
            
            try {
                // Exempel på anrop till servern för att pusha ändringar
                // val response = apiService.triggerDeploy(settings = _settings.value)
                // if (response.isSuccessful) { ... }
            } catch (e: Exception) {
                println("GitHub push misslyckades: ${e.message}")
            }
        }
    }

    fun fetchCurrentFuelPrices() {
        viewModelScope.launch {
            // Simulerar uppdatering till dagens priser
            val current = _settings.value
            updateSettings(current.copy(
                fuelPrice95 = 17.89,
                fuelPrice98 = 18.74,
                fuelPriceDiesel = 18.12,
                lastFuelUpdate = System.currentTimeMillis(),
                cheapestStation95 = "OKQ8 (Längs rutt)",
                cheapestStation98 = "Circle K (Längs rutt)",
                cheapestStationDiesel = "Preem (Längs rutt)"
            ))
        }
    }

    fun addDistanceTransaction(distance: Double) {
        val currentUser = _user.value ?: return
        val s = _settings.value
        
        // Använd användarspecifika inställningar om de finns, annars globala
        val fPrice = currentUser.fuelPrice ?: s.fuelPrice
        val fCons = currentUser.fuelConsumption ?: s.fuelConsumption
        
        val cost = (distance / 10.0) * fCons * fPrice
        
        viewModelScope.launch {
            val updatedUser = currentUser.copy(balance = currentUser.balance - cost)
            userRepository.updateUser(updatedUser)
            
            val transaction = Transaction(
                userId = currentUser.id,
                productId = -1, // Markör för distans/bränsle
                amount = cost,
                vatAmount = cost * 0.25 / 1.25, // Exempel på momsberäkning
                vatRate = 0.25,
                quantity = distance.toInt(),
                unitCost = s.fuelPrice,
                timestamp = System.currentTimeMillis(),
                category = TransactionCategory.OTHER_EXPENSE,
                type = TransactionType.EXPENSE,
                paymentMethod = "System",
                description = "Bränslekostnad för $distance km",
                receiverId = null
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun logout(onBeforeLogout: () -> Unit = {}) {
        viewModelScope.launch {
            onBeforeLogout() // Kör t.ex. återställning av lager här
            userPreferencesManager.clearUser()
        }
    }

    fun addTask(task: com.example.kksales.data.local.entity.Task) {
        viewModelScope.launch {
            taskRepository.addTask(task)
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
        }
    }

    class Factory(
        private val userRepository: UserRepository,
        private val transactionRepository: TransactionRepository,
        private val userInventoryRepository: UserInventoryRepository,
        private val settingsRepository: com.example.kksales.data.repository.SettingsRepository,
        private val userPreferencesManager: UserPreferencesManager,
        private val taskRepository: com.example.kksales.data.repository.TaskRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return UserViewModel(userRepository, transactionRepository, userInventoryRepository, settingsRepository, userPreferencesManager, taskRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
