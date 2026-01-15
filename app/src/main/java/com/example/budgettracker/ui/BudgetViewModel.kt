package com.example.budgettracker.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.data.Transaction
import com.example.budgettracker.data.TransactionRepository
import com.example.budgettracker.data.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class UserProfile(
    val name: String = "Guest User",
    val jobTitle: String = "Not Set",
    val location: String = "Not Set",
    val phone: String = "Not Set",
    val email: String = "Not Set"
)

class BudgetViewModel(
    private val repository: TransactionRepository,
    private val context: Context
) : ViewModel() {

    // User Profile Keys
    private val NAME = stringPreferencesKey("user_name")
    private val JOB = stringPreferencesKey("user_job")
    private val LOCATION = stringPreferencesKey("user_location")
    private val PHONE = stringPreferencesKey("user_phone")
    private val EMAIL = stringPreferencesKey("user_email")

    val userProfile: StateFlow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[NAME] ?: "Guest User",
            jobTitle = prefs[JOB] ?: "Not Set",
            location = prefs[LOCATION] ?: "Not Set",
            phone = prefs[PHONE] ?: "Not Set",
            email = prefs[EMAIL] ?: "Not Set"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[NAME] = profile.name
                prefs[JOB] = profile.jobTitle
                prefs[LOCATION] = profile.location
                prefs[PHONE] = profile.phone
                prefs[EMAIL] = profile.email
            }
        }
    }

    val homeUiState: StateFlow<HomeUiState> =
        repository.allTransactions.map { transactions ->
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            var currentBalance = 0.0
            val transactionsWithBalance = transactions.map {
                currentBalance += if (it.type == TransactionType.INCOME) it.amount else -it.amount
                TransactionWithBalance(it, currentBalance)
            }

            HomeUiState(
                transactionsWithBalance = transactionsWithBalance,
                totalIncome = income,
                totalExpense = expense
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun getTransaction(id: Int): Flow<Transaction> {
        return repository.getTransaction(id)
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun exportToJson(): String {
        val transactions = homeUiState.value.transactionsWithBalance.map { it.transaction }
        return Gson().toJson(transactions)
    }

    suspend fun importFromJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = Gson().fromJson(json, type)
            
            // Validation: Ensure the data is actually what we expect
            // If the JSON was completely wrong, Gson might return null or an empty list
            // We should check if the items have mandatory fields like category or type
            if (transactions != null && (transactions.isEmpty() || transactions.firstOrNull()?.category != null)) {
                // Only clear if we have valid-looking data
                repository.deleteAll()
                transactions.forEach {
                    repository.insert(it.copy(id = 0))
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
