package com.mobilecomputing.myapplication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val categoryData = viewModel.expensesByCategory()
    val totalExpense = viewModel.totalExpenseThisMonth
    val totalIncome = viewModel.totalIncomeThisMonth

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics 📊", fontWeight = FontWeight.Bold) },
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
            // Summary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("This Month", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryItem("Income ⬆️", totalIncome, PrimaryGreen)
                        SummaryItem("Expense ⬇️", totalExpense, AccentRed)
                        SummaryItem(
                            "Balance",
                            totalIncome - totalExpense,
                            if (totalIncome >= totalExpense) PrimaryGreen else AccentRed
                        )
                    }
                }
            }

            // Category Breakdown
            if (categoryData.isNotEmpty()) {
                Text("Spending by Category", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                val total = categoryData.values.sum().takeIf { it > 0 } ?: 1.0

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        categoryData.entries
                            .sortedByDescending { it.value }
                            .forEach { (category, amount) ->
                                CategoryProgressBar(
                                    category = category,
                                    amount = amount,
                                    percentage = (amount / total * 100).toInt()
                                )
                            }
                    }
                }
            }

            // Smart Insights
            Text("💡 Smart Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            SmartInsights(
                expenses = expenses,
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                categoryData = categoryData
            )

            // All Transactions
            Text("All Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet! 💸", color = TextSecondary)
                }
            } else {
                expenses.sortedByDescending { it.dateMillis }.forEach { expense ->
                    ExpenseListItem(expense = expense, onDelete = { viewModel.deleteExpense(expense.id) })
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            "LKR ${String.format("%,.0f", kotlin.math.abs(amount))}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
    }
}

@Composable
fun CategoryProgressBar(category: ExpenseCategory, amount: Double, percentage: Int) {
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(percentage) { animTarget = percentage / 100f }

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animTarget,
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "progress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(category.emoji, fontSize = 16.sp)
                Text(category.displayName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$percentage%", fontSize = 12.sp, color = TextSecondary)
                Text("LKR ${String.format("%,.0f", amount)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = category.color)
            }
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = category.color,
            trackColor = category.color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun SmartInsights(
    expenses: List<Expense>,
    totalExpense: Double,
    totalIncome: Double,
    categoryData: Map<ExpenseCategory, Double>
) {
    val insights = mutableListOf<Pair<String, String>>()

    // Top spending category
    val topCat = categoryData.maxByOrNull { it.value }
    if (topCat != null) {
        insights.add("🏆" to "Your biggest spending is on **${topCat.key.displayName}** — LKR ${String.format("%,.0f", topCat.value)}")
    }

    // Savings rate
    if (totalIncome > 0) {
        val savingsRate = ((totalIncome - totalExpense) / totalIncome * 100).toInt()
        when {
            savingsRate >= 30 -> insights.add("⭐" to "Excellent! You're saving **$savingsRate%** of your income!")
            savingsRate >= 10 -> insights.add("👍" to "Good job! Saving **$savingsRate%** this month.")
            savingsRate < 0 -> insights.add("⚠️" to "You spent **${-savingsRate}%** more than you earned!")
            else -> insights.add("💡" to "Try to save at least 20% of your income.")
        }
    }

    // Expense count
    if (expenses.size >= 5) {
        val avgExpense = totalExpense / expenses.filter { it.type == ExpenseType.EXPENSE }.size.coerceAtLeast(1)
        insights.add("📈" to "Your average expense is **LKR ${String.format("%,.0f", avgExpense)}**")
    }

    if (insights.isEmpty()) {
        insights.add("📝" to "Add more expenses to see insights!")
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 20.sp)
                    }
                    Text(
                        text = msg.replace("**", ""),
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
