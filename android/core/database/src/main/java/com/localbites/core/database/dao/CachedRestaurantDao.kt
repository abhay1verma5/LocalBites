package com.localbites.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localbites.core.database.entity.CachedRestaurantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedRestaurantDao {

    @Query("SELECT * FROM cached_restaurants ORDER BY rating DESC")
    fun getAllRestaurants(): Flow<List<CachedRestaurantEntity>>

    @Query(
        "SELECT * FROM cached_restaurants WHERE name LIKE '%' || :query || '%' OR cuisineType LIKE '%' || :query || '%' ORDER BY rating DESC"
    )
    suspend fun searchRestaurants(query: String): List<CachedRestaurantEntity>

    @Query("SELECT * FROM cached_restaurants WHERE cuisineType = :cuisineType ORDER BY rating DESC")
    suspend fun getByCuisine(cuisineType: String): List<CachedRestaurantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(restaurants: List<CachedRestaurantEntity>)

    @Query("DELETE FROM cached_restaurants WHERE cachedAt < :timestamp")
    suspend fun clearOlderThan(timestamp: Long)

    @Query("SELECT MIN(cachedAt) FROM cached_restaurants")
    suspend fun getOldestCacheTime(): Long?
}
