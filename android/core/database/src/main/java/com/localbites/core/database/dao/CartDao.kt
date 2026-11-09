package com.localbites.core.database.dao

import androidx.room.*
import com.localbites.core.database.entity.CartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<CartEntity>>

    @Query("SELECT * FROM cart_items WHERE restaurantId = :restaurantId")
    fun getItemsByRestaurant(restaurantId: Int): Flow<List<CartEntity>>

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT SUM(price * quantity) FROM cart_items")
    fun getTotalAmount(): Flow<Double?>

    @Query("SELECT DISTINCT restaurantId FROM cart_items LIMIT 1")
    suspend fun getCurrentRestaurantId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CartEntity)

    @Update
    suspend fun updateItem(item: CartEntity)

    @Delete
    suspend fun deleteItem(item: CartEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("DELETE FROM cart_items WHERE menuItemId = :menuItemId")
    suspend fun removeByMenuItemId(menuItemId: Int)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE menuItemId = :menuItemId")
    suspend fun updateQuantity(menuItemId: Int, quantity: Int)

    @Query("SELECT * FROM cart_items WHERE menuItemId = :menuItemId LIMIT 1")
    suspend fun getByMenuItemId(menuItemId: Int): CartEntity?
}
