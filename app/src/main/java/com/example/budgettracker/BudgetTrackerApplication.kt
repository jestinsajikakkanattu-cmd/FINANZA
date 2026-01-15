package com.example.budgettracker

import android.app.Application
import com.example.budgettracker.data.AppDatabase
import com.example.budgettracker.data.TransactionRepository

class BudgetTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: TransactionRepository by lazy { TransactionRepository(database.transactionDao()) }
}
