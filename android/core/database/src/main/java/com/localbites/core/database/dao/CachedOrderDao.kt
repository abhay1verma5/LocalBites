package com.localbites.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localbites.core.database.entity.CachedOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedOrderDao {

    @Query("SELECT * FROM cached_orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<CachedOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<CachedOrderEntity>)

    @Query("DELETE FROM cached_orders")
    suspend fun clearAll()
}
