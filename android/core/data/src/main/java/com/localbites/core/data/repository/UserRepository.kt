package com.localbites.core.data.repository

import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.database.dao.CachedUserProfileDao
import com.localbites.core.database.entity.CachedUserProfileEntity
import com.localbites.core.network.api.AuthApi
import com.localbites.core.network.model.Address
import com.localbites.core.network.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val authApi: AuthApi,
    private val cachedUserProfileDao: CachedUserProfileDao,
) {
    suspend fun getProfile(): Resource<UserProfile> {
        val networkResult = safeApiCall { authApi.getProfile() }
        when (networkResult) {
            is Resource.Success -> {
                // Cache the profile
                cachedUserProfileDao.insertProfile(networkResult.data.toEntity())
                return networkResult
            }
            is Resource.Error -> {
                // Try cached profile
                val cached = cachedUserProfileDao.getProfile()
                if (cached != null) {
                    return Resource.Success(cached.toUserProfile())
                }
                return networkResult
            }
            is Resource.Loading -> return networkResult
        }
    }

    suspend fun getAddresses(): Resource<List<Address>> {
        return when (val result = safeApiCall { authApi.getAddresses() }) {
            is Resource.Success -> Resource.Success(result.data.results)
            is Resource.Error -> result
            is Resource.Loading -> result
        }
    }

    suspend fun createAddress(address: Address): Resource<Address> {
        return safeApiCall { authApi.createAddress(address) }
    }

    suspend fun deleteAddress(id: Int): Resource<Unit> {
        return safeApiCall { authApi.deleteAddress(id) }
    }

    suspend fun updateProfile(body: Map<String, String>): Resource<UserProfile> {
        val result = safeApiCall { authApi.updateProfile(body) }
        if (result is Resource.Success) {
            cachedUserProfileDao.insertProfile(result.data.toEntity())
        }
        return result
    }

    suspend fun clearCachedProfile() {
        cachedUserProfileDao.clearProfile()
    }
}

// ---------- Mappers ----------

private fun UserProfile.toEntity(): CachedUserProfileEntity = CachedUserProfileEntity(
    id = id,
    phone = phone,
    firstName = firstName,
    lastName = lastName,
    email = email,
    role = role,
)

private fun CachedUserProfileEntity.toUserProfile(): UserProfile = UserProfile(
    id = id,
    phone = phone,
    firstName = firstName,
    lastName = lastName,
    email = email,
    role = role,
    username = phone,
    avatar = null,
)
