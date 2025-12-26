package com.wfbarn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.wfbarn.service.StorageService
import com.wfbarn.ui.MainViewModel
import com.wfbarn.App
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // On Android, we provide the files directory for storage
        val storageService = StorageService(filesDir)
        
        setContent {
            val viewModel = remember { MainViewModel(storageService) }
            App(viewModel, isDesktop = false)
        }
    }
}
