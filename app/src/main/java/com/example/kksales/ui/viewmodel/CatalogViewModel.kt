package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.CartItem
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.calculatePrice
import com.example.kksales.data.preferences.UserPreferencesManager
import com.example.kksales.data.repository.CartRepository
import com.example.kksales.data.repository.OrderRepository
import com.example.kksales.data.repository.ProductRepository
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val userRepository: UserRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val admins: StateFlow<List<User>> = userRepository.allUsers
        .map { it.filter { user -> user.isAdmin || user.isReseller } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = userPreferencesManager.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) cartRepository.getCartItemsByUserId(userId)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = combine(cartItems, products) { items, allProducts ->
        items.groupBy { it.productId }.entries.sumOf { (productId, cartItemsForProduct) ->
            val product = allProducts.find { it.id == productId }
            if (product != null) {
                val totalQuantity = cartItemsForProduct.sumOf { it.quantity }
                product.calculatePrice(totalQuantity)
            } else {
                0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _orderResult = MutableSharedFlow<Result<Unit>>()
    val orderResult = _orderResult.asSharedFlow()

    init {
        viewModelScope.launch {
            // Seed some products if catalog is empty
            productRepository.allProducts.first().let { 
                if (it.isEmpty()) {
                    seedDefaultProducts()
                }
            }
        }
    }

    private suspend fun seedDefaultProducts() {
        val defaultProducts = listOf(
            Product(name = "Product A", unitCost = 5.0, salesPrice = 10.0, quantity = 50),
            Product(name = "Product B", unitCost = 10.0, salesPrice = 20.0, quantity = 30),
            Product(name = "Product C", unitCost = 2.0, salesPrice = 5.0, quantity = 100)
        )
        defaultProducts.forEach { productRepository.insertProduct(it) }
    }

    fun placeOrder(productId: Int, quantity: Int, paymentMethod: String = "Direkt") {
        viewModelScope.launch {
            val userId = userPreferencesManager.currentUserId.first()
            if (userId != null) {
                // Vi anropar bara repository, som sköter lagersaldo och databas
                val result = orderRepository.placeOrder(userId, productId, quantity, paymentMethod)
                _orderResult.emit(result)
            } else {
                _orderResult.emit(Result.failure(Exception("User not logged in")))
            }
        }
    }

    fun addToCart(productId: Int, quantity: Int, unitType: String = "gram") {
        viewModelScope.launch {
            val userId = userPreferencesManager.currentUserId.first()
            if (userId != null) {
                val product = productRepository.getProductById(productId)
                if (product != null && product.quantity >= quantity) {
                    // Minska lagret direkt när man lägger i korgen (reservation)
                    productRepository.updateProduct(product.copy(quantity = product.quantity - quantity))
                    cartRepository.addToCart(CartItem(userId = userId, productId = productId, quantity = quantity, unitType = unitType))
                }
            }
        }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch {
            val product = productRepository.getProductById(item.productId)
            if (product != null) {
                // Lägg tillbaka i lagret när man tar bort från korgen
                productRepository.updateProduct(product.copy(quantity = product.quantity + item.quantity))
            }
            cartRepository.removeFromCart(item)
        }
    }

    fun checkout(paymentMethod: String, receiverId: Int? = null) {
        viewModelScope.launch {
            val userId = userPreferencesManager.currentUserId.first() ?: return@launch
            val items = cartItems.value
            if (items.isEmpty()) return@launch

            var anySuccess = false
            var errorMessage = ""

            for (item in items) {
                // Anropa placeOrder i repository som sköter lokala ändringar + transaktioner
                val result = orderRepository.placeOrder(userId, item.productId, item.quantity, paymentMethod, receiverId)
                if (result.isSuccess) {
                    anySuccess = true
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Okänt fel"
                }
            }

            if (anySuccess) {
                // Töm korgen helt efter att köpen processats
                cartRepository.clearCart(userId)
                _orderResult.emit(Result.success(Unit))
            } else if (errorMessage.isNotEmpty()) {
                _orderResult.emit(Result.failure(Exception(errorMessage)))
            }
        }
    }

    fun clearCartAndRestoreStock() {
        val items = cartItems.value
        if (items.isEmpty()) return
        
        viewModelScope.launch {
            val userId = userPreferencesManager.currentUserId.first() ?: return@launch
            items.forEach { item ->
                val product = productRepository.getProductById(item.productId)
                if (product != null) {
                    productRepository.updateProduct(product.copy(quantity = product.quantity + item.quantity))
                }
            }
            cartRepository.clearCart(userId)
        }
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val orderRepository: OrderRepository,
        private val cartRepository: CartRepository,
        private val userRepository: UserRepository,
        private val userPreferencesManager: UserPreferencesManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CatalogViewModel(productRepository, orderRepository, cartRepository, userRepository, userPreferencesManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
