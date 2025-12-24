import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.ui.screens.*

enum class Screen {
    DASHBOARD, ASSETS, DAILY_REVIEW, TRANSACTIONS, MACRO_CURVE
}

fun main() = application {
    val storageService = remember { StorageService() }
    val viewModel = remember { MainViewModel(storageService) }
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    Window(onCloseRequest = ::exitApplication, title = "WFBarn - 财务管理系统") {
        MaterialTheme {
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar
                NavigationRail(
                    modifier = Modifier.width(120.dp)
                ) {
                    NavigationRailItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                        label = { Text("总览") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == Screen.ASSETS,
                        onClick = { currentScreen = Screen.ASSETS },
                        icon = { Icon(Icons.Default.AccountBalance, "Assets") },
                        label = { Text("资产") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == Screen.DAILY_REVIEW,
                        onClick = { currentScreen = Screen.DAILY_REVIEW },
                        icon = { Icon(Icons.Default.EditCalendar, "Daily") },
                        label = { Text("复盘") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == Screen.TRANSACTIONS,
                        onClick = { currentScreen = Screen.TRANSACTIONS },
                        icon = { Icon(Icons.Default.History, "Transactions") },
                        label = { Text("流水") }
                    )
                    NavigationRailItem(
                        selected = currentScreen == Screen.MACRO_CURVE,
                        onClick = { currentScreen = Screen.MACRO_CURVE },
                        icon = { Icon(Icons.Default.ShowChart, "Macro") },
                        label = { Text("宏观") }
                    )
                }

                // Main Content
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (currentScreen) {
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
