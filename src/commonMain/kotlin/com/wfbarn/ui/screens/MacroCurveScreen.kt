package com.wfbarn.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfbarn.models.SyncConfig
import com.wfbarn.ui.MainViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalTextApi::class)
@Composable
fun MacroCurveScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    // 1. 计算每日总资产 (Red Curve)
    // 逻辑：对于每一个有记录的日期，计算所有资产在当日或当日之前的最新余额之和
    val allReviewDates = state.dailyRecords.map { it.date }.toSet()
    val allTransactionDates = state.transactions.map { it.date }.toSet()
    val allDates = (allReviewDates + allTransactionDates).sortedBy { it.toString() }

    val dailyWealth = allDates.associateWith { date ->
        state.assets.sumOf { asset ->
            // 找到该资产在 date 或 date 之前的最后一条记录
            state.dailyRecords
                .filter { it.assetId == asset.id && it.date <= date }
                .maxByOrNull { it.date }
                ?.balance ?: asset.initialAmount
        }
    }

    // 2. 根据流水记录计算每日总消费 (Blue Curve - 仅限 CONSUMPTION 类型)
    val dailyConsumption = state.transactions
        .filter { it.type == com.wfbarn.models.TransactionType.CONSUMPTION }
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    // 3. 准备绘图数据
    val dailyStats = allDates.map { date ->
        val wealth = dailyWealth[date] ?: 0.0
        val consumption = dailyConsumption[date] ?: 0.0
        date to (wealth to consumption)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("宏观经济曲线", style = MaterialTheme.typography.h5)
            
            var showSyncConfig by remember { mutableStateOf(false) }
            IconButton(onClick = { showSyncConfig = true }) {
                Icon(Icons.Default.Sync, contentDescription = "Sync Config")
            }
            
            if (showSyncConfig) {
                SyncConfigDialog(
                    viewModel = viewModel,
                    onDismiss = { showSyncConfig = false }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("红色：总资产 | 蓝色：当日消费", style = MaterialTheme.typography.caption)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (dailyStats.isNotEmpty()) {
            MacroChart(dailyStats, textMeasurer)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("历史记录", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            
            // 在可滚动列中，我们需要限制 LazyColumn 的高度或者改用普通的 Column
            // 既然外层已经可滚动了，这里直接用 Column 即可显示所有项
            dailyStats.reversed().forEach { (date, stats) ->
                val (total, consumption) = stats
                ListItem(
                    text = { Text(date.toString()) },
                    secondaryText = { 
                        Text("当日消费: ¥ -${String.format("%.2f", consumption)}", color = Color(0xFFF44336)) 
                    },
                    trailing = { Text("总资产: ¥ ${String.format("%.2f", total)}", style = MaterialTheme.typography.subtitle1) }
                )
                Divider()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("暂无复盘或消费记录")
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun MacroChart(stats: List<Pair<kotlinx.datetime.LocalDate, Pair<Double, Double>>>, textMeasurer: TextMeasurer) {
    val totalWealthValues = stats.map { it.second.first }
    val consumptionValues = stats.map { it.second.second }
    
    val labelFontSize = 10.sp
    val labelStyle = TextStyle(color = Color.Gray, fontSize = labelFontSize)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(350.dp).padding(start = 60.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)) {
            val width = size.width
            val height = size.height

            // 计算资产范围
            val maxWealth = totalWealthValues.maxOrNull() ?: 1.0
            val minWealth = totalWealthValues.minOrNull() ?: 0.0
            val wealthRange = (maxWealth - minWealth).let { if (it <= 0.0) maxWealth.let { m -> if (m <= 0.0) 1.0 else m } else it }

            // 计算消费范围 (为了防止消费曲线波动过大，设置一个合理的最小比例)
            val maxConsumption = consumptionValues.maxOrNull() ?: 1.0
            val consumptionRange = maxConsumption.let { if (it <= 100.0) 1000.0 else it } // 如果消费很小，按1000的比例显示，防止顶格

            // 绘制坐标轴
            drawLine(Color.Gray, start = Offset(0f, 0f), end = Offset(0f, height))
            drawLine(Color.Gray, start = Offset(0f, height), end = Offset(width, height))
            
            // 资产刻度 (左侧)
            val wealthLabels = 5
            for (i in 0..wealthLabels) {
                val ratio = i.toFloat() / wealthLabels
                val value = minWealth + ratio * wealthRange
                val y = height - ratio * height
                val label = String.format("%.0f", value)
                
                val textLayoutResult = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult,
                    topLeft = Offset(-textLayoutResult.size.width.toFloat() - 10f, y - textLayoutResult.size.height / 2f)
                )
                
                drawLine(Color.LightGray, start = Offset(-5f, y), end = Offset(0f, y))
            }

            // 绘制横轴标签 (日期)
            if (stats.isNotEmpty()) {
                val stepX = if (stats.size > 1) width / (stats.size - 1) else width / 2f
                stats.forEachIndexed { index, pair ->
                    val x = if (stats.size > 1) index * stepX else width / 2f
                    if (stats.size <= 7 || index % (stats.size / 5 + 1) == 0 || index == stats.size - 1) {
                        val dateStr = pair.first.toString().substring(5) // MM-DD
                        val textLayoutResult = textMeasurer.measure(dateStr, labelStyle)
                        drawText(
                            textLayoutResult,
                            topLeft = Offset(x - textLayoutResult.size.width / 2f, height + 10f)
                        )
                        drawLine(Color.LightGray, start = Offset(x, height), end = Offset(x, height + 5f))
                    }
                }
            }

            // 绘制曲线和点
            if (stats.size == 1) {
                val x = width / 2f
                val yWealth = height - ((totalWealthValues[0] - minWealth) / wealthRange * height).toFloat()
                val yConsumption = height - (consumptionValues[0] / consumptionRange * height).toFloat()
                drawCircle(Color.Red, radius = 6.dp.toPx(), center = Offset(x, yWealth))
                drawCircle(Color.Blue, radius = 6.dp.toPx(), center = Offset(x, yConsumption))
            } else if (stats.size > 1) {
                val stepX = width / (stats.size - 1)
                
                // 红色总资产曲线
                val wealthPath = Path().apply {
                    totalWealthValues.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - ((value - minWealth) / wealthRange * height).toFloat()
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(wealthPath, color = Color.Red, style = Stroke(width = 2.dp.toPx()))

                // 蓝色当日消费曲线 (不带基准线，直接根据金额绘制)
                val consumptionPath = Path().apply {
                    consumptionValues.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / consumptionRange * height).toFloat()
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(consumptionPath, color = Color.Blue, style = Stroke(width = 2.dp.toPx()))

                // 数据点
                stats.forEachIndexed { index, _ ->
                    val x = index * stepX
                    val yWealth = height - ((totalWealthValues[index] - minWealth) / wealthRange * height).toFloat()
                    val yConsumption = height - (consumptionValues[index] / consumptionRange * height).toFloat()
                    drawCircle(Color.Red, radius = 4.dp.toPx(), center = Offset(x, yWealth))
                    drawCircle(Color.Blue, radius = 4.dp.toPx(), center = Offset(x, yConsumption))
                }
            }
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

@Composable
fun SyncConfigDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    
    var url by remember { mutableStateOf(state.syncConfig.url) }
    var path by remember { mutableStateOf(state.syncConfig.path) }
    var username by remember { mutableStateOf(state.syncConfig.username) }
    var password by remember { mutableStateOf(state.syncConfig.password) }
    var autoSync by remember { mutableStateOf(state.syncConfig.autoSync) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 同步配置") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("WebDAV 基地址 (如: https://dav.jianguoyun.com/dav/)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("同步路径 (如: /WFBarn/state.json)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoSync, onCheckedChange = { autoSync = it })
                    Text("开启自动同步 (每 2 分钟)")
                }
                
                if (state.syncConfig.lastSyncTime > 0) {
                    Text(
                        "上次同步: ${formatTimestamp(state.syncConfig.lastSyncTime)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    viewModel.syncData()
                }) {
                    Text("立即同步")
                }
                Button(onClick = {
                    viewModel.updateSyncConfig(
                        SyncConfig(url, path, username, password, autoSync, state.syncConfig.lastSyncTime)
                    )
                    onDismiss()
                }) {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(date)
}
