package com.localbites.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_restaurants")
data class CachedRestaurantEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val cuisineType: String,
    val image: String?,
    val rating: Double,
    val totalRatings: Int,
    val avgDeliveryTime: Int,
    val deliveryFee: Double,
    val minOrderAmount: Double,
    val isVegOnly: Boolean,
    val isAcceptingOrders: Boolean,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    val cachedAt: Long = System.currentTimeMillis(),
)
