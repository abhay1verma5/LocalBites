package com.localbites.feature.customer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localbites.feature.customer.viewmodel.CustomerViewModel

// Navigation routes for customer screens
object CustomerRoutes {
    const val HOME = "customer_home"
    const val RESTAURANT_DETAIL = "restaurant_detail/{restaurantId}"
    const val CART = "cart"
    const val ORDER_HISTORY = "order_history"
    const val ORDER_TRACKING = "order_tracking/{orderId}"
    const val PROFILE = "profile"

    fun restaurantDetail(id: Int) = "restaurant_detail/$id"
    fun orderTracking(id: Int) = "order_tracking/$id"
}

// Bottom navigation items
enum class CustomerBottomTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(
        route = CustomerRoutes.HOME,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    ORDERS(
        route = CustomerRoutes.ORDER_HISTORY,
        title = "Orders",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt,
    ),
    PROFILE(
        route = CustomerRoutes.PROFILE,
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}

@Composable
fun CustomerMainScreen(
    viewModel: CustomerViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit = {},
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be visible
    val bottomBarVisible = CustomerBottomTab.entries.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        bottomBar = {
            if (bottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation,
                ) {
                    CustomerBottomTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == tab.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CustomerRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(CustomerRoutes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onRestaurantClick = { restaurantId ->
                        navController.navigate(CustomerRoutes.restaurantDetail(restaurantId))
                    },
                )
            }

            composable(CustomerRoutes.RESTAURANT_DETAIL) { backStackEntry ->
                val restaurantId = backStackEntry.arguments?.getString("restaurantId")?.toIntOrNull()
                if (restaurantId != null) {
                    RestaurantDetailScreen(
                        restaurantId = restaurantId,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                        onViewCartClick = {
                            navController.navigate(CustomerRoutes.CART)
                        },
                    )
                }
            }

            composable(CustomerRoutes.CART) {
                CartScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onCheckoutSuccess = { orderId ->
                        navController.navigate(CustomerRoutes.orderTracking(orderId)) {
                            popUpTo(CustomerRoutes.HOME) { inclusive = false }
                        }
                    },
                )
            }

            composable(CustomerRoutes.ORDER_HISTORY) {
                OrderHistoryScreen(
                    viewModel = viewModel,
                    onOrderClick = { orderId ->
                        navController.navigate(CustomerRoutes.orderTracking(orderId))
                    },
                )
            }

            composable(CustomerRoutes.ORDER_TRACKING) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId")?.toIntOrNull()
                if (orderId != null) {
                    OrderTrackingScreen(
                        orderId = orderId,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }

            composable(CustomerRoutes.PROFILE) {
                // Placeholder profile screen; delegates to app-level profile
                CustomerProfilePlaceholder(onNavigateToProfile = onNavigateToProfile)
            }
        }
    }
}

@Composable
private fun CustomerProfilePlaceholder(onNavigateToProfile: () -> Unit) {
    // Minimal placeholder -- the real profile screen lives in a shared module
    androidx.compose.foundation.layout.Box(
        modifier = Modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.padding(androidx.compose.ui.unit.dp.times(32)),
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.padding(top = androidx.compose.ui.unit.dp.times(16))
            )
            Button(onClick = onNavigateToProfile) {
                Text("Go to Full Profile")
            }
        }
    }
}
