package com.wfbarn.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfbarn.models.AssetType
import com.wfbarn.ui.MainViewModel

@Composable
fun AssetsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Asset")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            Text("资产管理", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(state.assets) { asset ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 2.dp) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(asset.name, style = MaterialTheme.typography.subtitle1)
                                Text(asset.type.displayName, style = MaterialTheme.typography.caption)
                            }
                            Text("¥ ${String.format("%.2f", asset.currentAmount)}", style = MaterialTheme.typography.h6)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, amount ->
                viewModel.addAsset(name, type, amount)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddAssetDialog(onDismiss: () -> Unit, onConfirm: (String, AssetType, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AssetType.STOCK) }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新资产") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                Spacer(modifier = Modifier.height(8.dp))
                
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("类型: ${type.displayName}")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        AssetType.values().forEach { t ->
                            DropdownMenuItem(onClick = {
                                type = t
                                expanded = false
                            }) {
                                Text(t.displayName)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = amount, onValueChange = { amount = it }, label = { Text("初始金额") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type, amount.toDoubleOrNull() ?: 0.0) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
