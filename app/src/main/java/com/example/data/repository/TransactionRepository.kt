package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.PersonDao
import com.example.data.local.TransactionDao
import com.example.data.model.Folder
import com.example.data.model.Person
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val folderDao: FolderDao,
    private val personDao: PersonDao,
    private val prefs: android.content.SharedPreferences
) {
    val allPersons: Flow<List<Person>> = personDao.getAllPersons()
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertPerson(person: Person) = personDao.insertPerson(person)
    suspend fun updatePerson(person: Person) = personDao.updatePerson(person)
    suspend fun deletePerson(person: Person) = personDao.deletePerson(person)

    private val SOURCES_KEY = "sources_list"
    
    fun getSources(): List<String> {
        val defaultSources = setOf("Banque", "Client", "Fournisseur", "Personnel", "Autre")
        return prefs.getStringSet(SOURCES_KEY, defaultSources)?.toList()?.sorted() ?: defaultSources.toList().sorted()
    }

    fun addSource(source: String) {
        if (source.isNotBlank()) {
            val current = prefs.getStringSet(SOURCES_KEY, emptySet()) ?: emptySet()
            prefs.edit().putStringSet(SOURCES_KEY, current + source.trim()).apply()
        }
    }

    fun removeSource(source: String) {
        val current = prefs.getStringSet(SOURCES_KEY, emptySet()) ?: emptySet()
        prefs.edit().putStringSet(SOURCES_KEY, current - source).apply()
    }

    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    fun getTransactionsForFolder(folderId: Int): Flow<List<Transaction>> = transactionDao.getTransactionsForFolder(folderId)

    suspend fun getOrCreatePdfExportFolder(): Folder {
        val folderName = "Backup " + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE).format(java.util.Date())
        val folderDaoName = folderDao.getFolderByName(folderName)
        if (folderDaoName != null) return folderDaoName
        
        val newFolder = Folder(
            name = folderName,
            personId = 0,
            startDateMillis = System.currentTimeMillis(),
            endDateMillis = System.currentTimeMillis(),
            isSettled = false
        )
        val id = folderDao.insertFolder(newFolder)
        return newFolder.copy(id = id.toInt())
    }
    suspend fun getTransactionById(id: Int): Transaction? = transactionDao.getTransactionById(id)
    fun getTotalCashIn(folderId: Int): Flow<Double?> = transactionDao.getTotalCashIn(folderId)
    fun getTotalCashOut(folderId: Int): Flow<Double?> = transactionDao.getTotalCashOut(folderId)

    suspend fun updateFolderDatesAndName(folderId: Int) {
        val folder = folderDao.getFolderById(folderId) ?: return
        val person = personDao.getPersonById(folder.personId)
        val txs = transactionDao.getTransactionsListForFolder(folderId)
        
        val startMillis = if (txs.isNotEmpty()) txs.minOf { it.dateMillis } else folder.startDateMillis
        val endMillis = if (txs.isNotEmpty()) txs.maxOf { it.dateMillis } else folder.endDateMillis
        val totalExpenses = txs.filter { it.type == com.example.data.model.TransactionType.CASH_OUT }.sumOf { it.amount }
        val personName = person?.name ?: "Dossier"

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
        val startStr = dateFormat.format(java.util.Date(startMillis))
        val endStr = dateFormat.format(java.util.Date(endMillis))
        val currencySymbol = txs.firstOrNull()?.currency?.symbol ?: "DZD"
        val expensesStr = String.format(java.util.Locale.FRANCE, "%.2f %s", totalExpenses, currencySymbol)

        val newName = "$personName - Dépenses: $expensesStr - Du $startStr au $endStr"

        val updated = folder.copy(
            name = newName,
            startDateMillis = startMillis,
            endDateMillis = endMillis
        )
        folderDao.updateFolder(updated)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        updateFolderDatesAndName(transaction.folderId)
    }
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
        updateFolderDatesAndName(transaction.folderId)
    }
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
        updateFolderDatesAndName(transaction.folderId)
    }
}
