package com.mobilecomputing.myapplication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onManageCategories: () -> Unit
) {
    val context = LocalContext.current
    val currentBudget by viewModel.monthlyBudget.collectAsState()
    val currentCurrency by viewModel.currency.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(currentBudget.toInt().toString()) }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPdfReportDialog by remember { mutableStateOf(false) }

    val currentPin by viewModel.appPin.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var currentPinVerifyInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf("") }

    // CSV Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportToCsv(outputStream)
                }
                Toast.makeText(context, "CSV exported successfully! 📄", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF Export launcher
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    viewModel.exportMonthlyReportPdf(outputStream)
                }
                Toast.makeText(context, "Monthly PDF Report saved! 📊📄", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPdfReportDialog) {
        ExportReportDialog(
            viewModel = viewModel,
            onDismiss = { showPdfReportDialog = false }
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Budget Amount (${currentCurrency.code})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = budgetInput.toDoubleOrNull() ?: 50000.0
                    viewModel.setMonthlyBudget(value)
                    showBudgetDialog = false
                    Toast.makeText(context, "Monthly budget updated!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCurrency.values().forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCurrency(curr)
                                    showCurrencyDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = curr == currentCurrency,
                                onClick = {
                                    viewModel.setCurrency(curr)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(curr.displayName, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPinDialog) {
        val hasExistingPin = isPinEnabled && currentPin.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (hasExistingPin) "Change or Disable PIN" else "Set 4-Digit Security PIN") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasExistingPin) {
                        Text("Enter your current PIN to modify security settings:", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = currentPinVerifyInput,
                            onValueChange = {
                                if (it.length <= 4) currentPinVerifyInput = it.filter { c -> c.isDigit() }
                                pinErrorMessage = ""
                            },
                            label = { Text("Current 4-digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Enter new 4-digit PIN (or leave empty if disabling):", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4) newPinInput = it.filter { c -> c.isDigit() }
                            pinErrorMessage = ""
                        },
                        label = { Text("New 4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 4) confirmPinInput = it.filter { c -> c.isDigit() }
                            pinErrorMessage = ""
                        },
                        label = { Text("Confirm New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinErrorMessage.isNotEmpty()) {
                        Text("⚠️ $pinErrorMessage", color = AccentRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (hasExistingPin && currentPinVerifyInput != currentPin) {
                            pinErrorMessage = "Current PIN is incorrect!"
                            return@TextButton
                        }
                        if (newPinInput.length != 4) {
                            pinErrorMessage = "New PIN must be exactly 4 digits!"
                            return@TextButton
                        }
                        if (newPinInput != confirmPinInput) {
                            pinErrorMessage = "New PIN and Confirm PIN do not match!"
                            return@TextButton
                        }

                        viewModel.setPin(newPinInput, true)
                        showPinDialog = false
                        Toast.makeText(context, "PIN successfully updated! 🔒", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (hasExistingPin) "Save New PIN" else "Enable PIN", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (hasExistingPin) {
                        TextButton(
                            onClick = {
                                if (currentPinVerifyInput != currentPin) {
                                    pinErrorMessage = "Enter correct current PIN to disable!"
                                    return@TextButton
                                }
                                viewModel.setPin("", false)
                                showPinDialog = false
                                Toast.makeText(context, "PIN Lock Disabled", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Disable PIN", color = AccentRed)
                        }
                    }
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Tools ⚙️", fontWeight = FontWeight.Bold) },
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
            // General Settings Group
            Text("General & Budget", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryGreen)

            SettingsCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Monthly Spending Budget",
                subtitle = "${currentCurrency.symbol} ${String.format("%,.0f", currentBudget)}",
                onClick = {
                    budgetInput = currentBudget.toInt().toString()
                    showBudgetDialog = true
                }
            )

            SettingsCard(
                icon = Icons.Default.CurrencyExchange,
                title = "Currency",
                subtitle = currentCurrency.displayName,
                onClick = { showCurrencyDialog = true }
            )

            SettingsCard(
                icon = Icons.Default.Category,
                title = "Custom Categories",
                subtitle = "Add, customize or delete categories & emojis",
                onClick = onManageCategories
            )

            // Security Group
            Spacer(Modifier.height(8.dp))
            Text("Security & Privacy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryGreen)

            SettingsCard(
                icon = Icons.Default.Lock,
                title = "App PIN Lock",
                subtitle = if (isPinEnabled) "Status: Enabled 🔒 (Tap to Change/Disable)" else "Status: Disabled 🔓 (Tap to Setup)",
                onClick = {
                    currentPinVerifyInput = ""
                    newPinInput = ""
                    confirmPinInput = ""
                    pinErrorMessage = ""
                    showPinDialog = true
                }
            )

            // Backup & Export Group
            Spacer(Modifier.height(8.dp))
            Text("Data & Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryGreen)

            SettingsCard(
                icon = Icons.Default.PictureAsPdf,
                title = "Export & Share PDF Statement",
                subtitle = "Generate statement by Date Range (Month, Custom) and Share/Open PDF",
                onClick = { showPdfReportDialog = true }
            )

            SettingsCard(
                icon = Icons.Default.FileDownload,
                title = "Export to CSV Spreadsheet",
                subtitle = "Download all your transactions as an Excel/CSV file",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("expenses_$timeStamp.csv")
                }
            )

            SettingsCard(
                icon = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                title = "Theme mode",
                subtitle = if (isDarkMode) "Dark theme enabled" else "Light theme enabled",
                onClick = { viewModel.toggleDarkMode() }
            )
        }
    }
}

@Composable
fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
        }
    }
}
