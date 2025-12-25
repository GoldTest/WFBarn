package com.wfbarn.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wfbarn.models.TransactionType
import com.wfbarn.ui.MainViewModel

@Composable
fun TransactionsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            Text("资金流水 (收入/支出)", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(state.transactions.reversed(), key = { it.id }) { transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = 2.dp) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${transaction.date}: ${transaction.category}", style = MaterialTheme.typography.subtitle1)
                                    if (transaction.note.isNotEmpty()) {
                                        Text(transaction.note, style = MaterialTheme.typography.caption)
                                    }
                                }
                                val color = when (transaction.type) {
                                    TransactionType.INCOME -> MaterialTheme.colors.primary
                                    TransactionType.EXPENSE -> Color.Gray
                                    TransactionType.CONSUMPTION -> MaterialTheme.colors.error
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${if (transaction.type == TransactionType.INCOME) "+" else "-"} ¥ ${String.format("%.2f", transaction.amount)}",
                                        style = MaterialTheme.typography.h6,
                                        color = color
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(transaction.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
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
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, amount, category, note ->
                viewModel.addTransaction(type, amount, category, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddTransactionDialog(onDismiss: () -> Unit, onConfirm: (TransactionType, Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.CONSUMPTION) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录流水") },
        text = {
            Column {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TransactionType.values().forEach { t ->
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t.displayName)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                TextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = category, onValueChange = { category = it }, label = { Text("分类 (如: 工资, 餐饮)") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = note, onValueChange = { note = it }, label = { Text("备注") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(type, amount.toDoubleOrNull() ?: 0.0, category, note) }) {
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
