package com.mobilecomputing.myapplication

import androidx.compose.ui.graphics.Color
import com.mobilecomputing.myapplication.ui.theme.*

enum class ExpenseType { EXPENSE, INCOME }

enum class WalletAccount(val displayName: String, val emoji: String) {
    CASH("Cash Wallet", "💵"),
    BANK("Bank Account", "🏦"),
    CREDIT_CARD("Credit Card", "💳"),
    SAVINGS("Savings", "🐷")
}

data class CategoryItem(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val type: ExpenseType = ExpenseType.EXPENSE,
    val isCustom: Boolean = false
) {
    val color: Color get() = Color(colorHex)
}

val defaultCategories = listOf(
    // Expense
    CategoryItem("cat_food", "Food & Drinks", "🍔", 0xFFFF5722, ExpenseType.EXPENSE),
    CategoryItem("cat_transport", "Transport", "🚗", 0xFF2196F3, ExpenseType.EXPENSE),
    CategoryItem("cat_shopping", "Shopping", "🛍️", 0xFFE91E63, ExpenseType.EXPENSE),
    CategoryItem("cat_health", "Health", "💊", 0xFF4CAF50, ExpenseType.EXPENSE),
    CategoryItem("cat_entertainment", "Entertainment", "🎮", 0xFF9C27B0, ExpenseType.EXPENSE),
    CategoryItem("cat_bills", "Bills & Utilities", "⚡", 0xFFFF9800, ExpenseType.EXPENSE),
    CategoryItem("cat_education", "Education", "📚", 0xFF00BCD4, ExpenseType.EXPENSE),
    CategoryItem("cat_other_exp", "Other Expense", "📦", 0xFF607D8B, ExpenseType.EXPENSE),

    // Income
    CategoryItem("cat_salary", "Salary", "💼", 0xFF00C853, ExpenseType.INCOME),
    CategoryItem("cat_business", "Business", "🏢", 0xFF3F51B5, ExpenseType.INCOME),
    CategoryItem("cat_freelance", "Freelance", "💻", 0xFF00B0FF, ExpenseType.INCOME),
    CategoryItem("cat_investment", "Investment", "📈", 0xFFFFAB00, ExpenseType.INCOME),
    CategoryItem("cat_gift", "Gift", "🎁", 0xFFFF4081, ExpenseType.INCOME),
    CategoryItem("cat_other_inc", "Other Income", "💰", 0xFF607D8B, ExpenseType.INCOME)
)

data class Expense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryColorHex: Long,
    val type: ExpenseType = ExpenseType.EXPENSE,
    val wallet: WalletAccount = WalletAccount.CASH,
    val note: String = "",
    val receiptImagePath: String? = null,
    val mood: Int = 3, // 1=regret, 2=meh, 3=ok, 4=happy, 5=great
    val dateMillis: Long = System.currentTimeMillis()
)

data class SavingsGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val emoji: String = "🎯"
) {
    val progress: Float get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted: Boolean get() = currentAmount >= targetAmount
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean = false
)

fun getDefaultAchievements(): List<Achievement> = listOf(
    Achievement("first_expense", "First Step! 👶", "Added your first expense", "🌱"),
    Achievement("saver_100", "Saver! 💰", "Saved LKR 100 this month", "💰"),
    Achievement("streak_3", "3-Day Streak! 🔥", "Tracked expenses 3 days in a row", "🔥"),
    Achievement("streak_7", "Week Warrior! ⚔️", "Tracked expenses 7 days in a row", "⚔️"),
    Achievement("goal_complete", "Goal Crusher! 🏆", "Completed a savings goal", "🏆"),
    Achievement("no_spend", "No Spend Day! 🧘", "Had a day with zero expenses", "🧘"),
    Achievement("budget_master", "Budget Master! 🎯", "Stayed under budget for a month", "🎯"),
    Achievement("ten_expenses", "Consistent! ✅", "Added 10 expenses total", "✅")
)
