package com.localbites.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.localbites.feature.auth.ui.PhoneInputScreen
import com.localbites.feature.auth.ui.OtpVerificationScreen
import com.localbites.feature.auth.ui.RoleSelectionScreen
import com.localbites.feature.customer.ui.CustomerMainScreen
import com.localbites.feature.restaurant.ui.RestaurantMainScreen
import com.localbites.feature.delivery.ui.DeliveryMainScreen

object Routes {
    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFICATION = "otp_verification/{phone}"
    const val ROLE_SELECTION = "role_selection"
    const val CUSTOMER_MAIN = "customer_main"
    const val RESTAURANT_MAIN = "restaurant_main"
    const val DELIVERY_MAIN = "delivery_main"
}

@Composable
fun RootNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PHONE_INPUT
    ) {
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
                        // Role already saved in TokenManager by ViewModel
                        navController.navigate(Routes.CUSTOMER_MAIN) {
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
                    when (role) {
                        "CUSTOMER" -> navController.navigate(Routes.CUSTOMER_MAIN) {
                            popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                        }
                        "RESTAURANT_OWNER" -> navController.navigate(Routes.RESTAURANT_MAIN) {
                            popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                        }
                        "DELIVERY_PARTNER" -> navController.navigate(Routes.DELIVERY_MAIN) {
                            popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                        }
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
            RestaurantMainScreen()
        }

        composable(Routes.DELIVERY_MAIN) {
            DeliveryMainScreen()
        }
    }
}
