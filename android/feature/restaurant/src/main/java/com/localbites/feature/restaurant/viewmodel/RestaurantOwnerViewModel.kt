package com.localbites.feature.restaurant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.network.api.OrderApi
import com.localbites.core.network.api.RestaurantApi
import com.localbites.core.network.model.MenuCategory
import com.localbites.core.network.model.MenuItem
import com.localbites.core.network.model.Order
import com.localbites.core.network.model.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantOwnerUiState(
    val isLoading: Boolean = false,
    val restaurant: Restaurant? = null,
    val isAcceptingOrders: Boolean = true,
    val incomingOrders: List<Order> = emptyList(),
    val menuCategories: List<MenuCategory> = emptyList(),
    val selectedStatusFilter: String? = null,
    val todayOrdersCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val error: String? = null,
)

@HiltViewModel
class RestaurantOwnerViewModel @Inject constructor(
    private val restaurantApi: RestaurantApi,
    private val orderApi: OrderApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantOwnerUiState())
    val uiState: StateFlow<RestaurantOwnerUiState> = _uiState.asStateFlow()

    init {
        loadRestaurant()
        loadIncomingOrders()
    }

    fun loadRestaurant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { restaurantApi.getOwnerRestaurants() }) {
                is Resource.Success -> {
                    val restaurant = result.data.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            restaurant = restaurant,
                            isAcceptingOrders = restaurant?.isAcceptingOrders ?: true,
                            error = null,
                        )
                    }
                    restaurant?.let { loadMenu(it.id) }
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

    fun loadIncomingOrders(statusFilter: String? = _uiState.value.selectedStatusFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedStatusFilter = statusFilter) }
            when (val result = safeApiCall { orderApi.getRestaurantOrders(status = statusFilter) }) {
                is Resource.Success -> {
                    val orders = result.data.results
                    val todayOrders = orders.size
                    val todayRevenue = orders.sumOf { it.total }
                    _uiState.update {
                        it.copy(
                            incomingOrders = orders,
                            todayOrdersCount = todayOrders,
                            todayRevenue = todayRevenue,
                            error = null,
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadMenu(restaurantId: Int) {
        viewModelScope.launch {
            when (val result = safeApiCall { restaurantApi.getRestaurantMenu(restaurantId) }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(menuCategories = result.data, error = null)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun toggleAcceptingOrders() {
        val restaurant = _uiState.value.restaurant ?: return
        viewModelScope.launch {
            when (val result = safeApiCall { restaurantApi.toggleAcceptingOrders(restaurant.id) }) {
                is Resource.Success -> {
                    val accepting = result.data["is_accepting_orders"] ?: !_uiState.value.isAcceptingOrders
                    _uiState.update { it.copy(isAcceptingOrders = accepting) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun acceptOrder(orderId: Int) {
        viewModelScope.launch {
            when (val result = safeApiCall { orderApi.acceptOrder(orderId) }) {
                is Resource.Success -> {
                    updateOrderInList(result.data)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun markOrderReady(orderId: Int) {
        viewModelScope.launch {
            when (val result = safeApiCall { orderApi.markOrderReady(orderId) }) {
                is Resource.Success -> {
                    updateOrderInList(result.data)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun toggleItemAvailability(categoryId: Int, itemId: Int) {
        val restaurant = _uiState.value.restaurant ?: return
        viewModelScope.launch {
            // Optimistic update: toggle locally first
            _uiState.update { state ->
                val updatedCategories = state.menuCategories.map { category ->
                    if (category.id == categoryId) {
                        category.copy(
                            items = category.items.map { item ->
                                if (item.id == itemId) {
                                    item.copy(isAvailable = !item.isAvailable)
                                } else item
                            }
                        )
                    } else category
                }
                state.copy(menuCategories = updatedCategories)
            }
            // Persist via API
            safeApiCall {
                restaurantApi.updateRestaurant(
                    restaurant.id,
                    mapOf("toggle_item" to itemId),
                )
            }
        }
    }

    fun setStatusFilter(status: String?) {
        loadIncomingOrders(statusFilter = status)
    }

    fun refreshAll() {
        loadRestaurant()
        loadIncomingOrders()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateOrderInList(updatedOrder: Order) {
        _uiState.update { state ->
            val updatedOrders = state.incomingOrders.map { order ->
                if (order.id == updatedOrder.id) updatedOrder else order
            }
            state.copy(incomingOrders = updatedOrders)
        }
    }
}
