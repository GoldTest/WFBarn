package com.wfbarn.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wfbarn.ui.MainViewModel
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MacroCurveScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // 1. 根据每日复盘记录计算每日总资产
    val dailyWealth = state.dailyRecords
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.balance } }

    // 2. 根据流水记录计算每日总消费 (TransactionType.CONSUMPTION)
    val dailyConsumption = state.transactions
        .filter { it.type == com.wfbarn.models.TransactionType.CONSUMPTION }
        .groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    // 3. 合并日期，确保即使某天只有复盘或只有消费也能显示
    val allDates = (dailyWealth.keys + dailyConsumption.keys).sortedBy { it.toString() }
    val dailyStats = allDates.map { date ->
        val wealth = dailyWealth[date] ?: 0.0
        val consumption = dailyConsumption[date] ?: 0.0
        date to (wealth to consumption)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Text("宏观经济曲线", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))
        Text("红色：总资产 | 蓝色：当日消费", style = MaterialTheme.typography.caption)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (dailyStats.isNotEmpty()) {
            MacroChart(dailyStats)
            
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
                        Text("当日消费: ¥ ${String.format("%.2f", consumption)}", color = Color.Gray) 
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

@Composable
fun MacroChart(stats: List<Pair<kotlinx.datetime.LocalDate, Pair<Double, Double>>>) {
    val totalWealthValues = stats.map { it.second.first }
    val consumptionValues = stats.map { it.second.second }
    
    val density = LocalDensity.current
    val labelFontSize = 10.sp
    val labelFontSizePx = with(density) { labelFontSize.toPx() }

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(350.dp).padding(start = 60.dp, end = 20.dp, top = 20.dp, bottom = 40.dp)) {
            val width = size.width
            val height = size.height

            // 计算资产范围
            val maxWealth = totalWealthValues.maxOrNull() ?: 1.0
            val minWealth = totalWealthValues.minOrNull() ?: 0.0
            val wealthRange = (maxWealth - minWealth).let { if (it <= 0.0) maxWealth.let { m -> if (m <= 0.0) 1.0 else m } else it }

            // 计算消费范围
            val maxConsumption = consumptionValues.maxOrNull() ?: 1.0
            val consumptionRange = maxConsumption.let { if (it <= 0.0) 1.0 else it }

            // 绘制坐标轴
            drawLine(Color.Gray, start = Offset(0f, 0f), end = Offset(0f, height))
            drawLine(Color.Gray, start = Offset(0f, height), end = Offset(width, height))
            
            // 绘制纵轴标签 (金额)
            val paint = Paint().apply {
                color = org.jetbrains.skia.Color.makeRGB(128, 128, 128)
            }
            val font = Font(null, labelFontSizePx)
            
            // 资产刻度 (左侧)
            val wealthLabels = 5
            for (i in 0..wealthLabels) {
                val ratio = i.toFloat() / wealthLabels
                val value = minWealth + ratio * wealthRange
                val y = height - ratio * height
                val label = String.format("%.0f", value)
                val textLine = TextLine.make(label, font)
                drawContext.canvas.nativeCanvas.drawTextLine(textLine, -textLine.width - 10f, y + textLine.height / 3f, paint)
                drawLine(Color.LightGray, start = Offset(-5f, y), end = Offset(0f, y))
            }

            // 绘制横轴标签 (日期)
            if (stats.isNotEmpty()) {
                val stepX = if (stats.size > 1) width / (stats.size - 1) else width / 2f
                stats.forEachIndexed { index, pair ->
                    val x = if (stats.size > 1) index * stepX else width / 2f
                    if (stats.size <= 7 || index % (stats.size / 5 + 1) == 0 || index == stats.size - 1) {
                        val dateStr = pair.first.toString().substring(5) // MM-DD
                        val textLine = TextLine.make(dateStr, font)
                        drawContext.canvas.nativeCanvas.drawTextLine(textLine, x - textLine.width / 2f, height + textLine.height + 10f, paint)
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
