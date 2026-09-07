package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val personId: Int = 0,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val isSettled: Boolean = false
)
