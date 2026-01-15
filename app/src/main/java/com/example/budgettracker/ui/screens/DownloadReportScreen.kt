package com.example.budgettracker.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.data.TransactionType
import com.example.budgettracker.ui.AppViewModelProvider
import com.example.budgettracker.ui.BudgetViewModel
import com.example.budgettracker.ui.TransactionWithBalance
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadReportScreen(
    navigateBack: () -> Unit,
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.homeUiState.collectAsState()
    val context = LocalContext.current

    val months = remember(uiState.transactionsWithBalance) {
        uiState.transactionsWithBalance
            .map { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.transaction.date)) }
            .distinct()
            .reversed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Reports") },
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
        if (months.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available for reports")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(months) { month ->
                    MonthReportItem(
                        month = month,
                        onDownload = {
                            val filteredTransactions = uiState.transactionsWithBalance.filter {
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.transaction.date)) == month
                            }
                            exportMonthlyReport(context, month, filteredTransactions)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthReportItem(month: String, onDownload: () -> Unit) {
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
            Text(text = month, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDownload) {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
            }
        }
    }
}

private fun exportMonthlyReport(context: Context, monthName: String, transactions: List<TransactionWithBalance>) {
    val fileName = "Finanza_${monthName.replace(" ", "_")}_Report.pdf"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Finanza")
        }
    }

    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    val uri = resolver.insert(collection, contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                generateStyledPdf(outputStream, monthName, transactions)
            }
            Toast.makeText(context, "Report saved to Documents/Finanza", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Could not create file", Toast.LENGTH_SHORT).show()
    }
}

private fun generateStyledPdf(outputStream: OutputStream, monthName: String, transactions: List<TransactionWithBalance>) {
    val writer = PdfWriter(outputStream)
    val pdf = PdfDocument(writer)
    val document = Document(pdf)

    // Header
    document.add(Paragraph("FINANZA").setBold().setFontSize(24f).setFontColor(DeviceRgb(0, 102, 204)).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("Monthly Financial Report - $monthName").setBold().setFontSize(16f).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("\n"))

    // Summary Table
    val income = transactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
    val expense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
    
    val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
    summaryTable.addCell(Cell().add(Paragraph("Total Income").setBold()).setBackgroundColor(DeviceRgb(204, 255, 204)))
    summaryTable.addCell(Cell().add(Paragraph("₹${String.format("%.2f", income)}").setBold()).setBackgroundColor(DeviceRgb(204, 255, 204)))
    summaryTable.addCell(Cell().add(Paragraph("Total Expense").setBold()).setBackgroundColor(DeviceRgb(255, 204, 204)))
    summaryTable.addCell(Cell().add(Paragraph("₹${String.format("%.2f", expense)}").setBold()).setBackgroundColor(DeviceRgb(255, 204, 204)))
    summaryTable.addCell(Cell().add(Paragraph("Net Balance").setBold()).setBackgroundColor(DeviceRgb(204, 229, 255)))
    summaryTable.addCell(Cell().add(Paragraph("₹${String.format("%.2f", income - expense)}").setBold()).setBackgroundColor(DeviceRgb(204, 229, 255)))
    
    document.add(summaryTable)
    document.add(Paragraph("\n"))

    // Transactions Table
    val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 25f, 20f, 15f, 20f))).useAllAvailableWidth()
    
    val headerBg = DeviceRgb(0, 102, 204)
    val headerFontColor = DeviceRgb(255, 255, 255)

    table.addHeaderCell(Cell().add(Paragraph("Date").setBold()).setBackgroundColor(headerBg).setFontColor(headerFontColor))
    table.addHeaderCell(Cell().add(Paragraph("Category").setBold()).setBackgroundColor(headerBg).setFontColor(headerFontColor))
    table.addHeaderCell(Cell().add(Paragraph("Note").setBold()).setBackgroundColor(headerBg).setFontColor(headerFontColor))
    table.addHeaderCell(Cell().add(Paragraph("Amount").setBold()).setBackgroundColor(headerBg).setFontColor(headerFontColor))
    table.addHeaderCell(Cell().add(Paragraph("Balance").setBold()).setBackgroundColor(headerBg).setFontColor(headerFontColor))

    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    transactions.forEach { item ->
        table.addCell(dateFormat.format(Date(item.transaction.date)))
        table.addCell(item.transaction.category)
        table.addCell(item.transaction.note)
        val amountText = if (item.transaction.type == TransactionType.INCOME) "+ ₹${item.transaction.amount}" else "- ₹${item.transaction.amount}"
        val amountColor = if (item.transaction.type == TransactionType.INCOME) DeviceRgb(0, 153, 0) else DeviceRgb(204, 0, 0)
        table.addCell(Paragraph(amountText).setFontColor(amountColor))
        table.addCell("₹${String.format("%.2f", item.balance)}")
    }

    document.add(table)
    document.close()
}
