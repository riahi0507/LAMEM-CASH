package com.example.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PdfConverterTab(viewModel: CashViewModel) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val parsedData by viewModel.parsedPdfData.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            viewModel.parsePdf(context, it)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Extraction des données en cours...")
            if (parsedData != null) isLoading = false
        } else {
            Button(onClick = { filePickerLauncher.launch("application/pdf") }) {
                Text("Scanner / Convertir un bilan PDF")
            }
            
            if (parsedData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(parsedData) { parsed ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Référence: ${parsed.description}", style = MaterialTheme.typography.titleMedium)
                                Text("Date: ${parsed.dateMillis}")
                                Text("Montant: ${parsed.amount} - Type: ${parsed.type}")
                            }
                        }
                    }
                }
            }
        }
    }
}
