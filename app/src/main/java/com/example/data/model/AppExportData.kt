package com.example.data.model

data class AppExportData(
    val persons: List<Person> = emptyList(),
    val folders: List<Folder>,
    val transactions: List<Transaction>
)
