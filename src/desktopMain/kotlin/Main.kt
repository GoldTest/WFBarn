import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.App

fun main() = application {
    val storageService = remember { StorageService() }
    val viewModel = remember { MainViewModel(storageService) }
    var isOpen by remember { mutableStateOf(true) }
    val trayState = rememberTrayState()
    
    // 加载图标并添加白色底色背景，处理半透明不好看的问题
    val baseIcon = painterResource("icon.png")
    val icon = remember(baseIcon) {
        object : Painter() {
            override val intrinsicSize = baseIcon.intrinsicSize
            override fun DrawScope.onDraw() {
                // 绘制白色圆角矩形背景
                drawRoundRect(
                    color = Color.Red,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(intrinsicSize.width * 0.2f)
                )
                // 绘制原图标
                with(baseIcon) {
                    draw(intrinsicSize)
                }
            }
        }
    }


    Tray(
        state = trayState,
        icon = icon,
        tooltip = "WFBarn Money Management System",
            menu = {
                Item("Show Window", onClick = { isOpen = true })
                Separator()
                Item("Exit Application", onClick = { exitApplication() })
            },
        onAction = { isOpen = true }
    )

    if (isOpen) {
        Window(
            onCloseRequest = { isOpen = false },
            title = "WFBarn - 财务管理系统",
            icon = icon,
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
