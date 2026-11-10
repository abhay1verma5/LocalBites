package com.localbites.core.network.api

import com.localbites.core.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface OrderApi {
    @POST("orders/place/")
    suspend fun placeOrder(@Body request: PlaceOrderRequest): Response<Order>

    @GET("orders/my/")
    suspend fun getCustomerOrders(@Query("page") page: Int = 1): Response<PaginatedResponse<Order>>

    @GET("orders/{id}/")
    suspend fun getOrderDetail(@Path("id") id: Int): Response<Order>

    @POST("orders/{id}/cancel/")
    suspend fun cancelOrder(@Path("id") id: Int, @Body reason: Map<String, String> = emptyMap()): Response<Order>

    @GET("orders/{id}/delivery-location/")
    suspend fun getDeliveryLocation(@Path("id") id: Int): Response<DeliveryLocationResponse>

    // Restaurant owner
    @GET("orders/restaurant/")
    suspend fun getRestaurantOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
    ): Response<PaginatedResponse<Order>>

    @POST("orders/{id}/accept/")
    suspend fun acceptOrder(@Path("id") id: Int): Response<Order>

    @POST("orders/{id}/ready/")
    suspend fun markOrderReady(@Path("id") id: Int): Response<Order>
}
