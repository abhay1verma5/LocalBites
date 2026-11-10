package com.localbites.feature.customer.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.datastore.TokenManager
import com.localbites.core.common.location.DistanceUtils
import com.localbites.core.common.location.LocationManager
import com.localbites.core.common.network.ConnectivityObserver
import com.localbites.core.common.network.NetworkType
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.data.repository.OrderRepository
import com.localbites.core.data.repository.RestaurantRepository
import com.localbites.core.data.repository.UserRepository
import com.localbites.core.network.api.RestaurantApi
import com.localbites.core.database.dao.CartDao
import com.localbites.core.database.entity.CartEntity
import com.localbites.core.network.api.OrderApi
import com.localbites.core.network.api.PaymentApi
import com.localbites.core.network.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val restaurantDistances: Map<Int, Double> = emptyMap(), // restaurantId -> distance in km
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
    // Location
    val userLocation: Location? = null,
    val locationPermissionGranted: Boolean = false,
    val deliveryAddressText: String = "Current Location",
    // Delivery tracking
    val deliveryPartnerLocation: DeliveryLocationResponse? = null,
    val trackingFailCount: Int = 0,
    val trackingRetryAvailable: Boolean = false,
    // Profile
    val userProfile: UserProfile? = null,
    val addresses: List<Address> = emptyList(),
    val isLoggedOut: Boolean = false,
    val ratingSubmitted: Boolean = false,
    // Network state
    val isOnline: Boolean = true,
    val networkType: NetworkType = NetworkType.WIFI,
)

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val orderApi: OrderApi,
    private val paymentApi: PaymentApi,
    private val restaurantApi: RestaurantApi,
    private val cartDao: CartDao,
    private val tokenManager: TokenManager,
    val locationManager: LocationManager,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var deliveryTrackingJob: Job? = null

    init {
        observeCart()
        observeConnectivity()
        loadRestaurants()
        observeLocation()
    }

    // --------------- Connectivity ---------------

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            connectivityObserver.networkType.collect { type ->
                _uiState.update { it.copy(networkType = type) }
            }
        }
    }

    // --------------- Location ---------------

    private fun observeLocation() {
        viewModelScope.launch {
            locationManager.currentLocation.collect { location ->
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            userLocation = location,
                            deliveryAddressText = "%.4f, %.4f".format(
                                location.latitude,
                                location.longitude,
                            ),
                        )
                    }
                    recalculateDistances()
                }
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
        if (granted) {
            fetchCurrentLocation()
        }
    }

    fun fetchCurrentLocation() {
        locationManager.getCurrentLocation { location ->
            if (location != null) {
                _uiState.update {
                    it.copy(
                        userLocation = location,
                        deliveryAddressText = "%.4f, %.4f".format(
                            location.latitude,
                            location.longitude,
                        ),
                    )
                }
                recalculateDistances()
            }
        }
    }

    private fun recalculateDistances() {
        val userLoc = _uiState.value.userLocation ?: return
        val restaurants = _uiState.value.restaurants
        val distances = mutableMapOf<Int, Double>()
        for (restaurant in restaurants) {
            val lat = restaurant.latitude
            val lon = restaurant.longitude
            if (lat != null && lon != null) {
                distances[restaurant.id] = DistanceUtils.calculateDistance(
                    userLoc.latitude,
                    userLoc.longitude,
                    lat,
                    lon,
                )
            }
        }
        _uiState.update { it.copy(restaurantDistances = distances) }
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
            restaurantRepository.getRestaurants(
                search = _uiState.value.searchQuery.ifBlank { null },
                cuisineType = _uiState.value.selectedCuisine,
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                restaurants = result.data,
                            )
                        }
                        recalculateDistances()
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
        viewModelScope.launch {
            restaurantRepository.getRestaurants(
                search = _uiState.value.searchQuery.ifBlank { null },
                cuisineType = _uiState.value.selectedCuisine,
                forceRefresh = true,
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                restaurants = result.data,
                            )
                        }
                        recalculateDistances()
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, error = result.message)
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    // --------------- Restaurant Detail & Menu ---------------

    fun loadRestaurantDetail(restaurantId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Detail — direct API
            when (val detailResult = restaurantRepository.getRestaurantDetail(restaurantId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(selectedRestaurant = detailResult.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = detailResult.message) }
                }
                is Resource.Loading -> Unit
            }

            // Menu — cached via repository
            restaurantRepository.getRestaurantMenu(restaurantId).collect { menuResult ->
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

            when (val result = orderRepository.placeOrder(request)) {
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
            orderRepository.getOrders(forceRefresh = true).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, orders = result.data)
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
    }

    // --------------- Order Tracking ---------------

    fun loadOrderDetail(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, currentOrder = result.data)
                    }
                    // Start tracking delivery partner location for active orders
                    val status = result.data.status
                    if (status in listOf("PICKED_UP", "ON_THE_WAY")) {
                        startDeliveryTracking(orderId)
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

    fun startDeliveryTracking(orderId: Int) {
        deliveryTrackingJob?.cancel()
        _uiState.update { it.copy(trackingFailCount = 0, trackingRetryAvailable = false) }
        deliveryTrackingJob = viewModelScope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                when (val result = safeApiCall { orderApi.getDeliveryLocation(orderId) }) {
                    is Resource.Success -> {
                        consecutiveFailures = 0
                        _uiState.update {
                            it.copy(
                                deliveryPartnerLocation = result.data,
                                trackingFailCount = 0,
                                trackingRetryAvailable = false,
                            )
                        }
                    }
                    is Resource.Error -> {
                        consecutiveFailures++
                        if (consecutiveFailures >= 3) {
                            _uiState.update {
                                it.copy(
                                    trackingFailCount = consecutiveFailures,
                                    trackingRetryAvailable = true,
                                )
                            }
                            return@launch // Stop polling after 3 failures
                        }
                    }
                    is Resource.Loading -> Unit
                }

                // Adaptive poll interval based on network type
                val pollInterval = when (_uiState.value.networkType) {
                    NetworkType.WIFI, NetworkType.CELLULAR_4G -> 10_000L
                    NetworkType.CELLULAR_3G -> 20_000L
                    NetworkType.CELLULAR_2G -> 45_000L
                    NetworkType.NONE -> return@launch // Stop polling when offline
                }
                delay(pollInterval)
            }
        }
    }

    fun retryDeliveryTracking(orderId: Int) {
        startDeliveryTracking(orderId)
    }

    fun stopDeliveryTracking() {
        deliveryTrackingJob?.cancel()
        deliveryTrackingJob = null
        _uiState.update {
            it.copy(
                deliveryPartnerLocation = null,
                trackingFailCount = 0,
                trackingRetryAvailable = false,
            )
        }
    }

    fun cancelOrder(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = orderRepository.cancelOrder(orderId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, currentOrder = result.data)
                    }
                    stopDeliveryTracking()
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

    // --------------- Profile ---------------

    fun loadProfile() {
        viewModelScope.launch {
            when (val result = userRepository.getProfile()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(userProfile = result.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadAddresses() {
        viewModelScope.launch {
            when (val result = userRepository.getAddresses()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(addresses = result.data) }
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun createAddress(address: Address) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = userRepository.createAddress(address)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    loadAddresses()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteAddress(id: Int) {
        viewModelScope.launch {
            when (userRepository.deleteAddress(id)) {
                is Resource.Success -> loadAddresses()
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    // --------------- Rating ---------------

    fun submitRating(restaurantId: Int, rating: Int, review: String = "") {
        viewModelScope.launch {
            val body = mutableMapOf<String, Any>(
                "rating" to rating,
            )
            if (review.isNotBlank()) body["review"] = review
            when (safeApiCall { restaurantApi.submitRating(restaurantId, body) }) {
                is Resource.Success -> {
                    _uiState.update { it.copy(ratingSubmitted = true) }
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun onRatingSubmittedHandled() {
        _uiState.update { it.copy(ratingSubmitted = false) }
    }

    fun updateProfile(firstName: String, lastName: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val body = mutableMapOf<String, String>()
            body["first_name"] = firstName
            body["last_name"] = lastName
            if (email.isNotBlank()) body["email"] = email
            when (val result = userRepository.updateProfile(body)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, userProfile = result.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            cartDao.clearCart()
            userRepository.clearCachedProfile()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    // --------------- Error Handling ---------------

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopDeliveryTracking()
    }
}
