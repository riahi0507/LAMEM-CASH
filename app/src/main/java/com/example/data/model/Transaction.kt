package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class TransactionType {
    CASH_IN,
    CASH_OUT
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("folderId")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val type: TransactionType,
    val amount: Double,
    val currency: CurrencyType,
    val source: String,
    val missionObject: String,
    val dateMillis: Long,
    val photoUris: String = ""
)
