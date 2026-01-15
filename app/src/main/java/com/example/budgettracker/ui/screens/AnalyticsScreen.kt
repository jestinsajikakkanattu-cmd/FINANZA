package com.example.budgettracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.Transaction
import com.example.budgettracker.data.TransactionCategory
import com.example.budgettracker.data.TransactionType
import com.example.budgettracker.ui.AppViewModelProvider
import com.example.budgettracker.ui.BudgetViewModel

enum class AnalyticsToggle {
    EXPENSE_BREAKDOWN, INCOME_VS_EXPENSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onCategoryClick: (String) -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.homeUiState.collectAsState()
    var selectedToggle by remember { mutableStateOf(AnalyticsToggle.EXPENSE_BREAKDOWN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Analytics") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedToggle.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedToggle == AnalyticsToggle.EXPENSE_BREAKDOWN,
                    onClick = { selectedToggle = AnalyticsToggle.EXPENSE_BREAKDOWN },
                    text = { Text("Expense Breakdown", fontSize = 14.sp) }
                )
                Tab(
                    selected = selectedToggle == AnalyticsToggle.INCOME_VS_EXPENSE,
                    onClick = { selectedToggle = AnalyticsToggle.INCOME_VS_EXPENSE },
                    text = { Text("Income vs Expense", fontSize = 14.sp) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedToggle == AnalyticsToggle.EXPENSE_BREAKDOWN) {
                    val expenseTransactions = uiState.transactionsWithBalance
                        .map { it.transaction }
                        .filter { it.type == TransactionType.EXPENSE }
                    
                    if (expenseTransactions.isEmpty()) {
                        Text("No expense data", color = Color.Gray)
                    } else {
                        ExpenseDonutChart(expenseTransactions)
                    }
                } else {
                    if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0) {
                        Text("No data available", color = Color.Gray)
                    } else {
                        IncomeVsExpenseChart(uiState.totalIncome, uiState.totalExpense)
                    }
                }
            }

            if (selectedToggle == AnalyticsToggle.INCOME_VS_EXPENSE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem("Income (₹${String.format("%.0f", uiState.totalIncome)})", Color(0xFF4CAF50))
                    LegendItem("Expense (₹${String.format("%.0f", uiState.totalExpense)})", Color(0xFFE57373))
                }
            }

            Text(
                "Expense Categories (Tap to View Details)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            CategoryListBreakdown(
                transactions = uiState.transactionsWithBalance.map { it.transaction },
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
fun ExpenseDonutChart(transactions: List<Transaction>) {
    val categoryTotals = transactions.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
    
    val total = categoryTotals.values.sum()

    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 40.dp.toPx()
        var currentAngle = -90f
        
        categoryTotals.entries.forEach { entry ->
            val sweepAngle = (entry.value / total * 360f).toFloat()
            val category = TransactionCategory.fromString(entry.key)
            drawArc(
                color = category.color,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            currentAngle += sweepAngle
        }
    }
}

@Composable
fun IncomeVsExpenseChart(income: Double, expense: Double) {
    val total = income + expense
    val incomeAngle = (income / total * 360f).toFloat()
    val expenseAngle = (expense / total * 360f).toFloat()

    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 40.dp.toPx()
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = incomeAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = Color(0xFFE57373),
            startAngle = -90f + incomeAngle,
            sweepAngle = expenseAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun CategoryListBreakdown(
    transactions: List<Transaction>,
    onCategoryClick: (String) -> Unit
) {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val allCategoryExpenses = expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }

    if (allCategoryExpenses.isEmpty()) {
        Text("No expenses found", color = Color.Gray, fontSize = 14.sp)
    } else {
        val top5 = allCategoryExpenses.take(5)
        val rest = allCategoryExpenses.drop(5)
        
        val displayList = top5.toMutableList()
        if (rest.isNotEmpty()) {
            displayList.add("OTHER" to rest.sumOf { it.second })
        }

        val maxVal = displayList.maxOf { it.second }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            displayList.forEach { (catKey, amount) ->
                val category = if (catKey == "OTHER") TransactionCategory.OTHER else TransactionCategory.fromString(catKey)
                
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(catKey) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(category.color, MaterialTheme.shapes.extraSmall))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text("₹${String.format("%.0f", amount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (maxVal > 0) (amount / maxVal).toFloat() else 0f)
                            .height(8.dp)
                            .background(category.color, MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(16.dp).background(color, MaterialTheme.shapes.extraSmall))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
