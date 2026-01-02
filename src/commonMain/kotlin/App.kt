package com.wfbarn

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.ui.screens.*

enum class Screen {
    DASHBOARD, ASSETS, DAILY_REVIEW, TRANSACTIONS, MACRO_CURVE
}

@Composable
fun AppTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val colors = if (isDark) {
        darkColors(
            primary = Color(0xFFBB86FC),
            primaryVariant = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF121212),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColors(
            primary = Color(0xFF6200EE),
            primaryVariant = Color(0xFF3700B3),
            secondary = Color(0xFF03DAC6)
        )
    }

    MaterialTheme(colors = colors, content = content)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun App(
    viewModel: MainViewModel,
    isDesktop: Boolean = false,
    currentScreen: Screen = Screen.DASHBOARD,
    onScreenChange: (Screen) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var internalScreen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
    
    val actualScreen = if (isDesktop) currentScreen else internalScreen
    val setActualScreen: (Screen) -> Unit = { 
        if (isDesktop) onScreenChange(it) else internalScreen = it 
    }

    AppTheme(isDark = state.isDarkMode) {
        Scaffold(
            bottomBar = {
                if (!isDesktop) {
                    BottomNavigation {
                        BottomNavigationItem(
                            selected = actualScreen == Screen.DASHBOARD,
                            onClick = { setActualScreen(Screen.DASHBOARD) },
                            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                            label = { Text("总览") }
                        )
                        BottomNavigationItem(
                            selected = actualScreen == Screen.ASSETS,
                            onClick = { setActualScreen(Screen.ASSETS) },
                            icon = { Icon(Icons.Default.AccountBalance, "Assets") },
                            label = { Text("资产") }
                        )
                        BottomNavigationItem(
                            selected = actualScreen == Screen.DAILY_REVIEW,
                            onClick = { setActualScreen(Screen.DAILY_REVIEW) },
                            icon = { Icon(Icons.Default.EditCalendar, "Daily") },
                            label = { Text("复盘") }
                        )
                        BottomNavigationItem(
                            selected = actualScreen == Screen.TRANSACTIONS,
                            onClick = { setActualScreen(Screen.TRANSACTIONS) },
                            icon = { Icon(Icons.Default.History, "Transactions") },
                            label = { Text("流水") }
                        )
                        BottomNavigationItem(
                            selected = actualScreen == Screen.MACRO_CURVE,
                            onClick = { setActualScreen(Screen.MACRO_CURVE) },
                            icon = { Icon(Icons.Default.ShowChart, "Macro") },
                            label = { Text("宏观") }
                        )
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isDesktop) {
                    // Sidebar for desktop
                    NavigationRail(
                        modifier = Modifier.width(120.dp),
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 8.dp
                    ) {
                        NavigationRailItem(
                            selected = actualScreen == Screen.DASHBOARD,
                            onClick = { setActualScreen(Screen.DASHBOARD) },
                            icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                            label = { Text("总览") }
                        )
                        NavigationRailItem(
                            selected = actualScreen == Screen.ASSETS,
                            onClick = { setActualScreen(Screen.ASSETS) },
                            icon = { Icon(Icons.Default.AccountBalance, "Assets") },
                            label = { Text("资产") }
                        )
                        NavigationRailItem(
                            selected = actualScreen == Screen.DAILY_REVIEW,
                            onClick = { setActualScreen(Screen.DAILY_REVIEW) },
                            icon = { Icon(Icons.Default.EditCalendar, "Daily") },
                            label = { Text("复盘") }
                        )
                        NavigationRailItem(
                            selected = actualScreen == Screen.TRANSACTIONS,
                            onClick = { setActualScreen(Screen.TRANSACTIONS) },
                            icon = { Icon(Icons.Default.History, "Transactions") },
                            label = { Text("流水") }
                        )
                        NavigationRailItem(
                            selected = actualScreen == Screen.MACRO_CURVE,
                            onClick = { setActualScreen(Screen.MACRO_CURVE) },
                            icon = { Icon(Icons.Default.ShowChart, "Macro") },
                            label = { Text("宏观") }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        NavigationRailItem(
                            selected = false,
                            onClick = { viewModel.toggleDarkMode() },
                            icon = { Icon(if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "Theme") },
                            label = { Text(if (state.isDarkMode) "浅色" else "深色") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    AnimatedContent(
                        targetState = actualScreen,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        }
                    ) { screen ->
                        when (screen) {
                            Screen.DASHBOARD -> DashboardScreen(viewModel)
                            Screen.ASSETS -> AssetsScreen(viewModel)
                            Screen.DAILY_REVIEW -> DailyReviewScreen(viewModel)
                            Screen.TRANSACTIONS -> TransactionsScreen(viewModel)
                            Screen.MACRO_CURVE -> MacroCurveScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
