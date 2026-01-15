package com.example.budgettracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.TransactionCategory
import com.example.budgettracker.ui.AppViewModelProvider
import com.example.budgettracker.ui.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownScreen(
    category: String,
    navigateBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.homeUiState.collectAsState()
    
    val categoryDisplayName = if (category == "OTHER") "Other" else TransactionCategory.fromString(category).displayName

    val transactions = uiState.transactionsWithBalance
        .map { it.transaction }
        .filter { 
            if (category == "OTHER") {
                // If it's Other, we need to find transactions that aren't in the top 5
                // This is a bit tricky with current state, so for now we'll match by name
                // To be robust, let's just use the category string passed from Analytics
                it.category == category
            } else {
                it.category == category 
            }
        }

    val groupedByNote = transactions.groupBy { it.note.ifBlank { "Uncategorized" } }
        .mapValues { entry -> 
            val total = entry.value.sumOf { it.amount }
            val count = entry.value.size
            total to count
        }
        .toList()
        .sortedByDescending { it.second.first }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryDisplayName) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Breakdown by Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (groupedByNote.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data for this category")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groupedByNote) { (note, stats) ->
                        NoteBreakdownItem(note = note, totalAmount = stats.first, count = stats.second)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteBreakdownItem(note: String, totalAmount: Double, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = "$count transaction(s)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = "₹${String.format("%.2f", totalAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
