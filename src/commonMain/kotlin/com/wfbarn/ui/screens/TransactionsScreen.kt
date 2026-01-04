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
import com.wfbarn.models.Transaction
import com.wfbarn.models.TransactionType
import com.wfbarn.ui.MainViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionsScreen(viewModel: MainViewModel, showAddDialogOnInit: Boolean = false) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(showAddDialogOnInit) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingTransaction = null
                showDialog = true 
            }) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            Text("资金流水 (收入/支出)", style = MaterialTheme.typography.h5, modifier = Modifier.padding(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(state.transactions.reversed(), key = { it.id }) { transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), 
                            elevation = 2.dp,
                            onClick = {
                                editingTransaction = transaction
                                showDialog = true
                            }
                        ) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
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

    if (showDialog) {
        TransactionDialog(
            transaction = editingTransaction,
            onDismiss = { showDialog = false },
            onConfirm = { type, amount, category, note, date ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(editingTransaction!!.id, type, amount, category, note, date)
                } else {
                    viewModel.addTransaction(type, amount, category, note)
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun TransactionDialog(
    transaction: Transaction? = null,
    onDismiss: () -> Unit, 
    onConfirm: (TransactionType, Double, String, String, LocalDate) -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(transaction?.category ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var type by remember { mutableStateOf(transaction?.type ?: TransactionType.CONSUMPTION) }
    var date by remember { mutableStateOf(transaction?.date ?: kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "记录流水" else "编辑流水") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransactionType.values().forEach { t ->
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t.displayName)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                TextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = category, 
                    onValueChange = { category = it }, 
                    label = { Text("分类 (如: 工资, 餐饮)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = note, 
                    onValueChange = { note = it }, 
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Note: Simple date editing can be added here if needed, 
                // but for now we keep the date from the original transaction or today.
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(type, amount.toDoubleOrNull() ?: 0.0, category, note, date) }) {
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
