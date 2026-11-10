package com.localbites.feature.delivery.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.datastore.TokenManager
import com.localbites.core.common.location.LocationManager
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.network.api.DeliveryApi
import com.localbites.core.network.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val isLoggedOut: Boolean = false,
    // Location
    val currentLocation: Location? = null,
    val locationPermissionGranted: Boolean = false,
)

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val deliveryApi: DeliveryApi,
    val locationManager: LocationManager,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryUiState())
    val uiState: StateFlow<DeliveryUiState> = _uiState.asStateFlow()

    private var locationTrackingJob: Job? = null

    init {
        loadProfile()
        loadEarnings()
    }

    // --------------- Location ---------------

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
        if (granted && _uiState.value.isOnline) {
            startLocationTracking()
        }
    }

    /**
     * Starts continuous location tracking, sending updates to the backend.
     * Should be called when the delivery partner goes online.
     */
    fun startLocationTracking() {
        if (locationTrackingJob?.isActive == true) return
        if (!locationManager.hasLocationPermission()) return

        locationTrackingJob = viewModelScope.launch {
            locationManager.getLocationUpdates(intervalMs = 10_000L).collect { location ->
                _uiState.update { it.copy(currentLocation = location) }
                // Send to backend
                sendLocationToBackend(location.latitude, location.longitude)
            }
        }
    }

    fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }

    private fun sendLocationToBackend(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                deliveryApi.updateLocation(
                    mapOf("latitude" to latitude, "longitude" to longitude)
                )
            } catch (_: Exception) {
                // Silently ignore -- location updates are best-effort
            }
        }
    }

    // --------------- Profile ---------------

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = safeApiCall { deliveryApi.getProfile() }) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isOnline = result.data.isAvailable)
                    }
                    if (result.data.isAvailable) {
                        loadAvailableOrders()
                        if (locationManager.hasLocationPermission()) {
                            startLocationTracking()
                        }
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
                        if (locationManager.hasLocationPermission()) {
                            startLocationTracking()
                        }
                    } else {
                        _uiState.update { it.copy(availableOrders = emptyList()) }
                        stopLocationTracking()
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

    fun logout() {
        viewModelScope.launch {
            stopLocationTracking()
            tokenManager.clearAll()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}
