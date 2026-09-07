package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis ASC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE folderId = :folderId ORDER BY dateMillis ASC")
    fun getTransactionsForFolder(folderId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE folderId = :folderId ORDER BY dateMillis ASC")
    suspend fun getTransactionsListForFolder(folderId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE folderId = :folderId AND type = 'CASH_IN'")
    fun getTotalCashIn(folderId: Int): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE folderId = :folderId AND type = 'CASH_OUT'")
    fun getTotalCashOut(folderId: Int): Flow<Double?>
}
