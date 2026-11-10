package com.localbites.feature.customer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localbites.core.network.model.Order
import com.localbites.core.ui.components.EmptyState
import com.localbites.core.ui.components.LoadingScreen
import com.localbites.core.ui.theme.*
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
                        text = "My Orders",
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
        when {
            uiState.isLoading && uiState.orders.isEmpty() -> {
                LoadingScreen(modifier = Modifier.padding(innerPadding))
            }
            uiState.orders.isEmpty() -> {
                EmptyOrdersState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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

@Composable
private fun EmptyOrdersState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No orders yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your order history will appear here once you place your first order",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OrderHistoryCard(
    order: Order,
    onOrderClick: () -> Unit,
    onReorderClick: () -> Unit,
) {
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
            // Header row: restaurant name + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.restaurantName ?: "Restaurant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            if (order.placedAt != null) {
                Text(
                    text = formatOrderDate(order.placedAt!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Items summary
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

            // Bottom row: total + reorder button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "₹${String.format("%.0f", order.total)}",
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
                            text = "Reorder",
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
    val (backgroundColor, textColor, label) = when (status) {
        "PLACED" -> Triple(OrderPlaced.copy(alpha = 0.15f), OrderPlaced, "Placed")
        "CONFIRMED" -> Triple(OrderPlaced.copy(alpha = 0.15f), OrderPlaced, "Confirmed")
        "PREPARING" -> Triple(OrderPreparing.copy(alpha = 0.15f), OrderPreparing, "Preparing")
        "READY" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, "Ready")
        "PICKED_UP" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, "Picked Up")
        "ON_THE_WAY" -> Triple(OrderReady.copy(alpha = 0.15f), OrderReady, "On the Way")
        "DELIVERED" -> Triple(OrderDelivered.copy(alpha = 0.15f), OrderDelivered, "Delivered")
        "CANCELLED" -> Triple(OrderCancelled.copy(alpha = 0.15f), OrderCancelled, "Cancelled")
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

/**
 * Formats an ISO date string into a user-friendly format.
 * Input example: "2024-01-15T10:30:00Z"
 * Output example: "15 Jan 2024, 10:30 AM"
 */
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
