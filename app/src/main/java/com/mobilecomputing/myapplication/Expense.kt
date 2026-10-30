package com.mobilecomputing.myapplication

import androidx.compose.ui.graphics.Color
import com.mobilecomputing.myapplication.ui.theme.*

enum class ExpenseType { EXPENSE, INCOME }

enum class ExpenseCategory(
    val displayName: String,
    val emoji: String,
    val color: Color,
    val type: ExpenseType = ExpenseType.EXPENSE
) {
    // Expense Categories
    FOOD("Food & Drinks", "🍔", CategoryFood, ExpenseType.EXPENSE),
    TRANSPORT("Transport", "🚗", CategoryTransport, ExpenseType.EXPENSE),
    SHOPPING("Shopping", "🛍️", CategoryShopping, ExpenseType.EXPENSE),
    HEALTH("Health", "💊", CategoryHealth, ExpenseType.EXPENSE),
    ENTERTAINMENT("Entertainment", "🎮", CategoryEntertainment, ExpenseType.EXPENSE),
    BILLS("Bills & Utilities", "⚡", CategoryBills, ExpenseType.EXPENSE),
    OTHER_EXPENSE("Other Expense", "📦", CategoryOther, ExpenseType.EXPENSE),

    // Income Categories
    SALARY("Salary", "💼", CategorySalary, ExpenseType.INCOME),
    BUSINESS("Business", "🏢", CategoryBusiness, ExpenseType.INCOME),
    FREELANCE("Freelance", "💻", CategoryFreelance, ExpenseType.INCOME),
    INVESTMENT("Investment", "📈", CategoryInvestment, ExpenseType.INCOME),
    GIFT("Gift", "🎁", CategoryGift, ExpenseType.INCOME),
    OTHER_INCOME("Other Income", "💰", CategoryOther, ExpenseType.INCOME);

    companion object {
        fun getCategoriesForType(type: ExpenseType): List<ExpenseCategory> {
            return entries.filter { it.type == type }
        }
    }
}

data class Expense(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: ExpenseCategory,
    val type: ExpenseType = ExpenseType.EXPENSE,
    val note: String = "",
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
