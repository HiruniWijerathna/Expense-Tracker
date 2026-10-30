package com.mobilecomputing.myapplication

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: ExpenseViewModel, onBack: () -> Unit) {
    val goals by viewModel.goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, target, emoji ->
                viewModel.addGoal(SavingsGoal(title = title, targetAmount = target, emoji = emoji))
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals 🎯", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Goal")
            }
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
            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("No goals yet!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Tap + to set a savings goal", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        onAddMoney = { amount -> viewModel.updateGoalProgress(goal.id, amount) },
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: SavingsGoal, onAddMoney: (Double) -> Unit, onDelete: () -> Unit) {
    var showAddMoney by remember { mutableStateOf(false) }
    var addAmount by remember { mutableStateOf("") }

    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress,
        animationSpec = tween(1000, easing = EaseOutQuart),
        label = "goal_progress"
    )

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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.emoji, fontSize = 28.sp)
                    Column {
                        Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (goal.isCompleted) "🎉 Completed!" else "In progress...",
                            fontSize = 12.sp,
                            color = if (goal.isCompleted) PrimaryGreen else TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = AccentRed.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = if (goal.isCompleted) PrimaryGreen else AccentPurple,
                trackColor = MaterialTheme.colorScheme.background
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "LKR ${String.format("%,.0f", goal.currentAmount)}",
                    color = AccentPurple, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
                Text(
                    "${(goal.progress * 100).toInt()}% • Target: LKR ${String.format("%,.0f", goal.targetAmount)}",
                    color = TextSecondary, fontSize = 12.sp
                )
            }

            if (!goal.isCompleted) {
                Spacer(Modifier.height(12.dp))
                if (showAddMoney) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = addAmount,
                            onValueChange = { addAmount = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Amount") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                val amt = addAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0) { onAddMoney(amt); addAmount = ""; showAddMoney = false }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) { Text("Add") }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddMoney = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentPurple)
                    ) {
                        Icon(Icons.Default.Add, null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Money", color = AccentPurple)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    val emojis = listOf("🎯", "🏠", "✈️", "🚗", "💻", "📱", "👟", "🎓", "💍", "🏖️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Emoji picker
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (e == emoji) AccentPurple.copy(alpha = 0.2f) else Color.Transparent)
                                .border(if (e == emoji) 1.5.dp else 0.dp, if (e == emoji) AccentPurple else Color.Transparent, CircleShape)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 18.sp) }
                    }
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Goal Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Amount (LKR)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) onSave(title, amt, emoji)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) { Text("Create Goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
