package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CurrencyType
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromCurrencyType(value: CurrencyType): String {
        return value.name
    }

    @TypeConverter
    fun toCurrencyType(value: String): CurrencyType {
        return CurrencyType.valueOf(value)
    }
}
