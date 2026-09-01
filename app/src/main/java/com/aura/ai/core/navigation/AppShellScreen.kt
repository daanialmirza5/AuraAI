package com.aura.ai.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aura.ai.core.designsystem.components.AuraBottomNavBar
import com.aura.ai.core.designsystem.components.BottomNavEntry
import com.aura.ai.core.designsystem.components.NavIconType
import com.aura.ai.core.designsystem.theme.AuraColors
import com.aura.ai.presentation.aura.AuraTabRoute
import com.aura.ai.presentation.automate.AutomateRoute
import com.aura.ai.presentation.home.HomeRoute
import com.aura.ai.presentation.profile.ProfileRoute
import com.aura.ai.presentation.workspace.WorkspaceRoute

private val Tabs =
    listOf(
        BottomNavEntry(NavIconType.Home, "Home", AppTabRoute.HOME),
        BottomNavEntry(NavIconType.Aura, "Aura", AppTabRoute.AURA),
        BottomNavEntry(NavIconType.Workspace, "Space", AppTabRoute.WORKSPACE),
        BottomNavEntry(NavIconType.Automate, "Automate", AppTabRoute.AUTOMATE),
        BottomNavEntry(NavIconType.Profile, "You", AppTabRoute.PROFILE),
    )

/**
 * The tabbed shell hosting Home / Aura / Workspace / Automate / Profile. The nav bar
 * floats over the content (matching the source's absolutely-positioned pill with a
 * fade-to-transparent backdrop) rather than reserving its own Scaffold slot — each
 * tab's own content carries ~140dp of bottom padding to clear it.
 *
 * Aura / Automate / Profile accept an optional `sub` query arg so Home's dashboard
 * tiles ("Device Health", "Smart Home", ...) can deep-link straight into a specific
 * sub-screen of another tab, exactly like the source's `go(tab, sub)` handler.
 */
@Composable
fun AppShellScreen(onSignOut: () -> Unit) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?') ?: AppTabRoute.HOME

    val navigateToTab: (String, String?) -> Unit = { tabRoute, sub ->
        val target = if (sub != null) "$tabRoute?sub=$sub" else tabRoute
        tabNavController.navigate(target) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AuraColors.Background)) {
        NavHost(
            navController = tabNavController,
            startDestination = AppTabRoute.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppTabRoute.HOME) {
                HomeRoute(onNavigateToTab = navigateToTab)
            }
            composable(
                route = "${AppTabRoute.AURA}?sub={sub}",
                arguments =
                    listOf(
                        navArgument("sub") {
                            type = NavType.StringType
                            defaultValue = "orb"
                        },
                    ),
            ) {
                AuraTabRoute()
            }
            composable(AppTabRoute.WORKSPACE) {
                WorkspaceRoute()
            }
            composable(
                route = "${AppTabRoute.AUTOMATE}?sub={sub}",
                arguments =
                    listOf(
                        navArgument("sub") {
                            type = NavType.StringType
                            defaultValue = "automation"
                        },
                    ),
            ) {
                AutomateRoute()
            }
            composable(
                route = "${AppTabRoute.PROFILE}?sub={sub}",
                arguments =
                    listOf(
                        navArgument("sub") {
                            type = NavType.StringType
                            defaultValue = "profile"
                        },
                    ),
            ) {
                ProfileRoute(onSignOut = onSignOut)
            }
        }

        AuraBottomNavBar(
            items = Tabs,
            selectedRoute = currentRoute,
            onSelect = { entry -> if (entry.route != currentRoute) navigateToTab(entry.route, null) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
