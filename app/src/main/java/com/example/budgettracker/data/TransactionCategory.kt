package com.example.budgettracker.data

import androidx.compose.ui.graphics.Color

enum class TransactionCategory(val displayName: String, val color: Color) {
    FOOD("Food", Color(0xFFFF9800)),          // Orange
    FUEL("Fuel", Color(0xFFF44336)),          // Red
    BILLS("Bills", Color(0xFF2196F3)),         // Blue
    SHOPPING("Shopping", Color(0xFF9C27B0)),      // Purple
    LOAN("Loan", Color(0xFF795548)),          // Brown
    SAVINGS("Savings", Color(0xFF4CAF50)),       // Green
    ENTERTAINMENT("Entertainment", Color(0xFFE91E63)), // Pink
    OTHER("Other", Color(0xFF9E9E9E));         // Gray

    companion object {
        fun fromString(value: String): TransactionCategory {
            return entries.find { it.name == value || it.displayName.equals(value, ignoreCase = true) } ?: OTHER
        }

        // Mapping layer for existing free-text categories
        fun mapFromFreeText(text: String, note: String = ""): TransactionCategory {
            val combined = (text + " " + note).lowercase()
            return when {
                combined.contains("food") || combined.contains("eat") || combined.contains("rest") || combined.contains("grocery") -> FOOD
                combined.contains("fuel") || combined.contains("petrol") || combined.contains("diesel") || combined.contains("gas") -> FUEL
                combined.contains("bill") || combined.contains("recharge") || combined.contains("electricity") || combined.contains("water") -> BILLS
                combined.contains("shop") || combined.contains("buy") || combined.contains("amazon") || combined.contains("flipkart") -> SHOPPING
                combined.contains("loan") || combined.contains("emi") -> LOAN
                combined.contains("save") || combined.contains("invest") || combined.contains("fd") || combined.contains("rd") -> SAVINGS
                combined.contains("movie") || combined.contains("play") || combined.contains("game") || combined.contains("netflix") -> ENTERTAINMENT
                else -> OTHER
            }
        }
    }
}
