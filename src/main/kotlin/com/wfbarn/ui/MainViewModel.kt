package com.wfbarn.ui

import com.wfbarn.models.*
import com.wfbarn.service.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

class MainViewModel(private val storageService: StorageService) {
    private val _state = MutableStateFlow(storageService.loadState())
    val state: StateFlow<AppState> = _state

    fun addAsset(name: String, type: AssetType, initialAmount: Double) {
        val newAsset = Asset(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            initialAmount = initialAmount,
            currentAmount = initialAmount
        )
        val newState = _state.value.copy(assets = _state.value.assets + newAsset)
        updateState(newState)
    }

    fun updateDailyProfitLoss(assetId: String, profitLoss: Double) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val asset = _state.value.assets.find { it.id == assetId } ?: return
        
        val newBalance = asset.currentAmount + profitLoss
        val newRecord = DailyProfitLoss(today, assetId, profitLoss, newBalance)
        
        val newAssets = _state.value.assets.map {
            if (it.id == assetId) it.copy(currentAmount = newBalance) else it
        }
        
        val newRecords = _state.value.dailyRecords.filterNot { it.date == today && it.assetId == assetId } + newRecord
        
        updateState(_state.value.copy(assets = newAssets, dailyRecords = newRecords))
    }

    fun addTransaction(type: TransactionType, amount: Double, category: String, note: String) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            date = today,
            type = type,
            amount = amount,
            category = category,
            note = note
        )
        
        // If it's cash or something similar, we might want to update an asset too
        // For now, let's just record the transaction
        updateState(_state.value.copy(transactions = _state.value.transactions + transaction))
    }

    fun addMacroRecord(value: Double, note: String) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val record = MacroRecord(today, value, note)
        val newRecords = _state.value.macroRecords.filterNot { it.date == today } + record
        updateState(_state.value.copy(macroRecords = newRecords))
    }

    private fun updateState(newState: AppState) {
        _state.value = newState
        storageService.saveState(newState)
    }
}
