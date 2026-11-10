package com.localbites.core.data.repository

import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.networkBoundResource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.database.dao.CachedMenuDao
import com.localbites.core.database.dao.CachedRestaurantDao
import com.localbites.core.database.entity.CachedMenuCategoryEntity
import com.localbites.core.database.entity.CachedMenuItemEntity
import com.localbites.core.database.entity.CachedRestaurantEntity
import com.localbites.core.network.api.RestaurantApi
import com.localbites.core.network.model.MenuCategory
import com.localbites.core.network.model.MenuItem
import com.localbites.core.network.model.Restaurant
import com.localbites.core.network.model.RestaurantDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepository @Inject constructor(
    private val restaurantApi: RestaurantApi,
    private val cachedRestaurantDao: CachedRestaurantDao,
    private val cachedMenuDao: CachedMenuDao,
) {
    companion object {
        private const val RESTAURANT_CACHE_TTL = 5 * 60 * 1000L // 5 minutes
        private const val MENU_CACHE_TTL = 10 * 60 * 1000L // 10 minutes
    }

    fun getRestaurants(
        search: String? = null,
        cuisineType: String? = null,
        forceRefresh: Boolean = false,
    ): Flow<Resource<List<Restaurant>>> = networkBoundResource(
        query = {
            cachedRestaurantDao.getAllRestaurants().map { entities ->
                var result = entities.map { it.toRestaurant() }
                if (!search.isNullOrBlank()) {
                    val q = search.lowercase()
                    result = result.filter {
                        it.name.lowercase().contains(q) || it.cuisineType.lowercase().contains(q)
                    }
                }
                if (!cuisineType.isNullOrBlank()) {
                    result = result.filter { it.cuisineType == cuisineType }
                }
                result
            }
        },
        fetch = {
            val response = restaurantApi.getRestaurants(
                search = search?.ifBlank { null },
                cuisineType = cuisineType,
            )
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                throw Exception(response.errorBody()?.string() ?: "Failed to fetch restaurants")
            }
        },
        saveFetchResult = { restaurants ->
            cachedRestaurantDao.insertAll(restaurants.map { it.toEntity() })
        },
        shouldFetch = { cached ->
            forceRefresh || cached.isNullOrEmpty() || isCacheStale(RESTAURANT_CACHE_TTL)
        },
    )

    suspend fun getRestaurantDetail(id: Int): Resource<RestaurantDetail> {
        return safeApiCall { restaurantApi.getRestaurantDetail(id) }
    }

    fun getRestaurantMenu(
        restaurantId: Int,
        forceRefresh: Boolean = false,
    ): Flow<Resource<List<MenuCategory>>> = networkBoundResource(
        query = {
            cachedMenuDao.getCategoriesByRestaurant(restaurantId).map { categories ->
                categories.map { category ->
                    val items = cachedMenuDao.getItemsByCategory(category.id)
                    category.toMenuCategory(items.map { it.toMenuItem() })
                }
            }
        },
        fetch = {
            val response = restaurantApi.getRestaurantMenu(restaurantId)
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                throw Exception(response.errorBody()?.string() ?: "Failed to fetch menu")
            }
        },
        saveFetchResult = { categories ->
            cachedMenuDao.clearCategoriesForRestaurant(restaurantId)
            cachedMenuDao.clearItemsForRestaurant(restaurantId)
            cachedMenuDao.insertCategories(categories.map { it.toEntity(restaurantId) })
            categories.forEach { category ->
                cachedMenuDao.insertItems(
                    category.items.map { it.toEntity(category.id, restaurantId) }
                )
            }
        },
        shouldFetch = { cached ->
            forceRefresh || cached.isNullOrEmpty() || isMenuCacheStale(restaurantId)
        },
    )

    private suspend fun isCacheStale(ttl: Long): Boolean {
        val oldest = cachedRestaurantDao.getOldestCacheTime() ?: return true
        return System.currentTimeMillis() - oldest > ttl
    }

    private suspend fun isMenuCacheStale(restaurantId: Int): Boolean {
        val oldest = cachedMenuDao.getOldestCacheTime(restaurantId) ?: return true
        return System.currentTimeMillis() - oldest > MENU_CACHE_TTL
    }
}

// ---------- Mappers ----------

fun Restaurant.toEntity(): CachedRestaurantEntity = CachedRestaurantEntity(
    id = id,
    name = name,
    cuisineType = cuisineType,
    image = image,
    imageUrl = imageUrl,
    rating = rating,
    totalRatings = totalRatings,
    avgDeliveryTime = avgDeliveryTime,
    deliveryFee = deliveryFee,
    minOrderAmount = minOrderAmount,
    isVegOnly = isVegOnly,
    isAcceptingOrders = isAcceptingOrders,
    city = city,
    latitude = latitude,
    longitude = longitude,
)

fun CachedRestaurantEntity.toRestaurant(): Restaurant = Restaurant(
    id = id,
    name = name,
    cuisineType = cuisineType,
    image = image,
    imageUrl = imageUrl,
    rating = rating,
    totalRatings = totalRatings,
    avgDeliveryTime = avgDeliveryTime,
    deliveryFee = deliveryFee,
    minOrderAmount = minOrderAmount,
    isVegOnly = isVegOnly,
    isAcceptingOrders = isAcceptingOrders,
    city = city,
    latitude = latitude,
    longitude = longitude,
)

fun MenuCategory.toEntity(restaurantId: Int): CachedMenuCategoryEntity =
    CachedMenuCategoryEntity(
        id = id,
        restaurantId = restaurantId,
        name = name,
        sortOrder = sortOrder,
    )

fun CachedMenuCategoryEntity.toMenuCategory(items: List<MenuItem>): MenuCategory =
    MenuCategory(
        id = id,
        name = name,
        sortOrder = sortOrder,
        items = items,
    )

fun MenuItem.toEntity(categoryId: Int, restaurantId: Int): CachedMenuItemEntity =
    CachedMenuItemEntity(
        id = id,
        categoryId = categoryId,
        restaurantId = restaurantId,
        name = name,
        description = description,
        price = price,
        discountedPrice = discountedPrice,
        effectivePrice = effectivePrice,
        image = image,
        foodType = foodType,
        isAvailable = isAvailable,
        isBestseller = isBestseller,
        preparationTime = preparationTime,
    )

fun CachedMenuItemEntity.toMenuItem(): MenuItem = MenuItem(
    id = id,
    name = name,
    description = description,
    price = price,
    discountedPrice = discountedPrice,
    effectivePrice = effectivePrice,
    image = image,
    foodType = foodType,
    isAvailable = isAvailable,
    isBestseller = isBestseller,
    preparationTime = preparationTime,
)
