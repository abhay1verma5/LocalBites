package com.localbites.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.localbites.core.common.datastore.TokenManager
import com.localbites.feature.auth.ui.PhoneInputScreen
import com.localbites.feature.auth.ui.OtpVerificationScreen
import com.localbites.feature.auth.ui.RoleSelectionScreen
import com.localbites.feature.customer.ui.CustomerMainScreen
import com.localbites.feature.restaurant.ui.RestaurantMainScreen
import com.localbites.feature.delivery.ui.DeliveryMainScreen

object Routes {
    const val SPLASH = "splash"
    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFICATION = "otp_verification/{phone}"
    const val ROLE_SELECTION = "role_selection"
    const val CUSTOMER_MAIN = "customer_main"
    const val RESTAURANT_MAIN = "restaurant_main"
    const val DELIVERY_MAIN = "delivery_main"
}

fun roleToRoute(role: String?): String = when (role) {
    "RESTAURANT_OWNER" -> Routes.RESTAURANT_MAIN
    "DELIVERY_PARTNER" -> Routes.DELIVERY_MAIN
    else -> Routes.CUSTOMER_MAIN
}

@Composable
fun RootNavHost(tokenManager: TokenManager) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Splash / auth check
        composable(Routes.SPLASH) {
            val isLoggedIn by tokenManager.isLoggedIn.collectAsState(initial = null)
            val role by tokenManager.userRole.collectAsState(initial = null)
            var checked by remember { mutableStateOf(false) }

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn == null) return@LaunchedEffect // still loading
                checked = true
                if (isLoggedIn == true && role != null) {
                    navController.navigate(roleToRoute(role)) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.PHONE_INPUT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }

            // Show loading while checking auth
            if (!checked) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        composable(Routes.PHONE_INPUT) {
            PhoneInputScreen(
                onOtpSent = { phone ->
                    navController.navigate("otp_verification/$phone")
                }
            )
        }

        composable(Routes.OTP_VERIFICATION) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpVerificationScreen(
                phone = phone,
                onVerified = { hasRole ->
                    if (hasRole) {
                        // Read the role from TokenManager (already saved by ViewModel)
                        val savedRole = navController.currentBackStackEntry
                            ?.arguments?.getString("role")
                        // We can't read flow here synchronously, so pass role via uiState
                        // The ViewModel already saved role to TokenManager.
                        // We'll navigate and let splash-like logic handle it.
                        // Actually, the AuthViewModel uiState has role. Let's use a simpler approach:
                        // Navigate to a route based on the role that was saved.
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(Routes.PHONE_INPUT) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.ROLE_SELECTION) {
                            popUpTo(Routes.PHONE_INPUT) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    val route = roleToRoute(role)
                    navController.navigate(route) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CUSTOMER_MAIN) {
            CustomerMainScreen(
                onLoggedOut = {
                    navController.navigate(Routes.PHONE_INPUT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.RESTAURANT_MAIN) {
            RestaurantMainScreen(
                onLoggedOut = {
                    navController.navigate(Routes.PHONE_INPUT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.DELIVERY_MAIN) {
            DeliveryMainScreen(
                onLoggedOut = {
                    navController.navigate(Routes.PHONE_INPUT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
