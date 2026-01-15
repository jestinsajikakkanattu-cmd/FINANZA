package com.example.budgettracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.Transaction
import com.example.budgettracker.data.TransactionType
import com.example.budgettracker.ui.AppViewModelProvider
import com.example.budgettracker.ui.BudgetViewModel
import com.example.budgettracker.ui.TransactionWithBalance
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToEntry: () -> Unit,
    navigateToEdit: (Int) -> Unit,
    navigateToReports: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.homeUiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FINANZA") },
                actions = {
                    IconButton(onClick = navigateToReports) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SummaryCard(
                income = uiState.totalIncome,
                expense = uiState.totalExpense
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.transactionsWithBalance.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet. Add one!")
                }
            } else {
                val groupedTransactions = uiState.transactionsWithBalance
                    .groupBy { getDayGroup(it.transaction.date) }

                val sortedDateKeys = groupedTransactions.keys.sortedBy { key ->
                    groupedTransactions[key]?.firstOrNull()?.transaction?.date ?: 0L
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    sortedDateKeys.forEach { dateStr ->
                        val transactions = groupedTransactions[dateStr] ?: emptyList()
                        item {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(transactions, key = { it.transaction.id }) { item ->
                            TransactionItem(
                                transactionWithBalance = item,
                                onDelete = { showDeleteConfirmation = item.transaction },
                                onClick = { navigateToEdit(item.transaction.id) }
                            )
                        }
                    }
                }
            }
        }

        showDeleteConfirmation?.let { transaction ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(transaction)
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double) {
    val balance = income - expense
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Balance", style = MaterialTheme.typography.labelMedium)
            Text(
                text = String.format("₹%.2f", balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) Color(0xFF4CAF50) else Color(0xFFE57373)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Income", style = MaterialTheme.typography.labelSmall)
                    Text(String.format("₹%.2f", income), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Expense", style = MaterialTheme.typography.labelSmall)
                    Text(String.format("₹%.2f", expense), color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transactionWithBalance: TransactionWithBalance,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val transaction = transactionWithBalance.transaction
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (transaction.note.isNotBlank()) {
                    Text(text = transaction.note, style = MaterialTheme.typography.bodySmall)
                }
                Text(text = dateFormat.format(Date(transaction.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("₹%.2f", transaction.amount),
                    color = if (transaction.type == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFE57373),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("Balance: ₹%.2f", transactionWithBalance.balance),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                IconButton(onClick = onDelete, modifier = Modifier.height(24.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

private fun getDayGroup(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis

    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateMillis))
    val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(today.time)
    val yesterdayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(yesterday.time)

    return when (dateStr) {
        todayStr -> "Today"
        yesterdayStr -> "Yesterday"
        else -> dateStr
    }
}
