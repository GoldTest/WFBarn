import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import androidx.compose.ui.unit.dp
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.App

fun main() = application {
    val storageService = remember { StorageService() }
    val viewModel = remember { MainViewModel(storageService) }
    var isOpen by remember { mutableStateOf(true) }
    val trayState = rememberTrayState()
    // val icon = painterResource("icon.png")

/*
    Tray(
        state = trayState,
        // icon = painterResource("icon.png"),
        menu = {
            Item("Show Window", onClick = { isOpen = true })
            Separator()
            Item("Exit", onClick = { exitApplication() })
        },
        onAction = { isOpen = true }
    )
*/

    if (isOpen) {
        Window(
            onCloseRequest = { isOpen = false },
            title = "WFBarn - 财务管理系统",
            state = rememberWindowState(
                position = WindowPosition(androidx.compose.ui.Alignment.TopCenter),
                width = 1000.dp,
                height = 800.dp
            )
        ) {
            App(viewModel, isDesktop = true)
        }
    }
}
