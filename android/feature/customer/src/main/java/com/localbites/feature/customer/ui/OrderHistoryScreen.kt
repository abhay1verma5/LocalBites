package com.localbites.feature.customer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localbites.core.network.model.Order
import com.localbites.core.common.network.NetworkType
import com.localbites.core.ui.components.EmptyState
import com.localbites.core.ui.components.LoadingScreen
import com.localbites.core.ui.components.LottieEmptyState
import com.localbites.core.ui.components.OfflineBanner
import com.localbites.core.ui.theme.*
import com.localbites.feature.customer.R
import com.localbites.feature.customer.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: CustomerViewModel,
    onOrderClick: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.my_orders),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineBanner(
                isOnline = uiState.isOnline,
                isSlowNetwork = uiState.networkType in listOf(
                    NetworkType.CELLULAR_2G,
                    NetworkType.CELLULAR_3G,
                ),
            )

            when {
                uiState.isLoading && uiState.orders.isEmpty() -> {
                    LoadingScreen()
                }
                uiState.orders.isEmpty() -> {
                    EmptyOrdersState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.orders,
                            key = { it.id },
                        ) { order ->
                            OrderHistoryCard(
                                order = order,
                                onOrderClick = { onOrderClick(order.id) },
                                onReorderClick = { viewModel.reorder(order) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(modifier: Modifier = Modifier) {
    LottieEmptyState(
        animationRes = com.localbites.core.ui.R.raw.empty_orders,
        title = stringResource(R.string.no_orders_yet),
        subtitle = stringResource(R.string.order_history_empty_subtitle),
        modifier = modifier,
    )
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    onOrderClick: () -> Unit,
    onReorderClick: () -> Unit,
) {
    val totalLabel = stringResource(com.localbites.core.ui.R.string.total)
    val reorderText = stringResource(R.string.reorder)
    val restaurantFallback = stringResource(com.localbites.core.ui.R.string.restaurant)

    Card(
        onClick = onOrderClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.restaurantName ?: restaurantFallback,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (order.placedAt != null) {
                Text(
                    text = formatOrderDate(order.placedAt!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val itemsSummary = order.items?.joinToString(", ") { item ->
                "${item.quantity}x ${item.name}"
            } ?: "Order #${order.id}"

            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = totalLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "\u20B9${String.format("%.0f", order.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (order.status == "DELIVERED") {
                    OutlinedButton(
                        onClick = onReorderClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reorderText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusBadge(status: String) {
    val placedLabel = stringResource(com.localbites.core.ui.R.string.status_placed)
    val confirmedLabel = stringResource(com.localbites.core.ui.R.string.status_confirmed)
    val preparingLabel = stringResource(com.localbites.core.ui.R.string.status_preparing)
    val readyLabel = stringResource(com.localbites.core.ui.R.string.status_ready)
    val pickedUpLabel = stringResource(com.localbites.core.ui.R.string.status_picked_up)
    val onTheWayLabel = stringResource(com.localbites.core.ui.R.string.status_on_the_way)
    val deliveredLabel = stringResource(com.localbites.core.ui.R.string.status_delivered)
    val cancelledLabel = stringResource(com.localbites.core.ui.R.string.status_cancelled)

    val (backgroundColor, textColor, label) = when (status) {
        "PLACED" -> Triple(OrderPlaced.copy(alpha = 0.15f), OrderPlaced, placedLabel)
        "CONFIRMED" -> Triple(OrderPlaced.copy(alpha = 0.15f), OrderPlaced, confirmedLabel)
        "PREPARING" -> Triple(OrderPreparing.copy(alpha = 0.15f), OrderPreparing, preparingLabel)
        "READY" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, readyLabel)
        "PICKED_UP" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, pickedUpLabel)
        "ON_THE_WAY" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, onTheWayLabel)
        "DELIVERED" -> Triple(OrderDelivered.copy(alpha = 0.15f), OrderDelivered, deliveredLabel)
        "CANCELLED" -> Triple(OrderCancelled.copy(alpha = 0.15f), OrderCancelled, cancelledLabel)
        else -> Triple(Color.Gray.copy(alpha = 0.15f), Color.Gray, status)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

private fun formatOrderDate(isoDate: String): String {
    return try {
        val parts = isoDate.split("T")
        if (parts.size < 2) return isoDate

        val dateParts = parts[0].split("-")
        if (dateParts.size < 3) return isoDate

        val year = dateParts[0]
        val monthNum = dateParts[1].toIntOrNull() ?: return isoDate
        val day = dateParts[2]

        val months = listOf(
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
        val monthName = months.getOrElse(monthNum) { "???" }

        val timePart = parts[1].substringBefore("Z").substringBefore("+").substringBefore(".")
        val timeParts = timePart.split(":")
        if (timeParts.size < 2) return "$day $monthName $year"

        val hour = timeParts[0].toIntOrNull() ?: return "$day $monthName $year"
        val minute = timeParts[1]
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        "$day $monthName $year, $displayHour:$minute $amPm"
    } catch (_: Exception) {
        isoDate
    }
}
