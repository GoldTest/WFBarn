package com.wfbarn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfbarn.ui.MainViewModel

@Composable
fun DailyReviewScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text("每日复盘", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
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
    }
}
