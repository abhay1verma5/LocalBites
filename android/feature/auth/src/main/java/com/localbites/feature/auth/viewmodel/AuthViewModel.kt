package com.localbites.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localbites.core.common.datastore.TokenManager
import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.network.api.AuthApi
import com.localbites.core.network.model.SendOtpRequest
import com.localbites.core.network.model.SetRoleRequest
import com.localbites.core.network.model.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val devOtp: String? = null,
    val isVerified: Boolean = false,
    val hasRole: Boolean = false,
    val role: String? = null,
    val roleSet: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = safeApiCall { authApi.sendOtp(SendOtpRequest(phone)) }) {
                is Resource.Success -> {
                    tokenManager.saveUserPhone(phone)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        devOtp = result.data.otp,
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = safeApiCall { authApi.verifyOtp(VerifyOtpRequest(phone, otp)) }) {
                is Resource.Success -> {
                    tokenManager.saveTokens(result.data.access, result.data.refresh)
                    result.data.role?.let { tokenManager.saveUserRole(it) }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerified = true,
                        hasRole = result.data.hasRole,
                        role = result.data.role,
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setRole(role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = safeApiCall { authApi.setRole(SetRoleRequest(role)) }) {
                is Resource.Success -> {
                    tokenManager.saveUserRole(result.data.role)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        roleSet = true,
                        role = result.data.role,
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
