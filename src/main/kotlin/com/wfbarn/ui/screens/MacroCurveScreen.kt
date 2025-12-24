package com.wfbarn.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wfbarn.ui.MainViewModel

@Composable
fun MacroCurveScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Macro Record")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            Text("宏观经济曲线", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (state.macroRecords.size > 1) {
                MacroChart(state.macroRecords.sortedBy { it.date.toString() }.map { it.value })
            } else {
                Text("记录至少两个点以显示曲线")
            }
        }
    }

    if (showAddDialog) {
        AddMacroDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { value, note ->
                viewModel.addMacroRecord(value, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MacroChart(values: List<Double>) {
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp)) {
            val maxVal = values.maxOrNull() ?: 1.0
            val minVal = values.minOrNull() ?: 0.0
            val range = (maxVal - minVal).let { if (it == 0.0) 1.0 else it }
            
            val width = size.width
            val height = size.height
            
            // Draw axes
            drawLine(Color.Gray, start = Offset(0f, 0f), end = Offset(0f, height))
            drawLine(Color.Gray, start = Offset(0f, height), end = Offset(width, height))

            if (values.size > 1) {
                val stepX = width / (values.size - 1)
                val path = Path().apply {
                    values.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - ((value - minVal) / range * height).toFloat()
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                
                drawPath(path, color = Color.Blue, style = Stroke(width = 2.dp.toPx()))
                
                // Draw points
                values.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - ((value - minVal) / range * height).toFloat()
                    drawCircle(Color.Red, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("起始记录", style = MaterialTheme.typography.caption)
            Text("最新记录", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
fun AddMacroDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录宏观指数") },
        text = {
            Column {
                TextField(value = value, onValueChange = { value = it }, label = { Text("指数值") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = note, onValueChange = { note = it }, label = { Text("备注") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.toDoubleOrNull() ?: 0.0, note) }) {
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
