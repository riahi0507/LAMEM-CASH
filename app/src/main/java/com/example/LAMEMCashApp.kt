package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.TransactionRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class LAMEMCashApp : Application() {
    lateinit var database: AppDatabase
        private set
    
    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "lamemcash_database"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        repository = TransactionRepository(
            database.transactionDao(),
            database.folderDao(),
            database.personDao(),
            getSharedPreferences("lamemcash_prefs", android.content.Context.MODE_PRIVATE)
        )
    }
}
