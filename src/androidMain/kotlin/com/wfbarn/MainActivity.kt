package com.wfbarn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.App
import java.io.File

class MainActivity : ComponentActivity() {
    private var actionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        
        // On Android, we provide the files directory for storage
        val storageService = StorageService(filesDir)
        
        setContent {
            val viewModel = remember { MainViewModel(storageService) }
            val currentAction by actionState
            
            val initialScreen = if (currentAction == "add_transaction") Screen.TRANSACTIONS else Screen.DASHBOARD
            val showAddDialog = currentAction == "add_transaction"

            App(
                viewModel = viewModel, 
                isDesktop = false, 
                initialScreen = initialScreen, 
                showAddDialog = showAddDialog
            )
            
            // 重置 actionState，避免配置更改（如旋转屏幕）时重复弹出
            LaunchedEffect(currentAction) {
                if (currentAction != null) {
                    actionState.value = null
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action")
        if (action != null) {
            actionState.value = action
        }
    }
}
