package com.localbites.feature.customer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.localbites.core.common.network.NetworkType
import com.localbites.core.network.model.Order
import com.localbites.core.ui.components.LoadingScreen
import com.localbites.core.ui.components.OfflineBanner
import com.localbites.core.ui.components.OrderStatusStepper
import com.localbites.core.ui.theme.*
import com.localbites.feature.customer.R
import com.localbites.feature.customer.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: Int,
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDeliveryTracking()
        }
    }

    val order = uiState.currentOrder

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.order_number, orderId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.localbites.core.ui.R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading && order == null) {
            LoadingScreen(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.error ?: stringResource(R.string.order_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Offline / slow network banner
            OfflineBanner(
                isOnline = uiState.isOnline,
                isSlowNetwork = uiState.networkType in listOf(
                    NetworkType.CELLULAR_2G,
                    NetworkType.CELLULAR_3G,
                ),
            )

            // Tracking retry message
            if (uiState.trackingRetryAvailable) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Unable to track delivery.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.retryDeliveryTracking(orderId) }) {
                            Text("Tap to retry")
                        }
                    }
                }
            }

            StatusHeaderCard(order = order)

            // Live delivery tracking map
            val deliveryLocation = uiState.deliveryPartnerLocation
            val userLocation = uiState.userLocation
            val isActiveDelivery = order.status in listOf("PICKED_UP", "ON_THE_WAY")

            if (isActiveDelivery && (deliveryLocation != null || userLocation != null)) {
                DeliveryTrackingMap(
                    deliveryLat = deliveryLocation?.latitude,
                    deliveryLng = deliveryLocation?.longitude,
                    customerLat = userLocation?.latitude,
                    customerLng = userLocation?.longitude,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.order_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OrderStatusStepper(currentStatus = order.status)
                }
            }

            if (order.deliveryPartnerName != null) {
                DeliveryPartnerCard(partnerName = order.deliveryPartnerName!!)
            }

            OrderItemsCard(order = order)
            PriceSummaryCard(order = order)

            if (order.status in listOf("PLACED", "CONFIRMED")) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.cancelOrder(orderId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                ) {
                    Text(
                        text = stringResource(R.string.cancel_order),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DeliveryTrackingMap(
    deliveryLat: Double?,
    deliveryLng: Double?,
    customerLat: Double?,
    customerLng: Double?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        val deliveryPosition = if (deliveryLat != null && deliveryLng != null) {
            LatLng(deliveryLat, deliveryLng)
        } else null

        val customerPosition = if (customerLat != null && customerLng != null) {
            LatLng(customerLat, customerLng)
        } else null

        // Default to delivery partner position or customer position
        val defaultPosition = deliveryPosition ?: customerPosition ?: LatLng(20.5937, 78.9629) // India center

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(defaultPosition, 14f)
        }

        // Update camera to fit both markers when both are available
        LaunchedEffect(deliveryPosition, customerPosition) {
            if (deliveryPosition != null && customerPosition != null) {
                val bounds = LatLngBounds.Builder()
                    .include(deliveryPosition)
                    .include(customerPosition)
                    .build()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 80),
                    durationMs = 500,
                )
            } else if (deliveryPosition != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(deliveryPosition, 15f),
                    durationMs = 500,
                )
            }
        }

        Column {
            Text(
                text = stringResource(R.string.live_tracking),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            ) {
                // Delivery partner marker
                if (deliveryPosition != null) {
                    Marker(
                        state = MarkerState(position = deliveryPosition),
                        title = stringResource(R.string.delivery_partner_label),
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE,
                        ),
                    )
                }

                // Customer location marker
                if (customerPosition != null) {
                    Marker(
                        state = MarkerState(position = customerPosition),
                        title = stringResource(R.string.your_location),
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN,
                        ),
                    )
                }

                // Route line between delivery partner and customer
                if (deliveryPosition != null && customerPosition != null) {
                    Polyline(
                        points = listOf(deliveryPosition, customerPosition),
                        color = androidx.compose.ui.graphics.Color(0xFF1976D2),
                        width = 8f,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusHeaderCard(order: Order) {
    val statusPlacedTitle = stringResource(R.string.status_placed_title)
    val statusPlacedDesc = stringResource(R.string.status_placed_desc)
    val statusConfirmedTitle = stringResource(R.string.status_confirmed_title)
    val statusConfirmedDesc = stringResource(R.string.status_confirmed_desc)
    val statusPreparingTitle = stringResource(R.string.status_preparing_title)
    val statusPreparingDesc = stringResource(R.string.status_preparing_desc)
    val statusReadyTitle = stringResource(R.string.status_ready_title)
    val statusReadyDesc = stringResource(R.string.status_ready_desc)
    val statusPickedUpTitle = stringResource(R.string.status_picked_up_title)
    val statusPickedUpDesc = stringResource(R.string.status_picked_up_desc)
    val statusOnTheWayTitle = stringResource(R.string.status_on_the_way_title)
    val statusOnTheWayDesc = stringResource(R.string.status_on_the_way_desc)
    val statusDeliveredTitle = stringResource(R.string.status_delivered_title)
    val statusDeliveredDesc = stringResource(R.string.status_delivered_desc)
    val statusCancelledTitle = stringResource(R.string.status_cancelled_title)
    val statusCancelledDesc = stringResource(R.string.status_cancelled_desc)

    val (statusText, statusDescription) = when (order.status) {
        "PLACED" -> statusPlacedTitle to statusPlacedDesc
        "CONFIRMED" -> statusConfirmedTitle to statusConfirmedDesc
        "PREPARING" -> statusPreparingTitle to statusPreparingDesc
        "READY" -> statusReadyTitle to statusReadyDesc
        "PICKED_UP" -> statusPickedUpTitle to statusPickedUpDesc
        "ON_THE_WAY" -> statusOnTheWayTitle to statusOnTheWayDesc
        "DELIVERED" -> statusDeliveredTitle to statusDeliveredDesc
        "CANCELLED" -> statusCancelledTitle to statusCancelledDesc
        else -> order.status to ""
    }

    // Choose Lottie animation based on order status
    val lottieRes = when (order.status) {
        "PREPARING" -> com.localbites.core.ui.R.raw.food_loading
        "DELIVERED" -> com.localbites.core.ui.R.raw.order_success
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Lottie animation for status
            if (lottieRes != null) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(100.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (order.restaurantName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.restaurantName!!,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryPartnerCard(partnerName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Orange60, Color(0xFFFF6B35)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val initial = partnerName.firstOrNull()?.uppercaseChar() ?: 'D'
                Text(
                    text = initial.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.delivery_partner_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = { /* Open dialer */ }) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = stringResource(R.string.call_delivery_partner),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun OrderItemsCard(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_items),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            order.items?.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "${item.quantity}x",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "\u20B9${(item.itemTotal ?: (item.price * item.quantity)).toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (!order.specialInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.special_instructions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.specialInstructions ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PriceSummaryCard(order: Order) {
    val subtotalLabel = stringResource(R.string.subtotal)
    val deliveryFeeLabel = stringResource(R.string.delivery_fee)
    val taxLabel = stringResource(R.string.tax)
    val discountLabel = stringResource(R.string.discount)
    val totalPaidLabel = stringResource(R.string.total_paid)
    val freeText = stringResource(com.localbites.core.ui.R.string.free)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.payment_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            SummaryRow(label = subtotalLabel, value = "\u20B9${order.subtotal.toInt()}")
            Spacer(modifier = Modifier.height(6.dp))
            SummaryRow(
                label = deliveryFeeLabel,
                value = if (order.deliveryFee == 0.0) freeText else "\u20B9${order.deliveryFee.toInt()}",
                isHighlight = order.deliveryFee == 0.0,
            )
            Spacer(modifier = Modifier.height(6.dp))
            SummaryRow(label = taxLabel, value = "\u20B9${order.tax.toInt()}")

            if (order.discount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                SummaryRow(
                    label = discountLabel,
                    value = "-\u20B9${order.discount.toInt()}",
                    isHighlight = true,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = totalPaidLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "\u20B9${order.total.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
