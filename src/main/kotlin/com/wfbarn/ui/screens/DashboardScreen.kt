package com.wfbarn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfbarn.ui.MainViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val totalBalance = state.assets.sumOf { it.currentAmount }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("资产总览", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("总资产", fontSize = 16.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                Text("¥ ${String.format("%.2f", totalBalance)}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
}
