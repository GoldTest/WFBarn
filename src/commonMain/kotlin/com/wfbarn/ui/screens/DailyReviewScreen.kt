package com.wfbarn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wfbarn.ui.MainViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DailyReviewScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("每日复盘", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        
        // 当日更新区域
        Text("当日更新", style = MaterialTheme.typography.subtitle1)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(0.5f)) {
            items(state.assets) { asset ->
                var profitLossInput by remember { mutableStateOf("") }
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 2.dp) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(asset.name, style = MaterialTheme.typography.subtitle1)
                            Text("当前余额: ¥ ${String.format("%.2f", asset.currentAmount)}", style = MaterialTheme.typography.caption)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = profitLossInput,
                                onValueChange = { profitLossInput = it },
                                label = { Text("当日盈亏") },
                                modifier = Modifier.width(120.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                val value = profitLossInput.toDoubleOrNull() ?: 0.0
                                viewModel.updateDailyProfitLoss(asset.id, value)
                                profitLossInput = ""
                            }) {
                                Text("更新")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // 历史记录区域
        Text("历史复盘记录", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.weight(0.5f)) {
            val groupedRecords = state.dailyRecords.groupBy { it.date }.toList().sortedByDescending { it.first.toString() }
            
            items(groupedRecords) { (date, records) ->
                val dayTotalProfit = records.sumOf { it.profitLoss }
                val dayTotalBalance = records.sumOf { it.balance }
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 1.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(date.toString(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(
                                "当日总盈亏: ¥ ${String.format("%.2f", dayTotalProfit)}",
                                color = if (dayTotalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                        Text("结算总资产: ¥ ${String.format("%.2f", dayTotalBalance)}", style = MaterialTheme.typography.caption)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        records.forEach { record ->
                            val assetName = state.assets.find { it.id == record.assetId }?.name ?: "未知资产"
                            ListItem(
                                text = { Text(assetName) },
                                secondaryText = { Text("余额: ¥ ${String.format("%.2f", record.balance)}") },
                                trailing = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${if (record.profitLoss >= 0) "+" else ""}¥ ${String.format("%.2f", record.profitLoss)}",
                                            color = if (record.profitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                        IconButton(onClick = { viewModel.deleteDailyRecord(record.date, record.assetId) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
