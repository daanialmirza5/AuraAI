package com.aura.ai.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.ai.presentation.login.LoginRoute
import com.aura.ai.presentation.onboarding.OnboardingRoute
import com.aura.ai.presentation.permissions.PermissionsRoute
import com.aura.ai.presentation.splash.SplashRoute

/** The top-level flow: splash decides where to resume, then the one-time auth setup,
 *  then the tabbed app shell for the rest of the app's lifetime. */
@Composable
fun AuraNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AuraRoute.SPLASH) {
        composable(AuraRoute.SPLASH) {
            SplashRoute(
                onFinished = { destination ->
                    navController.navigate(destination) {
                        popUpTo(AuraRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(AuraRoute.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(AuraRoute.LOGIN) {
                        popUpTo(AuraRoute.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(AuraRoute.LOGIN) {
            LoginRoute(
                onAuthenticated = {
                    navController.navigate(AuraRoute.PERMISSIONS) {
                        popUpTo(AuraRoute.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(AuraRoute.PERMISSIONS) {
            PermissionsRoute(
                onFinished = {
                    navController.navigate(AuraRoute.APP_SHELL) {
                        popUpTo(AuraRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(AuraRoute.APP_SHELL) {
            AppShellScreen(
                onSignOut = {
                    navController.navigate(AuraRoute.LOGIN) {
                        popUpTo(AuraRoute.SPLASH) { inclusive = true }
                    }
                },
            )
        }
    }
}
