package com.localbites.feature.delivery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.network.api.DeliveryApi
import com.localbites.core.network.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryUiState(
    val isLoading: Boolean = false,
    val isOnline: Boolean = false,
    val availableOrders: List<Order> = emptyList(),
    val activeOrder: Order? = null,
    val deliveryHistory: List<Order> = emptyList(),
    val totalDeliveries: Int = 0,
    val totalEarnings: String = "0",
    val todayDeliveries: Int = 0,
    val todayEarnings: String = "0",
    val error: String? = null,
)

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val deliveryApi: DeliveryApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryUiState())
    val uiState: StateFlow<DeliveryUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadEarnings()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = safeApiCall { deliveryApi.getProfile() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isOnline = result.data.isAvailable)
                    }
                    if (result.data.isAvailable) {
                        loadAvailableOrders()
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun toggleOnlineStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { deliveryApi.toggleAvailability() }) {
                is Resource.Success -> {
                    val nowOnline = result.data["is_available"] ?: !_uiState.value.isOnline
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOnline = nowOnline,
                            error = null,
                        )
                    }
                    if (nowOnline) {
                        loadAvailableOrders()
                    } else {
                        _uiState.update { it.copy(availableOrders = emptyList()) }
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

    fun loadAvailableOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { deliveryApi.getAvailableOrders() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availableOrders = result.data.results,
                            error = null,
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

    fun acceptDelivery(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { deliveryApi.acceptDelivery(orderId) }) {
                is Resource.Success -> {
                    val acceptedOrder = _uiState.value.availableOrders.find { it.id == orderId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeOrder = acceptedOrder,
                            availableOrders = it.availableOrders.filter { order -> order.id != orderId },
                            error = null,
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

    fun confirmDelivery(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { deliveryApi.confirmDelivery(orderId) }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeOrder = null,
                            error = null,
                        )
                    }
                    loadAvailableOrders()
                    loadDeliveryHistory()
                    loadEarnings()
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

    fun loadDeliveryHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = safeApiCall { deliveryApi.getDeliveryHistory() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            deliveryHistory = result.data.results,
                            error = null,
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

    fun loadEarnings() {
        viewModelScope.launch {
            when (val result = safeApiCall { deliveryApi.getEarnings() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            totalDeliveries = result.data.totalDeliveries,
                            totalEarnings = result.data.totalEarnings,
                            todayDeliveries = result.data.todayDeliveries,
                            todayEarnings = result.data.todayEarnings,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
