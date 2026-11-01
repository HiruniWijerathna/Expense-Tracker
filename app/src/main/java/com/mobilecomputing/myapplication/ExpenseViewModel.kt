package com.mobilecomputing.myapplication

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

val Context.dataStore by preferencesDataStore(name = "expense_prefs")

enum class AppCurrency(val code: String, val symbol: String, val displayName: String) {
    LKR("LKR", "Rs.", "Sri Lankan Rupee (LKR)"),
    USD("USD", "$", "US Dollar ($)"),
    EUR("EUR", "€", "Euro (€)"),
    GBP("GBP", "£", "British Pound (£)"),
    INR("INR", "₹", "Indian Rupee (₹)"),
    AED("AED", "AED", "UAE Dirham (AED)")
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val context = application.applicationContext

    private val EXPENSES_KEY = stringPreferencesKey("expenses")
    private val GOALS_KEY = stringPreferencesKey("goals")
    private val ACHIEVEMENTS_KEY = stringPreferencesKey("achievements")
    private val CATEGORIES_KEY = stringPreferencesKey("custom_categories")
    private val MONTHLY_BUDGET_KEY = doublePreferencesKey("monthly_budget")
    private val CURRENCY_KEY = stringPreferencesKey("app_currency")
    private val APP_PIN_KEY = stringPreferencesKey("app_pin")
    private val PIN_ENABLED_KEY = booleanPreferencesKey("pin_enabled")

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(defaultCategories)
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _goals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val goals: StateFlow<List<SavingsGoal>> = _goals.asStateFlow()

    private val _achievements = MutableStateFlow(getDefaultAchievements())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(50000.0) // default 50k
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _currency = MutableStateFlow(AppCurrency.LKR)
    val currency: StateFlow<AppCurrency> = _currency.asStateFlow()

    private val _appPin = MutableStateFlow("")
    val appPin: StateFlow<String> = _appPin.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

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
                val categoriesJson = prefs[CATEGORIES_KEY] ?: "[]"

                _monthlyBudget.value = prefs[MONTHLY_BUDGET_KEY] ?: 50000.0
                val currCode = prefs[CURRENCY_KEY] ?: AppCurrency.LKR.name
                _currency.value = AppCurrency.values().find { it.name == currCode } ?: AppCurrency.LKR
                val savedPin = prefs[APP_PIN_KEY] ?: ""
                val pinEnabled = prefs[PIN_ENABLED_KEY] ?: false
                _appPin.value = savedPin
                _isPinEnabled.value = pinEnabled
                // Only mark as unlocked if PIN lock is explicitly disabled
                if (!pinEnabled) {
                    _isAppUnlocked.value = true
                }

                val expenseType = object : TypeToken<List<ExpenseData>>() {}.type
                val goalType = object : TypeToken<List<GoalData>>() {}.type
                val achievementType = object : TypeToken<List<AchievementData>>() {}.type
                val categoryType = object : TypeToken<List<CategoryData>>() {}.type

                try {
                    val catList: List<CategoryData> = gson.fromJson(categoriesJson, categoryType) ?: emptyList()
                    if (catList.isNotEmpty()) {
                        val loadedCats = catList.map { it.toCategoryItem() }
                        // Merge default with custom
                        val merged = defaultCategories.toMutableList()
                        loadedCats.forEach { loaded ->
                            if (merged.none { it.id == loaded.id }) {
                                merged.add(loaded)
                            }
                        }
                        _categories.value = merged
                    } else {
                        _categories.value = defaultCategories
                    }

                    val expenseDataList: List<ExpenseData> = gson.fromJson(expensesJson, expenseType) ?: emptyList()
                    _expenses.value = expenseDataList.map { it.toExpense(_categories.value) }

                    val goalDataList: List<GoalData> = gson.fromJson(goalsJson, goalType) ?: emptyList()
                    _goals.value = goalDataList.map { it.toGoal() }

                    val achievementDataList: List<AchievementData> = gson.fromJson(achievementsJson, achievementType) ?: emptyList()
                    if (achievementDataList.isNotEmpty()) {
                        _achievements.value = achievementDataList.map { it.toAchievement() }
                    }
                } catch (e: Exception) {
                    // fallback
                }
            }
        }
    }

    fun unlockApp(pin: String): Boolean {
        if (!_isPinEnabled.value || pin == _appPin.value) {
            _isAppUnlocked.value = true
            return true
        }
        return false
    }

    fun lockApp() {
        if (_isPinEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setPin(pin: String, enabled: Boolean) {
        viewModelScope.launch {
            _appPin.value = pin
            _isPinEnabled.value = enabled
            if (!enabled) _isAppUnlocked.value = true
            context.dataStore.edit { prefs ->
                prefs[APP_PIN_KEY] = pin
                prefs[PIN_ENABLED_KEY] = enabled
            }
        }
    }

    fun setMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            _monthlyBudget.value = budget
            context.dataStore.edit { prefs ->
                prefs[MONTHLY_BUDGET_KEY] = budget
            }
        }
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            _currency.value = currency
            context.dataStore.edit { prefs ->
                prefs[CURRENCY_KEY] = currency.name
            }
        }
    }

    fun addCustomCategory(name: String, emoji: String, colorHex: Long, type: ExpenseType) {
        viewModelScope.launch {
            val newCat = CategoryItem(
                id = "custom_" + UUID.randomUUID().toString(),
                name = name,
                emoji = emoji,
                colorHex = colorHex,
                type = type,
                isCustom = true
            )
            val updated = _categories.value + newCat
            _categories.value = updated
            saveCategories(updated.filter { it.isCustom })
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val updated = _categories.value.filter { it.id != id }
            _categories.value = updated
            saveCategories(updated.filter { it.isCustom })
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

    fun updateExpense(updatedExpense: Expense) {
        viewModelScope.launch {
            val updated = _expenses.value.map {
                if (it.id == updatedExpense.id) updatedExpense else it
            }
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

    // Stats calculations
    val totalExpenseAllTime: Double get() {
        return _expenses.value.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
    }

    val totalIncomeAllTime: Double get() {
        return _expenses.value.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
    }

    val totalBalanceAllTime: Double get() {
        return totalIncomeAllTime - totalExpenseAllTime
    }

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

    val remainingBudget: Double get() {
        return (_monthlyBudget.value - totalExpenseThisMonth).coerceAtLeast(0.0)
    }

    fun expensesByCategory(): Map<String, Double> {
        return _expenses.value
            .filter { it.type == ExpenseType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    fun recentExpenses(limit: Int = 10): List<Expense> {
        return _expenses.value.sortedByDescending { it.dateMillis }.take(limit)
    }

    fun exportToCsv(outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        writer.append("ID,Date,Title,Type,Category,Wallet,Amount,Currency,Mood,Note\n")
        _expenses.value.sortedByDescending { it.dateMillis }.forEach { exp ->
            val dateStr = dateFormat.format(Date(exp.dateMillis))
            val cleanTitle = exp.title.replace(",", " ")
            val cleanNote = exp.note.replace(",", " ")
            writer.append("${exp.id},$dateStr,$cleanTitle,${exp.type.name},${exp.categoryName},${exp.wallet.name},${exp.amount},${_currency.value.code},${exp.mood},$cleanNote\n")
        }
        writer.flush()
        writer.close()
    }

    fun exportCustomRangePdf(
        outputStream: OutputStream,
        startDateMillis: Long,
        endDateMillis: Long,
        dateRangeLabel: String
    ) {
        val filtered = _expenses.value.filter {
            it.dateMillis in startDateMillis..endDateMillis
        }.sortedByDescending { it.dateMillis }

        val totalInc = filtered.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
        val totalExp = filtered.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }

        val catMap = filtered
            .filter { it.type == ExpenseType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        PdfReportGenerator.generateCustomReportPdf(
            outputStream = outputStream,
            currencySymbol = _currency.value.symbol,
            currencyCode = _currency.value.code,
            dateRangeTitle = dateRangeLabel,
            totalIncome = totalInc,
            totalExpense = totalExp,
            categoryBreakdown = catMap,
            expenses = filtered
        )
    }

    fun exportMonthlyReportPdf(outputStream: OutputStream) {
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

        val monthExpenses = _expenses.value.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear
        }.sortedByDescending { it.dateMillis }

        val catMap = monthExpenses
            .filter { it.type == ExpenseType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        PdfReportGenerator.generateCustomReportPdf(
            outputStream = outputStream,
            currencySymbol = _currency.value.symbol,
            currencyCode = _currency.value.code,
            dateRangeTitle = monthName,
            totalIncome = totalIncomeThisMonth,
            totalExpense = totalExpenseThisMonth,
            categoryBreakdown = catMap,
            expenses = monthExpenses
        )
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

    private suspend fun saveCategories(categories: List<CategoryItem>) {
        context.dataStore.edit { prefs ->
            prefs[CATEGORIES_KEY] = gson.toJson(categories.map { it.toData() })
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
data class CategoryData(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val type: String,
    val isCustom: Boolean
) {
    fun toCategoryItem() = CategoryItem(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = colorHex,
        type = try { ExpenseType.valueOf(type) } catch (e: Exception) { ExpenseType.EXPENSE },
        isCustom = isCustom
    )
}

data class ExpenseData(
    val id: String,
    val title: String,
    val amount: Double,
    val categoryId: String? = null,
    val category: String? = null, // fallback for legacy
    val categoryName: String? = null,
    val categoryEmoji: String? = null,
    val categoryColorHex: Long? = null,
    val type: String,
    val wallet: String? = null,
    val note: String,
    val receiptImagePath: String? = null,
    val mood: Int,
    val dateMillis: Long
) {
    fun toExpense(availableCategories: List<CategoryItem>): Expense {
        val parsedType = try { ExpenseType.valueOf(type) } catch (e: Exception) { ExpenseType.EXPENSE }
        val matchedCat = availableCategories.find { it.id == categoryId || it.name.equals(category, true) || it.name.equals(categoryName, true) }
            ?: (if (parsedType == ExpenseType.EXPENSE) defaultCategories[0] else defaultCategories[8])

        val parsedWallet = try {
            wallet?.let { WalletAccount.valueOf(it) } ?: WalletAccount.CASH
        } catch (e: Exception) {
            WalletAccount.CASH
        }

        return Expense(
            id = id,
            title = title,
            amount = amount,
            categoryId = categoryId ?: matchedCat.id,
            categoryName = categoryName ?: matchedCat.name,
            categoryEmoji = categoryEmoji ?: matchedCat.emoji,
            categoryColorHex = categoryColorHex ?: matchedCat.colorHex,
            type = parsedType,
            wallet = parsedWallet,
            note = note,
            receiptImagePath = receiptImagePath,
            mood = mood,
            dateMillis = dateMillis
        )
    }
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

fun CategoryItem.toData() = CategoryData(id, name, emoji, colorHex, type.name, isCustom)
fun Expense.toData() = ExpenseData(
    id = id,
    title = title,
    amount = amount,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryEmoji = categoryEmoji,
    categoryColorHex = categoryColorHex,
    type = type.name,
    wallet = wallet.name,
    note = note,
    receiptImagePath = receiptImagePath,
    mood = mood,
    dateMillis = dateMillis
)
fun SavingsGoal.toData() = GoalData(id, title, targetAmount, currentAmount, emoji)
fun Achievement.toData() = AchievementData(id, title, description, emoji, isUnlocked)
