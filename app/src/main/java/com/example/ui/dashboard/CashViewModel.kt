package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Folder
import com.example.data.model.Transaction
import com.example.data.repository.TransactionRepository
import com.example.data.util.ParsedPdfData
import com.example.data.util.PdfParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri

@OptIn(ExperimentalCoroutinesApi::class)
class CashViewModel(private val repository: TransactionRepository) : ViewModel() {
    
    private val _parsedPdfData = MutableStateFlow<List<ParsedPdfData>>(emptyList())
    val parsedPdfData: StateFlow<List<ParsedPdfData>> = _parsedPdfData.asStateFlow()

    fun parsePdf(context: Context, uri: Uri) = viewModelScope.launch {
        val text = PdfParser.extractTextFromPdf(context, uri)
        val parsedList = PdfParser.parseText(text)
        _parsedPdfData.value = parsedList
        
        // Also create the transactions
        val folder = repository.getOrCreatePdfExportFolder()
        parsedList.forEach { parsed ->
            val transaction = Transaction(
                folderId = folder.id,
                dateMillis = parsed.dateMillis ?: System.currentTimeMillis(),
                amount = parsed.amount ?: 0.0,
                missionObject = parsed.description ?: "Import PDF",
                type = parsed.type,
                source = "",
                currency = com.example.data.model.CurrencyType.DZD, // default currency
                photoUris = uri.toString()
            )
            repository.insertTransaction(transaction)
        }

        val dateMillisList = parsedList.mapNotNull { it.dateMillis }
        if (dateMillisList.isNotEmpty()) {
            val startDate = dateMillisList.minOrNull() ?: System.currentTimeMillis()
            val endDate = dateMillisList.maxOrNull() ?: System.currentTimeMillis()
            val endDateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(endDate))
            val updatedFolder = folder.copy(
                name = "Backup $endDateStr",
                startDateMillis = startDate,
                endDateMillis = endDate
            )
            repository.updateFolder(updatedFolder)
        }
    }

    fun clearParsedPdfData() {
        _parsedPdfData.value = emptyList()
    }

    private val _sources = MutableStateFlow<List<String>>(repository.getSources())
    val sources: StateFlow<List<String>> = _sources.asStateFlow()

    fun addSource(source: String) {
        repository.addSource(source)
        _sources.value = repository.getSources()
    }

    fun removeSource(source: String) {
        repository.removeSource(source)
        _sources.value = repository.getSources()
    }

    val persons: StateFlow<List<com.example.data.model.Person>> = repository.allPersons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedPersonId = MutableStateFlow<Int?>(null)
    val selectedPersonId: StateFlow<Int?> = _selectedPersonId.asStateFlow()

    fun selectPerson(personId: Int?) {
        _selectedPersonId.value = personId
        _selectedFolderId.value = null // reset folder when person changes
    }

    fun addPerson(person: com.example.data.model.Person) = viewModelScope.launch { repository.insertPerson(person) }
    fun updatePerson(person: com.example.data.model.Person) = viewModelScope.launch { repository.updatePerson(person) }
    fun deletePerson(person: com.example.data.model.Person) = viewModelScope.launch {
        repository.deletePerson(person)
        if (_selectedPersonId.value == person.id) _selectedPersonId.value = null
    }

    val folders: StateFlow<List<Folder>> = combine(repository.allFolders, _selectedPersonId) { allFolders, personId ->
        if (personId == null) emptyList() else allFolders.filter { it.personId == personId }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId.asStateFlow()

    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
    }

    val transactions: StateFlow<List<Transaction>> = _selectedFolderId
        .filterNotNull()
        .flatMapLatest { folderId ->
            repository.getTransactionsForFolder(folderId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCashIn: StateFlow<Double> = _selectedFolderId
        .filterNotNull()
        .flatMapLatest { folderId ->
            repository.getTotalCashIn(folderId).map { it ?: 0.0 }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalCashOut: StateFlow<Double> = _selectedFolderId
        .filterNotNull()
        .flatMapLatest { folderId ->
            repository.getTotalCashOut(folderId).map { it ?: 0.0 }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val balance: StateFlow<Double> = combine(totalCashIn, totalCashOut) { cashIn, cashOut ->
        cashIn - cashOut
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun addFolder(folder: Folder) = viewModelScope.launch {
        val id = repository.insertFolder(folder)
        if (_selectedFolderId.value == null) {
            _selectedFolderId.value = id.toInt()
        }
    }

    fun updateFolder(folder: Folder) = viewModelScope.launch {
        repository.updateFolder(folder)
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
        if (_selectedFolderId.value == folder.id) {
            _selectedFolderId.value = null
        }
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return repository.getTransactionById(id)
    }

    private val _autoSync = MutableStateFlow(false)
    val autoSync: StateFlow<Boolean> = _autoSync.asStateFlow()

    fun setAutoSync(enabled: Boolean) {
        _autoSync.value = enabled
    }

    fun addTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
    }

    fun updateTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    suspend fun getExportDataJson(): String {
        val personsList = repository.allPersons.first()
        val foldersList = repository.allFolders.first()
        val transactionsList = repository.allTransactions.first()
        val data = com.example.data.model.AppExportData(personsList, foldersList, transactionsList)
        return com.google.gson.Gson().toJson(data)
    }

    fun importDataJson(jsonString: String) = viewModelScope.launch {
        try {
            val data = com.google.gson.Gson().fromJson(jsonString, com.example.data.model.AppExportData::class.java)
            data?.persons?.forEach { repository.insertPerson(it) }
            data?.folders?.forEach { repository.insertFolder(it) }
            data?.transactions?.forEach { repository.insertTransaction(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class CashViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CashViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
