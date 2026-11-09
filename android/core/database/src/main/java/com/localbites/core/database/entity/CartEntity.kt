package com.localbites.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val restaurantId: Int,
    val restaurantName: String,
    val menuItemId: Int,
    val itemName: String,
    val price: Double,
    val quantity: Int,
    val foodType: String,
    val image: String? = null,
)
