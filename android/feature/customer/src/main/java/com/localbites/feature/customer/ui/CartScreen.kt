package com.localbites.feature.customer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localbites.core.database.entity.CartEntity
import com.localbites.core.network.model.Address
import com.localbites.core.ui.R as CoreR
import com.localbites.core.ui.components.FoodTypeIndicator
import com.localbites.core.ui.components.LottieEmptyState
import com.localbites.core.ui.theme.*
import com.localbites.feature.customer.R
import com.localbites.feature.customer.viewmodel.CustomerViewModel

private const val TAX_RATE = 0.05
private const val DEFAULT_DELIVERY_FEE = 30.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit,
    onCheckoutSuccess: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems = uiState.cartItems
    val subtotal = uiState.cartTotal
    val deliveryFee = if (cartItems.isNotEmpty()) DEFAULT_DELIVERY_FEE else 0.0
    val tax = subtotal * TAX_RATE
    val total = subtotal + deliveryFee + tax

    var selectedAddress by remember { mutableStateOf<Address?>(null) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    // Load addresses when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }

    // Auto-select default address or first address
    LaunchedEffect(uiState.addresses) {
        if (selectedAddress == null && uiState.addresses.isNotEmpty()) {
            selectedAddress = uiState.addresses.find { it.isDefault } ?: uiState.addresses.first()
        }
    }

    LaunchedEffect(uiState.orderPlacedSuccessfully) {
        if (uiState.orderPlacedSuccessfully) {
            uiState.currentOrder?.let { order ->
                onCheckoutSuccess(order.id)
            }
            viewModel.onOrderPlacedHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cart),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreR.string.back),
                        )
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text(
                                text = stringResource(CoreR.string.clear),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        if (cartItems.isEmpty()) {
            LottieEmptyState(
                animationRes = CoreR.raw.empty_cart,
                title = stringResource(R.string.your_cart_is_empty),
                subtitle = stringResource(R.string.browse_restaurants_cart),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    item {
                        RestaurantHeader(restaurantName = cartItems.first().restaurantName)
                    }

                    // Delivery address selector
                    item {
                        DeliveryAddressCard(
                            selectedAddress = selectedAddress,
                            onChangeClick = { showAddressSheet = true },
                            onAddClick = { showAddAddressDialog = true },
                        )
                    }

                    items(
                        items = cartItems,
                        key = { it.id },
                    ) { cartItem ->
                        SwipeToDismissCartItem(
                            cartItem = cartItem,
                            onIncrement = { viewModel.incrementCartItem(cartItem.menuItemId) },
                            onDecrement = { viewModel.decrementCartItem(cartItem.menuItemId) },
                            onDismiss = { viewModel.removeCartItem(cartItem) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        DashedDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }

                    item {
                        PriceBreakdown(
                            subtotal = subtotal,
                            deliveryFee = deliveryFee,
                            tax = tax,
                            total = total,
                        )
                    }
                }

                GradientCheckoutButton(
                    total = total,
                    isLoading = uiState.isLoading,
                    enabled = selectedAddress != null,
                    onCheckoutClick = {
                        selectedAddress?.let { addr ->
                            viewModel.placeOrder(deliveryAddressId = addr.id ?: 1)
                        }
                    },
                )
            }
        }
    }

    // Address selection bottom sheet
    if (showAddressSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressSheet = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Delivery Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                uiState.addresses.forEach { address ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedAddress = address
                                showAddressSheet = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAddress?.id == address.id)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = address.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${address.addressLine}, ${address.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showAddressSheet = false
                        showAddAddressDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.AddLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Address")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add address dialog
    if (showAddAddressDialog) {
        AddAddressDialog(
            onDismiss = { showAddAddressDialog = false },
            onSave = { address ->
                viewModel.createAddress(address)
                showAddAddressDialog = false
            },
        )
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}

@Composable
private fun DeliveryAddressCard(
    selectedAddress: Address?,
    onChangeClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (selectedAddress != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deliver to: ${selectedAddress.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${selectedAddress.addressLine}, ${selectedAddress.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onChangeClick) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No delivery address",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Add an address to place your order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onAddClick) {
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAddressDialog(
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit,
) {
    var label by remember { mutableStateOf("HOME") }
    var addressLine by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Delivery Address", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Label selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HOME", "WORK", "OTHER").forEach { opt ->
                        FilterChip(
                            selected = label == opt,
                            onClick = { label = opt },
                            label = { Text(opt) },
                        )
                    }
                }

                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text("Address Line") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                }
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (addressLine.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pincode.isNotBlank()) {
                        onSave(
                            Address(
                                label = label,
                                addressLine = addressLine,
                                landmark = landmark,
                                city = city,
                                state = state,
                                pincode = pincode,
                                latitude = 0.0,
                                longitude = 0.0,
                                isDefault = false,
                            )
                        )
                    }
                },
                enabled = addressLine.isNotBlank() && city.isNotBlank() && state.isNotBlank() && pincode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AddGreen),
            ) {
                Text("Save Address", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// Dashed line divider (Swiggy-style)
@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun RestaurantHeader(restaurantName: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.order_from),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = restaurantName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissCartItem(
    cartItem: CartEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "dismiss_bg",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove_item),
                    modifier = Modifier.padding(end = 20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        CartItemCard(
            cartItem = cartItem,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FoodTypeIndicator(foodType = cartItem.foodType)
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u20B9${(cartItem.price * cartItem.quantity).toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            QuantityControls(
                quantity = cartItem.quantity,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
            )
        }
    }
}

@Composable
private fun QuantityControls(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AddGreen,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.decrease_quantity),
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.widthIn(min = 24.dp),
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.increase_quantity),
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PriceBreakdown(
    subtotal: Double,
    deliveryFee: Double,
    tax: Double,
    total: Double,
) {
    val freeText = stringResource(CoreR.string.free)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.bill_details),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PriceRow(label = stringResource(R.string.subtotal), amount = subtotal)
        Spacer(modifier = Modifier.height(8.dp))
        PriceRow(
            label = stringResource(R.string.delivery_fee),
            amount = deliveryFee,
            highlight = deliveryFee == 0.0,
            freeText = freeText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PriceRow(label = stringResource(R.string.taxes_percent), amount = tax)

        Spacer(modifier = Modifier.height(12.dp))
        DashedDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.grand_total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "\u20B9${String.format("%.0f", total)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    amount: Double,
    highlight: Boolean = false,
    freeText: String = "FREE",
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
        if (highlight) {
            // FREE delivery green badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = RatingGreen.copy(alpha = 0.12f),
            ) {
                Text(
                    text = freeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = RatingGreen,
                )
            }
        } else {
            Text(
                text = "\u20B9${String.format("%.0f", amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// Gradient checkout button
@Composable
private fun GradientCheckoutButton(
    total: Double,
    isLoading: Boolean,
    enabled: Boolean = true,
    onCheckoutClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Button(
            onClick = onCheckoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading && enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            ),
            contentPadding = PaddingValues(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isLoading) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(AddGreen, Color(0xFF4CAF50))
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.proceed_to_checkout),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\u20B9${String.format("%.0f", total)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
