package com.localbites.core.network.api

import com.localbites.core.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface RestaurantApi {
    @GET("restaurants/")
    suspend fun getRestaurants(
        @Query("city") city: String? = null,
        @Query("cuisine_type") cuisineType: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("page") page: Int = 1,
    ): Response<PaginatedResponse<Restaurant>>

    @GET("restaurants/{id}/")
    suspend fun getRestaurantDetail(@Path("id") id: Int): Response<RestaurantDetail>

    @GET("menu/restaurant/{restaurantId}/")
    suspend fun getRestaurantMenu(@Path("restaurantId") restaurantId: Int): Response<PaginatedResponse<MenuCategory>>

    @GET("restaurants/{id}/ratings/")
    suspend fun getRestaurantRatings(@Path("id") id: Int): Response<PaginatedResponse<Map<String, Any>>>

    // Owner APIs
    @GET("restaurants/owner/")
    suspend fun getOwnerRestaurants(): Response<PaginatedResponse<Restaurant>>

    @POST("restaurants/owner/")
    suspend fun createRestaurant(@Body restaurant: Map<String, Any>): Response<Restaurant>

    @PATCH("restaurants/owner/{id}/")
    suspend fun updateRestaurant(@Path("id") id: Int, @Body data: Map<String, Any>): Response<Restaurant>

    @POST("restaurants/owner/{id}/toggle-orders/")
    suspend fun toggleAcceptingOrders(@Path("id") id: Int): Response<Map<String, Boolean>>
}
