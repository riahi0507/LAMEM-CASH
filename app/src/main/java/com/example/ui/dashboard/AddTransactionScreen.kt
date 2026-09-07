package com.example.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.CameraAlt
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurrencyType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: CashViewModel,
    transactionId: Int? = null,
    onNavigateBack: () -> Unit
) {
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var missionObject by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.CASH_OUT) }
    var selectedCurrency by remember { mutableStateOf(CurrencyType.DZD) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var existingTransactionDateMillis by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existingTransactionDateMillis ?: System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedData by viewModel.parsedPdfData.collectAsStateWithLifecycle()

    LaunchedEffect(parsedData) {
        parsedData.firstOrNull()?.let {
            amount = it.amount?.toString() ?: ""
            missionObject = it.description ?: ""
            existingTransactionDateMillis = it.dateMillis
            viewModel.clearParsedPdfData()
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            val t = viewModel.getTransactionById(transactionId)
            if (t != null) {
                amount = t.amount.toString()
                source = t.source
                missionObject = t.missionObject
                type = t.type
                selectedCurrency = t.currency
                if (t.photoUris.isNotBlank()) photoUri = Uri.parse(t.photoUris)
                existingTransactionDateMillis = t.dateMillis
            }
        }
    }

    var expandedCurrency by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "lamemcash_photo_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                photoUri = Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.filesDir, "lamemcash_photo_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                photoUri = Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var confirmSaveDialog by remember { mutableStateOf(false) }

    if (confirmSaveDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmSaveDialog = false },
            title = { Text(if (transactionId != null) "Modifier Transaction" else "Ajouter Transaction") },
            text = { Text("Êtes-vous sûr de vouloir enregistrer cette transaction ?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val amountValue = evaluateMathExpression(amount)
                        val folderId = selectedFolderId
                        if (amountValue != null && folderId != null) {
                            val newTransaction = Transaction(
                                id = transactionId ?: 0,
                                folderId = folderId,
                                type = type,
                                amount = amountValue,
                                currency = selectedCurrency,
                                source = if (type == TransactionType.CASH_OUT) "" else source.trim(),
                                missionObject = missionObject.trim(),
                                dateMillis = existingTransactionDateMillis ?: System.currentTimeMillis(),
                                photoUris = photoUri?.toString() ?: ""
                            )
                            if (transactionId == null) {
                                viewModel.addTransaction(newTransaction)
                            } else {
                                viewModel.updateTransaction(newTransaction)
                            }
                            confirmSaveDialog = false
                            onNavigateBack()
                        }
                    }
                ) { Text("Oui") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmSaveDialog = false }) { Text("Non") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId != null) "Modifier Transaction" else "Nouvelle Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = type == TransactionType.CASH_IN,
                    onClick = { type = TransactionType.CASH_IN },
                    label = { Text("Cash IN (Entrée)") }
                )
                FilterChip(
                    selected = type == TransactionType.CASH_OUT,
                    onClick = { type = TransactionType.CASH_OUT },
                    label = { Text("Cash OUT (Sortie)") }
                )
            }
            
            // Date Selection
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(existingTransactionDateMillis ?: System.currentTimeMillis())),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date de transaction") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            existingTransactionDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            val evaluatedAmount = evaluateMathExpression(amount)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Montant") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (evaluatedAmount != null && amount.isNotEmpty() && !amount.matches(Regex("^-?\\d+(\\.\\d+)?$"))) {
                        Text(
                            text = "= ${java.text.DecimalFormat("#,##0.00").format(evaluatedAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 4.dp)
                                .clickable { amount = evaluatedAmount.toString() }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedCurrency,
                    onExpandedChange = { expandedCurrency = !expandedCurrency },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCurrency.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Devise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCurrency,
                        onDismissRequest = { expandedCurrency = false }
                    ) {
                        CurrencyType.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.label} (${currency.symbol})") },
                                onClick = {
                                    selectedCurrency = currency
                                    expandedCurrency = false
                                }
                            )
                        }
                    }
                }
            }

            val sources by viewModel.sources.collectAsStateWithLifecycle()
            
            if (type == TransactionType.CASH_IN) {
                var expandedSource by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedSource,
                    onExpandedChange = { expandedSource = !expandedSource },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it },
                        label = { Text("Origine du montant (ex: Banque, Client)") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) }
                    )
                    if (sources.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedSource,
                            onDismissRequest = { expandedSource = false }
                        ) {
                            sources.forEach { src ->
                                DropdownMenuItem(
                                    text = { Text(src) },
                                    onClick = {
                                        source = src
                                        expandedSource = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = missionObject,
                onValueChange = { missionObject = it },
                label = { Text("Mission / Objet") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { 
                        try { filePickerLauncher.launch("image/*") } catch (e: Exception) { e.printStackTrace() }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Importer Image", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importer")
                }
                Button(
                    onClick = { 
                        try { takePictureLauncher.launch(null) } catch (e: Exception) { e.printStackTrace() }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Prendre Photo", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Caméra")
                }
                if (photoUri != null) {
                    Text(
                        text = "1 fichier prêt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val amountValue = evaluateMathExpression(amount)
                    val folderId = selectedFolderId
                    if (amountValue != null && causeIsValid(missionObject, source) && folderId != null) {
                        confirmSaveDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_transaction_button"),
                enabled = evaluateMathExpression(amount) != null && missionObject.isNotBlank() && selectedFolderId != null
            ) {
                Text("Enregistrer")
            }
        }
    }
}

fun causeIsValid(mission: String, src: String): Boolean {
    return mission.isNotBlank()
}
