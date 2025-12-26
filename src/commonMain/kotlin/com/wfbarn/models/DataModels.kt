package com.wfbarn.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
enum class AssetType(val displayName: String) {
    STOCK("股票"),
    FUND("基金"),
    CASH("现金"),
    MONEY_FUND("货币基金"),
    BITCOIN("比特币"),
    BOND("债券"),
    CONVERTIBLE_BOND("可转债"),
}

@Serializable
data class Asset(
    val id: String,
    val name: String,
    val type: AssetType,
    val initialAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val note: String = ""
)

@Serializable
data class DailyProfitLoss(
    val date: LocalDate,
    val assetId: String,
    val profitLoss: Double, // 当日盈亏
    val balance: Double    // 当日余额
)

@Serializable
enum class TransactionType(val displayName: String) {
    INCOME("收入"),      // 收入 (如工资)
    EXPENSE("普通支出"),  // 普通支出 (如转账、调账)
    CONSUMPTION("消费")  // 消费 (固定的亏损)
}

@Serializable
data class Transaction(
    val id: String,
    val date: LocalDate,
    val type: TransactionType,
    val amount: Double,
    val category: String, // e.g., "Salary", "Food", "Rent"
    val note: String = ""
)

@Serializable
data class MacroRecord(
    val date: LocalDate,
    val value: Double,
    val note: String = ""
)

@Serializable
data class AppState(
    val assets: List<Asset> = emptyList(),
    val dailyRecords: List<DailyProfitLoss> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val macroRecords: List<MacroRecord> = emptyList(),
    val monthlyBudgets: Map<String, Double> = emptyMap(), // key: "YYYY-MM"
    val isDarkMode: Boolean = false
)
