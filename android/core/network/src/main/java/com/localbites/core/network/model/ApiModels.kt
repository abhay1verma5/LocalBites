package com.localbites.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Auth
@JsonClass(generateAdapter = true)
data class SendOtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(val message: String, val otp: String?)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(val phone: String, @Json(name = "otp_code") val otpCode: String)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    val access: String,
    val refresh: String,
    @Json(name = "is_new_user") val isNewUser: Boolean,
    @Json(name = "has_role") val hasRole: Boolean,
    val role: String?,
)

@JsonClass(generateAdapter = true)
data class SetRoleRequest(val role: String)

@JsonClass(generateAdapter = true)
data class SetRoleResponse(val role: String)

// User
@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: Int,
    val phone: String,
    val username: String,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
    val email: String?,
    val role: String,
    val avatar: String?,
)

// Address
@JsonClass(generateAdapter = true)
data class Address(
    val id: Int? = null,
    val label: String = "HOME",
    @Json(name = "address_line") val addressLine: String,
    val landmark: String = "",
    val city: String,
    val state: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

// Restaurant
@JsonClass(generateAdapter = true)
data class Restaurant(
    val id: Int,
    val name: String,
    @Json(name = "cuisine_type") val cuisineType: String,
    val image: String?,
    val rating: Double,
    @Json(name = "total_ratings") val totalRatings: Int,
    @Json(name = "avg_delivery_time") val avgDeliveryTime: Int,
    @Json(name = "delivery_fee") val deliveryFee: Double,
    @Json(name = "min_order_amount") val minOrderAmount: Double,
    @Json(name = "is_veg_only") val isVegOnly: Boolean,
    @Json(name = "is_accepting_orders") val isAcceptingOrders: Boolean,
    val city: String? = null,
)

@JsonClass(generateAdapter = true)
data class RestaurantDetail(
    val id: Int,
    val name: String,
    val description: String?,
    @Json(name = "cuisine_type") val cuisineType: String,
    val image: String?,
    @Json(name = "cover_image") val coverImage: String?,
    val phone: String,
    val address: String,
    val city: String,
    val rating: Double,
    @Json(name = "total_ratings") val totalRatings: Int,
    @Json(name = "avg_delivery_time") val avgDeliveryTime: Int,
    @Json(name = "delivery_fee") val deliveryFee: Double,
    @Json(name = "min_order_amount") val minOrderAmount: Double,
    @Json(name = "is_veg_only") val isVegOnly: Boolean,
    @Json(name = "is_accepting_orders") val isAcceptingOrders: Boolean,
    @Json(name = "opening_time") val openingTime: String,
    @Json(name = "closing_time") val closingTime: String,
)

// Menu
@JsonClass(generateAdapter = true)
data class MenuCategory(
    val id: Int,
    val name: String,
    @Json(name = "sort_order") val sortOrder: Int,
    val items: List<MenuItem>,
)

@JsonClass(generateAdapter = true)
data class MenuItem(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    @Json(name = "discounted_price") val discountedPrice: Double?,
    @Json(name = "effective_price") val effectivePrice: Double,
    val image: String?,
    @Json(name = "food_type") val foodType: String,
    @Json(name = "is_available") val isAvailable: Boolean,
    @Json(name = "is_bestseller") val isBestseller: Boolean,
    @Json(name = "preparation_time") val preparationTime: Int,
)

// Order
@JsonClass(generateAdapter = true)
data class OrderItemCreate(
    @Json(name = "menu_item_id") val menuItemId: Int,
    val quantity: Int,
)

@JsonClass(generateAdapter = true)
data class PlaceOrderRequest(
    @Json(name = "restaurant_id") val restaurantId: Int,
    @Json(name = "delivery_address_id") val deliveryAddressId: Int,
    val items: List<OrderItemCreate>,
    @Json(name = "special_instructions") val specialInstructions: String = "",
)

@JsonClass(generateAdapter = true)
data class Order(
    val id: Int,
    @Json(name = "restaurant_name") val restaurantName: String?,
    @Json(name = "restaurant_image") val restaurantImage: String?,
    val status: String,
    val subtotal: Double,
    @Json(name = "delivery_fee") val deliveryFee: Double,
    val tax: Double,
    val discount: Double,
    val total: Double,
    @Json(name = "special_instructions") val specialInstructions: String?,
    @Json(name = "placed_at") val placedAt: String?,
    @Json(name = "delivered_at") val deliveredAt: String?,
    val items: List<OrderItem>?,
    @Json(name = "delivery_partner_name") val deliveryPartnerName: String?,
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: Int,
    val name: String,
    val price: Double,
    val quantity: Int,
    @Json(name = "item_total") val itemTotal: Double?,
)

// Payment
@JsonClass(generateAdapter = true)
data class CreatePaymentRequest(
    @Json(name = "order_id") val orderId: Int,
    val method: String,
)

@JsonClass(generateAdapter = true)
data class CreatePaymentResponse(
    @Json(name = "payment_id") val paymentId: Int? = null,
    @Json(name = "razorpay_order_id") val razorpayOrderId: String? = null,
    @Json(name = "razorpay_key") val razorpayKey: String? = null,
    val amount: Int? = null,
    val currency: String? = null,
)

// Delivery
@JsonClass(generateAdapter = true)
data class DeliveryProfile(
    val id: Int,
    val phone: String,
    val name: String?,
    @Json(name = "vehicle_type") val vehicleType: String,
    @Json(name = "vehicle_number") val vehicleNumber: String?,
    @Json(name = "is_available") val isAvailable: Boolean,
    @Json(name = "is_verified") val isVerified: Boolean,
    @Json(name = "total_deliveries") val totalDeliveries: Int,
    @Json(name = "total_earnings") val totalEarnings: Double,
)

@JsonClass(generateAdapter = true)
data class DeliveryEarnings(
    @Json(name = "total_deliveries") val totalDeliveries: Int,
    @Json(name = "total_earnings") val totalEarnings: String,
    @Json(name = "today_deliveries") val todayDeliveries: Int,
    @Json(name = "today_earnings") val todayEarnings: String,
)

// Pagination wrapper
@JsonClass(generateAdapter = true)
data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>,
)
