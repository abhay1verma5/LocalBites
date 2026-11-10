package com.localbites.feature.customer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.localbites.core.network.model.MenuCategory
import com.localbites.core.network.model.MenuItem
import com.localbites.core.network.model.RestaurantDetail
import com.localbites.core.ui.components.FoodTypeIndicator
import com.localbites.core.ui.components.LoadingScreen
import com.localbites.core.ui.components.RatingChip
import com.localbites.core.ui.theme.*
import com.localbites.feature.customer.R
import com.localbites.feature.customer.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RestaurantDetailScreen(
    restaurantId: Int,
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit,
    onViewCartClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurantDetail(restaurantId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSelectedRestaurant() }
    }

    val restaurant = uiState.selectedRestaurant

    if (uiState.isLoading && restaurant == null) {
        LoadingScreen()
        return
    }

    if (restaurant == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = uiState.error ?: stringResource(R.string.restaurant_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = restaurant.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    bottom = if (uiState.cartCount > 0) 88.dp else 16.dp,
                ),
            ) {
                // Cover image - taller with 3-stop gradient
                item {
                    CoverImageSection(
                        restaurant = restaurant,
                        collapseProgress = scrollBehavior.state.collapsedFraction,
                    )
                }

                // Restaurant info
                item {
                    RestaurantInfoSection(restaurant = restaurant)
                }

                // Offer banner strip
                item {
                    OfferBannerStrip()
                }

                // Menu categories with sticky headers
                uiState.menuCategories.forEach { category ->
                    stickyHeader(key = "header_${category.id}") {
                        MenuCategoryHeader(category = category)
                    }

                    items(
                        items = category.items,
                        key = { "item_${it.id}" },
                    ) { menuItem ->
                        MenuItemCard(
                            menuItem = menuItem,
                            onAddClick = { viewModel.addToCart(menuItem, restaurant) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    // Spacer between categories
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // Empty menu state
                if (uiState.menuCategories.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.menu_not_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Floating cart bar
        AnimatedVisibility(
            visible = uiState.cartCount > 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            FloatingCartBar(
                itemCount = uiState.cartCount,
                total = uiState.cartTotal,
                onViewCartClick = onViewCartClick,
            )
        }
    }
}

@Composable
private fun CoverImageSection(
    restaurant: RestaurantDetail,
    collapseProgress: Float,
) {
    val closedText = stringResource(com.localbites.core.ui.R.string.closed)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp) // Taller cover image
            .graphicsLayer {
                alpha = 1f - (collapseProgress * 0.4f)
            },
    ) {
        AsyncImage(
            model = restaurant.imageUrl ?: restaurant.coverImage ?: restaurant.image,
            contentDescription = "${restaurant.name} cover",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 3-stop gradient overlay for premium look
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.15f),
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.75f),
                        ),
                    )
                ),
        )

        RatingChip(
            rating = restaurant.rating.toFloat(),
            totalRatings = restaurant.totalRatings,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )

        if (!restaurant.isAcceptingOrders) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = closedText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RestaurantInfoSection(restaurant: RestaurantDetail) {
    val freeDeliveryText = stringResource(com.localbites.core.ui.R.string.free_delivery)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = restaurant.cuisineType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!restaurant.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = restaurant.description!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoChip(
                icon = Icons.Outlined.Schedule,
                text = stringResource(com.localbites.core.ui.R.string.delivery_time_min, restaurant.avgDeliveryTime),
            )
            InfoChip(
                icon = Icons.Outlined.CurrencyRupee,
                text = if (restaurant.deliveryFee == 0.0) freeDeliveryText
                else stringResource(com.localbites.core.ui.R.string.delivery_fee_format, restaurant.deliveryFee.toInt()),
            )
            InfoChip(
                icon = Icons.Outlined.CurrencyRupee,
                text = stringResource(com.localbites.core.ui.R.string.min_order_format, restaurant.minOrderAmount.toInt()),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// Offer banner strip between cover and menu
@Composable
private fun OfferBannerStrip() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = OfferBlue.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = OfferBlue,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "50% off up to \u20B9100 | Use WELCOME50",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OfferBlue,
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MenuCategoryHeader(category: MenuCategory) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Text(
            text = "${category.name} (${category.items.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MenuItemCard(
    menuItem: MenuItem,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val addText = stringResource(com.localbites.core.ui.R.string.add)
    val naText = stringResource(com.localbites.core.ui.R.string.not_available_short)
    val bestsellerText = stringResource(com.localbites.core.ui.R.string.bestseller)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodTypeIndicator(foodType = menuItem.foodType)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (menuItem.isBestseller) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        ) {
                            Text(
                                text = bestsellerText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = menuItem.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u20B9${menuItem.effectivePrice.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (menuItem.discountedPrice != null && menuItem.discountedPrice!! < menuItem.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\u20B9${menuItem.price.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }

                if (!menuItem.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = menuItem.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Larger menu item image
                if (menuItem.image != null) {
                    AsyncImage(
                        model = menuItem.image,
                        contentDescription = menuItem.name,
                        modifier = Modifier
                            .size(100.dp) // Larger image
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Spring-animated ADD button
                SpringAddButton(
                    isAvailable = menuItem.isAvailable,
                    addText = addText,
                    naText = naText,
                    onAddClick = onAddClick,
                )
            }
        }
    }
}

// Spring-animated ADD button with scale bounce
@Composable
private fun SpringAddButton(
    isAvailable: Boolean,
    addText: String,
    naText: String,
    onAddClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "add_button_scale",
    )

    Button(
        onClick = onAddClick,
        enabled = isAvailable,
        modifier = Modifier
            .widthIn(min = 80.dp)
            .height(36.dp)
            .scale(scale),
        shape = RoundedCornerShape(8.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = AddGreen,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        if (isAvailable) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = addText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        } else {
            Text(
                text = naText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FloatingCartBar(
    itemCount: Int,
    total: Double,
    onViewCartClick: () -> Unit,
) {
    val itemsText = if (itemCount > 1) {
        stringResource(R.string.items_added_plural, itemCount)
    } else {
        stringResource(R.string.items_added_singular, itemCount)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onViewCartClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemsText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    text = "\u20B9${String.format("%.0f", total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Text(
                text = stringResource(R.string.view_cart),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )
        }
    }
}
