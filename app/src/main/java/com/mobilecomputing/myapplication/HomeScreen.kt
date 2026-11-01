package com.mobilecomputing.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit = {},
    onNavigate: (String) -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val recent = viewModel.recentExpenses(8)

    val totalExpense = viewModel.totalExpenseThisMonth
    val totalIncome = viewModel.totalIncomeThisMonth
    val balance = totalIncome - totalExpense

    val totalExpenseAllTime = viewModel.totalExpenseAllTime
    val totalIncomeAllTime = viewModel.totalIncomeAllTime
    val totalBalanceAllTime = viewModel.totalBalanceAllTime

    val remainingBudget = (monthlyBudget - totalExpense).coerceAtLeast(0.0)
    val budgetProgress = if (monthlyBudget > 0) (totalExpense / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    // Balance pager state (0 = This Month, 1 = Total / All Time)
    val balancePagerState = rememberPagerState(pageCount = { 2 })

    // Animate balance
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(800, easing = EaseOutQuart),
        label = "balance"
    )

    val animatedTotalBalance by animateFloatAsState(
        targetValue = totalBalanceAllTime.toFloat(),
        animationSpec = tween(800, easing = EaseOutQuart),
        label = "totalBalance"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = PrimaryGreen,
                contentColor = Color.Black,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, "Add Expense", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Header with actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "My Budget 💰",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onNavigate("search_transactions") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryGreen)
                        }
                        IconButton(
                            onClick = { onNavigate("settings") },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                // Balance & Income/Expense Horizontal Pager (This Month & Total All-Time)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = balancePagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val isThisMonth = (page == 0)
                        val displayBalance = if (isThisMonth) balance else totalBalanceAllTime
                        val animBal = if (isThisMonth) animatedBalance else animatedTotalBalance
                        val inc = if (isThisMonth) totalIncome else totalIncomeAllTime
                        val exp = if (isThisMonth) totalExpense else totalExpenseAllTime

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isThisMonth) {
                                            listOf(
                                                Color(0xFF1A3A2A),
                                                Color(0xFF0D2018),
                                                Color(0xFF00C853).copy(alpha = 0.3f)
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFF1E293B),
                                                Color(0xFF0F172A),
                                                Color(0xFF38BDF8).copy(alpha = 0.25f)
                                            )
                                        }
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isThisMonth) PrimaryGreen.copy(alpha = 0.3f) else Color(0xFF38BDF8).copy(alpha = 0.3f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isThisMonth) "This Month's Balance" else "Total Overall Balance",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isThisMonth) "📅" else "🌐",
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${if (displayBalance < 0) "-" else ""}${currency.symbol} ${String.format("%,.0f", abs(animBal.toDouble()))}",
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (displayBalance >= 0) PrimaryGreen else AccentRed
                                )
                                Text(
                                    text = if (displayBalance >= 0) "✅ Healthy Financial Health" else "⚠️ Spending exceeding income!",
                                    color = if (displayBalance >= 0) PrimaryGreenLight else AccentRed.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    MiniStat("Income", inc, PrimaryGreen, "⬆️", currency.symbol)
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(40.dp)
                                            .background(TextSecondary.copy(alpha = 0.3f))
                                    )
                                    MiniStat("Expenses", exp, AccentRed, "⬇️", currency.symbol)
                                }
                            }
                        }
                    }

                    // Pager indicator dots with swipe hint
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { index ->
                            val isSelected = balancePagerState.currentPage == index
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
                            text = if (balancePagerState.currentPage == 0) "Swipe for Total ➡️" else "⬅️ Swipe for This Month",
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Monthly Budget & Remaining Budget Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎯", fontSize = 18.sp)
                                Column {
                                    Text("Monthly Budget", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Target: ${currency.symbol} ${String.format("%,.0f", monthlyBudget)}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Remaining", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    "${currency.symbol} ${String.format("%,.0f", remainingBudget)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (remainingBudget > 0) PrimaryGreen else AccentRed
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (budgetProgress > 0.85f) AccentRed else PrimaryGreen,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(Modifier.height(8.dp))
                        val percentage = (budgetProgress * 100).toInt()
                        Text(
                            text = if (percentage >= 100) "🚨 Budget exceeded by ${percentage - 100}%!" else "Spent $percentage% of monthly budget",
                            fontSize = 12.sp,
                            color = if (percentage >= 85) AccentRed else TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                // Quick Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Receipt,
                        label = "Transactions",
                        value = "${expenses.size}",
                        color = AccentBlue,
                        onClick = { onNavigate("search_transactions") }
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Category,
                        label = "Categories",
                        value = "${viewModel.categories.value.size}",
                        color = AccentPurple,
                        onClick = { onNavigate("manage_categories") }
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Flag,
                        label = "Goals",
                        value = "${viewModel.goals.value.size}",
                        color = AccentYellow,
                        onClick = { onNavigate("goals") }
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Mascot Section
            item {
                MascotSection(balance = balance, totalExpense = totalExpense)
                Spacer(Modifier.height(20.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { onNavigate("analytics") }) {
                        Text("See All & Charts", color = PrimaryGreen)
                    }
                }
            }

            if (recent.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No expenses yet!\nTap + to add your first one.",
                                textAlign = TextAlign.Center,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(recent, key = { it.id }) { expense ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn()
                    ) {
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
}

@Composable
fun MiniStat(label: String, amount: Double, color: Color, arrow: String, symbol: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$arrow $label", color = TextSecondary, fontSize = 12.sp)
        Text(
            "$symbol ${String.format("%,.0f", amount)}",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MascotSection(balance: Double, totalExpense: Double) {
    val mascotEmoji = when {
        balance > 5000 -> "🤩"
        balance > 0 -> "😊"
        balance > -2000 -> "😬"
        else -> "😱"
    }
    val mascotMsg = when {
        balance > 5000 -> "You're crushing it! Great savings! Keep going!"
        balance > 0 -> "Looking good! You're staying on budget 💪"
        balance > -2000 -> "Careful! Spending is getting close to limit..."
        else -> "Whoa! Way over budget! Time to cut back! 🚨"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "mascot")
            val bounce by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )
            Box(
                modifier = Modifier
                    .offset(y = bounce.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (balance >= 0)
                            PrimaryGreen.copy(alpha = 0.15f)
                        else
                            AccentRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(mascotEmoji, fontSize = 32.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Finance Buddy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    mascotMsg,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListItem(
    expense: Expense,
    currencySymbol: String = "Rs.",
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete?") },
            text = { Text("Delete \"${expense.title}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        .format(Date(expense.dateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(expense.categoryColorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(expense.categoryEmoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    expense.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${expense.categoryName} • ${expense.wallet.emoji} ${expense.wallet.displayName.split(" ").first()} • $dateStr",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (expense.type == ExpenseType.INCOME) "+" else "-"}$currencySymbol ${String.format("%,.0f", expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (expense.type == ExpenseType.INCOME) PrimaryGreen else AccentRed
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!expense.receiptImagePath.isNullOrEmpty()) {
                        Text("🧾", fontSize = 12.sp)
                    }
                    Text(
                        moodEmoji(expense.mood),
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Transaction",
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun moodEmoji(mood: Int) = when(mood) {
    1 -> "😞"
    2 -> "😐"
    3 -> "🙂"
    4 -> "😊"
    5 -> "🤩"
    else -> "🙂"
}

fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning ☀️"
        hour < 17 -> "Good afternoon 🌤️"
        else -> "Good evening 🌙"
    }
}
