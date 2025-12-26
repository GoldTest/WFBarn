package com.wfbarn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfbarn.ui.MainViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val totalBalance = state.assets.sumOf { it.currentAmount }
    val budget = viewModel.getCurrentMonthBudget()
    val consumption = viewModel.getCurrentMonthConsumption()
    val progress = if (budget > 0) (consumption / budget).toFloat().coerceIn(0f, 1f) else 0f
    
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(if (budget > 0) budget.toString() else "") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("资产总览", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(elevation = 4.dp, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("总资产", fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    Text("¥ ${String.format("%.2f", totalBalance)}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(elevation = 4.dp, modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("本月预算", fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        IconButton(onClick = { showBudgetDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, "Edit Budget", tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    if (budget > 0) {
                        Text("¥ ${String.format("%.2f", consumption)} / ¥ ${String.format("%.2f", budget)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (progress > 0.9f) Color.Red else MaterialTheme.colors.primary
                        )
                    } else {
                        Text("未设置预算", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                        TextButton(onClick = { showBudgetDialog = true }) {
                            Text("立即设置")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("资产分布", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(state.assets) { asset ->
                ListItem(
                    text = { Text(asset.name) },
                    secondaryText = { Text(asset.type.displayName) },
                    trailing = { Text("¥ ${String.format("%.2f", asset.currentAmount)}") }
                )
                Divider()
            }
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("设置本月预算") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("预算金额") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amount = budgetInput.toDoubleOrNull() ?: 0.0
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val key = String.format("%04d-%02d", now.year, now.monthNumber)
                    viewModel.setMonthlyBudget(key, amount)
                    showBudgetDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
