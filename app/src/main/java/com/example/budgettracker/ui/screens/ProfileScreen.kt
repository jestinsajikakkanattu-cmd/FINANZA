package com.example.budgettracker.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgettracker.ui.AppViewModelProvider
import com.example.budgettracker.ui.BudgetViewModel
import com.example.budgettracker.ui.UserProfile
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.homeUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSecondConfirmDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream))
                        val json = reader.readText()
                        val success = viewModel.importFromJson(json)
                        if (success) {
                            Toast.makeText(context, "Data restored!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid backup file!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
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
            // User Information Section
            SettingsSection(title = "User Information") {
                ProfileItem(label = "Name", value = userProfile.name, icon = Icons.Default.Person)
                ProfileItem(label = "Job Title", value = userProfile.jobTitle, icon = Icons.Default.Work)
                ProfileItem(label = "Location", value = userProfile.location, icon = Icons.Default.LocationOn)
                ProfileItem(label = "Phone", value = userProfile.phone, icon = Icons.Default.Phone)
                ProfileItem(label = "Email", value = userProfile.email, icon = Icons.Default.Email)
                
                Button(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Edit Profile")
                }
            }

            // Data Management Section
            SettingsSection(title = "Data Management") {
                SettingsActionItem(
                    label = "Local Backup (JSON)",
                    icon = Icons.Default.Backup,
                    onClick = {
                        if (uiState.transactionsWithBalance.isNotEmpty()) {
                            val json = viewModel.exportToJson()
                            saveJsonToFinanzaFolder(context, json)
                        } else {
                            Toast.makeText(context, "No data to backup", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsActionItem(
                    label = "Restore Data (JSON)",
                    icon = Icons.Default.Restore,
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }
                )
            }

            // Danger Zone Section
            SettingsSection(title = "Danger Zone", titleColor = Color.Red) {
                SettingsActionItem(
                    label = "Clear All Data",
                    icon = Icons.Default.DeleteForever,
                    contentColor = Color.Red,
                    onClick = { showDeleteDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Info
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color.Gray
            )
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = userProfile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedProfile ->
                viewModel.updateProfile(updatedProfile)
                showEditProfileDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("Are you sure you want to delete all transactions? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    showSecondConfirmDialog = true 
                }) {
                    Text("YES", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showSecondConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSecondConfirmDialog = false },
            title = { Text("Final Confirmation") },
            text = { Text("This will PERMANENTLY erase everything. Are you absolutely sure?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearAllData()
                    showSecondConfirmDialog = false 
                    Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Text("ERASE EVERYTHING", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var job by remember { mutableStateOf(profile.jobTitle) }
    var loc by remember { mutableStateOf(profile.location) }
    var phone by remember { mutableStateOf(profile.phone) }
    var email by remember { mutableStateOf(profile.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = job, onValueChange = { job = it }, label = { Text("Job Title") })
                OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(UserProfile(name, job, loc, phone, email)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun saveJsonToFinanzaFolder(context: android.content.Context, json: String) {
    val fileName = "Finanza_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
    
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
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
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            Toast.makeText(context, "Backup saved to Documents/Finanza", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = titleColor,
            modifier = Modifier.padding(start = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SettingsActionItem(
    label: String,
    value: String? = null,
    icon: ImageVector,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                if (value != null) {
                    Text(text = value, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
