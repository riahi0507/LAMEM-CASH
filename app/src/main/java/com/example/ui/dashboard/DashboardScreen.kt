package com.example.ui.dashboard

import android.content.Intent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.Folder
import com.example.data.model.Transaction
import com.example.ui.theme.ActionEmeraldGreen
import com.example.ui.theme.ActionCoralRed
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CashViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Int) -> Unit = {},
    onChangePersonClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = viewModel.getExportDataJson()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Sauvegarde exportée avec succès !", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Erreur lors de l'exportation", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    viewModel.importDataJson(json)
                }
                android.widget.Toast.makeText(context, "Sauvegarde restaurée avec succès (transactions, dossiers et informations) !", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Erreur lors de l'importation du fichier JSON", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val cashIn by viewModel.totalCashIn.collectAsStateWithLifecycle()
    val cashOut by viewModel.totalCashOut.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val cacheFolders by viewModel.folders.collectAsStateWithLifecycle()
    val folders = cacheFolders // This receives filtered folders based on Person
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val persons by viewModel.persons.collectAsStateWithLifecycle()
    val selectedPersonId by viewModel.selectedPersonId.collectAsStateWithLifecycle()
    val autoSync by viewModel.autoSync.collectAsStateWithLifecycle()
    val activePerson = persons.find { it.id == selectedPersonId }

    var currentTab by remember { mutableIntStateOf(0) }

    var showAddFolderDialog by remember { mutableStateOf(false) }

    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    )

    if (showAddFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var isSettled by remember { mutableStateOf(false) }
        val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        val endMillis = endDatePickerState.selectedDateMillis ?: System.currentTimeMillis()
        LaunchedEffect(endMillis) {
            val endDateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(endMillis))
            folderName = "Dossier $endDateStr"
        }

        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = startDatePickerState) }
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = endDatePickerState) }
        }

        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("Nouveau Dossier") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Nom du dossier") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showStartDatePicker = true }) {
                        Text("Début: ${formatDate(startDatePickerState.selectedDateMillis ?: System.currentTimeMillis()).split(" - ")[0]}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text("Fin: ${formatDate(endDatePickerState.selectedDateMillis ?: System.currentTimeMillis()).split(" - ")[0]}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isSettled = !isSettled }) {
                        Checkbox(checked = isSettled, onCheckedChange = { isSettled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clôturer / Solder le dossier")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.addFolder(
                                Folder(
                                    name = folderName.trim(),
                                    personId = selectedPersonId ?: 0,
                                    startDateMillis = startDatePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                                    endDateMillis = endDatePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                                    isSettled = isSettled
                                )
                            )
                            showAddFolderDialog = false
                        }
                    }
                ) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) { Text("Annuler") }
            }
        )
    }

    var showEditFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        val sources by viewModel.sources.collectAsStateWithLifecycle()
        var newSource by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Paramètres - Origines") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSource,
                            onValueChange = { newSource = it },
                            label = { Text("Nouvelle Origine") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.addSource(newSource); newSource = "" }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(sources, key = { it }) { src ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(src)
                                IconButton(onClick = { viewModel.removeSource(src) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showEditFolderDialog != null) {
        val folder = showEditFolderDialog!!
        var folderName by remember(folder) { mutableStateOf(folder.name) }
        var isSettled by remember(folder) { mutableStateOf(folder.isSettled) }
        val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = folder.startDateMillis)
        val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = folder.endDateMillis)
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        val endMillis = endDatePickerState.selectedDateMillis ?: folder.endDateMillis
        LaunchedEffect(endMillis) {
            val endDateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(endMillis))
            folderName = "Dossier $endDateStr"
        }

        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = startDatePickerState) }
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = endDatePickerState) }
        }

        AlertDialog(
            onDismissRequest = { showEditFolderDialog = null },
            title = { Text("Modifier Dossier") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Nom du dossier") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { showStartDatePicker = true }) {
                        Text("Début: ${formatDate(startDatePickerState.selectedDateMillis ?: folder.startDateMillis).split(" - ")[0]}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text("Fin: ${formatDate(endDatePickerState.selectedDateMillis ?: folder.endDateMillis).split(" - ")[0]}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isSettled = !isSettled }) {
                        Checkbox(checked = isSettled, onCheckedChange = { isSettled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clôturer / Solder le dossier")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.updateFolder(
                                folder.copy(
                                    name = folderName.trim(),
                                    startDateMillis = startDatePickerState.selectedDateMillis ?: folder.startDateMillis,
                                    endDateMillis = endDatePickerState.selectedDateMillis ?: folder.endDateMillis,
                                    isSettled = isSettled
                                )
                            )
                            showEditFolderDialog = null
                        }
                    }
                ) { Text("Sauvegarder") }
            },
            dismissButton = {
                TextButton(onClick = { showEditFolderDialog = null }) { Text("Annuler") }
            }
        )
    }

    val activeFolder = folders.find { it.id == selectedFolderId }
    
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Supprimer la transaction") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cette transaction ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) { Text("Annuler") }
            }
        )
    }

    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Supprimer le dossier") },
            text = { Text("Êtes-vous sûr de vouloir supprimer '${folderToDelete?.name}' ? Toutes les transactions seront perdues.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderToDelete?.let { viewModel.deleteFolder(it) }
                        folderToDelete = null
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (currentTab != 3) {
            TopAppBar(
                title = { Text(if (activeFolder == null) "Dossiers: ${activePerson?.name ?: ""}" else activeFolder.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (activeFolder != null) {
                        IconButton(onClick = { viewModel.selectFolder(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = { 
                            viewModel.selectPerson(null)
                            onChangePersonClick() 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Changer Personne", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (activeFolder != null) {
                        IconButton(onClick = { showEditFolderDialog = activeFolder }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier Dossier", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        if (!activeFolder.isSettled) {
                            IconButton(onClick = { folderToDelete = activeFolder }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer Dossier", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        IconButton(onClick = {
                            if (transactions.isNotEmpty()) {
                                try {
                                    val pdfFile = PdfExporter.exportTransactionsToPdf(context, transactions, activeFolder, activePerson)
                                    if (pdfFile != null) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Exporter vers Google Drive..."))
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Erreur d'export: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Exporter PDF", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = { showAddFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Nouveau Dossier", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
            } else {
                TopAppBar(
                    title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Historique") },
                    label = { Text("Historique") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres") }
                )
            }
        },
        floatingActionButton = {
            if (activeFolder != null && !activeFolder.isSettled) {
                FloatingActionButton(
                    onClick = onAddTransactionClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            } else if (activeFolder == null) {
                FloatingActionButton(
                    onClick = { showAddFolderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_folder_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Folder")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(brush)) {
            AsyncImage(
                model = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/1000_Algerian_dinar.jpg/640px-1000_Algerian_dinar.jpg",
                contentDescription = "DZD Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.15f
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    0 -> { // Dashboard / Home
                        if (activeFolder == null) {
                            // Historique Global / Folders List
                            if (folders.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Aucun dossier créé.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(folders, key = { it.id }) { folder ->
                                        val folderTransactions = allTransactions.filter { it.folderId == folder.id }
                                        val fCashIn = folderTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
                                        val fCashOut = folderTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
                                        val bal = fCashIn - fCashOut
                                        val dev = folderTransactions.firstOrNull()?.currency?.symbol ?: "DZD"
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                viewModel.selectFolder(folder.id)
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                val formattedStartDate = formatDate(folder.startDateMillis).split(" - ")[0]
                                                val formattedEndDate = formatDate(folder.endDateMillis).split(" - ")[0]
                                                val folderStatus = if (folder.isSettled) "soldé" else "en cours"
                                                val titleText = "${folder.name} ($formattedStartDate -- $formattedEndDate) - $folderStatus"
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(titleText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Solde: ${formatCurrency(bal)} $dev", style = MaterialTheme.typography.bodyLarge, color = if (bal >= 0) ActionEmeraldGreen else ActionCoralRed)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val currencySymbol = transactions.firstOrNull()?.currency?.symbol ?: "DZD"
                            BalanceCard(balance = balance, cashIn = cashIn, cashOut = cashOut, currency = currencySymbol)

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transactions Récentes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { currentTab = 1 }) { Text("Voir Tout") }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (transactions.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Aucune transaction.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(transactions.take(5), key = { it.id }) { transaction ->
                                        TransactionItem(
                                            transaction = transaction,
                                            onClick = { if (!activeFolder.isSettled) onEditTransactionClick(transaction.id) },
                                            onDelete = { if (!activeFolder.isSettled) transactionToDelete = transaction }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // History
                        var searchQuery by remember { mutableStateOf("") }
                        var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Historique Complet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Rechercher (titre, origine)") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Effacer")
                                        }
                                    }
                                }
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedTypeFilter == null,
                                    onClick = { selectedTypeFilter = null },
                                    label = { Text("Tout") }
                                )
                                FilterChip(
                                    selected = selectedTypeFilter == TransactionType.CASH_IN,
                                    onClick = { selectedTypeFilter = TransactionType.CASH_IN },
                                    label = { Text("Revenus") }
                                )
                                FilterChip(
                                    selected = selectedTypeFilter == TransactionType.CASH_OUT,
                                    onClick = { selectedTypeFilter = TransactionType.CASH_OUT },
                                    label = { Text("Dépenses") }
                                )
                            }

                            val historyTransactions = if (activeFolder != null) transactions else allTransactions.filter { it.folderId in folders.map { f -> f.id } }
                            val filteredTransactions = historyTransactions.filter { 
                                val matchesSearch = it.missionObject.contains(searchQuery, ignoreCase = true) || it.source.contains(searchQuery, ignoreCase = true)
                                val matchesType = selectedTypeFilter == null || it.type == selectedTypeFilter
                                matchesSearch && matchesType
                            }.sortedByDescending { it.dateMillis }

                            if (filteredTransactions.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Aucune transaction trouvée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredTransactions, key = { it.id }) { transaction ->
                                        val isSettled = activeFolder?.isSettled ?: folders.find { it.id == transaction.folderId }?.isSettled ?: false
                                        TransactionItem(
                                            transaction = transaction,
                                            onClick = { if (!isSettled) onEditTransactionClick(transaction.id) },
                                            onDelete = { if (!isSettled) transactionToDelete = transaction }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Analytics
                        val historyTransactions = if (activeFolder != null) transactions else allTransactions.filter { it.folderId in folders.map { f -> f.id } }
                        val currencySymbol = transactions.firstOrNull()?.currency?.symbol ?: "DZD"
                        AnalyticsTab(transactions = historyTransactions, currency = currencySymbol)
                    }

                    3 -> { // Settings
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Général", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                ListItem(
                                    headlineContent = { Text("Gérer les Catégories (Origines)") },
                                    leadingContent = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                                    modifier = Modifier.clickable { showSettingsDialog = true }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Sauvegarde & Restauration", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                
                                
                                ListItem(
                                    headlineContent = { Text("Synchronisation Auto (10 min)") },
                                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                    trailingContent = {
                                        Switch(checked = autoSync, onCheckedChange = { viewModel.setAutoSync(it) })
                                    },
                                    modifier = Modifier.clickable { viewModel.setAutoSync(!autoSync) }
                                )

                                ListItem(
                                    headlineContent = { Text("Exporter Sauvegarde (JSON)") },
                                    supportingContent = { Text("Exporter toutes les transactions, dossiers et données") },
                                    leadingContent = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        try {
                                            exportLauncher.launch("lamemcash_backup_${System.currentTimeMillis()}.json")
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Erreur", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                ListItem(
                                    headlineContent = { Text("Sélectionner Fichier JSON (Restaurer)") },
                                    supportingContent = { Text("Importer un fichier JSON pour restaurer toutes les données") },
                                    leadingContent = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        try {
                                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Erreur", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
    }
    }
}

@Composable
fun BalanceCard(balance: Double, cashIn: Double, cashOut: Double, currency: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Solde Total",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatCurrency(balance)} $currency",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CashStat(label = "Cash IN", amount = cashIn, color = ActionEmeraldGreen, icon = Icons.Rounded.ArrowUpward, currency = currency)
                CashStat(label = "Cash OUT", amount = cashOut, color = ActionCoralRed, icon = Icons.Rounded.ArrowDownward, currency = currency)
            }
        }
    }
}

@Composable
fun CashStat(label: String, amount: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, currency: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatCurrency(amount)} $currency",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    val isCashIn = transaction.type == TransactionType.CASH_IN
    val color = if (isCashIn) ActionEmeraldGreen else ActionCoralRed
    val icon = if (isCashIn) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val sign = if (isCashIn) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.missionObject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (transaction.source.isNotBlank()) {
                    Text(
                        text = "Origine: ${transaction.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (transaction.photoUris.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo jointe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(transaction.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatCurrency(transaction.amount)} ${transaction.currency.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = java.text.DecimalFormat("#,##0.00")
    return format.format(amount)
}

fun formatDate(millis: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.FRANCE)
    return format.format(Date(millis))
}
