package com.localbites.core.data.repository

import com.localbites.core.common.util.Resource
import com.localbites.core.common.util.networkBoundResource
import com.localbites.core.common.util.safeApiCall
import com.localbites.core.database.dao.CachedOrderDao
import com.localbites.core.database.entity.CachedOrderEntity
import com.localbites.core.network.api.OrderApi
import com.localbites.core.network.model.Order
import com.localbites.core.network.model.PlaceOrderRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderApi: OrderApi,
    private val cachedOrderDao: CachedOrderDao,
) {
    fun getOrders(forceRefresh: Boolean = false): Flow<Resource<List<Order>>> =
        networkBoundResource(
            query = {
                cachedOrderDao.getAllOrders().map { entities ->
                    entities.map { it.toOrder() }
                }
            },
            fetch = {
                val response = orderApi.getCustomerOrders()
                if (response.isSuccessful) {
                    response.body()?.results ?: emptyList()
                } else {
                    throw Exception(response.errorBody()?.string() ?: "Failed to fetch orders")
                }
            },
            saveFetchResult = { orders ->
                cachedOrderDao.insertAll(orders.map { it.toEntity() })
            },
            shouldFetch = { forceRefresh || it.isNullOrEmpty() },
        )

    suspend fun getOrderDetail(id: Int): Resource<Order> {
        return safeApiCall { orderApi.getOrderDetail(id) }
    }

    suspend fun placeOrder(request: PlaceOrderRequest): Resource<Order> {
        return safeApiCall { orderApi.placeOrder(request) }
    }

    suspend fun cancelOrder(id: Int): Resource<Order> {
        return safeApiCall { orderApi.cancelOrder(id) }
    }
}

// ---------- Mappers ----------

private fun Order.toEntity(): CachedOrderEntity {
    val itemsSummary = items?.joinToString("|") { "${it.name}:${it.quantity}:${it.price}" } ?: ""
    return CachedOrderEntity(
        id = id,
        restaurantName = restaurantName,
        restaurantImage = restaurantImage,
        status = status,
        total = total,
        placedAt = placedAt,
        itemsSummaryJson = itemsSummary,
    )
}

private fun CachedOrderEntity.toOrder(): Order {
    val parsedItems = if (itemsSummaryJson.isNotBlank()) {
        itemsSummaryJson.split("|").mapNotNull { part ->
            val parts = part.split(":")
            if (parts.size >= 3) {
                com.localbites.core.network.model.OrderItem(
                    id = 0,
                    name = parts[0],
                    quantity = parts[1].toIntOrNull() ?: 1,
                    price = parts[2].toDoubleOrNull() ?: 0.0,
                    itemTotal = null,
                )
            } else null
        }
    } else null

    return Order(
        id = id,
        restaurantName = restaurantName,
        restaurantImage = restaurantImage,
        status = status,
        subtotal = total,
        deliveryFee = 0.0,
        tax = 0.0,
        discount = 0.0,
        total = total,
        specialInstructions = null,
        placedAt = placedAt,
        deliveredAt = null,
        items = parsedItems,
        deliveryPartnerName = null,
    )
}
