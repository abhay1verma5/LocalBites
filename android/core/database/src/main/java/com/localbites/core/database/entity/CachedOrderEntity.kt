package com.localbites.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_orders")
data class CachedOrderEntity(
    @PrimaryKey val id: Int,
    val restaurantName: String?,
    val restaurantImage: String?,
    val status: String,
    val total: Double,
    val placedAt: String?,
    val itemsSummaryJson: String, // JSON array of {name, quantity, price}
    val cachedAt: Long = System.currentTimeMillis(),
)
