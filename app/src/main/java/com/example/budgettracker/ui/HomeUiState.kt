package com.example.budgettracker.ui

import com.example.budgettracker.data.Transaction

// UI State for the Home Screen
data class HomeUiState(
    val transactionsWithBalance: List<TransactionWithBalance> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

data class TransactionWithBalance(
    val transaction: Transaction,
    val balance: Double
)
