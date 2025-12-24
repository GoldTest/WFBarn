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

    fun updateAsset(id: String, name: String, type: AssetType, currentAmount: Double) {
        val newAssets = _state.value.assets.map {
            if (it.id == id) {
                it.copy(name = name, type = type, currentAmount = currentAmount)
            } else it
        }
        updateState(_state.value.copy(assets = newAssets))
    }

    fun deleteAsset(id: String) {
        val newAssets = _state.value.assets.filterNot { it.id == id }
        // Optionally also delete records associated with this asset
        val newRecords = _state.value.dailyRecords.filterNot { it.assetId == id }
        updateState(_state.value.copy(assets = newAssets, dailyRecords = newRecords))
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

    fun setMonthlyBudget(yearMonth: String, amount: Double) {
        val newBudgets = _state.value.monthlyBudgets.toMutableMap()
        newBudgets[yearMonth] = amount
        updateState(_state.value.copy(monthlyBudgets = newBudgets))
    }

    fun toggleDarkMode() {
        updateState(_state.value.copy(isDarkMode = !_state.value.isDarkMode))
    }

    fun getCurrentMonthBudget(): Double {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val key = String.format("%04d-%02d", now.year, now.monthNumber)
        return _state.value.monthlyBudgets[key] ?: 0.0
    }

    fun getCurrentMonthConsumption(): Double {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return _state.value.transactions
            .filter { it.date.year == now.year && it.date.monthNumber == now.monthNumber && it.type == TransactionType.CONSUMPTION }
            .sumOf { it.amount }
    }

    private fun updateState(newState: AppState) {
        _state.value = newState
        storageService.saveState(newState)
    }
}
