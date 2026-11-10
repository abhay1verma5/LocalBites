package com.localbites.feature.customer.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.localbites.core.common.location.DistanceUtils
import com.localbites.core.network.model.Restaurant
import com.localbites.core.common.network.NetworkType
import com.localbites.core.ui.components.EmptyState
import com.localbites.core.ui.components.OfflineBanner
import com.localbites.core.ui.components.RatingChip
import com.localbites.core.ui.components.ShimmerBox
import com.localbites.core.ui.theme.*
import com.localbites.feature.customer.R
import com.localbites.feature.customer.viewmodel.CustomerViewModel
import kotlinx.coroutines.delay

private val cuisineCategories = listOf(
    "North Indian" to Icons.Outlined.Restaurant,
    "South Indian" to Icons.Outlined.Restaurant,
    "Chinese" to Icons.Outlined.Fastfood,
    "Fast Food" to Icons.Outlined.Fastfood,
    "Biryani" to Icons.Outlined.Restaurant,
    "Pizza" to Icons.Outlined.LocalPizza,
    "Desserts" to Icons.Outlined.Fastfood,
    "Beverages" to Icons.Outlined.Fastfood,
    "Street Food" to Icons.Outlined.Fastfood,
    "Healthy" to Icons.Outlined.Restaurant,
)

private data class PromoBanner(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
)

private val promoBanners = listOf(
    PromoBanner("50% OFF", "Up to \u20B9100 on first order", listOf(Color(0xFFFC8019), Color(0xFFFF6B35))),
    PromoBanner("FREE Delivery", "On orders above \u20B9199", listOf(Color(0xFF48C479), Color(0xFF2E7D32))),
    PromoBanner("Flat \u20B975 OFF", "Use code LOCALBITES75", listOf(Color(0xFF5D8ED5), Color(0xFF1565C0))),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: CustomerViewModel,
    onRestaurantClick: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request location permission
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.onLocationPermissionResult(true)
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshRestaurants() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Offline / slow network banner
            item {
                OfflineBanner(
                    isOnline = uiState.isOnline,
                    isSlowNetwork = uiState.networkType in listOf(
                        NetworkType.CELLULAR_2G,
                        NetworkType.CELLULAR_3G,
                    ),
                )
            }

            // Location bar
            item {
                LocationBar(
                    deliveryAddress = uiState.deliveryAddressText,
                    hasPermission = uiState.locationPermissionGranted,
                    onRequestPermission = {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    },
                )
            }

            // Pill search bar
            item {
                PillSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Promo banner carousel
            item {
                PromoBannerCarousel()
            }

            // Circular cuisine filters (Swiggy-style)
            item {
                CircularCuisineFilters(
                    selectedCuisine = uiState.selectedCuisine,
                    onCuisineSelected = { viewModel.onCuisineSelected(it) },
                )
            }

            // Section header
            item {
                Text(
                    text = stringResource(R.string.restaurants_near_you),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            // Loading shimmer
            if (uiState.isLoading && uiState.restaurants.isEmpty()) {
                items(4) {
                    RestaurantCardShimmer(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            // Restaurant list
            if (!uiState.isLoading || uiState.restaurants.isNotEmpty()) {
                items(
                    items = uiState.restaurants,
                    key = { it.id },
                ) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        distanceKm = uiState.restaurantDistances[restaurant.id],
                        onClick = { onRestaurantClick(restaurant.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }

            // Empty state
            if (!uiState.isLoading && uiState.restaurants.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.no_restaurants_found),
                        subtitle = if (uiState.searchQuery.isNotBlank())
                            stringResource(R.string.try_different_search)
                        else
                            stringResource(R.string.no_restaurants_in_area),
                    )
                }
            }

            // Error state
            if (uiState.error != null && uiState.restaurants.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.something_went_wrong),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadRestaurants() }) {
                            Text(stringResource(com.localbites.core.ui.R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationBar(
    deliveryAddress: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!hasPermission) onRequestPermission()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = stringResource(R.string.location_label),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deliver_to),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hasPermission) deliveryAddress
                    else stringResource(R.string.tap_to_enable_location),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Pill-shaped search bar (Swiggy style)
@Composable
private fun PillSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(52.dp),
        placeholder = {
            Text(
                text = stringResource(R.string.search_restaurants_cuisines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_label),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50), // Full pill shape
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

// Auto-scrolling promo banner carousel
@Composable
private fun PromoBannerCarousel() {
    val pagerState = rememberPagerState(pageCount = { promoBanners.size })

    // Auto-scroll every 4 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % promoBanners.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val banner = promoBanners[page]
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(banner.gradientColors)
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = banner.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }
        }

        // Dot indicators
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(promoBanners.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

// Circular cuisine filters (Swiggy-style)
@Composable
private fun CircularCuisineFilters(
    selectedCuisine: String?,
    onCuisineSelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(cuisineCategories) { _, (cuisine, icon) ->
            val isSelected = selectedCuisine == cuisine
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable { onCuisineSelected(cuisine) }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = cuisine,
                        modifier = Modifier.size(28.dp),
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cuisine,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

// Photo-forward restaurant card with stronger gradient scrim
@Composable
private fun RestaurantCard(
    restaurant: Restaurant,
    distanceKm: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pureVegText = stringResource(com.localbites.core.ui.R.string.pure_veg)
    val currentlyClosedText = stringResource(com.localbites.core.ui.R.string.currently_closed)
    val freeText = stringResource(com.localbites.core.ui.R.string.free)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            // Restaurant image - taller for photo-forward look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                AsyncImage(
                    model = restaurant.imageUrl ?: restaurant.image,
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                // Stronger 3-stop gradient scrim for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GradientScrimStart,
                                    GradientScrimMid,
                                    GradientScrimEnd,
                                ),
                            )
                        ),
                )

                // Offer badge overlay (top-left)
                if (restaurant.deliveryFee == 0.0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = OfferBlue,
                    ) {
                        Text(
                            text = "FREE DELIVERY",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Rating chip on the image
                RatingChip(
                    rating = restaurant.rating.toFloat(),
                    totalRatings = restaurant.totalRatings,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                )

                // Restaurant name overlaid on scrim for Swiggy feel
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 36.dp, end = 60.dp),
                )

                // Veg only badge
                if (restaurant.isVegOnly) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = VegGreen,
                    ) {
                        Text(
                            text = pureVegText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Closed overlay
                if (!restaurant.isAcceptingOrders) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = currentlyClosedText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Compact delivery info row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = restaurant.cuisineType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Delivery time
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.delivery_time_label),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(com.localbites.core.ui.R.string.delivery_time_min, restaurant.avgDeliveryTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Delivery fee
                Icon(
                    imageVector = Icons.Outlined.DeliveryDining,
                    contentDescription = stringResource(R.string.delivery_fee_label),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (restaurant.deliveryFee == 0.0) freeText else "\u20B9${restaurant.deliveryFee.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (restaurant.deliveryFee == 0.0) RatingGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (restaurant.deliveryFee == 0.0) FontWeight.Bold else FontWeight.Normal,
                )

                // Distance
                if (distanceKm != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = stringResource(R.string.distance_label),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = DistanceUtils.formatDistance(distanceKm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantCardShimmer(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp),
                )
            }
        }
    }
}
