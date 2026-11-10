package com.localbites.core.network.api

import com.localbites.core.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/send-otp/")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<SendOtpResponse>

    @POST("auth/verify-otp/")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("auth/set-role/")
    suspend fun setRole(@Body request: SetRoleRequest): Response<SetRoleResponse>

    @GET("auth/profile/")
    suspend fun getProfile(): Response<UserProfile>

    @PATCH("auth/profile/")
    suspend fun updateProfile(@Body profile: Map<String, String>): Response<UserProfile>

    @GET("auth/addresses/")
    suspend fun getAddresses(): Response<PaginatedResponse<Address>>

    @POST("auth/addresses/")
    suspend fun createAddress(@Body address: Address): Response<Address>

    @DELETE("auth/addresses/{id}/")
    suspend fun deleteAddress(@Path("id") id: Int): Response<Unit>
}
