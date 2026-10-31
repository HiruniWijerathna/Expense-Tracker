package com.mobilecomputing.myapplication

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    var selectedType by remember { mutableStateOf(ExpenseType.EXPENSE) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = categories.filter { it.type == selectedType }

    if (showAddDialog) {
        CreateCategoryDialog(
            type = selectedType,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji, colorHex ->
                if (name.isBlank()) {
                    Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addCustomCategory(name.trim(), emoji, colorHex, selectedType)
                    showAddDialog = false
                    Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab for Expense / Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                listOf(ExpenseType.EXPENSE to "Expenses", ExpenseType.INCOME to "Income").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected)
                                    (if (type == ExpenseType.EXPENSE) AccentRed else PrimaryGreen)
                                else Color.Transparent
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Text("Categories (${filtered.size})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { cat ->
                    CategoryManageCard(
                        category = cat,
                        onDelete = {
                            if (cat.isCustom) {
                                viewModel.deleteCategory(cat.id)
                                Toast.makeText(context, "Deleted category", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Default category cannot be deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryManageCard(
    category: CategoryItem,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    if (category.isCustom) "Custom" else "Default",
                    fontSize = 11.sp,
                    color = if (category.isCustom) PrimaryGreen else TextSecondary
                )
            }
            if (category.isCustom) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete", tint = AccentRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CreateCategoryDialog(
    type: ExpenseType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, colorHex: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val emojiList = listOf(
        "🍔", "🍕", "🚗", "✈️", "🛍️", "👗", "💊", "🏥", "🎮", "🎬",
        "⚡", "💡", "📚", "🎓", "📦", "🐾", "☕", "🏋️", "💻", "💼",
        "🏢", "📈", "🎁", "💰", "💵", "🏦", "🏠", "🎨", "⚽", "🏖️"
    )
    val colorList = listOf(
        0xFFFF5722, 0xFF2196F3, 0xFFE91E63, 0xFF4CAF50, 0xFF9C27B0,
        0xFFFF9800, 0xFF00BCD4, 0xFF607D8B, 0xFF00C853, 0xFF3F51B5,
        0xFFFFAB00, 0xFFFF4081, 0xFF795548, 0xFF673AB7, 0xFF009688
    )

    var selectedEmoji by remember { mutableStateOf(emojiList[0]) }
    var selectedColor by remember { mutableStateOf(colorList[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New ${if (type == ExpenseType.EXPENSE) "Expense" else "Income"} Category") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick an Icon / Emoji", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                // Emoji grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiList.forEach { emoji ->
                        val isSel = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSel) PrimaryGreen.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (isSel) 2.dp else 0.dp, if (isSel) PrimaryGreen else Color.Transparent, CircleShape)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }

                Text("Pick a Theme Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                // Color grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorList.forEach { colorVal ->
                        val isSel = colorVal == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(if (isSel) 3.dp else 0.dp, if (isSel) Color.White else Color.Transparent, CircleShape)
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, selectedEmoji, selectedColor) }) {
                Text("Create", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
