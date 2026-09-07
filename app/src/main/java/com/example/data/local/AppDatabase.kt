package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Folder
import com.example.data.model.Person
import com.example.data.model.Transaction

@Database(entities = [Transaction::class, Folder::class, Person::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun folderDao(): FolderDao
    abstract fun personDao(): PersonDao
}
