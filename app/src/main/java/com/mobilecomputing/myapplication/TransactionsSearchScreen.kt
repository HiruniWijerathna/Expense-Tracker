package com.mobilecomputing.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import java.util.*

enum class TimeFilter(val displayName: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    TODAY("Today")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsSearchScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (String) -> Unit,
    onBack: () -> Unit
) {
    val allExpenses by viewModel.expenses.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currency by viewModel.currency.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<ExpenseType?>(null) } // null = All
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) } // null = All
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL) }
    var selectedWalletFilter by remember { mutableStateOf<WalletAccount?>(null) }

    // Filter calculations
    val filteredList = remember(allExpenses, searchQuery, selectedTypeFilter, selectedCategoryFilter, selectedTimeFilter, selectedWalletFilter) {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_YEAR)
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        allExpenses.filter { exp ->
            // Search query
            val matchesQuery = searchQuery.isBlank() ||
                    exp.title.contains(searchQuery, ignoreCase = true) ||
                    exp.categoryName.contains(searchQuery, ignoreCase = true) ||
                    exp.note.contains(searchQuery, ignoreCase = true)

            // Type
            val matchesType = selectedTypeFilter == null || exp.type == selectedTypeFilter

            // Category
            val matchesCategory = selectedCategoryFilter == null || exp.categoryId == selectedCategoryFilter

            // Wallet
            val matchesWallet = selectedWalletFilter == null || exp.wallet == selectedWalletFilter

            // Time filter
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.dateMillis }
            val matchesTime = when (selectedTimeFilter) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> expCal.get(Calendar.DAY_OF_YEAR) == currentDay && expCal.get(Calendar.YEAR) == currentYear
                TimeFilter.THIS_WEEK -> expCal.get(Calendar.WEEK_OF_YEAR) == currentWeek && expCal.get(Calendar.YEAR) == currentYear
                TimeFilter.THIS_MONTH -> expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
            }

            matchesQuery && matchesType && matchesCategory && matchesWallet && matchesTime
        }.sortedByDescending { it.dateMillis }
    }

    val totalIncome = filteredList.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredList.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Filter 🔍", fontWeight = FontWeight.Bold) },
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
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by title, note, or category...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Horizontal filters row: Type filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All Types") }
                )
                FilterChip(
                    selected = selectedTypeFilter == ExpenseType.EXPENSE,
                    onClick = { selectedTypeFilter = ExpenseType.EXPENSE },
                    label = { Text("⬇️ Expenses") }
                )
                FilterChip(
                    selected = selectedTypeFilter == ExpenseType.INCOME,
                    onClick = { selectedTypeFilter = ExpenseType.INCOME },
                    label = { Text("⬆️ Income") }
                )

                // Time filter chips
                TimeFilter.values().forEach { timeF ->
                    FilterChip(
                        selected = selectedTimeFilter == timeF,
                        onClick = { selectedTimeFilter = timeF },
                        label = { Text(timeF.displayName) }
                    )
                }
            }

            // Wallet filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedWalletFilter == null,
                    onClick = { selectedWalletFilter = null },
                    label = { Text("All Wallets") }
                )
                WalletAccount.values().forEach { wallet ->
                    FilterChip(
                        selected = selectedWalletFilter == wallet,
                        onClick = { selectedWalletFilter = wallet },
                        label = { Text("${wallet.emoji} ${wallet.displayName}") }
                    )
                }
            }

            // Summary of filtered results
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Count", fontSize = 11.sp, color = TextSecondary)
                        Text("${filteredList.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", fontSize = 11.sp, color = TextSecondary)
                        Text("+${currency.symbol} ${String.format("%,.0f", totalIncome)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense", fontSize = 11.sp, color = TextSecondary)
                        Text("-${currency.symbol} ${String.format("%,.0f", totalExpense)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentRed)
                    }
                }
            }

            // Results List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No transactions found matching your filter.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        ExpenseListItem(
                            expense = item,
                            currencySymbol = currency.symbol,
                            onClick = { onEditExpense(item.id) },
                            onDelete = { viewModel.deleteExpense(item.id) }
                        )
                    }
                }
            }
        }
    }
}
