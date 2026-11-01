package com.mobilecomputing.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ChartPeriod(val label: String) {
    DAILY("Daily (7 Days)"),
    WEEKLY("Weekly (4 Weeks)"),
    MONTHLY("Monthly (6 Months)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val totalExpense = viewModel.totalExpenseThisMonth
    val totalIncome = viewModel.totalIncomeThisMonth
    val totalExpenseAllTime = viewModel.totalExpenseAllTime
    val totalIncomeAllTime = viewModel.totalIncomeAllTime

    val summaryPagerState = rememberPagerState(pageCount = { 2 })

    var selectedPeriod by remember { mutableStateOf(ChartPeriod.DAILY) }

    // Category breakdown
    val categoryData = remember(expenses) {
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)
        expenses.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            it.type == ExpenseType.EXPENSE && c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear
        }.groupBy { it.categoryName to (it.categoryEmoji to it.categoryColorHex) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics 📊", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Summary Horizontal Pager (This Month & Total Overall)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = summaryPagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val isThisMonth = (page == 0)
                    val inc = if (isThisMonth) totalIncome else totalIncomeAllTime
                    val exp = if (isThisMonth) totalExpense else totalExpenseAllTime
                    val net = inc - exp

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isThisMonth) "This Month Summary" else "Total Overall Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isThisMonth) "📅 This Month" else "🌐 All-Time",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SummaryItem("Income ⬆️", inc, PrimaryGreen, currency.symbol)
                                SummaryItem("Expense ⬇️", exp, AccentRed, currency.symbol)
                                SummaryItem(
                                    "Net Saved",
                                    net,
                                    if (net >= 0) PrimaryGreen else AccentRed,
                                    currency.symbol
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val isSelected = summaryPagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) PrimaryGreen else TextSecondary.copy(alpha = 0.4f)
                                )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (summaryPagerState.currentPage == 0) "Swipe for Total ➡️" else "⬅️ Swipe for This Month",
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            // 2. Spending by Category Breakdown (Placed First)
            if (categoryData.isNotEmpty()) {
                val totalCatExpense = categoryData.values.sum().takeIf { it > 0 } ?: 1.0
                val highestCategory = categoryData.maxByOrNull { it.value }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    highestCategory?.let {
                        Text("Top: ${it.key.second.first} ${it.key.first}", fontSize = 12.sp, color = AccentYellow, fontWeight = FontWeight.SemiBold)
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        categoryData.entries
                            .sortedByDescending { it.value }
                            .forEach { entry ->
                                val (name, meta) = entry.key
                                val (emoji, colorHex) = meta
                                val amount = entry.value
                                CategoryProgressRow(
                                    name = name,
                                    emoji = emoji,
                                    color = Color(colorHex),
                                    amount = amount,
                                    percentage = (amount / totalCatExpense * 100).toInt(),
                                    currencySymbol = currency.symbol
                                )
                            }
                    }
                }
            }

            // 3. Clear Spending Activity & Trends (Placed Below Category Chart)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📈 Spending Activity & Trends", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Period Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                ChartPeriod.values().forEach { period ->
                    val isSel = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) PrimaryGreen else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSel) Color.Black else TextSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Crystal Clear Spending Bar Chart Card
            SpendingBarChartCard(
                expenses = expenses.filter { it.type == ExpenseType.EXPENSE },
                period = selectedPeriod,
                currencySymbol = currency.symbol
            )

            // 4. Smart Financial Insights
            Text("💡 Smart Insights & Spending Habits", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            SmartInsightsCard(
                expenses = expenses,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                currencySymbol = currency.symbol
            )

            // 5. All Transactions with Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All Transactions (${expenses.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet! 💸", color = TextSecondary)
                }
            } else {
                expenses.sortedByDescending { it.dateMillis }.forEach { expense ->
                    ExpenseListItem(
                        expense = expense,
                        currencySymbol = currency.symbol,
                        onClick = { onEditExpense(expense.id) },
                        onDelete = { viewModel.deleteExpense(expense.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingBarChartCard(
    expenses: List<Expense>,
    period: ChartPeriod,
    currencySymbol: String
) {
    val barData = remember(expenses, period) {
        val list = mutableListOf<Pair<String, Double>>()

        when (period) {
            ChartPeriod.DAILY -> {
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
                    val year = dayCal.get(Calendar.YEAR)
                    val sum = expenses.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                        c.get(Calendar.DAY_OF_YEAR) == dayOfYear && c.get(Calendar.YEAR) == year
                    }.sumOf { it.amount }
                    list.add(dayFormat.format(dayCal.time) to sum)
                }
            }
            ChartPeriod.WEEKLY -> {
                for (w in 3 downTo 0) {
                    val weekCal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -w) }
                    val week = weekCal.get(Calendar.WEEK_OF_YEAR)
                    val year = weekCal.get(Calendar.YEAR)
                    val sum = expenses.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                        c.get(Calendar.WEEK_OF_YEAR) == week && c.get(Calendar.YEAR) == year
                    }.sumOf { it.amount }
                    list.add("Wk ${4 - w}" to sum)
                }
            }
            ChartPeriod.MONTHLY -> {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                for (m in 5 downTo 0) {
                    val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -m) }
                    val month = mCal.get(Calendar.MONTH)
                    val year = mCal.get(Calendar.YEAR)
                    val sum = expenses.filter {
                        val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                        c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
                    }.sumOf { it.amount }
                    list.add(monthFormat.format(mCal.time) to sum)
                }
            }
        }
        list
    }

    val maxAmount = barData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    period.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Highest: $currencySymbol ${String.format("%,.0f", maxAmount)}",
                    fontSize = 11.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Chart area with clear height and explicit amount values
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                barData.forEach { (label, amount) ->
                    val fraction = if (maxAmount > 0) (amount / maxAmount).toFloat().coerceIn(0.04f, 1f) else 0.04f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Amount above bar if > 0
                        if (amount > 0) {
                            Text(
                                text = if (amount >= 1000) "${(amount / 1000).toInt()}k" else "${amount.toInt()}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (amount > 0) PrimaryGreen
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        // Day/Week/Month Label
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color, symbol: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            "$symbol ${String.format("%,.0f", kotlin.math.abs(amount))}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
fun CategoryProgressRow(
    name: String,
    emoji: String,
    color: Color,
    amount: Double,
    percentage: Int,
    currencySymbol: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(emoji, fontSize = 16.sp)
                Text(name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$percentage%", fontSize = 12.sp, color = TextSecondary)
                Text("$currencySymbol ${String.format("%,.0f", amount)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color)
            }
        }
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun SmartInsightsCard(
    expenses: List<Expense>,
    totalExpense: Double,
    totalIncome: Double,
    currencySymbol: String
) {
    val insights = remember(expenses, totalExpense, totalIncome) {
        val list = mutableListOf<Pair<String, String>>()

        if (totalIncome > 0) {
            val savingsRate = ((totalIncome - totalExpense) / totalIncome * 100).toInt()
            when {
                savingsRate >= 30 -> list.add("🌟" to "Excellent! You saved $savingsRate% of your earnings this month.")
                savingsRate >= 10 -> list.add("👍" to "Good discipline! You're saving $savingsRate% this month.")
                savingsRate < 0 -> list.add("⚠️" to "Warning! You spent ${-savingsRate}% more than your total income!")
                else -> list.add("💡" to "Aim to save at least 20% of your earnings each month.")
            }
        }

        val expenseList = expenses.filter { it.type == ExpenseType.EXPENSE }
        if (expenseList.isNotEmpty()) {
            val topCategory = expenseList.groupBy { it.categoryName }.maxByOrNull { it.value.sumOf { e -> e.amount } }
            topCategory?.let {
                val sum = it.value.sumOf { e -> e.amount }
                list.add("🎯" to "Your highest expenditure is on '${it.key}' with $currencySymbol ${String.format("%,.0f", sum)}.")
            }
            val avg = totalExpense / expenseList.size
            list.add("📊" to "Your average cost per transaction is $currencySymbol ${String.format("%,.0f", avg)}.")
        } else {
            list.add("📝" to "Keep tracking your daily expenses to unlock smart AI budget recommendations!")
        }

        list
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        insights.forEach { (icon, msg) ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
