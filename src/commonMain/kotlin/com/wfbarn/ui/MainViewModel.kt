package com.wfbarn.ui

import com.wfbarn.models.*
import com.wfbarn.service.StorageService
import com.wfbarn.service.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

enum class SyncStage(val displayName: String) {
    IDLE("空闲"),
    PULLING("正在拉取远程数据..."),
    MERGING("正在合并本地与远程数据..."),
    PUSHING("正在推送更新到远程..."),
    COMPLETED("同步完成"),
    FAILED("同步失败")
}

data class SyncStatus(
    val stage: SyncStage = SyncStage.IDLE,
    val message: String = "",
    val lastSyncTime: Long = 0,
    val isError: Boolean = false
)

class MainViewModel(
    private val storageService: StorageService,
    private val syncService: SyncService = SyncService()
) {
    private val _state = MutableStateFlow(storageService.loadState())
    val state: StateFlow<AppState> = _state

    private val _syncStatus = MutableStateFlow(SyncStatus(lastSyncTime = _state.value.syncConfig.lastSyncTime))
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private var syncJob: Job? = null
    private var isSyncing = false

    init {
        startPeriodicSync()
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (isActive) {
                if (_state.value.syncConfig.autoSync && !isSyncing) {
                    syncData()
                }
                // 每 2 分钟检查一次 (120,000 毫秒)
                delay(120_000)
            }
        }
    }

    fun updateSyncConfig(config: SyncConfig) {
        val oldAutoSync = _state.value.syncConfig.autoSync
        updateState(_state.value.copy(syncConfig = config))
        
        // 如果 autoSync 状态发生变化，重新启动定时任务
        if (oldAutoSync != config.autoSync) {
            startPeriodicSync()
        }
    }

    fun syncData() {
        if (isSyncing) return
        viewModelScope.launch {
            try {
                isSyncing = true
                val config = _state.value.syncConfig
                if (config.url.isBlank()) {
                    _syncStatus.value = SyncStatus(SyncStage.FAILED, "未配置 WebDAV 地址", isError = true)
                    return@launch
                }

                // 1. Pulling
                _syncStatus.value = SyncStatus(SyncStage.PULLING, "连接服务器中...")
                val remoteState = try {
                    syncService.download(config)
                } catch (e: Exception) {
                    _syncStatus.value = SyncStatus(SyncStage.FAILED, "拉取失败: ${e.message}", isError = true)
                    return@launch
                }
                
                if (remoteState == null) {
                    // Remote doesn't exist, try initial upload
                    _syncStatus.value = SyncStatus(SyncStage.PUSHING, "远程无数据，正在进行首次上传...")
                    try {
                        val success = syncService.upload(config, _state.value)
                        if (success) {
                            val now = Clock.System.now().toEpochMilliseconds()
                            val newState = _state.value.copy(syncConfig = config.copy(lastSyncTime = now))
                            updateState(newState)
                            _syncStatus.value = SyncStatus(SyncStage.COMPLETED, "首次上传成功", lastSyncTime = now)
                        } else {
                            _syncStatus.value = SyncStatus(SyncStage.FAILED, "首次上传失败: 服务器拒绝请求", isError = true)
                        }
                    } catch (e: Exception) {
                        _syncStatus.value = SyncStatus(SyncStage.FAILED, "首次上传失败: ${e.message}", isError = true)
                    }
                    return@launch
                }

                // 2. Merging
                _syncStatus.value = SyncStatus(SyncStage.MERGING, "正在对比本地与远程差异...")
                val mergedState = mergeStates(_state.value, remoteState)
                
                // 3. Pushing
                _syncStatus.value = SyncStatus(SyncStage.PUSHING, "正在保存合并后的数据到远程...")
                val now = Clock.System.now().toEpochMilliseconds()
                val finalConfig = config.copy(lastSyncTime = now)
                val finalState = mergedState.copy(syncConfig = finalConfig)
                
                val pushSuccess = syncService.upload(finalConfig, finalState)
                if (pushSuccess) {
                    updateState(finalState)
                    _syncStatus.value = SyncStatus(SyncStage.COMPLETED, "同步成功", lastSyncTime = now)
                } else {
                    _syncStatus.value = SyncStatus(SyncStage.FAILED, "推送更新失败", isError = true)
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus(SyncStage.FAILED, "同步发生异常: ${e.message}", isError = true)
            } finally {
                isSyncing = false
                // 3秒后回到空闲状态，但保留上次同步时间
                delay(3000)
                if (_syncStatus.value.stage == SyncStage.COMPLETED || _syncStatus.value.stage == SyncStage.FAILED) {
                    _syncStatus.value = _syncStatus.value.copy(stage = SyncStage.IDLE, message = "")
                }
            }
        }
    }

    private fun mergeStates(local: AppState, remote: AppState): AppState {
        // Merge Assets: Union by ID
        val allAssetIds = (local.assets.map { it.id } + remote.assets.map { it.id }).distinct()
        val mergedAssets = allAssetIds.map { id ->
            local.assets.find { it.id == id } ?: remote.assets.find { it.id == id }!!
        }

        // Merge DailyRecords: Union by Date + AssetId
        val mergedRecords = (local.dailyRecords + remote.dailyRecords)
            .distinctBy { "${it.date}_${it.assetId}" }
            .sortedBy { it.date }

        // Merge Transactions: Union by ID
        val mergedTransactions = (local.transactions + remote.transactions)
            .distinctBy { it.id }
            .sortedBy { it.date }

        // Merge MacroRecords: Union by Date
        val mergedMacro = (local.macroRecords + remote.macroRecords)
            .distinctBy { it.date }
            .sortedBy { it.date }

        // Merge Budgets: Union by Key
        val mergedBudgets = local.monthlyBudgets + remote.monthlyBudgets

        return local.copy(
            assets = mergedAssets,
            dailyRecords = mergedRecords,
            transactions = mergedTransactions,
            macroRecords = mergedMacro,
            monthlyBudgets = mergedBudgets
        )
    }

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

    fun deleteTransaction(id: String) {
        val newTransactions = _state.value.transactions.filterNot { it.id == id }
        updateState(_state.value.copy(transactions = newTransactions))
    }

    fun deleteDailyRecord(date: kotlinx.datetime.LocalDate, assetId: String) {
        val newRecords = _state.value.dailyRecords.filterNot { it.date == date && it.assetId == assetId }
        updateState(_state.value.copy(dailyRecords = newRecords))
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
