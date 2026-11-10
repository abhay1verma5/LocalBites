package com.localbites.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_menu_categories")
data class CachedMenuCategoryEntity(
    @PrimaryKey val id: Int,
    val restaurantId: Int,
    val name: String,
    val sortOrder: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_menu_items")
data class CachedMenuItemEntity(
    @PrimaryKey val id: Int,
    val categoryId: Int,
    val restaurantId: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val discountedPrice: Double?,
    val effectivePrice: Double,
    val image: String?,
    val foodType: String,
    val isAvailable: Boolean,
    val isBestseller: Boolean,
    val preparationTime: Int,
    val cachedAt: Long = System.currentTimeMillis(),
)
