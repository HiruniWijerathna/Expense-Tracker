package com.mobilecomputing.myapplication

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilecomputing.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ReportDateRangeType(val title: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Date Range")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedRangeType by remember { mutableStateOf(ReportDateRangeType.THIS_MONTH) }

    val cal = Calendar.getInstance()
    var startMillis by remember { mutableLongStateOf(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) }
    var endMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endMillis)

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { startMillis = it }
                    showStartDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { endMillis = it }
                    showEndDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    fun computeDates(): Triple<Long, Long, String> {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return when (selectedRangeType) {
            ReportDateRangeType.THIS_MONTH -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time)
                Triple(start.timeInMillis, end.timeInMillis, label)
            }
            ReportDateRangeType.LAST_MONTH -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(start.time)
                Triple(start.timeInMillis, end.timeInMillis, label)
            }
            ReportDateRangeType.LAST_30_DAYS -> {
                val end = System.currentTimeMillis()
                val start = end - (30L * 24 * 60 * 60 * 1000)
                val label = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"
                Triple(start, end, label)
            }
            ReportDateRangeType.THIS_YEAR -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val label = "Year ${now.get(Calendar.YEAR)}"
                Triple(start.timeInMillis, System.currentTimeMillis(), label)
            }
            ReportDateRangeType.ALL_TIME -> {
                Triple(0L, Long.MAX_VALUE, "All Time Records")
            }
            ReportDateRangeType.CUSTOM -> {
                val label = "${dateFormat.format(Date(startMillis))} - ${dateFormat.format(Date(endMillis))}"
                Triple(startMillis, endMillis + 86399000L, label)
            }
        }
    }

    // Save as local file launcher
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val (s, e, label) = computeDates()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    viewModel.exportCustomRangePdf(output, s, e, label)
                }
                Toast.makeText(context, "PDF Report saved successfully!", Toast.LENGTH_LONG).show()
                onDismiss()
            } catch (ex: Exception) {
                Toast.makeText(context, "Failed to save: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryGreen)
                Text("Generate PDF Report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Date Range Period:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                ReportDateRangeType.values().forEach { rangeType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedRangeType = rangeType }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRangeType == rangeType,
                            onClick = { selectedRangeType = rangeType }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(rangeType.title, fontSize = 14.sp)
                    }
                }

                if (selectedRangeType == ReportDateRangeType.CUSTOM) {
                    val dFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("From: ${dFormat.format(Date(startMillis))}")
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("To: ${dFormat.format(Date(endMillis))}")
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share button
                Button(
                    onClick = {
                        val (s, e, label) = computeDates()
                        val fileName = "Report_${System.currentTimeMillis()}.pdf"
                        ReportShareHelper.sharePdfReport(context, fileName) { outStream ->
                            viewModel.exportCustomRangePdf(outStream, s, e, label)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share / Open", fontWeight = FontWeight.Bold)
                }

                // Save button
                OutlinedButton(
                    onClick = {
                        val (_, _, label) = computeDates()
                        val cleanName = "Expense_Report_${System.currentTimeMillis()}.pdf"
                        savePdfLauncher.launch(cleanName)
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
