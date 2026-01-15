package com.example.budgettracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.budgettracker.BudgetTrackerApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val application = budgetTrackerApplication()
            BudgetViewModel(application.repository, application.applicationContext)
        }
    }
}

fun CreationExtras.budgetTrackerApplication(): BudgetTrackerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as BudgetTrackerApplication)
