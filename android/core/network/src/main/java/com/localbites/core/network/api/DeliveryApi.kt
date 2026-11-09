package com.localbites.core.network.api

import com.localbites.core.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface DeliveryApi {
    @GET("delivery/profile/")
    suspend fun getProfile(): Response<DeliveryProfile>

    @PATCH("delivery/profile/")
    suspend fun updateProfile(@Body data: Map<String, Any>): Response<DeliveryProfile>

    @POST("delivery/toggle-available/")
    suspend fun toggleAvailability(): Response<Map<String, Boolean>>

    @POST("delivery/update-location/")
    suspend fun updateLocation(@Body location: Map<String, Double>): Response<Map<String, String>>

    @GET("delivery/available-orders/")
    suspend fun getAvailableOrders(): Response<PaginatedResponse<Order>>

    @POST("delivery/orders/{id}/accept/")
    suspend fun acceptDelivery(@Path("id") id: Int): Response<Map<String, String>>

    @POST("delivery/orders/{id}/delivered/")
    suspend fun confirmDelivery(@Path("id") id: Int): Response<Map<String, String>>

    @GET("delivery/history/")
    suspend fun getDeliveryHistory(@Query("page") page: Int = 1): Response<PaginatedResponse<Order>>

    @GET("delivery/earnings/")
    suspend fun getEarnings(): Response<DeliveryEarnings>
}
