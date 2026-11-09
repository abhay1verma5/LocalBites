package com.localbites.feature.customer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.datastore.TokenManager
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.database.dao.CartDao
import com.localbites.core.database.entity.CartEntity
import com.localbites.core.network.api.OrderApi
import com.localbites.core.network.api.PaymentApi
import com.localbites.core.network.api.RestaurantApi
import com.localbites.core.network.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val selectedRestaurant: RestaurantDetail? = null,
    val menuCategories: List<MenuCategory> = emptyList(),
    val cartItems: List<CartEntity> = emptyList(),
    val cartTotal: Double = 0.0,
    val cartCount: Int = 0,
    val orders: List<Order> = emptyList(),
    val currentOrder: Order? = null,
    val searchQuery: String = "",
    val selectedCuisine: String? = null,
    val error: String? = null,
    val orderPlacedSuccessfully: Boolean = false,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val restaurantApi: RestaurantApi,
    private val orderApi: OrderApi,
    private val paymentApi: PaymentApi,
    private val cartDao: CartDao,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    init {
        observeCart()
        loadRestaurants()
    }

    // --------------- Cart observation ---------------

    private fun observeCart() {
        viewModelScope.launch {
            cartDao.getAllItems().collect { items ->
                val total = items.sumOf { it.price * it.quantity }
                _uiState.update {
                    it.copy(
                        cartItems = items,
                        cartTotal = total,
                        cartCount = items.sumOf { item -> item.quantity },
                    )
                }
            }
        }
    }

    // --------------- Restaurants ---------------

    fun loadRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = safeApiCall {
                restaurantApi.getRestaurants(
                    search = _uiState.value.searchQuery.ifBlank { null },
                    cuisineType = _uiState.value.selectedCuisine,
                )
            }
            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            restaurants = result.data.results,
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message,
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadRestaurants()
    }

    fun onCuisineSelected(cuisine: String?) {
        _uiState.update {
            it.copy(selectedCuisine = if (it.selectedCuisine == cuisine) null else cuisine)
        }
        loadRestaurants()
    }

    fun refreshRestaurants() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadRestaurants()
    }

    // --------------- Restaurant Detail & Menu ---------------

    fun loadRestaurantDetail(restaurantId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val detailResult = safeApiCall { restaurantApi.getRestaurantDetail(restaurantId) }
            val menuResult = safeApiCall { restaurantApi.getRestaurantMenu(restaurantId) }

            when (detailResult) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedRestaurant = detailResult.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = detailResult.message) }
                }
                is Resource.Loading -> Unit
            }

            when (menuResult) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, menuCategories = menuResult.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = menuResult.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearSelectedRestaurant() {
        _uiState.update {
            it.copy(selectedRestaurant = null, menuCategories = emptyList())
        }
    }

    // --------------- Cart Management ---------------

    fun addToCart(menuItem: MenuItem, restaurant: RestaurantDetail) {
        viewModelScope.launch {
            val currentRestaurantId = cartDao.getCurrentRestaurantId()

            // If cart has items from a different restaurant, clear it first
            if (currentRestaurantId != null && currentRestaurantId != restaurant.id) {
                cartDao.clearCart()
            }

            val existingItem = cartDao.getByMenuItemId(menuItem.id)
            if (existingItem != null) {
                cartDao.updateQuantity(menuItem.id, existingItem.quantity + 1)
            } else {
                cartDao.insertItem(
                    CartEntity(
                        restaurantId = restaurant.id,
                        restaurantName = restaurant.name,
                        menuItemId = menuItem.id,
                        itemName = menuItem.name,
                        price = menuItem.effectivePrice,
                        quantity = 1,
                        foodType = menuItem.foodType,
                        image = menuItem.image,
                    )
                )
            }
        }
    }

    fun incrementCartItem(menuItemId: Int) {
        viewModelScope.launch {
            val item = cartDao.getByMenuItemId(menuItemId)
            if (item != null) {
                cartDao.updateQuantity(menuItemId, item.quantity + 1)
            }
        }
    }

    fun decrementCartItem(menuItemId: Int) {
        viewModelScope.launch {
            val item = cartDao.getByMenuItemId(menuItemId)
            if (item != null) {
                if (item.quantity > 1) {
                    cartDao.updateQuantity(menuItemId, item.quantity - 1)
                } else {
                    cartDao.removeByMenuItemId(menuItemId)
                }
            }
        }
    }

    fun removeCartItem(cartEntity: CartEntity) {
        viewModelScope.launch {
            cartDao.deleteItem(cartEntity)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartDao.clearCart()
        }
    }

    // --------------- Order Placement ---------------

    fun placeOrder(deliveryAddressId: Int, specialInstructions: String = "") {
        viewModelScope.launch {
            val cartItems = _uiState.value.cartItems
            if (cartItems.isEmpty()) return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = PlaceOrderRequest(
                restaurantId = cartItems.first().restaurantId,
                deliveryAddressId = deliveryAddressId,
                items = cartItems.map { item ->
                    OrderItemCreate(
                        menuItemId = item.menuItemId,
                        quantity = item.quantity,
                    )
                },
                specialInstructions = specialInstructions,
            )

            when (val result = safeApiCall { orderApi.placeOrder(request) }) {
                is Resource.Success -> {
                    cartDao.clearCart()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentOrder = result.data,
                            orderPlacedSuccessfully = true,
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onOrderPlacedHandled() {
        _uiState.update { it.copy(orderPlacedSuccessfully = false) }
    }

    // --------------- Order History ---------------

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { orderApi.getCustomerOrders() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, orders = result.data.results)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // --------------- Order Tracking ---------------

    fun loadOrderDetail(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { orderApi.getOrderDetail(orderId) }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, currentOrder = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun cancelOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = safeApiCall { orderApi.cancelOrder(orderId) }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, currentOrder = result.data)
                    }
                    loadOrders()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // --------------- Reorder ---------------

    fun reorder(order: Order) {
        viewModelScope.launch {
            val items = order.items ?: return@launch
            cartDao.clearCart()
            items.forEach { item ->
                cartDao.insertItem(
                    CartEntity(
                        restaurantId = 0, // Will be set from restaurant detail
                        restaurantName = order.restaurantName ?: "",
                        menuItemId = item.id,
                        itemName = item.name,
                        price = item.price,
                        quantity = item.quantity,
                        foodType = "VEG", // Default; actual type resolved on restaurant detail
                    )
                )
            }
        }
    }

    // --------------- Payment ---------------

    fun initiatePayment(orderId: Int, method: String = "COD") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = CreatePaymentRequest(orderId = orderId, method = method)
            when (val result = safeApiCall { paymentApi.createPayment(request) }) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // Payment created; for COD nothing more needed.
                    // For Razorpay, the response contains razorpayOrderId / razorpayKey.
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // --------------- Error Handling ---------------

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
