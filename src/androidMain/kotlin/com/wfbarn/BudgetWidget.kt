package com.wfbarn

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wfbarn.models.AppState
import com.wfbarn.models.TransactionType
import com.wfbarn.service.StorageService
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import java.io.File

class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val storageService = StorageService(context.filesDir)
        val state = storageService.loadState()
        
        provideContent {
            GlanceTheme {
                BudgetWidgetContent(state)
            }
        }
    }

    @Composable
    private fun BudgetWidgetContent(state: AppState) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonthKey = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
        
        val totalBudget = state.monthlyBudgets[currentMonthKey] ?: 0.0
        val spent = state.transactions
            .filter { it.date.year == now.year && it.date.monthNumber == now.monthNumber && it.type == TransactionType.CONSUMPTION }
            .sumOf { it.amount }
        
        val remainingBudget = (totalBudget - spent).coerceAtLeast(0.0)
        
        // 计算本月剩余天数 (包括今天)
        val today = now.date
        val firstDayOfNextMonth = if (now.monthNumber == 12) {
            kotlinx.datetime.LocalDate(now.year + 1, 1, 1)
        } else {
            kotlinx.datetime.LocalDate(now.year, now.monthNumber + 1, 1)
        }
        val remainingDays = today.daysUntil(firstDayOfNextMonth)
        
        val dailyAvailable = if (remainingDays > 0) remainingBudget / remainingDays else remainingBudget
        val breakfast = 4.0
        val lunchDinner = ((dailyAvailable - breakfast) / 2.0).coerceAtLeast(0.0)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.White))
                .padding(6.dp),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "谷仓预算",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ColorProvider(Color.Black)
                    )
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = currentMonthKey,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
                )
            }
            
            Row(modifier = GlanceModifier.padding(vertical = 2.dp)) {
                Text(
                    text = "余:¥${String.format("%.0f", remainingBudget)}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorProvider(Color(0xFF2E7D32)))
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "已花:¥${spent.toInt()}",
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.Red))
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "建议:",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ColorProvider(Color.Black))
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "早¥${breakfast.toInt()} 午晚¥${lunchDinner.toInt()}",
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color.DarkGray))
                )
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
