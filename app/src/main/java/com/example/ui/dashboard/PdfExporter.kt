package com.example.ui.dashboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.data.model.Folder
import com.example.data.model.Person
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun exportTransactionsToPdf(context: Context, transactions: List<Transaction>, folder: Folder, person: Person?): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size 72 PPI
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        var yPosition = 40f
        
        fun drawTitleInfo() {
            // Draw person photo if available
            if (!person?.photoUri.isNullOrBlank()) {
                try {
                    val uri = android.net.Uri.parse(person?.photoUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true)
                        canvas.drawBitmap(scaledBitmap, 505f, yPosition, paint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            paint.textSize = 18f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.color = Color.rgb(79, 70, 229) // Primary Indigo color
            canvas.drawText("LAMEM Cash - Rapport de Dossier", 30f, yPosition + 20f, paint)
            yPosition += 45f

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.color = Color.BLACK
            paint.textSize = 11f
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val startStr = dateFormat.format(Date(folder.startDateMillis))
            val endStr = dateFormat.format(Date(folder.endDateMillis))
            val statusStr = if (folder.isSettled) "SOLDÉ" else "EN COURS"

            if (person != null) {
                canvas.drawText("Personne : ${person.name}", 30f, yPosition, paint)
                yPosition += 18f
            }
            canvas.drawText("Dossier : ${folder.name}", 30f, yPosition, paint)
            yPosition += 18f
            canvas.drawText("Statut : $statusStr", 30f, yPosition, paint)
            yPosition += 18f
            canvas.drawText("Période : du $startStr au $endStr (Dernière transaction)", 30f, yPosition, paint)
            yPosition += 18f
            canvas.drawText("Date d'export : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())}", 30f, yPosition, paint)
            yPosition += 25f
        }

        drawTitleInfo()

        val col1 = 30f   // Date
        val col2 = 110f  // Libellé
        val col3 = 350f  // Débit
        val col4 = 450f  // Crédit
        val rightMargin = 565f
        val rowHeight = 22f

        fun drawTableHeader() {
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(230, 230, 230)
            canvas.drawRect(col1, yPosition, rightMargin, yPosition + rowHeight, paint)
            
            paint.color = Color.BLACK
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("Date", col1 + 5f, yPosition + 15f, paint)
            canvas.drawText("Libellé (Origine / Mission)", col2 + 5f, yPosition + 15f, paint)
            canvas.drawText("Débit (-)", col3 + 5f, yPosition + 15f, paint)
            canvas.drawText("Crédit (+)", col4 + 5f, yPosition + 15f, paint)
            
            paint.color = Color.DKGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(col1, yPosition, rightMargin, yPosition, paint)
            canvas.drawLine(col1, yPosition + rowHeight, rightMargin, yPosition + rowHeight, paint)
            canvas.drawLine(col1, yPosition, col1, yPosition + rowHeight, paint)
            canvas.drawLine(col2, yPosition, col2, yPosition + rowHeight, paint)
            canvas.drawLine(col3, yPosition, col3, yPosition + rowHeight, paint)
            canvas.drawLine(col4, yPosition, col4, yPosition + rowHeight, paint)
            canvas.drawLine(rightMargin, yPosition, rightMargin, yPosition + rowHeight, paint)
            
            yPosition += rowHeight
        }

        drawTableHeader()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        var totalDebit = 0.0
        var totalCredit = 0.0

        transactions.sortedBy { it.dateMillis }.forEach { transaction ->
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
                drawTableHeader()
            }
            
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.color = Color.BLACK
            
            canvas.drawText(dateFormat.format(Date(transaction.dateMillis)), col1 + 5f, yPosition + 15f, paint)
            val desc = "${transaction.source} - ${transaction.missionObject}".take(35)
            canvas.drawText(desc, col2 + 5f, yPosition + 15f, paint)
            
            if (transaction.type == TransactionType.CASH_OUT) {
                totalDebit += transaction.amount
                paint.color = Color.rgb(211, 47, 47) // Red
                canvas.drawText("${transaction.amount} ${transaction.currency.symbol}", col3 + 5f, yPosition + 15f, paint)
            } else {
                totalCredit += transaction.amount
                paint.color = Color.rgb(56, 142, 60) // Green
                canvas.drawText("${transaction.amount} ${transaction.currency.symbol}", col4 + 5f, yPosition + 15f, paint)
            }
            
            paint.color = Color.DKGRAY
            canvas.drawLine(col1, yPosition + rowHeight, rightMargin, yPosition + rowHeight, paint)
            canvas.drawLine(col1, yPosition, col1, yPosition + rowHeight, paint)
            canvas.drawLine(col2, yPosition, col2, yPosition + rowHeight, paint)
            canvas.drawLine(col3, yPosition, col3, yPosition + rowHeight, paint)
            canvas.drawLine(col4, yPosition, col4, yPosition + rowHeight, paint)
            canvas.drawLine(rightMargin, yPosition, rightMargin, yPosition + rowHeight, paint)

            yPosition += rowHeight
        }

        if (yPosition > 750f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }

        // Draw Totals
        yPosition += 10f
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRect(col2, yPosition, rightMargin, yPosition + rowHeight, paint)
        
        paint.color = Color.DKGRAY
        canvas.drawLine(col2, yPosition, rightMargin, yPosition, paint)
        canvas.drawLine(col2, yPosition + rowHeight, rightMargin, yPosition + rowHeight, paint)
        canvas.drawLine(col2, yPosition, col2, yPosition + rowHeight, paint)
        canvas.drawLine(col3, yPosition, col3, yPosition + rowHeight, paint)
        canvas.drawLine(col4, yPosition, col4, yPosition + rowHeight, paint)
        canvas.drawLine(rightMargin, yPosition, rightMargin, yPosition + rowHeight, paint)

        paint.color = Color.BLACK
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("Total", col2 + 5f, yPosition + 15f, paint)
        val dev = transactions.firstOrNull()?.currency?.symbol ?: "DZD"
        
        paint.color = Color.rgb(211, 47, 47)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f %s", totalDebit, dev), col3 + 5f, yPosition + 15f, paint)
        paint.color = Color.rgb(56, 142, 60)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f %s", totalCredit, dev), col4 + 5f, yPosition + 15f, paint)

        yPosition += rowHeight
        
        val solde = totalCredit - totalDebit
        paint.color = Color.rgb(240, 240, 255)
        canvas.drawRect(col2, yPosition, rightMargin, yPosition + rowHeight, paint)
        
        paint.color = Color.DKGRAY
        canvas.drawLine(col2, yPosition + rowHeight, rightMargin, yPosition + rowHeight, paint)
        canvas.drawLine(col2, yPosition, col2, yPosition + rowHeight, paint)
        canvas.drawLine(rightMargin, yPosition, rightMargin, yPosition + rowHeight, paint)

        paint.color = Color.BLACK
        canvas.drawText("Mouvement Net (Solde Actuel)", col2 + 5f, yPosition + 15f, paint)
        if (solde >= 0) {
            paint.color = Color.rgb(56, 142, 60)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f %s", solde, dev), col4 + 5f, yPosition + 15f, paint)
        } else {
            paint.color = Color.rgb(211, 47, 47)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f %s", -solde, dev), col3 + 5f, yPosition + 15f, paint)
        }

        pdfDocument.finishPage(page)

        try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val directory = File(baseDir, "LAMEMCash")
            if (!directory.exists()) directory.mkdirs()
            
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
            val startDateStr = dateFormat.format(Date(folder.startDateMillis))
            val endDateStr = dateFormat.format(Date(folder.endDateMillis))
            val personSafeName = person?.name?.replace(Regex("[^A-Za-z0-9]"), "_") ?: "Dossier"
            val file = File(directory, "LAMEM_SOLDE_${personSafeName}_${startDateStr}_${endDateStr}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
        return null
    }
}
