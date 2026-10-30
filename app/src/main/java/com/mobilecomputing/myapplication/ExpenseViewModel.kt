package com.mobilecomputing.myapplication

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

val Context.dataStore by preferencesDataStore(name = "expense_prefs")

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val context = application.applicationContext

    private val EXPENSES_KEY = stringPreferencesKey("expenses")
    private val GOALS_KEY = stringPreferencesKey("goals")
    private val ACHIEVEMENTS_KEY = stringPreferencesKey("achievements")

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val goals: StateFlow<List<SavingsGoal>> = _goals.asStateFlow()

    private val _achievements = MutableStateFlow(getDefaultAchievements())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val expensesJson = prefs[EXPENSES_KEY] ?: "[]"
                val goalsJson = prefs[GOALS_KEY] ?: "[]"
                val achievementsJson = prefs[ACHIEVEMENTS_KEY] ?: "[]"

                val expenseType = object : TypeToken<List<ExpenseData>>() {}.type
                val goalType = object : TypeToken<List<GoalData>>() {}.type
                val achievementType = object : TypeToken<List<AchievementData>>() {}.type

                try {
                    val expenseDataList: List<ExpenseData> = gson.fromJson(expensesJson, expenseType) ?: emptyList()
                    _expenses.value = expenseDataList.map { it.toExpense() }

                    val goalDataList: List<GoalData> = gson.fromJson(goalsJson, goalType) ?: emptyList()
                    _goals.value = goalDataList.map { it.toGoal() }

                    val achievementDataList: List<AchievementData> = gson.fromJson(achievementsJson, achievementType) ?: emptyList()
                    if (achievementDataList.isNotEmpty()) {
                        _achievements.value = achievementDataList.map { it.toAchievement() }
                    }
                } catch (e: Exception) {
                    // Start fresh if data is corrupted
                }
            }
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            val updated = _expenses.value + expense
            _expenses.value = updated
            saveExpenses(updated)
            checkAchievements(updated)
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            val updated = _expenses.value.filter { it.id != id }
            _expenses.value = updated
            saveExpenses(updated)
        }
    }

    fun addGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            val updated = _goals.value + goal
            _goals.value = updated
            saveGoals(updated)
        }
    }

    fun updateGoalProgress(goalId: String, amount: Double) {
        viewModelScope.launch {
            val updated = _goals.value.map {
                if (it.id == goalId) it.copy(currentAmount = it.currentAmount + amount) else it
            }
            _goals.value = updated
            saveGoals(updated)

            // Check goal achievement
            if (updated.any { it.id == goalId && it.isCompleted }) {
                unlockAchievement("goal_complete")
            }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            val updated = _goals.value.filter { it.id != id }
            _goals.value = updated
            saveGoals(updated)
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Stats
    val totalExpenseThisMonth: Double get() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        return _expenses.value.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            it.type == ExpenseType.EXPENSE &&
            c.get(Calendar.MONTH) == month &&
            c.get(Calendar.YEAR) == year
        }.sumOf { it.amount }
    }

    val totalIncomeThisMonth: Double get() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        return _expenses.value.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            it.type == ExpenseType.INCOME &&
            c.get(Calendar.MONTH) == month &&
            c.get(Calendar.YEAR) == year
        }.sumOf { it.amount }
    }

    fun expensesByCategory(): Map<ExpenseCategory, Double> {
        return _expenses.value
            .filter { it.type == ExpenseType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    fun recentExpenses(limit: Int = 10): List<Expense> {
        return _expenses.value.sortedByDescending { it.dateMillis }.take(limit)
    }

    private fun checkAchievements(expenses: List<Expense>) {
        if (expenses.isNotEmpty()) unlockAchievement("first_expense")
        if (expenses.size >= 10) unlockAchievement("ten_expenses")
    }

    private fun unlockAchievement(id: String) {
        val updated = _achievements.value.map {
            if (it.id == id) it.copy(isUnlocked = true) else it
        }
        _achievements.value = updated
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[ACHIEVEMENTS_KEY] = gson.toJson(updated.map { it.toData() })
            }
        }
    }

    private suspend fun saveExpenses(expenses: List<Expense>) {
        context.dataStore.edit { prefs ->
            prefs[EXPENSES_KEY] = gson.toJson(expenses.map { it.toData() })
        }
    }

    private suspend fun saveGoals(goals: List<SavingsGoal>) {
        context.dataStore.edit { prefs ->
            prefs[GOALS_KEY] = gson.toJson(goals.map { it.toData() })
        }
    }
}

// --- Serializable Data Classes ---
data class ExpenseData(
    val id: String, val title: String, val amount: Double,
    val category: String, val type: String, val note: String,
    val mood: Int, val dateMillis: Long
) {
    fun toExpense() = Expense(
        id = id, title = title, amount = amount,
        category = ExpenseCategory.valueOf(category),
        type = ExpenseType.valueOf(type),
        note = note, mood = mood, dateMillis = dateMillis
    )
}

data class GoalData(
    val id: String, val title: String, val targetAmount: Double,
    val currentAmount: Double, val emoji: String
) {
    fun toGoal() = SavingsGoal(id, title, targetAmount, currentAmount, emoji)
}

data class AchievementData(
    val id: String, val title: String, val description: String,
    val emoji: String, val isUnlocked: Boolean
) {
    fun toAchievement() = Achievement(id, title, description, emoji, isUnlocked)
}

fun Expense.toData() = ExpenseData(id, title, amount, category.name, type.name, note, mood, dateMillis)
fun SavingsGoal.toData() = GoalData(id, title, targetAmount, currentAmount, emoji)
fun Achievement.toData() = AchievementData(id, title, description, emoji, isUnlocked)
