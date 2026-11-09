package com.localbites.feature.delivery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localbites.feature.delivery.viewmodel.DeliveryViewModel

sealed class DeliveryTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Orders : DeliveryTab(
        route = "available_orders",
        label = "Orders",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange,
    )

    data object History : DeliveryTab(
        route = "delivery_history",
        label = "History",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange,
    )

    data object Earnings : DeliveryTab(
        route = "earnings",
        label = "Earnings",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star,
    )

    data object Profile : DeliveryTab(
        route = "profile",
        label = "Profile",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
    )
}

private val tabs = listOf(
    DeliveryTab.Orders,
    DeliveryTab.History,
    DeliveryTab.Earnings,
    DeliveryTab.Profile,
)

@Composable
fun DeliveryMainScreen(
    viewModel: DeliveryViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
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
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DeliveryTab.Orders.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(DeliveryTab.Orders.route) {
                AvailableOrdersScreen(viewModel = viewModel)
            }
            composable(DeliveryTab.History.route) {
                DeliveryHistoryScreen(viewModel = viewModel)
            }
            composable(DeliveryTab.Earnings.route) {
                EarningsScreen(viewModel = viewModel)
            }
            composable(DeliveryTab.Profile.route) {
                DeliveryProfilePlaceholder(onNavigateToProfile = onNavigateToProfile)
            }
        }
    }
}

@Composable
private fun DeliveryProfilePlaceholder(onNavigateToProfile: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onNavigateToProfile) {
            Text("View Profile Settings")
        }
    }
}
