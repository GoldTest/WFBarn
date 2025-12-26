package com.wfbarn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wfbarn.models.Asset
import com.wfbarn.models.AssetType
import com.wfbarn.ui.MainViewModel

@Composable
fun AssetsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }
    var assetToDelete by remember { mutableStateOf<Asset?>(null) }

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
                items(state.assets, key = { it.id }) { asset ->
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 2.dp) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(asset.name, style = MaterialTheme.typography.subtitle1)
                                    Text(asset.type.displayName, style = MaterialTheme.typography.caption)
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "¥ ${String.format("%.2f", asset.currentAmount)}",
                                        style = MaterialTheme.typography.h6,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    IconButton(onClick = { editingAsset = asset }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colors.primary)
                                    }
                                    
                                    IconButton(onClick = { assetToDelete = asset }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colors.error)
                                    }
                                }
                            }
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

    editingAsset?.let { asset ->
        EditAssetDialog(
            asset = asset,
            onDismiss = { editingAsset = null },
            onConfirm = { name, type, amount ->
                viewModel.updateAsset(asset.id, name, type, amount)
                editingAsset = null
            }
        )
    }

    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除资产 \"${asset.name}\" 吗？这将同时删除相关的历史记录。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(asset.id)
                        assetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                ) {
                    Text("删除", color = MaterialTheme.colors.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun EditAssetDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onConfirm: (String, AssetType, Double) -> Unit
) {
    var name by remember { mutableStateOf(asset.name) }
    var type by remember { mutableStateOf(asset.type) }
    var amount by remember { mutableStateOf(asset.currentAmount.toString()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资产") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
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
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("当前金额") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type, amount.toDoubleOrNull() ?: asset.currentAmount) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
