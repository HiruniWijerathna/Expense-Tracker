package com.mobilecomputing.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var expenseType by remember { mutableStateOf(ExpenseType.EXPENSE) }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var mood by remember { mutableIntStateOf(3) }
    var showError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(ExpenseType.EXPENSE, ExpenseType.INCOME).forEach { type ->
                    val isSelected = expenseType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    if (type == ExpenseType.EXPENSE) AccentRed else PrimaryGreen
                                else Color.Transparent
                            )
                            .clickable {
                                if (expenseType != type) {
                                    expenseType = type
                                    selectedCategory = if (type == ExpenseType.EXPENSE) ExpenseCategory.FOOD else ExpenseCategory.SALARY
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == ExpenseType.EXPENSE) "⬇️ Expense" else "⬆️ Income",
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.length <= 10) amount = input.filter { c -> c.isDigit() || c == '.' }
                },
                label = { Text("Amount (LKR)") },
                leadingIcon = { Text("💰", modifier = Modifier.padding(start = 12.dp)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = showError && amount.isBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (expenseType == ExpenseType.INCOME) PrimaryGreen else AccentRed,
                    focusedLabelColor = if (expenseType == ExpenseType.INCOME) PrimaryGreen else AccentRed
                )
            )

            // Title Input (Optional)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(
                        if (expenseType == ExpenseType.EXPENSE)
                            "What did you spend on? (Optional)"
                        else
                            "Where did you earn from? (Optional)"
                    )
                },
                placeholder = { Text(selectedCategory.displayName) },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Date Picker Field
            val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = PrimaryGreen)
                        Column {
                            Text("Transaction Date", fontSize = 11.sp, color = TextSecondary)
                            Text(formattedDate, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                    Text("Change", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Category Selection
            Text(
                if (expenseType == ExpenseType.EXPENSE) "Expense Category" else "Income Category",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            CategoryGrid(
                type = expenseType,
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            // Note (optional)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Mood Selector
            Text("How do you feel about this? 🤔", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            MoodSelector(selected = mood, onSelect = { mood = it })

            // Error message
            AnimatedVisibility(visible = showError) {
                Text(
                    "⚠️ Please enter a valid amount!",
                    color = AccentRed,
                    fontSize = 13.sp
                )
            }

            // Save Button
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull()
                    if (amount.isBlank() || amountVal == null || amountVal <= 0.0) {
                        showError = true
                    } else {
                        val finalTitle = title.trim().ifEmpty { selectedCategory.displayName }
                        viewModel.addExpense(
                            Expense(
                                title = finalTitle,
                                amount = amountVal,
                                category = selectedCategory,
                                type = expenseType,
                                note = note.trim(),
                                mood = mood,
                                dateMillis = selectedDateMillis
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (expenseType == ExpenseType.INCOME) PrimaryGreen else AccentRed
                )
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save ${if (expenseType == ExpenseType.INCOME) "Income" else "Expense"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CategoryGrid(
    type: ExpenseType,
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit
) {
    val categories = ExpenseCategory.getCategoriesForType(type)
    val rows = categories.chunked(4)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in row) {
                    val isSelected = cat == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) cat.color.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) cat.color else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(cat) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(cat.emoji, fontSize = 22.sp)
                            Text(
                                text = cat.displayName.split(" ").first(),
                                fontSize = 9.sp,
                                color = if (isSelected) cat.color else TextSecondary
                            )
                        }
                    }
                }
                // Fill empty slots in last row
                val empty = 4 - row.size
                repeat(empty) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MoodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val moods = listOf(1 to "😞", 2 to "😐", 3 to "🙂", 4 to "😊", 5 to "🤩")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        for ((value, emoji) in moods) {
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) PrimaryGreen.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) PrimaryGreen else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}
