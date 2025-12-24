package com.wfbarn.service

import com.wfbarn.models.AppState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class StorageService {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val storageDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val documents = File(userHome, "Documents")
        val wfBarnDir = File(documents, "WFBarn")
        if (!wfBarnDir.exists()) {
            wfBarnDir.mkdirs()
        }
        wfBarnDir
    }
    
    private val stateFile: File
        get() = File(storageDir, "state.json")

    fun saveState(state: AppState) {
        val jsonString = json.encodeToString(state)
        stateFile.writeText(jsonString)
    }

    fun loadState(): AppState {
        return if (stateFile.exists()) {
            try {
                val jsonString = stateFile.readText()
                json.decodeFromString<AppState>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                AppState()
            }
        } else {
            AppState()
        }
    }
}
