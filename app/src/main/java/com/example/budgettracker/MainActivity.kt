package com.example.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.budgettracker.ui.screens.*
import com.example.budgettracker.ui.theme.BudgetTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetTrackerTheme {
                BudgetTrackerApp()
            }
        }
    }
}

enum class BudgetScreen {
    Main,
    Add,
    Edit,
    Reports,
    CategoryBreakdown
}

enum class TabScreen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    Home("home_tab", Icons.Default.Home, "Home"),
    Analytics("analytics_tab", Icons.Default.Analytics, "Analytics"),
    Profile("profile_tab", Icons.Default.Person, "Profile")
}

@Composable
fun BudgetTrackerApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = BudgetScreen.Main.name) {
        composable(BudgetScreen.Main.name) {
            MainContainer(
                onNavigateToEntry = { navController.navigate(BudgetScreen.Add.name) },
                onNavigateToEdit = { id -> navController.navigate("${BudgetScreen.Edit.name}/$id") },
                onNavigateToReports = { navController.navigate(BudgetScreen.Reports.name) },
                onNavigateToCategoryBreakdown = { category ->
                    navController.navigate("${BudgetScreen.CategoryBreakdown.name}/$category")
                }
            )
        }
        composable(BudgetScreen.Add.name) {
            AddTransactionScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${BudgetScreen.Edit.name}/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
        ) { backStackEntry ->
            EditTransactionScreen(
                transactionId = backStackEntry.arguments?.getInt("transactionId") ?: 0,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(BudgetScreen.Reports.name) {
            DownloadReportScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${BudgetScreen.CategoryBreakdown.name}/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            CategoryBreakdownScreen(
                category = backStackEntry.arguments?.getString("category") ?: "",
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainContainer(
    onNavigateToEntry: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCategoryBreakdown: (String) -> Unit
) {
    val tabNavController = rememberNavController()
    val items = listOf(TabScreen.Home, TabScreen.Analytics, TabScreen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            tabNavController.navigate(screen.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = TabScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TabScreen.Home.route) {
                HomeScreen(
                    navigateToEntry = onNavigateToEntry,
                    navigateToEdit = onNavigateToEdit,
                    navigateToReports = onNavigateToReports
                )
            }
            composable(TabScreen.Analytics.route) {
                AnalyticsScreen(onCategoryClick = onNavigateToCategoryBreakdown)
            }
            composable(TabScreen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
