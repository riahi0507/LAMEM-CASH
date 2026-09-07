package com.example.ui.person

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Person
import com.example.ui.dashboard.CashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonSelectionScreen(
    viewModel: CashViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val persons by viewModel.persons.collectAsStateWithLifecycle()
    val selectedPersonId by viewModel.selectedPersonId.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var selectedPerson by remember(selectedPersonId, persons) { 
        mutableStateOf(persons.find { it.id == selectedPersonId })
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Person?>(null) }
    var personNameInput by remember { mutableStateOf("") }
    var personPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "person_photo_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                personPhotoUri = Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sélection de la Personne") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Veuillez sélectionner la personne concernée par le dossier :", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPerson?.name ?: "Sélectionner une personne",
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        if (selectedPerson?.photoUri?.isNotBlank() == true) {
                            AsyncImage(
                                model = selectedPerson?.photoUri,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    persons.forEach { person ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (person.photoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = person.photoUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(28.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(person.name)
                                }
                            },
                            onClick = {
                                selectedPerson = person
                                expanded = false
                            },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showEditDialog = person }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Person")
                                    }
                                    IconButton(onClick = { viewModel.deletePerson(person) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Person")
                                    }
                                }
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Ajouter une personne...", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showAddDialog = true
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Add Person") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    selectedPerson?.let {
                        viewModel.selectPerson(it.id)
                        onNavigateToDashboard()
                    }
                },
                enabled = selectedPerson != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Accéder au Dashboard")
            }
        }
    }
    
    if (showAddDialog || showEditDialog != null) {
        val isEdit = showEditDialog != null
        LaunchedEffect(isEdit, showEditDialog) {
            if (isEdit) {
                personNameInput = showEditDialog!!.name
                personPhotoUri = if (showEditDialog!!.photoUri.isNotBlank()) Uri.parse(showEditDialog!!.photoUri) else null
            } else {
                personNameInput = ""
                personPhotoUri = null
            }
        }
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                showEditDialog = null
            },
            title = { Text(if (isEdit) "Modifier la personne" else "Ajouter une personne") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (personPhotoUri != null) {
                            AsyncImage(
                                model = personPhotoUri,
                                contentDescription = "Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Choisir une photo")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = personNameInput,
                        onValueChange = { personNameInput = it },
                        label = { Text("Nom de la personne") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (personNameInput.isNotBlank()) {
                        val photoStr = personPhotoUri?.toString() ?: ""
                        if (isEdit) {
                            viewModel.updatePerson(showEditDialog!!.copy(name = personNameInput.trim(), photoUri = photoStr))
                        } else {
                            viewModel.addPerson(Person(name = personNameInput.trim(), photoUri = photoStr))
                        }
                    }
                    showAddDialog = false
                    showEditDialog = null
                }) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    showEditDialog = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}
