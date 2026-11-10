package com.localbites.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localbites.core.database.entity.CachedMenuCategoryEntity
import com.localbites.core.database.entity.CachedMenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMenuDao {

    @Query("SELECT * FROM cached_menu_categories WHERE restaurantId = :restaurantId ORDER BY sortOrder")
    fun getCategoriesByRestaurant(restaurantId: Int): Flow<List<CachedMenuCategoryEntity>>

    @Query("SELECT * FROM cached_menu_items WHERE categoryId = :categoryId ORDER BY name")
    suspend fun getItemsByCategory(categoryId: Int): List<CachedMenuItemEntity>

    @Query("SELECT * FROM cached_menu_items WHERE restaurantId = :restaurantId ORDER BY name")
    suspend fun getItemsByRestaurant(restaurantId: Int): List<CachedMenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CachedMenuCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CachedMenuItemEntity>)

    @Query("DELETE FROM cached_menu_categories WHERE restaurantId = :restaurantId")
    suspend fun clearCategoriesForRestaurant(restaurantId: Int)

    @Query("DELETE FROM cached_menu_items WHERE restaurantId = :restaurantId")
    suspend fun clearItemsForRestaurant(restaurantId: Int)

    @Query("SELECT MIN(cachedAt) FROM cached_menu_categories WHERE restaurantId = :restaurantId")
    suspend fun getOldestCacheTime(restaurantId: Int): Long?
}
