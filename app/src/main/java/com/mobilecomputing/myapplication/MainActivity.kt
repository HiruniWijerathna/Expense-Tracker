package com.mobilecomputing.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.mobilecomputing.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val expenseViewModel: ExpenseViewModel = viewModel()
            val isDarkMode by expenseViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                ExpenseTrackerApp(viewModel = expenseViewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Goals : Screen("goals", "Goals", Icons.Default.Flag)
    object Achievements : Screen("achievements", "Badges", Icons.Default.EmojiEvents)
}

val bottomScreens = listOf(
    Screen.Home,
    Screen.Analytics,
    Screen.Goals,
    Screen.Achievements
)

@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomScreens.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally { it / 2 } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it / 2 } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it / 2 } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 2 } + fadeOut() }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onAddExpense = { navController.navigate("add_expense") },
                    onNavigate = { navController.navigate(it) }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.Goals.route) {
                GoalsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.Achievements.route) {
                AchievementsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("add_expense") {
                AddExpenseScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}