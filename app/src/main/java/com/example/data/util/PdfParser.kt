package com.example.data.util

import android.content.Context
import android.net.Uri
import com.example.data.model.TransactionType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedPdfData(
    val dateMillis: Long?,
    val amount: Double?,
    val description: String?,
    val type: TransactionType
)

object PdfParser {
    suspend fun extractTextFromPdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parseText(text: String): List<ParsedPdfData> {
        val transactions = mutableListOf<ParsedPdfData>()
        val lines = text.split("\n")
        
        // Simple Regexes for finding date, amount and type in each line
        val dateRegex = Regex("""(\d{2}/\d{2}/\d{4})""")
        val amountRegex = Regex("""(\d+[.,]\d{2})""")
        
        for (line in lines) {
            val dateMatch = dateRegex.find(line)
            val amountMatch = amountRegex.find(line)
            
            if (dateMatch != null && amountMatch != null) {
                val dateMillis = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).parse(dateMatch.groupValues[1])?.time
                } catch (e: Exception) { null }
                
                val amount = amountMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
                // Simple heuristic: if line contains "ENTREE" or "IN" it's CASH_IN, else CASH_OUT
                val type = if (line.contains("IN", ignoreCase = true) || line.contains("ENTREE", ignoreCase = true)) 
                    TransactionType.CASH_IN else TransactionType.CASH_OUT
                
                transactions.add(ParsedPdfData(
                    dateMillis = dateMillis,
                    amount = amount,
                    description = line.take(50), // Generic description from line
                    type = type
                ))
            }
        }
        return transactions
    }
}
